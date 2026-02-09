package iuh.fit.se.servicemusic.exception;

import lombok.Data;

@Data
public class ApiResponse {
    private int code;
    private String message;
}
