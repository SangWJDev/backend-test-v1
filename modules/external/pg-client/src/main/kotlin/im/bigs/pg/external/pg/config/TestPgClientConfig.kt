package im.bigs.pg.external.pg.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient


@Configuration
class TestPgClientConfig {

    @Bean
    fun testPgWebClient(): WebClient {
        return WebClient.builder()
            .baseUrl("https://api-test-pg.bigs.im")
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .build()
    }
}