package iuh.fit.se.serviceidentity.service.impl;

import iuh.fit.se.serviceidentity.config.RabbitMQConfig;
import iuh.fit.se.serviceidentity.dto.event.NotificationEvent;
import iuh.fit.se.serviceidentity.dto.mapper.UserMapper;
import iuh.fit.se.serviceidentity.dto.request.UserCreationRequest;
import iuh.fit.se.serviceidentity.dto.request.VerifyEmailRequest;
import iuh.fit.se.serviceidentity.dto.response.UserResponse;
import iuh.fit.se.serviceidentity.entity.User;
import iuh.fit.se.serviceidentity.entity.enums.AccountStatus;
import iuh.fit.se.serviceidentity.entity.enums.UserRole;
import iuh.fit.se.serviceidentity.exception.AppException;
import iuh.fit.se.serviceidentity.exception.ErrorCode;
import iuh.fit.se.serviceidentity.repository.UserRepository;
import iuh.fit.se.serviceidentity.service.OtpService;
import iuh.fit.se.serviceidentity.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RabbitTemplate rabbitTemplate;
    private final OtpService otpService;

    @Override
    @Transactional
    public UserResponse register(UserCreationRequest request) {
        if (userRepository.existsByEmail(request.getEmail()))
            throw new AppException(ErrorCode.USER_EXISTED);

        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.USER);
        user.setStatus(AccountStatus.PENDING_VERIFICATION);

        user = userRepository.save(user);

        String otp = otpService.generateOtp(user.getEmail());

        NotificationEvent event = NotificationEvent.builder()
                .channel("EMAIL")
                .recipient(user.getEmail())
                .templateCode("WELCOME_VERIFY_EMAIL")
                .params(Map.of("otp", otp, "name", user.getFirstName()))
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.EMAIL_ROUTING_KEY,
                event
        );

        log.info("Registered user: {}. Sent OTP event to RabbitMQ.", user.getEmail());

        return userMapper.toUserResponse(user);
    }

    @Override
    public void verifyEmail(VerifyEmailRequest request) {
        boolean isValid = otpService.validateOtp(request.getEmail(), request.getOtpCode());
        if (!isValid) {
            throw new AppException(ErrorCode.INVALID_OTP);
        }

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        user.setStatus(AccountStatus.ACTIVE);
        userRepository.save(user);
    }

    @Override
    public UserResponse getMyInfo() {
        var context = SecurityContextHolder.getContext();
        String userId = context.getAuthentication().getName();

        User user = userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userMapper.toUserResponse(user);
    }
}