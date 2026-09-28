package com.example.jobtracker.repository;

import com.example.jobtracker.model.ProcessedGmailMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedGmailMessageRepository
        extends JpaRepository<ProcessedGmailMessage, String> {
}