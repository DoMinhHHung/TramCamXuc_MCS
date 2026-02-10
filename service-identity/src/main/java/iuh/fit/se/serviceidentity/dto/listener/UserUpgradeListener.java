package iuh.fit.se.serviceidentity.listener;

import iuh.fit.se.serviceidentity.config.RabbitMQConfig;
import iuh.fit.se.serviceidentity.dto.event.UserUpgradedEvent;
import iuh.fit.se.serviceidentity.entity.User;
import iuh.fit.se.serviceidentity.entity.UserFeatures;
import iuh.fit.se.serviceidentity.repository.UserFeaturesRepository;
import iuh.fit.se.serviceidentity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserUpgradeListener {

    private final UserRepository userRepository;
    private final UserFeaturesRepository userFeaturesRepository;

    @RabbitListener(queues = RabbitMQConfig.IDENTITY_UPGRADE_QUEUE)
    public void handleUserUpgrade(UserUpgradedEvent event) {
        log.info(">>> Nhận yêu cầu nâng cấp cho User: {}", event.getUserId());

        try {
            User user = userRepository.findById(event.getUserId())
                    .orElseThrow(() -> new RuntimeException("User không tồn tại"));

            user.setSubscriptionActive(true);
            user.setSubscriptionEndDate(event.getEndDate());
            user.setCurrentPlanName(event.getPlanName());

            userRepository.save(user);

            UserFeatures userFeatures = userFeaturesRepository.findByUserId(user.getId())
                    .orElse(UserFeatures.builder().user(user).build());

            userFeatures.setFeatures(event.getFeatures());
            userFeaturesRepository.save(userFeatures);

            log.info(">>> Nâng cấp thành công User {} với quyền lợi: {}", event.getUserId(), event.getFeatures());

        } catch (Exception e) {
            log.error("!!! Lỗi xử lý nâng cấp user", e);
            throw e; // Ném lỗi để RabbitMQ biết mà retry
        }
    }
}