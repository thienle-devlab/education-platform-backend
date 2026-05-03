package com.lethien.auth_service.service;

import com.lethien.auth_service.dto.*;
import com.lethien.auth_service.entity.*;
import com.lethien.auth_service.exception.*;
import com.lethien.auth_service.repository.AccountRepository;
import com.lethien.auth_service.repository.LoginAttemptRepository;
import com.lethien.auth_service.repository.LoginSessionRepository;
import com.lethien.auth_service.repository.RefreshTokenRepository;
import com.lethien.common_lib.event.AccountCreatedEvent;
import com.lethien.common_lib.event.EmailChangeCancelledEvent;
import com.lethien.common_lib.event.EmailChangeConfirmedEvent;
import com.lethien.common_lib.event.EmailChangeRequestedEvent;
import com.lethien.common_lib.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import com.lethien.auth_service.exception.AccountLockedException;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Service for authentication operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AccountRepository accountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginSessionRepository loginSessionRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final EventPublisherService eventPublisherService;

    // Remember me: 30 days instead of 7 days
    private static final long REMEMBER_ME_REFRESH_EXPIRATION = 30L * 24 * 60 * 60 * 1000; // 30 days

    /**
     * User login
     */
    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        log.info("Login attempt for email: {}", request.getEmail());

        // 1. Find account by email
        Account account = accountRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        // 2. Check if account is locked
        LoginAttempt loginAttempt = loginAttemptRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (loginAttempt != null && loginAttempt.isLocked()) {
            long remainingMinutes = loginAttempt.getRemainingLockMinutes();
            throw new AccountLockedException(
                    "Account is locked due to too many failed login attempts. " +
                            "Please try again in " + remainingMinutes + " minutes."
            );
        }

        // 3. Verify password
        if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            // Increment failed login attempts
            incrementLoginAttempts(request.getEmail(), account.getId());
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // 4. Check account status
        if (!account.isActive()) {
            if (account.getStatus() == AccountStatus.PENDING) {
                throw new AccountNotVerifiedException("Please verify your email first");
            } else if (account.getStatus() == AccountStatus.LOCKED) {
                throw new AccountLockedException("Your account has been locked. Please contact support.");
            } else if (account.getStatus() == AccountStatus.SUSPENDED) {
                throw new AccountSuspendedException("Your account has been suspended. Please contact support.");
            }
        }

        // 5. Reset login attempts on successful login
        if (loginAttempt != null) {
            loginAttempt.reset();
            loginAttemptRepository.save(loginAttempt);
        }

        // 6. Generate JWT tokens
        String accessToken = jwtService.generateAccessToken(account.getId(), account.getEmail());

        Boolean rememberMe = request.getRememberMe();
        String refreshTokenString = (rememberMe != null && rememberMe)
                ? jwtService.generateRefreshToken(account.getId(), account.getEmail(), REMEMBER_ME_REFRESH_EXPIRATION)
                : jwtService.generateRefreshToken(account.getId(), account.getEmail());

        // 7. Save refresh token
        Long refreshExpiration = (rememberMe != null && rememberMe)
                ? REMEMBER_ME_REFRESH_EXPIRATION
                : jwtService.getExpirationTime(refreshTokenString);

        RefreshToken refreshToken = RefreshToken.builder()
                .account(account)
                .token(refreshTokenString)
                .expiredAt(ZonedDateTime.now().plusSeconds(refreshExpiration / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        // 8. Create login session
        LoginSession session = LoginSession.builder()
                .account(account)
                .deviceInfo(userAgent)
                .ipAddress(ipAddress)
                .isActive(true)
                .build();
        loginSessionRepository.save(session);

        log.info("Login successful for email: {}", request.getEmail());

        return LoginResponse.builder()
                .accountId(account.getId())
                .email(account.getEmail())
                .status(account.getStatus().name())
                .accessToken(accessToken)
                .refreshToken(refreshTokenString)
                .expiresIn(jwtService.getExpirationTime(accessToken))
                .loginAt(ZonedDateTime.now())
                .build();
    }

    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        log.info("Refreshing access token");

        if (!jwtService.validateToken((request.getRefreshToken()))) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (!refreshToken.isValid()) {
            throw new InvalidTokenException("Refresh token is expired or revoked");
        }

        Account account = refreshToken.getAccount();

        if (!account.isActive()) {
            throw new AccountNotActiveException("Account is not active");
        }

        // ✅ Bước 1: Revoke refresh token cũ
        refreshToken.revoke();  // dùng method revoke() có sẵn giống logout
        refreshTokenRepository.save(refreshToken);

        // ✅ Bước 2: Tạo access token mới
        String newAccessToken = jwtService.generateAccessToken(account.getId(), account.getEmail());


        // ✅ Bước 3: Tạo refresh token mới
        String newRefreshTokenValue = jwtService.generateRefreshToken(account.getId(), account.getEmail());
        RefreshToken newRefreshToken = RefreshToken.builder()
                .account(account)
                .token(newRefreshTokenValue)
                .expiredAt(ZonedDateTime.now().plusSeconds(
                        jwtService.getExpirationTime(newRefreshTokenValue) / 1000
                ))
                .revoked(false)
                .build();
        refreshTokenRepository.save(newRefreshToken);

        log.info("Access token refreshed for email: {}", account.getEmail());

        // ✅ Bước 4: Trả về token mới
        return LoginResponse.builder()
                .accountId(account.getId())
                .email(account.getEmail())
                .status(account.getStatus().name())
                .accessToken(newAccessToken)
                .refreshToken(newRefreshTokenValue)
                .expiresIn(jwtService.getExpirationTime(newAccessToken))
                .loginAt(ZonedDateTime.now())
                .build();
    }

    /**
     * Logout (revoke refresh token)
     */
    @Transactional
    public void logout(String refreshTokenString) {
        log.info("Logout attempt");

        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        // Revoke token
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);

        // Deactivate session (optional - can find session by account and timestamp)
        log.info("Logout successful for account: {}", refreshToken.getAccount().getEmail());
    }

    /**
     * Logout from all devices
     */
    @Transactional
    public void logoutFromAllDevices(UUID accountId) {
        log.info("Logout from all devices for account: {}", accountId);

        ZonedDateTime now = ZonedDateTime.now();

        // Revoke all refresh tokens
        int revokeTokens = refreshTokenRepository.revokeAllTokensByAccountId(accountId, now);

        // Deactivate all sessions
        int deactivatedSessions = loginSessionRepository.deactivateAllSessionsByAccountId(accountId, now);

        log.info("Revoked {} tokens and deactivated {} sessions", revokeTokens, deactivatedSessions);
    }

    /**
     * Increment failed login attempts
     */
    private void incrementLoginAttempts(String email, UUID accountId) {
        LoginAttempt loginAttempt = loginAttemptRepository.findByEmail(email)
                .orElse(LoginAttempt.builder()
                        .email(email)
                        .attemptCount(0)
                        .build());

        loginAttempt.incrementAttempt();
        loginAttemptRepository.save(loginAttempt);

        log.warn("Failed login attempt for email: {}. Count: {}", email, loginAttempt.getAttemptCount());

        // If locked, optionally send email notification
        if (loginAttempt.isLocked()) {
            log.warn("Account locked for email: {}", email);
            // emailService.sendAccountLockedEmail(email);
        }
    }

    /**
     * Register new account
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request){
        log.info("Registering new account for email: {}", request.getEmail());

        // 1. Validate password match
        if (!request.isPasswordMatching()) {
            throw new PasswordMismatchException("Password and confirm password do not match");
        }

        // 2. Check if email already exists
        if (accountRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists: " + request.getEmail());
        }

        // 3. Create verification token
        String verificationToken = UUID.randomUUID().toString();
        ZonedDateTime verificationExpiry = ZonedDateTime.now().plusHours(24);

        // 4. Create account
        Account account = Account.builder()
                .email(request.getEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status(AccountStatus.PENDING)
                .emailVerified(false)
                .verificationToken(verificationToken)
                .verificationExpiredAt(verificationExpiry)
                .build();

        // 5. Save account
        Account savedAccount = accountRepository.save(account);
        log.info("Account created successfully with ID: {}", savedAccount.getId());

        // 6. Send verification email (async)
        try {
            emailService.sendVerificationEmail(
                    savedAccount.getEmail(),
                    savedAccount.getVerificationToken(),
                    request.getFullName()

            );
            log.info("Verification email sent to: {}", savedAccount.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", savedAccount.getEmail(), e);
            // Don't fail registration if email fails
        }

        // 7. ← Publish AccountCreatedEvent to Kafka
        // Note: event is published AFTER DB commit is guaranteed (end of @Transactional).
        // If Kafka is down here, the event will be lost — see EventPublisherService for details.
        AccountCreatedEvent event = AccountCreatedEvent.builder()
                .accountId(savedAccount.getId())
                .email(savedAccount.getEmail())
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .accountCreateAt(savedAccount.getCreatedAt())
                .occurredAt(ZonedDateTime.now())
                .build();
        // eventId và eventType tự được set bởi @Builder.Default trong AccountCreatedEvent
        eventPublisherService.publishAccountCreatedEvent(event);

        // 8. Build response
        return RegisterResponse.builder()
                .accountId(savedAccount.getId())
                .email(savedAccount.getEmail())
                .status(savedAccount.getStatus().name())
                .emailVerified(savedAccount.getEmailVerified())
                .fullName(request.getFullName())
                .createdAt(savedAccount.getCreatedAt())
                .message("Registration successful! Please check your email to verify your account.")
                .build();
    }

    /**
     * Verify email with token
     */
    @Transactional
    public void verifyEmail(String token){
        log.info("Verifying email with token: {}", token);

        Account account = accountRepository.findByVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        // Check if token expired
        if (account.getVerificationExpiredAt() != null &&
                account.getVerificationExpiredAt().isBefore(ZonedDateTime.now())) {
            throw new IllegalArgumentException("Verification token has expired");
        }

        // Activate account
        account.activate();
        accountRepository.save(account);

        log.info("Email verified successfully for: {}", account.getEmail());
    }

    /**
     * Resend verification email
     */
    @Transactional
    public void resendVerificationEmail(String email){
        log.info("Resending verification email to: {}", email);

        Account account = accountRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        // Check if already verified
        if (account.getEmailVerified()) {
            throw new IllegalStateException("Email already verified");
        }

        // Generate new token if expired
        if (account.getVerificationExpiredAt() == null ||
                account.getVerificationExpiredAt().isBefore(ZonedDateTime.now())) {
            account.setVerificationToken(UUID.randomUUID().toString());
            account.setVerificationExpiredAt(ZonedDateTime.now().plusHours(24));
            accountRepository.save(account);
        }

        // Resend email
        emailService.sendVerificationEmail(
                account.getEmail(),
                account.getVerificationToken(),
                email // Use email as name for now
        );

        // Thông báo bảo mật tới email cũ — phòng trường hợp bị chiếm tài khoản
        emailService.sendEmailChangeNotification(
                account.getEmail(),
                account.getPendingEmail()
        );

        log.info("Verification email resent to: {}", email);
    }

    /**
     * Step 1: Request email change
     * User cung cấp email mới + password hiện tại để xác nhận danh tính
     */
    @Transactional
    public EmailChangeResponse requestEmailChange(UUID accountId, EmailChangeRequest request) {
        log.info("Email change requested: accountId={}", accountId);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));

        // 1. Xác nhận mật khẩu — tránh kẻ khác đổi email nếu quên đăng xuất
        if (!passwordEncoder.matches(request.getCurrentPassword(), account.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        // 2. Không cho đổi sang email đang dùng
        if (account.getEmail().equalsIgnoreCase(request.getNewEmail())) {
            throw new IllegalArgumentException("New email must be different from current email");
        }

        // 3. Kiểm tra email mới chưa được dùng bởi account khác
        if (accountRepository.existsByEmailIgnoreCase(request.getNewEmail())) {
            throw new EmailAlreadyExistsException("Email already exists: " + request.getNewEmail());
        }

        // 4. Kiểm tra email mới không đang pending ở account khác
        if (accountRepository.existsByPendingEmail(request.getNewEmail())) {
            throw new EmailAlreadyExistsException("Email is already pending for another account");
        }

        // 5. Tạo token — theo đúng pattern verificationToken đang có
        String token = UUID.randomUUID().toString();
        ZonedDateTime expiredAt = ZonedDateTime.now().plusHours(24);

        account.requestEmailChange(request.getNewEmail(), token, expiredAt);
        accountRepository.save(account);

        // 6. Gửi email xác nhận tới email MỚI — theo pattern sendVerificationEmail
        try {
            emailService.sendEmailChangeVerification(
                    request.getNewEmail(),
                    token,
                    account.getEmail() // Thông báo email cũ đang được thay thế
            );
            log.info("Email change verification sent to: {}", request.getNewEmail());
        } catch (Exception e) {
            log.error("Failed to send email change verification to: {}", request.getNewEmail(), e);
            // Không fail request — user có thể resend
        }

        // 7. Publish event — theo đúng pattern publishAccountCreatedEvent
        eventPublisherService.publishEmailChangeRequestedEvent(
                EmailChangeRequestedEvent.builder()
                        .accountId(accountId)
                        .currentEmail(account.getEmail())
                        .requestedEmail(request.getNewEmail())
                        .expiresAt(expiredAt)
                        .build()
        );

        return EmailChangeResponse.builder()
                .message("Verification email sent to " + request.getNewEmail() +
                        ". Please check your inbox to confirm.")
                .pendingEmail(request.getNewEmail())
                .expiredAt(expiredAt)
                .build();
    }

    /**
     * Step 2: Confirm email change
     * User click link trong email → token được verify → email chính thức đổi
     */
    @Transactional
    public void confirmEmailChange (String token) {
        log.info("Confirming email change with token: {}", token);

        Account account = accountRepository.findByEmailChangeToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid email change token"));

        // 1. Kiểm tra token còn hạn — theo pattern verifyEmail()
        if (account.isEmailChangeTokenExpired()) {
            // Auto cancel nếu expired
            String cancelledEmail = account.getPendingEmail();
            account.cancelEmailChange();
            accountRepository.save(account);

            // Publish CANCELLED event
            eventPublisherService.publishEmailChangeCancelledEvent(
                    EmailChangeCancelledEvent.builder()
                            .accountId(account.getId())
                            .cancelledEmail(cancelledEmail)
                            .reason("EXPIRED")
                            .build()
            );
            throw new InvalidTokenException("Email change token has expired. Please request again.");
        }
        String oldEmail   = account.getEmail();
        String newEmail   = account.getPendingEmail();

        // 2. Confirm — dùng business method trên entity
        account.confirmEmailChange();
        accountRepository.save(account);

        log.info("Email changed successfully: accountId={}, oldEmail={}, newEmail={}",
                account.getId(), oldEmail, newEmail);

        // 3. Publish CONFIRMED — User Service sẽ sync email tại đây
        eventPublisherService.publishEmailChangeConfirmedEvent(
                EmailChangeConfirmedEvent.builder()
                        .accountId(account.getId())
                        .oldEmail(oldEmail)
                        .newEmail(newEmail)
                        .confirmedAt(ZonedDateTime.now())
                        .build()
        );

        // 4. Revoke tất cả refresh token cũ — bắt user login lại với email mới
        int revoked = refreshTokenRepository.revokeAllTokensByAccountId(account.getId(), ZonedDateTime.now());
        log.info("Revoked {} refresh tokens after email change: accountId={}", revoked, account.getId());
    }

    /**
     * Step 3: Cancel email change (user chủ động huỷ)
     */
    public void cancelEmailChange (UUID accountId) {
        log.info("Cancelling email change: accountId={}", accountId);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));

        if (!account.hasEmailChangePending()) {
            throw new IllegalStateException("No pending email change request found");
        }

        String cancelledEmail = account.getPendingEmail();
        account.cancelEmailChange();
        accountRepository.save(account);

        // Publish CANCELLED event
        eventPublisherService.publishEmailChangeCancelledEvent(
                EmailChangeCancelledEvent.builder()
                        .accountId(accountId)
                        .cancelledEmail(cancelledEmail)
                        .reason("USER_CANCELLED")
                        .build()
        );
        log.info("Email change cancelled: accountId={}, cancelledEmail={}", accountId, cancelledEmail);
    }

    /**
     * Resend email change verification (token còn hạn → gửi lại, hết hạn → tạo token mới)
     * Theo đúng pattern resendVerificationEmail() hiện tại
     */
    @Transactional
    public EmailChangeResponse resendEmailChangeVerification (UUID accountId) {
        log.info("Resending email change verification: accountId={}", accountId);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));

        if (!account.hasEmailChangePending()) {
            throw new IllegalStateException("No pending email change request found");
        }

        // Tạo token mới nếu đã expired
        if (account.isEmailChangeTokenExpired()) {
            account.requestEmailChange(
                    account.getPendingEmail(),
                    UUID.randomUUID().toString(),
                    ZonedDateTime.now().plusHours(24)
            );
            accountRepository.save(account);
        }

        emailService.sendEmailChangeVerification(
                account.getPendingEmail(),
                account.getEmailChangeToken(),
                account.getEmail()
        );


        log.info("Email change verification resent to: {}", account.getPendingEmail());

        return EmailChangeResponse.builder()
                .message("Verification email resent to " + account.getPendingEmail())
                .pendingEmail(account.getPendingEmail())
                .expiredAt(account.getEmailChangeExpiredAt())
                .build();
    }
}
