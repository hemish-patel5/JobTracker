package com.example.jobtracker.service;

import com.example.jobtracker.model.EmailOutcome;
import com.example.jobtracker.model.ProcessedGmailMessage;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NtfyServiceTest {

    @Test
    void retriesRateLimitedRequestsUntilSuccessful()
            throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<Void> rateLimited = response(429);
        HttpResponse<Void> successful = response(200);
        List<Duration> delays = new ArrayList<>();

        when(httpClient.<Void>send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)
        ))
                .thenReturn(rateLimited, rateLimited, successful);

        NtfyService service = new NtfyService(
                httpClient,
                URI.create("https://ntfy.sh/test-topic"),
                delays::add
        );

        service.sendApplicationUpdate(message());

        verify(httpClient, times(3)).send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)
        );
        assertThat(delays).containsExactly(
                Duration.ofSeconds(5),
                Duration.ofSeconds(10)
        );
    }

    @Test
    void stopsAfterTheMaximumNumberOfRateLimitedAttempts()
            throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<Void> rateLimited = response(429);
        List<Duration> delays = new ArrayList<>();

        when(httpClient.<Void>send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)
        ))
                .thenReturn(rateLimited);

        NtfyService service = new NtfyService(
                httpClient,
                URI.create("https://ntfy.sh/test-topic"),
                delays::add
        );

        assertThatThrownBy(
                () -> service.sendApplicationUpdate(message())
        )
                .isInstanceOf(java.io.IOException.class)
                .hasMessage("ntfy returned HTTP 429 after 4 attempt(s)");

        verify(httpClient, times(4)).send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)
        );
        assertThat(delays).containsExactly(
                Duration.ofSeconds(5),
                Duration.ofSeconds(10),
                Duration.ofSeconds(20)
        );
    }

    @Test
    void doesNotRetryPermanentErrors()
            throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<Void> forbidden = response(403);
        List<Duration> delays = new ArrayList<>();

        when(httpClient.<Void>send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)
        ))
                .thenReturn(forbidden);

        NtfyService service = new NtfyService(
                httpClient,
                URI.create("https://ntfy.sh/test-topic"),
                delays::add
        );

        assertThatThrownBy(
                () -> service.sendApplicationUpdate(message())
        )
                .isInstanceOf(java.io.IOException.class)
                .hasMessage("ntfy returned HTTP 403 after 1 attempt(s)");

        verify(httpClient).send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)
        );
        assertThat(delays).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<Void> response(int status) {
        HttpResponse<Void> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        return response;
    }

    private ProcessedGmailMessage message() {
        return new ProcessedGmailMessage(
                "gmail-id",
                "jobs@example.com",
                "Application update",
                "Your application has progressed",
                true,
                1L,
                true,
                true,
                EmailOutcome.SUCCESS,
                true
        );
    }
}
