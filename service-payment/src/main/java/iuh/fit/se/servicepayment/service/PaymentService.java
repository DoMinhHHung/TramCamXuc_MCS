package iuh.fit.se.servicepayment.service;

import iuh.fit.se.servicepayment.dto.request.CreatePaymentRequest;
import iuh.fit.se.servicepayment.dto.response.PaymentResponse;
import vn.payos.model.webhooks.Webhook;

import java.util.UUID;

public interface PaymentService {
    PaymentResponse createPaymentLink(UUID userId, CreatePaymentRequest request);
    void handleWebhook(Webhook webhook);
}