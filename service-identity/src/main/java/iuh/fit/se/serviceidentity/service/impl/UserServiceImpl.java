package iuh.fit.se.serviceidentity.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import iuh.fit.se.serviceidentity.config.RabbitMQConfig;
import iuh.fit.se.serviceidentity.dto.event.NotificationEvent;
import iuh.fit.se.serviceidentity.dto.mapper.UserMapper;
import iuh.fit.se.serviceidentity.dto.request.ChangePasswordRequest;
import iuh.fit.se.serviceidentity.dto.request.UserCreationRequest;
import iuh.fit.se.serviceidentity.dto.request.UserUpdateRequest;
import iuh.fit.se.serviceidentity.dto.request.VerifyEmailRequest;
import iuh.fit.se.serviceidentity.dto.response.PageResponse;
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
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RabbitTemplate rabbitTemplate;
    private final OtpService otpService;
    private final Cloudinary cloudinary;
    /**
     * Creates a new user account, persists it, generates a verification OTP, and publishes an email notification event.
     *
     * @param request the user creation payload containing registration details (e.g., name, email, password)
     * @return a UserResponse representing the newly created user (status set to pending verification)
     * @throws AppException if a user with the given email already exists
     */
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

    /**
     * Verifies a user's email by validating the provided OTP and setting the account status to ACTIVE.
     *
     * @param request contains the user's email and the one-time password (OTP) to validate
     * @throws AppException with ErrorCode.INVALID_OTP if the provided OTP is invalid
     * @throws AppException with ErrorCode.USER_NOT_EXISTED if no user exists for the given email
     */
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

    /**
     * Retrieves the current authenticated user's profile.
     *
     * Loads the user by the principal (authentication name) from the security context
     * and returns a mapped UserResponse.
     *
     * @return the authenticated user's UserResponse
     * @throws AppException if no user exists for the authenticated principal (ErrorCode.USER_NOT_EXISTED)
     */
    @Override
    public UserResponse getMyInfo() {
        var context = SecurityContextHolder.getContext();
        String userId = context.getAuthentication().getName();

        User user = userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userMapper.toUserResponse(user);
    }

    /**
     * Resends a registration one-time password (OTP) to the specified user's email.
     *
     * @param email the email address of the user to resend the registration OTP to
     * @throws AppException if the user does not exist (ErrorCode.USER_NOT_EXISTED) or if the account is already verified (ErrorCode.ACCOUNT_ALREADY_VERIFIED)
     */
    @Override
    public void resendRegistrationOtp(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.getStatus() == AccountStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_ALREADY_VERIFIED);
        }

        String otp = otpService.generateOtp(email);

        NotificationEvent event = NotificationEvent.builder()
                .channel("EMAIL")
                .recipient(user.getEmail())
                .templateCode("REGISTER_OTP")
                .params(Map.of("otp", otp, "name", user.getFirstName()))
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.EMAIL_ROUTING_KEY, event);
    }

    @Override
    public UserResponse updateProfile(UserUpdateRequest request) {
        var context = SecurityContextHolder.getContext();
        String userId = context.getAuthentication().getName();

        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (request.getAvatarUrl() != null && !request.getAvatarUrl().equals(user.getAvatarUrl())) {
            if (user.getAvatarUrl() != null && user.getAvatarUrl().contains("cloudinary")) {
                String oldPublicId = extractPublicId(user.getAvatarUrl());
                try {
                    cloudinary.uploader().destroy(oldPublicId, ObjectUtils.emptyMap());
                    log.info("Deleted old avatar: {}", oldPublicId);
                } catch (IOException e) {
                    log.error("Failed to delete old avatar", e);}
            }
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getDob() != null) user.setDob(request.getDob());

        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        var context = SecurityContextHolder.getContext();
        String userId = context.getAuthentication().getName();
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.WRONG_PASSWORD);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<UserResponse> getUsers(String keyword, int page, int size) {
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        var pageData = userRepository.searchUsers(keyword, pageable);

        return PageResponse.<UserResponse>builder()
                .currentPage(page)
                .pageSize(pageData.getSize())
                .totalPages(pageData.getTotalPages())
                .totalElements(pageData.getTotalElements())
                .data(pageData.getContent().stream().map(userMapper::toUserResponse).toList())
                .build();
    }

    /**
     * Retrieves a paginated list of users sorted by creation time in descending order.
     *
     * @param page  the 1-based page number to retrieve
     * @param size  the number of users per page
     * @return      a PageResponse containing current page, page size, total pages, total elements,
     *              and a list of UserResponse objects for the requested page (sorted by createdAt descending)
     */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<UserResponse> getAllUsers(int page, int size) {
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        var pageData = userRepository.findAll(pageable);

        return PageResponse.<UserResponse>builder()
                .currentPage(page)
                .pageSize(pageData.getSize())
                .totalPages(pageData.getTotalPages())
                .totalElements(pageData.getTotalElements())
                .data(pageData.getContent().stream().map(userMapper::toUserResponse).toList())
                .build();

    }

    /**
     * Retrieve detailed information for a user identified by their UUID string.
     *
     * @param userId the user's UUID as a string
     * @return the user's detailed representation as a UserResponse
     * @throws AppException with ErrorCode.USER_NOT_EXISTED if no user exists for the given UUID
     */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse getUserDetail(String userId) {
        var user = userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userMapper.toUserResponse(user);
    }

    /**
     * Toggle the specified user's account status between ACTIVE and LOCKED.
     *
     * <p>Looks up the user by the given UUID string, flips ACTIVE to LOCKED or LOCKED to ACTIVE,
     * and persists the change.</p>
     *
     * @param userId the user's UUID as a string
     * @throws AppException with ErrorCode.USER_NOT_EXISTED if no user exists for the given id
     * @throws AppException with ErrorCode.UNAUTHORIZED if the target user has the ADMIN role
     */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void toggleUserStatus(String userId) {
        var user = userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.getRole() == UserRole.ADMIN) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (user.getStatus() == AccountStatus.ACTIVE) {
            user.setStatus(AccountStatus.LOCKED);
        } else if (user.getStatus() == AccountStatus.LOCKED) {
            user.setStatus(AccountStatus.ACTIVE);
        }

        userRepository.save(user);

    }

    private String extractPublicId(String url) {
        try {
            String[] parts = url.split("/");
            String filename = parts[parts.length - 1];
            String publicId = filename.substring(0, filename.lastIndexOf("."));

            return publicId;
        } catch (Exception e) {
            return null;
        }
    }
}