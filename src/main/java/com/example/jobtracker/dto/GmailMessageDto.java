package com.example.jobtracker.dto;

import com.example.jobtracker.model.EmailOutcome;

public record GmailMessageDto(
        String id,
        String from,
        String subject,
        String snippet,
        Long receivedAt,
        EmailOutcome outcome
) {
}
