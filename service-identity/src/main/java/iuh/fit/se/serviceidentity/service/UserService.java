package iuh.fit.se.serviceidentity.service;

import iuh.fit.se.serviceidentity.dto.request.*;
import iuh.fit.se.serviceidentity.dto.response.*;

public interface UserService {
    UserResponse register(UserCreationRequest request);
    void verifyEmail(VerifyEmailRequest request);
    UserResponse getMyInfo();
}
