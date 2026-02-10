package iuh.fit.se.servicepayment.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Invalid message key", HttpStatus.BAD_REQUEST),
    PLAN_EXISTED(1005, "Subscription plan name already exists", HttpStatus.BAD_REQUEST),
    PLAN_NOT_FOUND(1006, "Subscription plan not found", HttpStatus.NOT_FOUND);

    private final int code;
    private final String message;
    private final HttpStatus statusCode;

    /**
     * Create a new ErrorCode enum entry with a numeric code, human-readable message, and corresponding HTTP status.
     *
     * @param code       the numeric identifier for the error
     * @param message    a short human-readable description of the error
     * @param statusCode the HTTP status that represents this error
     */
    ErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}