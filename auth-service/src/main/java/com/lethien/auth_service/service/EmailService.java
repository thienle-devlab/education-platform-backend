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

    // ============================================
    // [NEW] Email change
    // ============================================

    /**
     * Send email change verification to the NEW email address.
     * User must click the link to confirm the change.
     *
     * @param newEmail     Email mới cần xác nhận
     * @param token        Token xác nhận
     * @param currentEmail Email hiện tại — để user biết email nào đang được thay thế
     */
    public void sendEmailChangeVerification(String newEmail, String token, String currentEmail) {
        log.info("Sending email change verification to: {}", newEmail);

        String confirmLink = String.format(
                "http://localhost:8081/api/auth/email/change/confirm?token=%s",
                token
        );

        log.info("========================================");
        log.info("[DEV] Email Change Verification");
        log.info("[DEV] Current email : {}", currentEmail);
        log.info("[DEV] New email     : {}", newEmail);
        log.info("[DEV] Confirm link  : {}", confirmLink);
        log.info("========================================");

        // TODO: Replace with real email provider
        // emailClient.send(
        //     to: newEmail,
        //     subject: "Confirm your new email address",
        //     template: "email-change-verification",
        //     data: { currentEmail, newEmail, confirmLink, expiresIn: "24 hours" }
        // );
    }

    /**
     * Notify OLD email that a change was requested.
     * Security notification — lets user know if they didn't initiate this.
     *
     * @param currentEmail Email hiện tại nhận thông báo
     * @param newEmail     Email mới đang chờ xác nhận
     */
    public void sendEmailChangeNotification(String currentEmail, String newEmail) {
        log.info("Sending email change notification to current email: {}", currentEmail);

        log.info("========================================");
        log.info("[DEV] Email Change Security Notification");
        log.info("[DEV] Notifying    : {}", currentEmail);
        log.info("[DEV] Pending change to : {}", newEmail);
        log.info("[DEV] If not you, contact support immediately");
        log.info("========================================");

        // TODO: Replace with real email provider
        // emailClient.send(
        //     to: currentEmail,
        //     subject: "Email change request initiated",
        //     template: "email-change-notification",
        //     data: { newEmail, supportLink }
        // );
    }
}
