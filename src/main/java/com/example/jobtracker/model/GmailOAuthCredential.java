package com.example.jobtracker.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "gmail_oauth_credentials")
public class GmailOAuthCredential {

    @Id
    @Column(name = "credential_key", nullable = false)
    private String credentialKey;

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "expiration_time_milliseconds")
    private Long expirationTimeMilliseconds;

    protected GmailOAuthCredential() {
    }

    public GmailOAuthCredential(
            String credentialKey,
            String accessToken,
            String refreshToken,
            Long expirationTimeMilliseconds
    ) {
        this.credentialKey = credentialKey;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expirationTimeMilliseconds = expirationTimeMilliseconds;
    }

    public String getCredentialKey() {
        return credentialKey;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public Long getExpirationTimeMilliseconds() {
        return expirationTimeMilliseconds;
    }
}
