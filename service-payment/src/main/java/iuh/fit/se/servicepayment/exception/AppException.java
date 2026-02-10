package iuh.fit.se.servicepayment.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {
    private ErrorCode errorCode;

    /**
     * Creates an AppException representing the provided domain error.
     *
     * The exception's message is derived from the given ErrorCode and the code is retained
     * for later retrieval.
     *
     * @param errorCode the ErrorCode representing the specific domain error
     */
    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}