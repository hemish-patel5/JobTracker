package com.example.jobtracker.service;

import com.example.jobtracker.dto.GmailMessageDto;
import com.example.jobtracker.model.EmailOutcome;
import com.example.jobtracker.model.ProcessedGmailMessage;
import com.example.jobtracker.repository.ProcessedGmailMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GmailUpdateServiceTest {

    @Mock
    private GmailService gmailService;

    @Mock
    private ProcessedGmailMessageRepository repository;

    @Mock
    private NtfyService ntfyService;

    @Test
    void sendsAndRecordsPendingNotifications()
            throws Exception {

        ProcessedGmailMessage message =
                new ProcessedGmailMessage(
                        "gmail-id",
                        "jobs@example.com",
                        "Application update",
                        "We have an update",
                        true,
                        1L,
                        true,
                        true,
                        EmailOutcome.SUCCESS,
                        true
                );

        when(gmailService.getUpdateMessages())
                .thenReturn(List.of(
                        new GmailMessageDto(
                                "gmail-id",
                                "jobs@example.com",
                                "Application update",
                                "We have an update",
                                1L,
                                EmailOutcome.SUCCESS
                        )
                ));
        when(repository
                .findByUpdateRelatedTrueAndAllowedCategoryTrueAndNotificationSentFalseOrderByProcessedAtAsc())
                .thenReturn(List.of(message));

        GmailUpdateResult result =
                new GmailUpdateService(
                        gmailService,
                        repository,
                        ntfyService
                ).checkForUpdates();

        verify(ntfyService)
                .sendApplicationUpdate(message);
        verify(repository)
                .save(message);
        assertThat(message.getNotificationSent())
                .isTrue();
        assertThat(result.updateEmailCount())
                .isEqualTo(1);
        assertThat(result.notificationsSent())
                .isEqualTo(1);
    }
}
