package im.bigs.pg.common.exception

open class BusinessException(
    val code: ErrorCode,
    override val message: String? = code.message,
    open val upstreamStatus: Int
) : RuntimeException(message)


enum class ErrorCode(val message: String) {
    PAYMENT_APPROVAL_FAILED("결제 승인에 실패했습니다."),
    PG_CLIENT_ERROR("PG 연동 오류가 발생했습니다."),
}