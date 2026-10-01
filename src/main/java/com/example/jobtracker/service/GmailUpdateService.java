package com.example.jobtracker.service;

import com.example.jobtracker.model.ProcessedGmailMessage;
import com.example.jobtracker.repository.ProcessedGmailMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class GmailUpdateService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GmailUpdateService.class);

    private final GmailService gmailService;
    private final ProcessedGmailMessageRepository processedMessageRepository;
    private final NtfyService ntfyService;
    private final Executor gmailUpdateExecutor;
    private final AtomicBoolean updateRunning =
            new AtomicBoolean(false);

    public GmailUpdateService(
            GmailService gmailService,
            ProcessedGmailMessageRepository processedMessageRepository,
            NtfyService ntfyService,
            @Qualifier("gmailUpdateExecutor")
            Executor gmailUpdateExecutor
    ) {
        this.gmailService = gmailService;
        this.processedMessageRepository = processedMessageRepository;
        this.ntfyService = ntfyService;
        this.gmailUpdateExecutor = gmailUpdateExecutor;
    }

    public boolean startUpdateCheck() {

        if (!updateRunning.compareAndSet(false, true)) {
            return false;
        }

        gmailUpdateExecutor.execute(() -> {
            try {
                GmailUpdateResult result = checkForUpdates();

                LOGGER.info(
                        "Scheduled Gmail check completed: {} update emails, {} notifications sent",
                        result.updateEmailCount(),
                        result.notificationsSent()
                );
            } catch (Exception exception) {
                LOGGER.error(
                        "Scheduled Gmail check failed",
                        exception
                );
            } finally {
                updateRunning.set(false);
            }
        });

        return true;
    }

    GmailUpdateResult checkForUpdates()
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
