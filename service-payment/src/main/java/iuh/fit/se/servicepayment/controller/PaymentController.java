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

    @PostMapping("/checkout")
    public ApiResponse<PaymentResponse> createPayment(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody CreatePaymentRequest request
    ) {
        return ApiResponse.<PaymentResponse>builder()
                .result(paymentService.createPaymentLink(UUID.fromString(userId), request))
                .build();
    }

    @PostMapping("/payos_transfer_handler")
    public ApiResponse<Void> handlePayOSWebhook(@RequestBody Webhook webhook) {
        paymentService.handleWebhook(webhook);

        return ApiResponse.<Void>builder()
                .message("Webhook received")
                .build();
    }
}