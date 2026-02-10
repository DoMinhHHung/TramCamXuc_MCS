package iuh.fit.se.servicepayment.controller;

import iuh.fit.se.servicepayment.dto.request.CreatePaymentRequest;
import iuh.fit.se.servicepayment.dto.response.ApiResponse;
import iuh.fit.se.servicepayment.dto.response.PaymentResponse;
import iuh.fit.se.servicepayment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.payos.model.webhooks.Webhook;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Creates a payment link for the specified user based on the provided payment details.
     *
     * @param userId the value of the `X-User-Id` request header; a UUID string identifying the user
     * @param request the payment creation details
     * @return an ApiResponse containing the created PaymentResponse with the payment link and related metadata
     */
    @PostMapping("/checkout")
    public ApiResponse<PaymentResponse> createPayment(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody CreatePaymentRequest request
    ) {
        return ApiResponse.<PaymentResponse>builder()
                .result(paymentService.createPaymentLink(UUID.fromString(userId), request))
                .build();
    }

    /**
     * Processes an incoming PayOS webhook and returns an acknowledgement response.
     *
     * @param webhook the PayOS webhook payload to be processed
     * @return an ApiResponse with no data and a message indicating the webhook was received
     */
    @PostMapping("/payos_transfer_handler")
    public ApiResponse<Void> handlePayOSWebhook(@RequestBody Webhook webhook) {
        paymentService.handleWebhook(webhook);

        return ApiResponse.<Void>builder()
                .message("Webhook received")
                .build();
    }
}