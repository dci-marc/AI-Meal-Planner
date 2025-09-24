package org.dci.aimealplanner.services.users;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.services.utils.AppProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final AppProperties appProperties;

    public void sendVerificationEmail(String toEmail, String token) {
        String base = stripTrailingSlash(safeUrl(appProperties.getUrl()));
        String verificationUrl = base + "/auth/verify?token=" + token;

        try {
            String htmlContent = loadHtmlTemplate("emails/verification-email.html")
                    .replace("{{VERIFICATION_URL}}", verificationUrl);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setTo(toEmail);
            helper.setSubject("Please verify your email");
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
        } catch (IOException | MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendPasswordResetEmail(String email, String link, Long ttlMinutes) {
        long ttl = ttlMinutes != null ? ttlMinutes : 120L;
        try {
            String html = loadHtmlTemplate("emails/password-reset-email.html")
                    .replace("{{RESET_URL}}", link)
                    .replace("{{TTL_MINUTES}}", String.valueOf(ttl));

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setTo(email);
            helper.setSubject("Reset your password");
            helper.setText(html, true);
            mailSender.send(mimeMessage);
        } catch (IOException | MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    private String safeUrl(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:8080";
        }
        return url;
    }

    private String stripTrailingSlash(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String loadHtmlTemplate(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
