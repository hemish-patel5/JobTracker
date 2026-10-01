package com.example.jobtracker.service;

public record GmailUpdateResult(
        int updateEmailCount,
        int notificationsSent
) {
}
