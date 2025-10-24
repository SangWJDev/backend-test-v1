import com.fasterxml.jackson.databind.ObjectMapper
import im.bigs.pg.external.pg.util.TestPgCrypto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName

import kotlin.test.DefaultAsserter.assertNotEquals

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TestPgCryptoTest {
    private val crypto = TestPgCrypto(ObjectMapper())

    @Test
    @DisplayName("성공: 평문을 AES-256-GCM으로 암호화하면 Base64URL(패딩 없음) 형식으로 반환한다")
    fun `평문 암호화 결과는 Base64URL 형식이어야 한다`() {
        // given
        val plaintext = """{"amount":1000}"""
        val apiKey = "dummy-api-key"
        val iv12Base64Url = "AAAAAAAAAAAAAAAA"

        // when
        val enc = crypto.encryptAesGcmBase64Url(plaintext, apiKey, iv12Base64Url)

        // then
        assertTrue(enc.matches(Regex("^[A-Za-z0-9_-]+$")), "암호문은 Base64URL 문자만 포함해야 한다")
        assertFalse(enc.contains("="), "암호문에는 '=' 패딩이 없어야 한다")
        assertTrue(enc.length > 16, "암호문 길이가 비정상적으로 짧다")
    }

    @Test
    @DisplayName("성공: 같은 키·IV·평문이면 동일한 암호문을 생성한다(결정적이어야 한다)")
    fun `같은 입력이면 같은 암호문을 생성한다`() {
        // given
        val plaintext = """{\"amount\":1000}"""
        val apiKey = "same-key"
        val iv12Base64Url = "AAAAAAAAAAAAAAAA"

        // when
        val enc1 = crypto.encryptAesGcmBase64Url(plaintext, apiKey, iv12Base64Url)
        val enc2 = crypto.encryptAesGcmBase64Url(plaintext, apiKey, iv12Base64Url)

        // then
        assertEquals(enc1, enc2, "같은 키·IV·평문이면 암호문이 동일해야 한다")
    }

    @Test
    @DisplayName("성공: 키 또는 IV가 다르면 암호문이 달라진다")
    fun `다른 입력이면 다른 암호문을 생성한다`() {
        // given
        val plaintext = """{"amount":1000}"""
        val apiKeyA = "key-A"
        val apiKeyB = "key-B"
        val ivA = "AAAAAAAAAAAAAAAA"
        val ivB = "DDDDDDDDDDDDDDDD"

        // when
        val encKeyA = crypto.encryptAesGcmBase64Url(plaintext, apiKeyA, ivA)
        val encKeyB = crypto.encryptAesGcmBase64Url(plaintext, apiKeyB, ivA)
        val encIvB  = crypto.encryptAesGcmBase64Url(plaintext, apiKeyA, ivB)

        // then
        assertNotEquals(encKeyA, encKeyB, "키가 다르면 암호문이 달라져야 한다")
        assertNotEquals(encKeyA, encIvB, "IV가 다르면 암호문이 달라져야 한다")
    }

    @Test
    @DisplayName("실패: IV 길이가 12바이트가 아니면 예외를 던진다")
    fun `IV 길이가 12바이트가 아니면 예외`() {
        // given
        val plaintext = """{"amount":1000}"""
        val apiKey = "dummy"
        val invalidIv = "QUJDREVGRw" // Base64URL 디코드 시 7바이트 정도로 가정(예시)

        // when / then
        val ex = assertThrows(IllegalArgumentException::class.java) {
            crypto.encryptAesGcmBase64Url(plaintext, apiKey, invalidIv)
        }
        assertTrue(
            ex.message?.contains("12 bytes") == true,
            "예외 메시지에 '12 bytes' 문구가 포함되어야 한다"
        )
    }

    @Test
    @DisplayName("실패: Base64URL 형식이 아닌 IV 문자열이면 예외를 던진다")
    fun `Base64URL 형식이 아닌 IV면 예외`() {
        // given
        val plaintext = """{"amount":1000}"""
        val apiKey = "dummy"
        val notBase64Url = "!!!!invalid!!!!" // Base64URL 아님

        // when / then
        val ex = assertThrows(IllegalArgumentException::class.java) {
            crypto.encryptAesGcmBase64Url(plaintext, apiKey, notBase64Url)
        }
        // Base64 디코드 과정에서 IAE 발생 가능 → IllegalArgumentException 래핑/전파 가정
        assertNotNull(ex, "Base64URL 형식이 아니면 예외가 발생해야 한다")
    }
}