package im.bigs.pg.external.pg.exception

import im.bigs.pg.common.exception.BusinessException
import im.bigs.pg.common.exception.ErrorCode

class PgClientException(
    val errorCode: String?,
    override val message: String,
    override val upstreamStatus: Int
) : BusinessException(ErrorCode.PG_CLIENT_ERROR, message, upstreamStatus)