package com.example.jobtracker.controller;

import com.example.jobtracker.service.GmailAuthService;
import com.example.jobtracker.service.GmailUpdateResult;
import com.example.jobtracker.service.GmailUpdateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/api/internal")
public class InternalController {

    private final GmailUpdateService gmailUpdateService;
    private final GmailAuthService gmailAuthService;
    private final String schedulerSecret;

    public InternalController(
            GmailUpdateService gmailUpdateService,
            GmailAuthService gmailAuthService,
            @Value("${scheduler.secret}") String schedulerSecret
    ) {
        this.gmailUpdateService = gmailUpdateService;
        this.gmailAuthService = gmailAuthService;
        this.schedulerSecret = schedulerSecret;
    }

    @PostMapping("/check-gmail")
    public ResponseEntity<Map<String, Object>> checkGmail(
            @RequestHeader(
                    value = "X-Scheduler-Secret",
                    required = false
            ) String secret
    ) throws Exception {
        if (!hasValidSecret(secret)) {

            return ResponseEntity
                    .status(401)
                    .body(Map.of(
                            "error",
                            "Unauthorized"
                    ));
        }

        if (!gmailAuthService.isConnected()) {
            return ResponseEntity
                    .status(409)
                    .body(Map.of(
                            "error",
                            "Gmail is not connected"
                    ));
        }

        GmailUpdateResult result =
                gmailUpdateService.checkForUpdates();

        return ResponseEntity
                .ok(Map.of(
                        "status",
                        "completed",
                        "updateEmailCount",
                        result.updateEmailCount(),
                        "notificationsSent",
                        result.notificationsSent()
                ));
    }

    private boolean hasValidSecret(String providedSecret) {

        if (providedSecret == null ||
                schedulerSecret.isBlank()) {
            return false;
        }

        return MessageDigest.isEqual(
                schedulerSecret.getBytes(
                        StandardCharsets.UTF_8
                ),
                providedSecret.getBytes(
                        StandardCharsets.UTF_8
                )
        );
    }
}
