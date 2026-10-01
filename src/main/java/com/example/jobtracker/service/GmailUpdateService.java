package com.example.jobtracker.service;

import com.example.jobtracker.model.ProcessedGmailMessage;
import com.example.jobtracker.repository.ProcessedGmailMessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GmailUpdateService {

    private final GmailService gmailService;
    private final ProcessedGmailMessageRepository processedMessageRepository;
    private final NtfyService ntfyService;

    public GmailUpdateService(
            GmailService gmailService,
            ProcessedGmailMessageRepository processedMessageRepository,
            NtfyService ntfyService
    ) {
        this.gmailService = gmailService;
        this.processedMessageRepository = processedMessageRepository;
        this.ntfyService = ntfyService;
    }

    public synchronized GmailUpdateResult checkForUpdates()
            throws Exception {

        int updateEmailCount =
                gmailService
                        .getUpdateMessages()
                        .size();

        List<ProcessedGmailMessage> pendingNotifications =
                processedMessageRepository
                        .findByUpdateRelatedTrueAndAllowedCategoryTrueAndNotificationSentFalseOrderByProcessedAtAsc();

        int notificationsSent = 0;

        for (ProcessedGmailMessage message : pendingNotifications) {
            ntfyService.sendApplicationUpdate(message);
            message.markNotificationSent();
            processedMessageRepository.save(message);
            notificationsSent++;
        }

        return new GmailUpdateResult(
                updateEmailCount,
                notificationsSent
        );
    }
}
