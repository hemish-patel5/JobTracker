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

    @Column(columnDefinition = "TEXT")
    private String fullBody;

    private Boolean jobRelated;

    private Long receivedAt;

    private Boolean allowedCategory;

    private Boolean updateRelated;

    @Enumerated(EnumType.STRING)
    private EmailOutcome outcome;

    @Enumerated(EnumType.STRING)
    private EmailClassification classification;

    private String matchedPhrase;

    private Boolean fullBodyClassified;

    private Boolean notificationSent;

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
            String fullBody,
            boolean jobRelated,
            Long receivedAt,
            boolean allowedCategory,
            boolean updateRelated,
            EmailOutcome outcome,
            boolean fullBodyClassified
    ) {
        this.gmailMessageId = gmailMessageId;
        this.processedAt = LocalDateTime.now();
        this.sender = sender;
        this.subject = subject;
        this.snippet = snippet;
        this.fullBody = fullBody;
        this.jobRelated = jobRelated;
        this.receivedAt = receivedAt;
        this.allowedCategory = allowedCategory;
        this.updateRelated = updateRelated;
        this.outcome = outcome;
        this.fullBodyClassified = fullBodyClassified;
        this.notificationSent = false;
    }

    public ProcessedGmailMessage(
            String gmailMessageId,
            String sender,
            String subject,
            String snippet,
            boolean jobRelated,
            Long receivedAt,
            boolean allowedCategory,
            boolean updateRelated,
            EmailOutcome outcome,
            boolean fullBodyClassified
    ) {
        this(
                gmailMessageId,
                sender,
                subject,
                snippet,
                null,
                jobRelated,
                receivedAt,
                allowedCategory,
                updateRelated,
                outcome,
                fullBodyClassified
        );
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

    public String getFullBody() {
        return fullBody;
    }

    public void setFullBody(String fullBody) {
        this.fullBody = fullBody;
    }

    public boolean hasStoredFullBody() {
        return fullBody != null;
    }

    public Boolean getJobRelated() {
        return jobRelated;
    }

    public void setJobRelated(Boolean jobRelated) {
        this.jobRelated = jobRelated;
    }

    public Long getReceivedAt() {
        return receivedAt;
    }

    public Boolean getAllowedCategory() {
        return allowedCategory;
    }

    public Boolean getUpdateRelated() {
        return updateRelated;
    }

    public void setUpdateRelated(Boolean updateRelated) {
        this.updateRelated = updateRelated;
    }

    public EmailOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(EmailOutcome outcome) {
        this.outcome = outcome;
    }

    public EmailClassification getClassification() {
        return classification;
    }

    public void setClassification(EmailClassification classification) {
        this.classification = classification;
    }

    public String getMatchedPhrase() {
        return matchedPhrase;
    }

    public void setMatchedPhrase(String matchedPhrase) {
        this.matchedPhrase = matchedPhrase;
    }

    public Boolean getFullBodyClassified() {
        return fullBodyClassified;
    }

    public Boolean getNotificationSent() {
        return notificationSent;
    }

    public void markNotificationSent() {
        this.notificationSent = true;
    }

    public boolean hasMessageData() {
        return jobRelated != null &&
                receivedAt != null &&
                allowedCategory != null &&
                updateRelated != null &&
                outcome != null &&
                Boolean.TRUE.equals(fullBodyClassified);
    }
}
