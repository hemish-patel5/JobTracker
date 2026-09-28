package com.example.jobtracker.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
public class GmailAuthService {

    private static final String USER_KEY = "jobtracker-user";

    private final HttpTransport httpTransport;
    private final JsonFactory jsonFactory;
    private final GoogleAuthorizationCodeFlow flow;
    private final String redirectUri;

    public GmailAuthService(
            @Value("${google.gmail.client-id}") String clientId,
            @Value("${google.gmail.client-secret}") String clientSecret,
            @Value("${google.gmail.redirect-uri}") String redirectUri
    ) throws Exception {

        this.redirectUri = redirectUri;
        this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        this.jsonFactory = GsonFactory.getDefaultInstance();

        GoogleClientSecrets.Details details =
                new GoogleClientSecrets.Details();

        details.setClientId(clientId);
        details.setClientSecret(clientSecret);

        GoogleClientSecrets secrets =
                new GoogleClientSecrets()
                        .setWeb(details);

        flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport,
                jsonFactory,
                secrets,
                List.of(GmailScopes.GMAIL_READONLY)
        )
                .setAccessType("offline")
                .setDataStoreFactory(
                        new FileDataStoreFactory(
                                new File("tokens")
                        )
                )
                .build();
    }

    public String createAuthorizationUrl(String state) {

        return flow.newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .setState(state)
                .set("prompt", "consent")
                .build();
    }

    public void exchangeCode(String code)
            throws IOException {

        GoogleTokenResponse response =
                flow.newTokenRequest(code)
                        .setRedirectUri(redirectUri)
                        .execute();

        flow.createAndStoreCredential(
                response,
                USER_KEY
        );
    }

    public Credential getCredential()
            throws IOException {

        return flow.loadCredential(USER_KEY);
    }

    public boolean isConnected()
            throws IOException {

        return getCredential() != null;
    }

    public Gmail getGmailClient()
            throws IOException {

        Credential credential = getCredential();

        if (credential == null) {
            throw new IllegalStateException(
                    "Gmail account is not connected"
            );
        }

        return new Gmail.Builder(
                httpTransport,
                jsonFactory,
                credential
        )
                .setApplicationName("JobTracker")
                .build();
    }
}
