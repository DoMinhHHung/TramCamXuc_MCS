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

    @PostMapping("/registration")
    public ApiResponse<UserResponse> createUser(@RequestBody @Valid UserCreationRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.register(request))
                .build();
    }

    @PostMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@RequestBody VerifyEmailRequest request) {
        userService.verifyEmail(request);
        return ApiResponse.<Void>builder()
                .message("Email verified successfully")
                .build();
    }

    @GetMapping("/my-info")
    public ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }
}