package com.example.jobtracker.dto;

import com.example.jobtracker.model.EmailOutcome;
import com.example.jobtracker.model.EmailClassification;

public record GmailMessageDto(
        String id,
        String from,
        String subject,
        String snippet,
        String fullBody,
        Long receivedAt,
        EmailOutcome outcome,
        EmailClassification classification,
        String matchedPhrase
) {
    public GmailMessageDto(
            String id,
            String from,
            String subject,
            String snippet,
            String fullBody,
            Long receivedAt,
            EmailOutcome outcome
    ) {
        this(
                id,
                from,
                subject,
                snippet,
                fullBody,
                receivedAt,
                outcome,
                null,
                null
        );
    }

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
                outcome,
                null,
                null
        );
    }
}
