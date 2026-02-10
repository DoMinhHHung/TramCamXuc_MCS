package iuh.fit.se.serviceidentity.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {
    private ErrorCode errorCode;

    /**
     * Create an AppException associated with a specific ErrorCode.
     *
     * <p>The exception's detail message is initialized from {@code errorCode.getMessage()}, and the
     * provided ErrorCode can be retrieved with {@code getErrorCode()}.
     *
     * @param errorCode the ErrorCode that identifies the error and provides the exception message
     */
    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}