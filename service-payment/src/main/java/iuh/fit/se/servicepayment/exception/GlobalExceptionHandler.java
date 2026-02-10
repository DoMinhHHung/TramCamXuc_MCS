package iuh.fit.se.servicepayment.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle an AppException by producing an ApiResponse built from its ErrorCode and returning it with the corresponding HTTP status.
     *
     * @param exception the caught AppException whose ErrorCode provides the response code, message, and HTTP status
     * @return a ResponseEntity whose body is an ApiResponse populated from the exception's ErrorCode and whose status matches that ErrorCode
     */
    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse> handlingAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());
        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }
}