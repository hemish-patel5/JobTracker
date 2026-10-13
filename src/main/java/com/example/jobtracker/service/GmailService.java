package com.example.jobtracker.service;

import com.example.jobtracker.dto.GmailMessageDto;
import com.example.jobtracker.model.EmailOutcome;
import com.example.jobtracker.model.JobApplication;
import com.example.jobtracker.model.ProcessedGmailMessage;
import com.example.jobtracker.repository.JobApplicationRepository;
import com.example.jobtracker.repository.ProcessedGmailMessageRepository;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.Optional;
import java.util.Base64;
import java.util.Objects;
import java.nio.charset.StandardCharsets;

@Service
public class GmailService {

    private static final String JOB_EMAIL_QUERY =
            "in:inbox newer_than:90d " +
            "{category:primary category:updates} " +
            JobEmailKeywords.asGmailSearchGroup();

    private static final String UPDATE_EMAIL_QUERY = JOB_EMAIL_QUERY;

    private final GmailAuthService authService;
    private final ProcessedGmailMessageRepository processedMessageRepository;
    private final JobEmailDetector jobEmailDetector;
    private final UpdateEmailDetector updateEmailDetector;
    private final JobEmailClassifier jobEmailClassifier;
    private final JobApplicationRepository jobApplicationRepository;
    private final JobApplicationMatcher jobApplicationMatcher;

    public GmailService(
            GmailAuthService authService,
            ProcessedGmailMessageRepository processedMessageRepository,
            JobEmailDetector jobEmailDetector,
            UpdateEmailDetector updateEmailDetector,
            JobEmailClassifier jobEmailClassifier,
            JobApplicationRepository jobApplicationRepository,
            JobApplicationMatcher jobApplicationMatcher
    ) {
        this.authService = authService;
        this.processedMessageRepository = processedMessageRepository;
        this.jobEmailDetector = jobEmailDetector;
        this.updateEmailDetector = updateEmailDetector;
        this.jobEmailClassifier = jobEmailClassifier;
        this.jobApplicationRepository = jobApplicationRepository;
        this.jobApplicationMatcher = jobApplicationMatcher;
    }

    public List<GmailMessageDto> getRecentMessages()
            throws Exception {

        syncMessages(JOB_EMAIL_QUERY);
        refreshStoredClassifications();

        return getSavedJobMessages();
    }

    public List<GmailMessageDto> getUpdateMessages()
            throws Exception {

        syncMessages(UPDATE_EMAIL_QUERY);
        refreshStoredClassifications();

        return getSavedUpdateMessages();
    }

    private synchronized void syncMessages(String query)
            throws Exception {

        if (authService.getCredential() == null) {
            return;
        }

        Gmail gmail = authService.getGmailClient();
        List<JobApplication> applications =
                jobApplicationRepository.findAll();

        ListMessagesResponse response =
                gmail.users()
                        .messages()
                        .list("me")
                        .setQ(query)
                        .setMaxResults(100L)
                        .execute();

        if (response.getMessages() == null) {
            return;
        }

        for (Message messageReference :
                response.getMessages()) {

            String gmailMessageId =
                    messageReference.getId();

            Optional<ProcessedGmailMessage> processedMessage =
                    processedMessageRepository.findById(
                            gmailMessageId
                    );

            if (processedMessage.isPresent() &&
                    processedMessage.get().hasMessageData()) {
                continue;
            }

            Message message =
                    gmail.users()
                            .messages()
                            .get(
                                    "me",
                                    gmailMessageId
                            )
                            .setFormat("full")
                            .execute();

            String fullBody =
                    extractFullBody(message.getPayload());

            GmailMessageDto gmailMessage =
                    new GmailMessageDto(
                            message.getId(),
                            decodeHtml(getHeader(message, "From")),
                            decodeHtml(getHeader(message, "Subject")),
                            decodeHtml(message.getSnippet()),
                            fullBody,
                            message.getInternalDate(),
                            null
                    );

            boolean jobRelated =
                    jobEmailDetector.isJobRelated(
                            gmailMessage
                    );

            EmailClassificationResult classification =
                    jobEmailClassifier.classify(gmailMessage);

            boolean updateRelated =
                    matchesExistingApplication(
                            gmailMessage,
                            applications
                    ) && jobEmailClassifier.isUpdateClassification(
                            classification.classification()
                    );

            EmailOutcome outcome =
                    jobEmailClassifier.detectOutcome(
                            gmailMessage.subject(),
                            gmailMessage.fullBody()
                    );

            ProcessedGmailMessage savedMessage =
                    new ProcessedGmailMessage(
                            gmailMessageId,
                            gmailMessage.from(),
                            gmailMessage.subject(),
                            gmailMessage.snippet(),
                            gmailMessage.fullBody(),
                            jobRelated,
                            gmailMessage.receivedAt(),
                            true,
                            updateRelated,
                            outcome,
                            true
                    );

            savedMessage.setClassification(
                    classification.classification()
            );
            savedMessage.setMatchedPhrase(
                    classification.matchedPhrase()
            );
            processedMessageRepository.save(savedMessage);
        }

    }

    private void refreshStoredClassifications() {
        List<JobApplication> applications =
                jobApplicationRepository.findAll();

        processedMessageRepository.findAll()
                .forEach(message ->
                        refreshClassification(message, applications)
                );
    }

    public Optional<String> getMessageBody(
            String gmailMessageId
    ) throws Exception {

        Optional<ProcessedGmailMessage> savedMessage =
                processedMessageRepository.findById(gmailMessageId);

        if (savedMessage.isEmpty()) {
            return Optional.empty();
        }

        ProcessedGmailMessage message = savedMessage.get();

        if (message.hasStoredFullBody()) {
            String cleanedBody =
                    sanitizeBodyForDisplay(
                            message.getFullBody()
                    );

            if (!cleanedBody.equals(message.getFullBody())) {
                message.setFullBody(cleanedBody);
                processedMessageRepository.save(message);
            }

            return Optional.of(cleanedBody);
        }

        if (authService.getCredential() == null) {
            throw new IllegalStateException(
                    "Gmail is not connected"
            );
        }

        Message gmailMessage =
                authService.getGmailClient()
                        .users()
                        .messages()
                        .get("me", gmailMessageId)
                        .setFormat("full")
                        .execute();

        String fullBody =
                extractFullBody(gmailMessage.getPayload());

        message.setFullBody(fullBody);
        processedMessageRepository.save(message);

        return Optional.of(fullBody);
    }

    private void refreshClassification(
            ProcessedGmailMessage message,
            List<JobApplication> applications
    ) {
        GmailMessageDto savedMessage = new GmailMessageDto(
                message.getGmailMessageId(),
                message.getSender(),
                message.getSubject(),
                message.getSnippet(),
                message.getFullBody(),
                message.getReceivedAt(),
                message.getOutcome()
        );

        EmailClassificationResult classification =
                jobEmailClassifier.classify(savedMessage);
        boolean updateRelated =
                matchesExistingApplication(
                        savedMessage,
                        applications
                ) && updateEmailDetector.isUpdateRelated(savedMessage);
        boolean jobRelated =
                jobEmailDetector.isJobRelated(savedMessage);
        EmailOutcome outcome =
                jobEmailClassifier.detectOutcome(savedMessage);

        boolean changed =
                updateRelated != Boolean.TRUE.equals(message.getUpdateRelated()) ||
                jobRelated != Boolean.TRUE.equals(message.getJobRelated()) ||
                outcome != message.getOutcome() ||
                classification.classification() != message.getClassification() ||
                !Objects.equals(
                        classification.matchedPhrase(),
                        message.getMatchedPhrase()
                );

        if (changed) {
            message.setUpdateRelated(updateRelated);
            message.setJobRelated(jobRelated);
            message.setOutcome(outcome);
            message.setClassification(classification.classification());
            message.setMatchedPhrase(classification.matchedPhrase());
            processedMessageRepository.save(message);
        }
    }

    private boolean matchesExistingApplication(
            GmailMessageDto email,
            List<JobApplication> applications
    ) {
        return jobApplicationMatcher.findBestMatch(
                email,
                applications
        ) != null;
    }

    private List<GmailMessageDto> getSavedJobMessages() {

        return processedMessageRepository
                .findByJobRelatedTrueAndAllowedCategoryTrueOrderByProcessedAtDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    private List<GmailMessageDto> getSavedUpdateMessages() {

        return processedMessageRepository
                .findByUpdateRelatedTrueAndAllowedCategoryTrueOrderByProcessedAtDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    private GmailMessageDto toDto(
            ProcessedGmailMessage message
    ) {

        EmailOutcome outcome = message.getOutcome();
        GmailMessageDto savedMessage =
                new GmailMessageDto(
                        message.getGmailMessageId(),
                        decodeHtml(message.getSender()),
                        decodeHtml(message.getSubject()),
                        decodeHtml(message.getSnippet()),
                        decodeHtml(message.getFullBody()),
                        message.getReceivedAt(),
                        outcome
                );

        EmailOutcome savedTextOutcome =
                jobEmailClassifier.detectOutcome(
                        savedMessage.subject(),
                        savedMessage.fullBody()
                );

        if (outcome == null ||
                (outcome == EmailOutcome.SUCCESS &&
                        savedTextOutcome == EmailOutcome.REJECTION)) {
            outcome = savedTextOutcome;
            message.setOutcome(outcome);
            processedMessageRepository.save(message);
        }

        return new GmailMessageDto(
                message.getGmailMessageId(),
                decodeHtml(message.getSender()),
                decodeHtml(message.getSubject()),
                decodeHtml(message.getSnippet()),
                null,
                message.getReceivedAt(),
                outcome,
                message.getClassification(),
                message.getMatchedPhrase()
        );
    }

    private String decodeHtml(String value) {

        return value == null
                ? ""
                : HtmlUtils.htmlUnescape(value);
    }

    private String extractFullBody(MessagePart part) {

        String plainText = extractBodyByMimeType(
                part,
                "text/plain"
        );

        if (!plainText.isBlank()) {
            return normalizeBodyText(plainText);
        }

        String html = extractBodyByMimeType(
                part,
                "text/html"
        );

        return normalizeBodyText(
                htmlToText(html)
        );
    }

    private String extractBodyByMimeType(
            MessagePart part,
            String targetMimeType
    ) {

        if (part == null) {
            return "";
        }

        StringBuilder body = new StringBuilder();
        String mimeType = part.getMimeType();

        if (targetMimeType.equalsIgnoreCase(mimeType) &&
                part.getBody() != null &&
                part.getBody().getData() != null) {

            try {
                byte[] decoded = Base64.getUrlDecoder()
                        .decode(part.getBody().getData());

                body.append(
                        new String(
                                decoded,
                                StandardCharsets.UTF_8
                        )
                );
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed body sections and continue with other parts.
            }
        }

        if (part.getParts() != null) {
            for (MessagePart childPart : part.getParts()) {
                body.append(' ')
                        .append(
                                extractBodyByMimeType(
                                        childPart,
                                        targetMimeType
                                )
                        );
            }
        }

        return body.toString();
    }

    private String htmlToText(String html) {

        if (html == null || html.isBlank()) {
            return "";
        }

        return decodeHtml(html)
                .replaceAll(
                        "(?is)<(script|style)[^>]*>.*?</\\1>",
                        " "
                )
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p\\s*>", "\n\n")
                .replaceAll("(?s)<[^>]+>", " ");
    }

    private String normalizeBodyText(String value) {

        if (value == null || value.isBlank()) {
            return "";
        }

        String normalizedBody = decodeHtml(value)
                .replace('\u00a0', ' ')
                .replace("\u200B", "")
                .replace("\u200C", "")
                .replace("\u200D", "")
                .replace("\uFEFF", "")
                .replaceAll("[\\t\\x0B\\f\\r ]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();

        return sanitizeBodyForDisplay(normalizedBody);
    }

    private String sanitizeBodyForDisplay(String value) {

        if (value == null || value.isBlank()) {
            return "";
        }

        return value
                .replaceAll(
                        "(?i)(https?://[^\\s?]+)\\?[^\\s]+",
                        "$1"
                )
                .replaceAll(
                        "(?im)^\\s*(tracking(?:id)?|midtoken|tr[kc]|lipi|eid|otptoken)=[^\\n]*$",
                        ""
                )
                .replaceAll("[ ]+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String getHeader(
            Message message,
            String headerName
    ) {

        if (message.getPayload() == null ||
                message.getPayload()
                        .getHeaders() == null) {

            return "";
        }

        return message
                .getPayload()
                .getHeaders()
                .stream()
                .filter(header ->
                        headerName.equalsIgnoreCase(
                                header.getName()
                        )
                )
                .map(header ->
                        header.getValue()
                )
                .findFirst()
                .orElse("");
    }
}
