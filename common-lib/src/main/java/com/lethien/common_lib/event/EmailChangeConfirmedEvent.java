package com.lethien.common_lib.event;

import lombok.*;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

// email/EmailChangeConfirmedEvent.java
// Auth publish khi user click link xác nhận → đây là lúc User Service sync email
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailChangeConfirmedEvent implements Serializable {

    @Builder.Default private UUID eventId     = UUID.randomUUID();
    @Builder.Default private String eventType = "EMAIL_CHANGE_CONFIRMED";
    @Builder.Default private ZonedDateTime occurredAt = ZonedDateTime.now();

    private UUID accountId;
    private String oldEmail;
    private String newEmail;          // Email đã được xác nhận, chính thức
    private ZonedDateTime confirmedAt;

    public String idempotencyKey() {
        return eventType + ":" + accountId + ":" + newEmail;
    }

    public boolean isValid() {
        return accountId != null
                && newEmail != null && !newEmail.isBlank()
                && oldEmail != null && !oldEmail.isBlank()
                && !oldEmail.equals(newEmail);
    }
}
