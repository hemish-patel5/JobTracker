package com.example.jobtracker.dto;

import com.example.jobtracker.model.EmailOutcome;

public record GmailMessageDto(
        String id,
        String from,
        String subject,
        String snippet,
        String fullBody,
        Long receivedAt,
        EmailOutcome outcome
) {
    public GmailMessageDto(
            String id,
            String from,
            String subject,
            String snippet,
            Long receivedAt,
            EmailOutcome outcome
    ) {
        this(
                id,
                from,
                subject,
                snippet,
                null,
                receivedAt,
                outcome
        );
    }
}
