package org.dci.aimealplanner.services.users;

import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.entities.users.PasswordResetToken;
import org.dci.aimealplanner.entities.users.User;
import org.dci.aimealplanner.models.UserType;
import org.dci.aimealplanner.repositories.users.PasswordResetTokenRepository;
import org.dci.aimealplanner.repositories.users.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final UserService userService;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.url}")
    private String appUrl;

    @Value("${app.reset.ttl.minutes}")
    private Long ttlMinutes;

    private static final SecureRandom RNG = new SecureRandom();

    @Transactional
    public void requestReset(String rawEmail) {
        final String email = normalize(rawEmail);

        if (userService.emailIsNotTaken(email)) {
            return;
        }

        User user;
        try {
            user = userService.findByEmail(email);
        } catch (RuntimeException ex) {
            return;
        }

        if (user.getUserType() != UserType.LOCAL || user.isDeleted()) {
            return;
        }

        PasswordResetToken token = new PasswordResetToken();
        token.setToken(generateToken());
        token.setUser(user);
        token.setExpiresAt(Instant.now().plus(Duration.ofMinutes(ttlMinutes)));
        token.setUsed(false);
        tokenRepository.save(token);

        String link = appUrl + "/auth/reset-password/" + token.getToken();
        emailService.sendPasswordResetEmail(user.getEmail(), link, ttlMinutes);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Token expired or already used");
        }

        User user = resetToken.getUser();
        if (user.getUserType() != UserType.LOCAL || user.isDeleted()) {
            throw new IllegalStateException("Reset not allowed for this account");
        }

        userService.checkPasswordValidity(newPassword);

        user.setPassword(passwordEncoder.encode(newPassword));
        userService.update(user);


        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        jdbcTemplate.update("DELETE FROM persistent_logins WHERE username = ?", user.getEmail());
    }

    private static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private static String generateToken() {
        byte[] buf = new byte[48];
        RNG.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
