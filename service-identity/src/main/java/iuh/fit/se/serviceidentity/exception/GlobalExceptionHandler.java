package iuh.fit.se.serviceidentity.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles AppException and maps it to an HTTP response containing an ApiResponse with the error details.
     *
     * @param exception the thrown AppException whose ErrorCode will determine the response body and HTTP status
     * @return a ResponseEntity whose body is an ApiResponse populated with the error code and message, and whose HTTP status is taken from the exception's ErrorCode
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