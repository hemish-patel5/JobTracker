package com.example.jobtracker.repository;

import com.example.jobtracker.model.ProcessedGmailMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcessedGmailMessageRepository
        extends JpaRepository<ProcessedGmailMessage, String> {

    List<ProcessedGmailMessage>
    findByJobRelatedTrueAndAllowedCategoryTrueOrderByProcessedAtDesc();
}
