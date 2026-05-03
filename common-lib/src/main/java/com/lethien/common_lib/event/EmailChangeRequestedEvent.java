package com.lethien.common_lib.event;

import lombok.*;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

// email/EmailChangeRequestedEvent.java
// Auth publish khi user submit form đổi email — chưa confirm
// User Service KHÔNG sync email ở bước này, chỉ có thể notify UI nếu cần
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailChangeRequestedEvent implements Serializable {
    @Builder.Default private UUID eventId = UUID.randomUUID();
    @Builder.Default private String eventType = "EMAIL_CHANGE_REQUESTED";
    @Builder.Default private ZonedDateTime occurredAt = ZonedDateTime.now();

    private UUID accountId;
    private String currentEmail;
    private String requestedEmail;    // Email mới muốn đổi sang
    private ZonedDateTime expiresAt;  // Link verification hết hạn lúc nào

    public String idempotencyKey() {
        return eventType + ":" + accountId + ":" + requestedEmail;
    }

    public boolean isValid() {
        return accountId != null
                && requestedEmail != null && !requestedEmail.isBlank()
                && currentEmail != null && !currentEmail.isBlank()
                && !currentEmail.equals(requestedEmail);
    }
}
