package iuh.fit.se.serviceidentity.service;

public interface OtpService {
    /**
 * Generate a one-time password associated with the specified key.
 *
 * @param key a value that identifies the subject or context for which the OTP is generated (e.g., user ID, session, or destination)
 * @return the generated one-time password as a string
 */
String generateOtp(String key);
    /**
 * Validates a one-time password for a given key.
 *
 * @param key identifier associated with the OTP (e.g., user ID, session ID, or device token)
 * @param otp the one-time password to validate
 * @return true if the provided `otp` is valid for the `key`, false otherwise
 */
boolean validateOtp(String key, String otp);
}