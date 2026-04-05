package com.lethien.auth_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for sending emails
 * TODO: Implement with real email provider (SendGrid, AWS SES, etc.)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    /**
     * Send verification email
     */
    public void sendVerificationEmail(String email, String verificationToken, String fullName) {
        log.info("Sending verification email to: {}", email);

        // TODO: Implement with real email provider
        // For now, just log the verification link
        String verificationLink = String.format(
                "http://localhost:8081/api/auth/verify-email?token=%s",
                verificationToken
        );

        log.info("Verification link for {}: {}", email, verificationLink);

        // In production, send actual email:
        // emailClient.send(
        //     to: email,
        //     subject: "Verify your email",
        //     template: "verification-email",
        //     data: { fullName, verificationLink }
        // );
    }

    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(String email, String resetToken, String fullName) {
        log.info("Sending password reset email to: {}", email);

        String resetLink = String.format(
                "http://localhost:8081/api/auth/reset-password?token=%s",
                resetToken
        );

        log.info("Password reset link for {}: {}", email, resetLink);
    }

    /**
     * Send welcome email after verification
     */
    public void sendWelcomeEmail(String email, String fullName) {
        log.info("Sending welcome email to: {}", email);

        // TODO: Implement welcome email
    }
}
