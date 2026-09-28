package com.example.jobtracker.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "processed_gmail_messages")
public class ProcessedGmailMessage {

    @Id
    private String gmailMessageId;

    @Column(nullable = false)
    private LocalDateTime processedAt;

    private String sender;

    private String subject;

    @Column(length = 4000)
    private String snippet;

    private Boolean jobRelated;

    public ProcessedGmailMessage() {
    }

    public ProcessedGmailMessage(String gmailMessageId) {
        this.gmailMessageId = gmailMessageId;
        this.processedAt = LocalDateTime.now();
    }

    public ProcessedGmailMessage(
            String gmailMessageId,
            String sender,
            String subject,
            String snippet,
            boolean jobRelated
    ) {
        this.gmailMessageId = gmailMessageId;
        this.processedAt = LocalDateTime.now();
        this.sender = sender;
        this.subject = subject;
        this.snippet = snippet;
        this.jobRelated = jobRelated;
    }

    public String getGmailMessageId() {
        return gmailMessageId;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public String getSender() {
        return sender;
    }

    public String getSubject() {
        return subject;
    }

    public String getSnippet() {
        return snippet;
    }

    public Boolean getJobRelated() {
        return jobRelated;
    }

    public boolean hasMessageData() {
        return jobRelated != null;
    }
}
