package iuh.fit.se.servicepayment.exception;

import lombok.Data;

@Data
public class ApiResponse {
    private int code;
    private String message;
}
