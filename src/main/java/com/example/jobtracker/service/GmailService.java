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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
            throw new IllegalStateException(
                    "Gmail is not connected"
            );
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
            return Collections.emptyList();
        }

        List<GmailMessageDto> messages =
                new ArrayList<>();

        for (Message messageReference :
                response.getMessages()) {

            String gmailMessageId =
                    messageReference.getId();

            if (processedMessageRepository
                    .existsById(gmailMessageId)) {
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

            if (jobEmailDetector.isJobRelated(
                    gmailMessage
            )) {
                messages.add(gmailMessage);
            }

            processedMessageRepository.save(
                    new ProcessedGmailMessage(
                            gmailMessageId
                    )
            );
        }

        return messages;
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
