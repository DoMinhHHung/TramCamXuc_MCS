package iuh.fit.se.servicepayment.dto.request;

import lombok.Data;
import java.util.UUID;

@Data
public class CreatePaymentRequest {
    private UUID planId;
    private String returnUrl;
    private String cancelUrl;
}