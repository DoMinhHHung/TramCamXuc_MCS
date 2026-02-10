package iuh.fit.se.servicepayment.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.servicepayment.config.RabbitMQConfig;
import iuh.fit.se.servicepayment.dto.event.UserUpgradedEvent;
import iuh.fit.se.servicepayment.dto.request.CreatePaymentRequest;
import iuh.fit.se.servicepayment.dto.response.PaymentResponse;
import iuh.fit.se.servicepayment.entity.SubscriptionPlan;
import iuh.fit.se.servicepayment.entity.Transaction;
import iuh.fit.se.servicepayment.entity.UserSubscription;
import iuh.fit.se.servicepayment.entity.enums.PaymentStatus;
import iuh.fit.se.servicepayment.entity.enums.SubscriptionStatus;
import iuh.fit.se.servicepayment.exception.AppException;
import iuh.fit.se.servicepayment.exception.ErrorCode;
import iuh.fit.se.servicepayment.repository.*;
import iuh.fit.se.servicepayment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {
    private final PayOS payOS;
    private final SubscriptionPlanRepository planRepository;
    private final TransactionRepository transactionRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public PaymentResponse createPaymentLink(UUID userId, CreatePaymentRequest request) {
        var plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_FOUND));

        long orderCode = System.currentTimeMillis();

        PaymentLinkItem item = PaymentLinkItem.builder()
                .name(plan.getName())
                .quantity(1)
                .price(plan.getPrice().longValue())
                .build();

        CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                .orderCode(orderCode)
                .amount(plan.getPrice().longValue())
                .description(plan.getName())
                .returnUrl(request.getReturnUrl())
                .cancelUrl(request.getCancelUrl())
                .item(item)
                .build();

        try {
            CreatePaymentLinkResponse data = payOS.paymentRequests().create(paymentData);

            Transaction transaction = Transaction.builder()
                    .orderCode(orderCode)
                    .userId(userId)
                    .planId(plan.getId())
                    .amount(plan.getPrice())
                    .status(PaymentStatus.PENDING)
                    .paymentLinkId(data.getPaymentLinkId())
                    .checkoutUrl(data.getCheckoutUrl())
                    .build();

            transactionRepository.save(transaction);

            return PaymentResponse.builder()
                    .checkoutUrl(data.getCheckoutUrl())
                    .orderCode(orderCode)
                    .qrCode(data.getQrCode())
                    .build();

        } catch (Exception e) {
            log.error("PayOS Create Link Error: ", e);
            throw new RuntimeException("Failed to create payment link");
        }
    }

    @Override
    public void handleWebhook(Webhook webhook) {
        try{
            WebhookData data = payOS.webhooks().verify(webhook);
            Transaction transaction = transactionRepository.findByOrderCode(data.getOrderCode())
                    .orElse(null);
            if (transaction == null) {
                log.warn("Transaction not found for OrderCode: {}", data.getOrderCode());
                return;
            }

            if (transaction.getStatus() == PaymentStatus.SUCCESS) {
                return;
            }

            if ("00".equals(data.getCode())) {
                transaction.setStatus(PaymentStatus.SUCCESS);
                transactionRepository.save(transaction);

                activateSubscription(transaction.getUserId(), transaction.getPlanId());
            } else {
                transaction.setStatus(PaymentStatus.FAILED);
                transactionRepository.save(transaction);
            }
        }
        catch (Exception e) {
            log.error("Webhook processing error: ", e);
        }
    }
    private void activateSubscription(UUID userId, UUID planId) {
        var plan = planRepository.findById(planId).orElseThrow();

        var subscription = userSubscriptionRepository.findByUserId(userId)
                .orElse(UserSubscription.builder()
                        .userId(userId)
                        .status(SubscriptionStatus.EXPIRED)
                        .build());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = getLocalDateTime(now, subscription, plan);

        subscription.setCurrentPlanId(planId);
        subscription.setStartDate(now);
        subscription.setEndDate(endDate);
        subscription.setStatus(SubscriptionStatus.ACTIVE);

        userSubscriptionRepository.save(subscription);
        log.info("Activated subscription for User: {} until {}", userId, endDate);

        try {
            Map<String, Object> featuresMap = objectMapper.convertValue(
                    plan.getFeatures(),
                    new TypeReference<Map<String, Object>>() {}
            );

            UserUpgradedEvent event = UserUpgradedEvent.builder()
                    .userId(userId)
                    .planName(plan.getName())
                    .startDate(subscription.getStartDate())
                    .endDate(subscription.getEndDate())
                    .features(featuresMap)
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.INTERNAL_EXCHANGE,
                    RabbitMQConfig.USER_UPGRADE_ROUTING_KEY,
                    event
            );
            log.info(">>> Đã bắn tin UserUpgradedEvent sang RabbitMQ cho User: {}", userId);

        } catch (Exception e) {
            log.error("!!! Lỗi bắn tin RabbitMQ: ", e);
        }
    }

    @NotNull
    private static LocalDateTime getLocalDateTime(LocalDateTime now, UserSubscription subscription, SubscriptionPlan plan) {
        LocalDateTime startDate = now;
        LocalDateTime endDate;

        if (subscription.getStatus() == SubscriptionStatus.ACTIVE
                && subscription.getEndDate() != null
                && subscription.getEndDate().isAfter(now)) {
            startDate = subscription.getEndDate();
        }

        int duration = plan.getDuration();
        endDate = switch (plan.getDurationUnit()) {
            case DAYS -> startDate.plusDays(duration);
            case MONTHS -> startDate.plusMonths(duration);
            case YEARS -> startDate.plusYears(duration);
        };
        return endDate;
    }
}
