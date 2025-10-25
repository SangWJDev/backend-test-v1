package im.bigs.pg.api.exception

import im.bigs.pg.common.exception.ApiErrorResponse
import im.bigs.pg.common.exception.BusinessException
import im.bigs.pg.common.exception.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(ex: BusinessException, req: HttpServletRequest): ResponseEntity<ApiErrorResponse> {
        val status = when (ex.code) {
            ErrorCode.PG_CLIENT_ERROR -> HttpStatus.BAD_GATEWAY
            ErrorCode.PAYMENT_APPROVAL_FAILED -> HttpStatus.UNPROCESSABLE_ENTITY
            else -> HttpStatus.BAD_REQUEST
        }

        val body = ApiErrorResponse(
            code = ex.code.name,
            message = ex.message ?: ex.code.message,
            upstreamStatus = ex.upstreamStatus,
            path = req.requestURI
        )

        return ResponseEntity.status(status).body(body)
    }
}