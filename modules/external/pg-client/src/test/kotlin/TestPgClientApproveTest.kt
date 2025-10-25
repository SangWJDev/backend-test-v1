import com.fasterxml.jackson.databind.ObjectMapper
import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.domain.payment.PaymentStatus
import im.bigs.pg.external.pg.client.TestPgClient
import im.bigs.pg.external.pg.config.TestPgCredentials
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestPgClientAdapterContractTest {

    private lateinit var server: MockWebServer
    private lateinit var client: TestPgClient
    private val objectMapper = ObjectMapper()

    private val creds = TestPgCredentials(
        apiKey = "dummy-api-key",
        ivBase64url = "AAAAAAAAAAAAAAAA"
    )

    @BeforeAll
    fun beforeAll() {
        server = MockWebServer()
        server.start()

        val webClient = WebClient.builder()
            .baseUrl(server.url("/").toString().removeSuffix("/"))
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build()

        client = TestPgClient(
            http = webClient,
            credential = creds
        )
    }

    @AfterAll
    fun afterAll() {
        server.shutdown()
    }

    @Test
    @DisplayName("성공(200): 승인 응답을 PgApproveResult로 정확히 매핑한다")
    fun `성공 200 응답 매핑`() {
        // given
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
            {
              "approvalCode": "10080728",
              "approvedAt": "2025-10-08T03:31:34.181568",
              "status": "APPROVED"
            }
            """.trimIndent()
                )
        )

        // when
        val res = client.approve(
            PgApproveRequest(
                partnerId = 1L,
                amount = BigDecimal(10000L),
                productName = "테스트상품",
                cardBin = "111111",
                cardLast4 = "1111",
                pgEncToken = UUID.randomUUID().toString(),
            )
        )

        // then: 결과 필드 매핑
        assertEquals("10080728", res.approvalCode)
        assertEquals(LocalDateTime.of(2025, 10, 8, 3, 31, 34, 181568000), res.approvedAt)
        assertEquals(PaymentStatus.APPROVED, res.status)

        // 그리고 실제로 보낸 요청도 검증
        val recorded = server.takeRequest()
        assertEquals("/api/v1/pay/credit-card", recorded.path, "요청 경로가 맞아야 한다")
        assertEquals(creds.apiKey, recorded.getHeader("API-KEY"), "API-KEY 헤더가 포함되어야 한다")

        val sentJson = objectMapper.readTree(recorded.body.readUtf8())
        assertTrue(sentJson.has("enc"), "요청 본문에 enc 필드가 있어야 한다")
        // enc는 Base64URL 형식이어야 함
        assertTrue(
            sentJson.get("enc").asText().matches(Regex("^[A-Za-z0-9_-]+$")),
            "enc는 Base64URL 형식이어야 한다"
        )
    }

    @Test
    @DisplayName("인증 실패(401): Unauthorized를 예외로 변환한다")
    fun `인증 실패 401`() {
        // given
        server.enqueue(MockResponse().setResponseCode(401))

        // when & then
        val ex = assertThrows<RuntimeException> {
            client.approve(dummyReq())
        }
        assertTrue(ex.message?.contains("Unauthorized") == true, "예외 메시지에 Unauthorized가 포함되어야 한다")

        // 헤더 포함 여부도 확인 (서버는 401로 응답했지만, 요청은 올바르게 구성되어야 함)
        val recorded = server.takeRequest()
        assertEquals(creds.apiKey, recorded.getHeader("API-KEY"))
    }

    @Test
    @DisplayName("승인 거절(422): PG 에러 응답을 예외로 변환한다")
    fun `승인 거절 422`() {
        // given
        server.enqueue(
            MockResponse()
                .setResponseCode(422)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
            {
              "code": 422,
              "errorCode": "INSUFFICIENT_LIMIT",
              "message": "한도 초과",
              "referenceId": "ref_1"
            }
            """.trimIndent()
                )
        )

        // when & then
        val ex = assertThrows<RuntimeException> {
            client.approve(dummyReq())
        }

        assertTrue(ex.message?.contains("INSUFFICIENT_LIMIT") == true, "에러코드가 메시지에 포함되어야 한다")
        assertTrue(ex.message?.contains("한도 초과") == true, "에러 메시지가 포함되어야 한다")
    }

    private fun dummyReq() = PgApproveRequest(
        partnerId = 1L,
        amount = BigDecimal(10000L),
        productName = "테스트상품",
        cardBin = "111111",
        cardLast4 = "1111",
        pgEncToken = UUID.randomUUID().toString(),
    )
}