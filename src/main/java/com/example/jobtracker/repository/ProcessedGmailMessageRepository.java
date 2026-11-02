package com.example.jobtracker.repository;

import com.example.jobtracker.model.ProcessedGmailMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ProcessedGmailMessageRepository
        extends JpaRepository<ProcessedGmailMessage, String> {

    List<ProcessedGmailMessage>
    findByJobRelatedTrueAndAllowedCategoryTrueOrderByProcessedAtDesc();

    List<ProcessedGmailMessage>
    findByUpdateRelatedTrueAndAllowedCategoryTrueOrderByProcessedAtDesc();

    List<ProcessedGmailMessage>
    findByUpdateRelatedTrueAndAllowedCategoryTrueAndNotificationSentFalseOrderByProcessedAtAsc();

    @Query("""
            select message.gmailMessageId
            from ProcessedGmailMessage message
            where message.gmailMessageId in :gmailMessageIds
              and message.jobRelated is not null
              and message.receivedAt is not null
              and message.allowedCategory is not null
              and message.updateRelated is not null
              and message.outcome is not null
              and message.fullBodyClassified = true
            """)
    Set<String> findCompleteMessageIds(
            @Param("gmailMessageIds")
            Collection<String> gmailMessageIds
    );
}
