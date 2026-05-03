package com.lethien.auth_service.dto;

import lombok.*;

import java.time.ZonedDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailChangeResponse {

    private String message;
    private String pendingEmail;
    private ZonedDateTime expiredAt;
}
