package com.example.jobtracker.dto;

public record GmailMessageDto(
        String id,
        String from,
        String subject,
        String snippet
) {
}
