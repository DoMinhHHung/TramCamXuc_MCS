package iuh.fit.se.serviceidentity.service;

import iuh.fit.se.serviceidentity.dto.request.*;
import iuh.fit.se.serviceidentity.dto.response.*;

public interface UserService {
    UserResponse register(UserCreationRequest request);
    void verifyEmail(VerifyEmailRequest request);
    UserResponse getMyInfo();
    void resendRegistrationOtp(String email);
    PageResponse<UserResponse> getAllUsers(int page, int size);
    UserResponse getUserDetail(String userId);
    void toggleUserStatus(String userId);
}
