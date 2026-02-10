package iuh.fit.se.serviceidentity.service.impl;

import iuh.fit.se.serviceidentity.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final int OTP_TTL_MINUTES = 5;

    @Override
    public String generateOtp(String email) {
        String otp = String.format("%06d", new SecureRandom().nextInt(999999));
        String key = "OTP:" + email;

        redisTemplate.opsForValue().set(key, otp, OTP_TTL_MINUTES, TimeUnit.MINUTES);

        return otp;
    }

    @Override
    public boolean validateOtp(String email, String otpInput) {
        String key = "OTP:" + email;
        Object cachedOtp = redisTemplate.opsForValue().get(key);

        if (cachedOtp != null && cachedOtp.toString().equals(otpInput)) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }
}