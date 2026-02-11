package iuh.fit.se.servicemusic.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    SONG_NOT_FOUND(1001, "Song not found", HttpStatus.NOT_FOUND),
    FILE_UPLOAD_FAILED(1002, "File upload failed", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_FILE_FORMAT(1003, "Invalid file format", HttpStatus.BAD_REQUEST),
    GENRE_EXISTED(1004, "Genre already exists", HttpStatus.CONFLICT),
    GENRE_NOT_FOUND(1005, "Genre not found", HttpStatus.NOT_FOUND),

    ARTIST_ALREADY_REGISTERED(1007, "Artist profile already exists", HttpStatus.BAD_REQUEST),
    ARTIST_NOT_FOUND(1008, "Artist profile not found", HttpStatus.NOT_FOUND),
    TERMS_NOT_ACCEPTED(1009, "You must accept the terms and conditions", HttpStatus.BAD_REQUEST),
    NOT_ELIGIBLE_FOR_ARTIST(1010, "Your subscription plan does not include artist features", HttpStatus.FORBIDDEN),

    ONBOARDING_ALREADY_COMPLETED(1011, "Onboarding already completed", HttpStatus.BAD_REQUEST),
    USER_PREFERENCES_NOT_FOUND(1012, "User preferences not found", HttpStatus.NOT_FOUND);

    private int code;
    private String message;
    private HttpStatus statusCode;

    ErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}