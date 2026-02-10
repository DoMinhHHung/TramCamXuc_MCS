package iuh.fit.se.serviceidentity.service;

import iuh.fit.se.serviceidentity.dto.request.*;
import iuh.fit.se.serviceidentity.dto.response.*;

public interface UserService {
    /**
 * Create a new user account from the provided registration data.
 *
 * @param request the registration details required to create the user (e.g., name, email, password, and any other mandatory fields)
 * @return a UserResponse representing the newly created user
 */
UserResponse register(UserCreationRequest request);
    /**
 * Verifies a user's email address using the provided verification request.
 *
 * @param request the verification request containing the information required to confirm the user's email (e.g., verification token or code)
 */
void verifyEmail(VerifyEmailRequest request);
    /**
 * Retrieve the currently authenticated user's profile information.
 *
 * @return a {@link UserResponse} containing the authenticated user's details
 */
UserResponse getMyInfo();
    /**
 * Resends the registration one-time password (OTP) to the specified email address.
 *
 * @param email the recipient email address to which the registration OTP will be sent
 */
void resendRegistrationOtp(String email);
    /**
 * Retrieves a paginated list of users.
 *
 * @param page the page number to retrieve
 * @param size the number of users per page
 * @return a PageResponse containing UserResponse items and pagination metadata
 */
PageResponse<UserResponse> getAllUsers(int page, int size);
    /**
 * Retrieve detailed information for the user identified by the given userId.
 *
 * @param userId the unique identifier of the user whose details are being requested
 * @return a UserResponse containing the user's detailed information
 */
UserResponse getUserDetail(String userId);
    /**
 * Toggles the active/inactive status of the user identified by the given ID.
 *
 * @param userId the unique identifier of the user whose status should be toggled
 */
void toggleUserStatus(String userId);
}