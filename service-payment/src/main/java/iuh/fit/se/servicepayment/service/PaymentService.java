package iuh.fit.se.servicepayment.service;

import iuh.fit.se.servicepayment.dto.request.CreatePaymentRequest;
import iuh.fit.se.servicepayment.dto.response.PaymentResponse;
import vn.payos.model.webhooks.Webhook;

import java.util.UUID;

public interface PaymentService {
    /**
 * Creates a payment link for the specified user using the provided payment request data.
 *
 * @param userId the UUID of the user for whom the payment link will be created
 * @param request the payment creation details such as amount, currency, and description
 * @return a PaymentResponse containing the generated payment link and related metadata
 */
PaymentResponse createPaymentLink(UUID userId, CreatePaymentRequest request);
    /**
 * Processes an incoming payment gateway webhook event and updates application state accordingly.
 *
 * @param webhook the webhook payload received from the payment provider
 */
void handleWebhook(Webhook webhook);
}