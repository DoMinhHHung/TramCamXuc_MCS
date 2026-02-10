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

    /**
     * Creates a payment link for the specified user's requested subscription plan.
     *
     * @param userId the ID of the user initiating the payment
     * @param request contains the requested plan ID and return/cancel URLs for checkout
     * @return a PaymentResponse containing the checkout URL, generated order code, and QR code
     * @throws AppException if the requested subscription plan does not exist (PLAN_NOT_FOUND)
     * @throws RuntimeException if creation of the payment link with the external provider or persistence fails
     */
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

    /**
     * Processes a PayOS webhook: verifies the payload, updates the corresponding transaction's status,
     * and activates the user's subscription when payment succeeded.
     *
     * If the transaction for the webhook's order code is not found the method returns without action.
     * If the transaction is already marked SUCCESS, the method is idempotent and returns.
     * A webhook data code of "00" marks the transaction SUCCESS and triggers subscription activation;
     * any other code marks the transaction FAILED.
     *
     * @param webhook the incoming webhook payload from PayOS
     */
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
    /**
     * Activate or renew a user's subscription for the specified plan and publish a UserUpgradedEvent.
     *
     * Updates or creates the user's subscription record with appropriate start and end dates based on the plan,
     * sets the subscription status to ACTIVE, persists the subscription, and emits a UserUpgradedEvent to RabbitMQ.
     * Failures while publishing the event are caught and logged.
     *
     * @param userId the UUID of the user whose subscription will be activated or renewed
     * @param planId the UUID of the subscription plan to apply to the user
     */
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

    /**
     * Compute the subscription end date based on the current time, the user's existing subscription, and the plan's duration.
     *
     * If the existing subscription is ACTIVE and its endDate is after `now`, the calculation starts from that endDate; otherwise it starts from `now`.
     *
     * @param now the reference time to use when the existing subscription does not extend past `now`
     * @param subscription the user's current subscription (used to determine a deferred start when active)
     * @param plan the subscription plan providing duration and duration unit
     * @return the computed end date/time after adding the plan's duration (in its configured unit) to the chosen start date
     */
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