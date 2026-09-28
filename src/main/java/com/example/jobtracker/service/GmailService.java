package com.example.jobtracker.service;

import com.example.jobtracker.dto.GmailMessageDto;
import com.example.jobtracker.model.ProcessedGmailMessage;
import com.example.jobtracker.repository.ProcessedGmailMessageRepository;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GmailService {

    private final GmailAuthService authService;
    private final ProcessedGmailMessageRepository processedMessageRepository;
    private final JobEmailDetector jobEmailDetector;

    public GmailService(
            GmailAuthService authService,
            ProcessedGmailMessageRepository processedMessageRepository,
            JobEmailDetector jobEmailDetector
    ) {
        this.authService = authService;
        this.processedMessageRepository = processedMessageRepository;
        this.jobEmailDetector = jobEmailDetector;
    }

    public List<GmailMessageDto> getRecentMessages()
            throws Exception {

        Credential credential =
                authService.getCredential();

        if (credential == null) {
            return getSavedJobMessages();
        }

        Gmail gmail =
                new Gmail.Builder(
                        GoogleNetHttpTransport
                                .newTrustedTransport(),
                        GsonFactory
                                .getDefaultInstance(),
                        credential
                )
                        .setApplicationName(
                                "JobTracker"
                        )
                        .build();

        ListMessagesResponse response =
                gmail.users()
                        .messages()
                        .list("me")
                        .setQ("in:inbox newer_than:30d {category:primary category:updates}")
                        .setMaxResults(20L)
                        .execute();

        if (response.getMessages() == null) {
            return getSavedJobMessages();
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
                continue;
            }

            Message message =
                    gmail.users()
                            .messages()
                            .get(
                                    "me",
                                    gmailMessageId
                            )
                            .setFormat("metadata")
                            .setMetadataHeaders(
                                    List.of(
                                            "From",
                                            "Subject"
                                    )
                            )
                            .execute();

            GmailMessageDto gmailMessage =
                    new GmailMessageDto(
                            message.getId(),
                            getHeader(message, "From"),
                            getHeader(message, "Subject"),
                            message.getSnippet()
                    );

            boolean jobRelated =
                    jobEmailDetector.isJobRelated(
                            gmailMessage
                    );

            processedMessageRepository.save(
                    new ProcessedGmailMessage(
                            gmailMessageId,
                            gmailMessage.from(),
                            gmailMessage.subject(),
                            gmailMessage.snippet(),
                            jobRelated
                    )
            );
        }

        return getSavedJobMessages();
    }

    private List<GmailMessageDto> getSavedJobMessages() {

        return processedMessageRepository
                .findByJobRelatedTrueOrderByProcessedAtDesc()
                .stream()
                .map(message ->
                        new GmailMessageDto(
                                message.getGmailMessageId(),
                                message.getSender(),
                                message.getSubject(),
                                message.getSnippet()
                        )
                )
                .toList();
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
