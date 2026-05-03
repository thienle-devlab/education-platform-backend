package com.lethien.common_lib.event;

import lombok.*;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

// email/EmailChangeCancelledEvent.java
// Auth publish khi link hết hạn hoặc user huỷ yêu cầu
// User Service dùng để rollback trạng thái pending nếu có lưu
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailChangeCancelledEvent implements Serializable {

    @Builder.Default private UUID eventId     = UUID.randomUUID();
    @Builder.Default private String eventType = "EMAIL_CHANGE_CANCELLED";
    @Builder.Default private ZonedDateTime occurredAt = ZonedDateTime.now();

    private UUID accountId;
    private String cancelledEmail;    // Email bị huỷ (requestedEmail)
    private String reason;            // "EXPIRED" | "USER_CANCELLED"

    public String idempotencyKey() {
        return eventType + ":" + accountId + ":" + cancelledEmail;
    }
}
