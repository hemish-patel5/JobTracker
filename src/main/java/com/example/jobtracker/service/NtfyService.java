package com.example.jobtracker.service;

import com.example.jobtracker.model.ProcessedGmailMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class NtfyService {

    private final HttpClient httpClient;
    private final URI topicUri;

    public NtfyService(
            @Value("${ntfy.url}") String ntfyUrl,
            @Value("${ntfy.topic}") String ntfyTopic
    ) {
        if (!ntfyTopic.matches("[-_A-Za-z0-9]{1,64}")) {
            throw new IllegalArgumentException(
                    "NTFY_TOPIC must contain only letters, numbers, hyphens, or underscores"
            );
        }

        this.httpClient = HttpClient.newHttpClient();
        this.topicUri = URI.create(
                ntfyUrl.replaceFirst("/+$", "") + "/" + ntfyTopic
        );
    }

    public void sendApplicationUpdate(
            ProcessedGmailMessage message
    ) throws IOException, InterruptedException {

        String notificationBody = String.format(
                "Outcome: %s%n%s%nFrom: %s",
                message.getOutcome(),
                message.getSubject(),
                message.getSender()
        );

        HttpRequest request = HttpRequest.newBuilder(topicUri)
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "text/plain; charset=utf-8")
                .header("Title", "BetterTracker application update")
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                notificationBody,
                                StandardCharsets.UTF_8
                        )
                )
                .build();

        HttpResponse<Void> response;
        try {
            response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.discarding()
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw exception;
        }

        if (response.statusCode() < 200 ||
                response.statusCode() >= 300) {
            throw new IOException(
                    "ntfy returned HTTP " + response.statusCode()
            );
        }
    }
}
