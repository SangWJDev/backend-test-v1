package im.bigs.pg.external.pg.config

import org.springframework.boot.context.properties.ConfigurationProperties


@ConfigurationProperties(prefix = "testpg.credentials")
data class TestPgCredentials(
    val apiKey: String,
    val ivBase64url: String,
)