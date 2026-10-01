package com.example.jobtracker.repository;

import com.example.jobtracker.model.GmailOAuthCredential;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GmailOAuthCredentialRepository
        extends JpaRepository<GmailOAuthCredential, String> {
}
