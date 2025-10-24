package im.bigs.pg.external.pg.util

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

/**
 * TestPG 암호화 유틸.
 * - AES-256-GCM (NoPadding)
 * - Key = SHA-256(API-KEY) 32바이트
 * - IV  = Base64URL(12바이트) 디코드
 * - 출력 = Base64URL(ciphertext || tag), padding 없음
 *
 * 민감정보(카드번호 등)를 인자로 받지만, 이 클래스는 로깅/보관을 절대 하지 않습니다.
 */
@Component
class TestPgCrypto(
    private val objectMapper: ObjectMapper
) {

    /**
     * 이미 직렬화된 평문 JSON 문자열을 암호화하여 enc(Base64URL)를 반환.
     */
    fun encryptAesGcmBase64Url(
        plaintextJson: String,
        apiKey: String,
        ivBase64Url: String
    ): String {
        val key = deriveKeyFromApiKey(apiKey)
        val iv = decodeIvBase64Url(ivBase64Url)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(128, iv) // GCM tag 128 bits
        )
        val cipherWithTag = cipher.doFinal(plaintextJson.toByteArray(StandardCharsets.UTF_8))
        // Base64 URL (no padding)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(cipherWithTag)
    }

    /**
     * Map/DTO를 받아 내부에서 JSON 직렬화 후 암호화하고 싶을 때 사용.
     */
    fun encryptPayloadAsEnc(
        payload: Any,
        apiKey: String,
        ivBase64Url: String
    ): String {
        // ObjectMapper는 Boot에서 자동 주입됨 (jackson-module-kotlin 포함)
        val json = objectMapper.writeValueAsString(payload)
        return encryptAesGcmBase64Url(json, apiKey, ivBase64Url)
    }

    /**
     * TestPG 스펙에 따라 API-KEY(문자열)를 SHA-256 해시로 32바이트 키로 파생.
     */
    private fun deriveKeyFromApiKey(apiKey: String): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(apiKey.toByteArray(StandardCharsets.UTF_8)) // 32 bytes
    }

    /**
     * IV는 반드시 Base64URL 인코딩된 12바이트여야 한다.
     */
    private fun decodeIvBase64Url(ivBase64Url: String): ByteArray {
        val iv = Base64.getUrlDecoder().decode(ivBase64Url)
        require(iv.size == 12) { "TestPG IV must be 12 bytes, but was ${iv.size}" }
        return iv
    }
}