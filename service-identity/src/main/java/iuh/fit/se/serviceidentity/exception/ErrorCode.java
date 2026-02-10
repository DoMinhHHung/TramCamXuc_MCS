package iuh.fit.se.serviceidentity.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Invalid message key", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "User existed", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1003, "User not existed", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1004, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1005, "You do not have permission", HttpStatus.FORBIDDEN),
    INVALID_OTP(1006, "Invalid OTP", HttpStatus.BAD_REQUEST),
    EMAIL_SEND_FAILED(1007, "Failed to send email", HttpStatus.INTERNAL_SERVER_ERROR),
    PASSWORD_EXISTED(1008, "Password existed", HttpStatus.BAD_REQUEST),
    ACCOUNT_ALREADY_VERIFIED(1009, "Account already verified", HttpStatus.BAD_REQUEST),
    USER_LOCKED(1010, "User account is locked", HttpStatus.FORBIDDEN);

    private final int code;
    private final String message;
    private final HttpStatus statusCode;

    ErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}