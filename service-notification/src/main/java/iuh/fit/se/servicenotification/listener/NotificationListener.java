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