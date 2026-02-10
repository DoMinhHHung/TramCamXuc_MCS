package iuh.fit.se.serviceidentity.controller;

import iuh.fit.se.serviceidentity.dto.request.*;
import iuh.fit.se.serviceidentity.dto.response.*;
import iuh.fit.se.serviceidentity.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    /**
     * Create a new user account from the provided registration data.
     *
     * @param request the user registration payload containing required fields for account creation
     * @return an ApiResponse containing the created user's details
     */
    @PostMapping("/registration")
    public ApiResponse<UserResponse> createUser(@RequestBody @Valid UserCreationRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.register(request))
                .build();
    }

    /**
     * Verify a user's email using the provided verification details.
     *
     * @param request the verification details (e.g., email and verification token or OTP)
     * @return an ApiResponse with no data and a success message indicating the email was verified
     */
    @PostMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@RequestBody VerifyEmailRequest request) {
        userService.verifyEmail(request);
        return ApiResponse.<Void>builder()
                .message("Email verified successfully")
                .build();
    }

    /**
     * Retrieve the current authenticated user's profile information.
     *
     * @return an ApiResponse whose `result` is the current user's UserResponse
     */
    @GetMapping("/my-info")
    public ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }

    /**
     * Resends the registration one-time password (OTP) to the specified email address.
     *
     * @param email the recipient email address for the OTP
     * @return an ApiResponse with no result data and message "OTP has been resent"
     */
    @PostMapping("/resend-registration-otp")
    public ApiResponse<Void> resendRegistrationOtp(@RequestParam String email) {
        userService.resendRegistrationOtp(email);
        return ApiResponse.<Void>builder()
                .message("OTP has been resent")
                .build();
    }

    /**
     * Retrieve a paginated list of users.
     *
     * @param page the page number starting at 1
     * @param size the number of users per page
     * @return an ApiResponse containing a PageResponse of UserResponse for the requested page
     */
    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<UserResponse>>builder()
                .result(userService.getAllUsers(page, size))
                .build();
    }

    /**
     * Retrieve detailed information for a specific user.
     *
     * @param userId the unique identifier of the user to retrieve
     * @return an ApiResponse wrapping a UserResponse with the specified user's details
     */
    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUserDetail(@PathVariable String userId) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getUserDetail(userId))
                .build();
    }


    /**
     * Toggle the enabled/disabled status of the user identified by the given ID.
     *
     * @param userId the identifier of the user whose status will be toggled
     * @return an ApiResponse with a success message indicating the status was updated
     */
    @PutMapping("/{userId}/status")
    public ApiResponse<Void> toggleUserStatus(@PathVariable String userId) {
        userService.toggleUserStatus(userId);
        return ApiResponse.<Void>builder()
                .message("User status updated successfully")
                .build();
    }
}