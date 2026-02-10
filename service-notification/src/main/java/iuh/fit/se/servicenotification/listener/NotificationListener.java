package iuh.fit.se.servicenotification.listener;

import iuh.fit.se.servicenotification.config.RabbitMQConfig;
import iuh.fit.se.servicenotification.dto.event.NotificationEvent;
import iuh.fit.se.servicenotification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final EmailService emailService;

    /**
     * Handle incoming NotificationEvent messages from the notification queue and send templated emails when the channel is EMAIL.
     *
     * <p>When the event's channel equals "EMAIL" (case-insensitive), selects an email subject and template (defaults:
     * subject "Thông báo từ TramCamXuc", template "register-otp") and overrides them for known template codes:
     * "REGISTER_OTP" -> subject "Xác thực đăng ký tài khoản", template "register-otp";
     * "FORGOT_PASSWORD_OTP" -> subject "Mã OTP đặt lại mật khẩu", template "forgot-password". The selected subject,
     * template, recipient, and event parameters are passed to the EmailService for delivery.</p>
     *
     * @param event the notification event containing recipient, channel, templateCode, and template parameters
     */
    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void listen(NotificationEvent event) {
        log.info("Received event: {}", event);

        if ("EMAIL".equalsIgnoreCase(event.getChannel())) {
            String subject = "Thông báo từ TramCamXuc";
            String templateName = "register-otp";

            if ("REGISTER_OTP".equals(event.getTemplateCode())) {
                subject = "Xác thực đăng ký tài khoản";
                templateName = "register-otp";
            }
            else if ("FORGOT_PASSWORD_OTP".equals(event.getTemplateCode())) {
                subject = "Mã OTP đặt lại mật khẩu";
                templateName = "forgot-password";
            }

            emailService.sendEmail(
                    event.getRecipient(),
                    subject,
                    templateName,
                    event.getParams()
            );
        }
    }
}