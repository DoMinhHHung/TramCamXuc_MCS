package iuh.fit.se.serviceidentity.exception;

import lombok.Data;

@Data
public class ApiResponse {
    private int code;
    private String message;
}
