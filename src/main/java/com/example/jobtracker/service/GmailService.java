package com.example.jobtracker.service;

import com.example.jobtracker.dto.GmailMessageDto;
import com.example.jobtracker.model.EmailOutcome;
import com.example.jobtracker.model.ProcessedGmailMessage;
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
import java.nio.charset.StandardCharsets;

@Service
public class GmailService {

    private static final String JOB_EMAIL_QUERY =
            "in:inbox newer_than:90d " +
            "{category:primary category:updates} " +
            JobEmailKeywords.asGmailSearchGroup();

    private static final String UPDATE_EMAIL_QUERY =
            "in:inbox newer_than:90d " +
            "{category:primary category:updates} " +
            "{\"next stage\" progressed update application}";

    private final GmailAuthService authService;
    private final ProcessedGmailMessageRepository processedMessageRepository;
    private final JobEmailDetector jobEmailDetector;
    private final UpdateEmailDetector updateEmailDetector;
    private final JobEmailClassifier jobEmailClassifier;

    public GmailService(
            GmailAuthService authService,
            ProcessedGmailMessageRepository processedMessageRepository,
            JobEmailDetector jobEmailDetector,
            UpdateEmailDetector updateEmailDetector,
            JobEmailClassifier jobEmailClassifier
    ) {
        this.authService = authService;
        this.processedMessageRepository = processedMessageRepository;
        this.jobEmailDetector = jobEmailDetector;
        this.updateEmailDetector = updateEmailDetector;
        this.jobEmailClassifier = jobEmailClassifier;
    }

    public List<GmailMessageDto> getRecentMessages()
            throws Exception {

        syncMessages(JOB_EMAIL_QUERY);

        return getSavedJobMessages();
    }

    public List<GmailMessageDto> getUpdateMessages()
            throws Exception {

        syncMessages(UPDATE_EMAIL_QUERY);

        return getSavedUpdateMessages();
    }

    private synchronized void syncMessages(String query)
            throws Exception {

        if (authService.getCredential() == null) {
            return;
        }

        Gmail gmail = authService.getGmailClient();

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
                    processedMessage.get()
                            .hasMessageData()) {
                refreshUpdateClassification(
                        processedMessage.get()
                );
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

            GmailMessageDto gmailMessage =
                    new GmailMessageDto(
                            message.getId(),
                            decodeHtml(getHeader(message, "From")),
                            decodeHtml(getHeader(message, "Subject")),
                            decodeHtml(message.getSnippet()),
                            message.getInternalDate(),
                            null
                    );

            boolean jobRelated =
                    jobEmailDetector.isJobRelated(
                            gmailMessage
                    );

            boolean updateRelated =
                    updateEmailDetector.isUpdateRelated(
                            gmailMessage
                    );

            EmailOutcome outcome =
                    jobEmailClassifier.detectOutcome(
                            gmailMessage.subject(),
                            extractFullBody(message.getPayload())
                    );

            processedMessageRepository.save(
                    new ProcessedGmailMessage(
                            gmailMessageId,
                            gmailMessage.from(),
                            gmailMessage.subject(),
                            gmailMessage.snippet(),
                            jobRelated,
                            gmailMessage.receivedAt(),
                            true,
                            updateRelated,
                            outcome,
                            true
                    )
            );
        }

    }

    private void refreshUpdateClassification(
            ProcessedGmailMessage message
    ) {
        GmailMessageDto savedMessage = new GmailMessageDto(
                message.getGmailMessageId(),
                message.getSender(),
                message.getSubject(),
                message.getSnippet(),
                message.getReceivedAt(),
                message.getOutcome()
        );

        boolean updateRelated =
                updateEmailDetector.isUpdateRelated(savedMessage);

        if (updateRelated !=
                Boolean.TRUE.equals(message.getUpdateRelated())) {
            message.setUpdateRelated(updateRelated);
            processedMessageRepository.save(message);
        }
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

        if (outcome == null) {
            GmailMessageDto unclassifiedMessage =
                    new GmailMessageDto(
                            message.getGmailMessageId(),
                            decodeHtml(message.getSender()),
                            decodeHtml(message.getSubject()),
                            decodeHtml(message.getSnippet()),
                            message.getReceivedAt(),
                            null
                    );

            outcome = jobEmailClassifier.detectOutcome(
                    unclassifiedMessage
            );
            message.setOutcome(outcome);
            processedMessageRepository.save(message);
        }

        return new GmailMessageDto(
                message.getGmailMessageId(),
                decodeHtml(message.getSender()),
                decodeHtml(message.getSubject()),
                decodeHtml(message.getSnippet()),
                message.getReceivedAt(),
                outcome
        );
    }

    private String decodeHtml(String value) {

        return value == null
                ? ""
                : HtmlUtils.htmlUnescape(value);
    }

    private String extractFullBody(MessagePart part) {

        if (part == null) {
            return "";
        }

        StringBuilder body = new StringBuilder();
        String mimeType = part.getMimeType();

        if (mimeType != null &&
                mimeType.startsWith("text/") &&
                part.getBody() != null &&
                part.getBody().getData() != null) {

            try {
                byte[] decoded = Base64.getUrlDecoder()
                        .decode(part.getBody().getData());

                body.append(
                        decodeHtml(
                                new String(
                                        decoded,
                                        StandardCharsets.UTF_8
                                )
                        )
                );
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed body sections and continue with other parts.
            }
        }

        if (part.getParts() != null) {
            for (MessagePart childPart : part.getParts()) {
                body.append(' ')
                        .append(extractFullBody(childPart));
            }
        }

        return body.toString();
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
