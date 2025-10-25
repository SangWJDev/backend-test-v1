package im.bigs.pg.external.pg.client

import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.application.pg.port.out.PgClientOutPort
import im.bigs.pg.domain.payment.PaymentStatus
import im.bigs.pg.external.pg.config.TestPgCredentials
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


@Component
@Order(1)
class TestPgClient(
    private val http: WebClient,
    private val credential: TestPgCredentials,
) : PgClientOutPort {
    override fun supports(partnerId: Long): Boolean = true;

    override fun approve(request: PgApproveRequest): PgApproveResult {
        val response = http.post()
            .uri("/api/v1/pay/credit-card")
            .header("API-KEY", credential.apiKey)
            .bodyValue(mapOf("enc" to request.pgEncToken))
            .retrieve()
            .onStatus({ it.value() == 401 }) {
                Mono.error(RuntimeException("TestPG Unauthorized (401)"))
            }
            .onStatus({ it.value() == 422 }) {
                it.bodyToMono(TestPgError::class.java)
                    .flatMap { err ->
                        Mono.error(RuntimeException("TestPG Error ${err.errorCode}: ${err.message}"))
                    }
            }
            .bodyToMono(TestPgSuccess::class.java)
            .block()!!


        val approvedAtLocal = LocalDateTime.parse(
            response.approvedAt,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
        )

        val mappedStatus = PaymentStatus.from(response.status)

        return PgApproveResult(
            approvalCode = response.approvalCode,
            approvedAt = approvedAtLocal,
            status = mappedStatus
        )
    }

    private data class TestPgSuccess(
        val approvalCode: String,
        val approvedAt: String,
        val status: String
    )

    private data class TestPgError(
        val code: Int,
        val errorCode: String,
        val message: String,
        val referenceId: String?
    )

}