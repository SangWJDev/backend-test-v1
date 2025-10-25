package im.bigs.pg.common.exception

data class ApiErrorResponse(
    val code: String,
    val message: String,
    val upstreamStatus: Int,
    val path: String?,
    val timestamp: String = java.time.OffsetDateTime.now().toString()
)