package com.example.jobtracker.service;

import com.example.jobtracker.model.ProcessedGmailMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final Logger LOGGER =
            LoggerFactory.getLogger(NtfyService.class);
    private static final int MAX_ATTEMPTS = 4;
    private static final long INITIAL_RETRY_DELAY_SECONDS = 5;

    private final HttpClient httpClient;
    private final URI topicUri;
    private final RetrySleeper retrySleeper;

    @Autowired
    public NtfyService(
            @Value("${ntfy.url}") String ntfyUrl,
            @Value("${ntfy.topic}") String ntfyTopic
    ) {
        this(
                HttpClient.newHttpClient(),
                createTopicUri(ntfyUrl, ntfyTopic),
                delay -> Thread.sleep(delay.toMillis())
        );
    }

    NtfyService(
            HttpClient httpClient,
            URI topicUri,
            RetrySleeper retrySleeper
    ) {
        this.httpClient = httpClient;
        this.topicUri = topicUri;
        this.retrySleeper = retrySleeper;
    }

    private static URI createTopicUri(
            String ntfyUrl,
            String ntfyTopic
    ) {
        if (!ntfyTopic.matches("[-_A-Za-z0-9]{1,64}")) {
            throw new IllegalArgumentException(
                    "NTFY_TOPIC must contain only letters, numbers, hyphens, or underscores"
            );
        }

        return URI.create(
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
                .header("Title", "Job Update")
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                notificationBody,
                                StandardCharsets.UTF_8
                        )
                )
                .build();

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            HttpResponse<Void> response = send(request);

            if (response.statusCode() >= 200 &&
                    response.statusCode() < 300) {
                return;
            }

            if (response.statusCode() != 429 ||
                    attempt == MAX_ATTEMPTS) {
                throw new IOException(
                        "ntfy returned HTTP " + response.statusCode() +
                                " after " + attempt + " attempt(s)"
                );
            }

            long delaySeconds =
                    INITIAL_RETRY_DELAY_SECONDS << (attempt - 1);

            LOGGER.warn(
                    "ntfy rate limited notification; retrying in {} seconds (attempt {}/{})",
                    delaySeconds,
                    attempt + 1,
                    MAX_ATTEMPTS
            );

            sleepBeforeRetry(Duration.ofSeconds(delaySeconds));
        }
    }

    private HttpResponse<Void> send(HttpRequest request)
            throws IOException, InterruptedException {
        try {
            return httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.discarding()
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw exception;
        }
    }

    private void sleepBeforeRetry(Duration delay)
            throws InterruptedException {
        try {
            retrySleeper.sleep(delay);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw exception;
        }
    }

    @FunctionalInterface
    interface RetrySleeper {

        void sleep(Duration delay)
                throws InterruptedException;
    }
}
