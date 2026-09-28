package com.example.jobtracker.controller;

import com.example.jobtracker.service.GmailAuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/gmail")
public class GmailController {

    private final GmailAuthService gmailAuthService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public GmailController(
            GmailAuthService gmailAuthService
    ) {
        this.gmailAuthService = gmailAuthService;
    }

    @GetMapping("/connect")
    public RedirectView connect(
            HttpSession session
    ) {

        String state =
                UUID.randomUUID().toString();

        session.setAttribute(
                "gmail_oauth_state",
                state
        );

        String authorizationUrl =
                gmailAuthService
                        .createAuthorizationUrl(state);

        return new RedirectView(
                authorizationUrl
        );
    }

    @GetMapping("/oauth2/callback")
    public RedirectView callback(
            @RequestParam String code,
            @RequestParam String state,
            HttpSession session
    ) throws IOException {

        String expectedState =
                (String) session.getAttribute(
                        "gmail_oauth_state"
                );

        if (expectedState == null ||
                !expectedState.equals(state)) {

            throw new IllegalStateException(
                    "Invalid OAuth state"
            );
        }

        gmailAuthService.exchangeCode(code);

        session.removeAttribute(
                "gmail_oauth_state"
        );

        return new RedirectView(
                frontendUrl + "?gmail=connected"
        );
    }

    @GetMapping("/status")
    public Map<String, Boolean> status()
            throws IOException {

        return Map.of(
                "connected",
                gmailAuthService.isConnected()
        );
    }
}