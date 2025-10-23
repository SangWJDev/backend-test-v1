package im.bigs.pg.external.pg.client

import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.application.pg.port.out.PgClientOutPort
import im.bigs.pg.external.pg.util.TestPgCrypto
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient


@Component
@Order(1)
class TestPgClient(
    private val http: WebClient,
    private val crypto: TestPgCrypto
) : PgClientOutPort {
    override fun supports(partnerId: Long): Boolean = true;

    override fun approve(request: PgApproveRequest): PgApproveResult {
        TODO("Not yet implemented")
    }

}