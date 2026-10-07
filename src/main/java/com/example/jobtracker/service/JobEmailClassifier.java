package com.example.jobtracker.service;

import com.example.jobtracker.dto.GmailMessageDto;
import com.example.jobtracker.model.ApplicationStatus;
import com.example.jobtracker.model.EmailOutcome;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class JobEmailClassifier {

    private static final List<String> REJECTION_PHRASES = List.of(
            "not progressing",
            "not be progressing",
            "won't be progressing",
            "will not be progressing",
            "not moving forward",
            "not be moving forward",
            "won't be moving forward",
            "will not be moving forward",
            "unable to move forward",
            "decided not to move forward",
            "not proceed with",
            "will not proceed with",
            "unable to progress",
            "cannot progress",
            "not selected",
            "not been selected",
            "not shortlisted",
            "not been shortlisted",
            "not successful",
            "haven't been successful",
            "have not been successful",
            "unsuccessful",
            "position has been filled",
            "position has now been filled",
            "role has been filled",
            "role has now been filled",
            "filled the position",
            "regret to inform",
            "no longer under consideration",
            "another candidate",
            "other candidates",
            "decline your application",
            "declined your application",
            "reject your application",
            "rejected your application",
            "not taking your application further",
            "won't be taking your application further",
            "will not be taking your application further",
            "unable to offer you",
            "cannot offer you"
    );

    public Optional<ApplicationStatus> detectStatus(
            GmailMessageDto email
    ) {

        String text = normalize(
                email.subject(),
                email.snippet()
        );

        if (isRejection(text)) {
            return Optional.of(
                    ApplicationStatus.REJECTED
            );
        }

        if (
                text.contains("interview invitation") ||
                text.contains("invite you to interview") ||
                text.contains("invited to interview") ||
                text.contains("interview with")
        ) {
            return Optional.of(
                    ApplicationStatus.INTERVIEW
            );
        }

        if (
                text.contains("assessment") ||
                text.contains("online test") ||
                text.contains("coding test")
        ) {
            return Optional.of(
                    ApplicationStatus.ONLINE_ASSESSMENT
            );
        }

        if (
                text.contains("offer") &&
                (
                    text.contains("pleased") ||
                    text.contains("congratulations")
                )
        ) {
            return Optional.of(
                    ApplicationStatus.OFFER
            );
        }

        return Optional.empty();
    }

    public EmailOutcome detectOutcome(
            GmailMessageDto email
    ) {

        return detectOutcome(
                email.subject(),
                email.snippet()
        );
    }

    public EmailOutcome detectOutcome(
            String subject,
            String fullBody
    ) {

        String text = normalize(subject, fullBody);

        return isRejection(text)
                ? EmailOutcome.REJECTION
                : EmailOutcome.SUCCESS;
    }

    private boolean isRejection(String text) {

        return REJECTION_PHRASES.stream()
                .anyMatch(text::contains);
    }

    private String normalize(String... values) {

        StringBuilder normalized = new StringBuilder();

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                normalized.append(' ')
                        .append(value);
            }
        }

        return normalized.toString()
                .toLowerCase(Locale.ROOT)
                .replace('\u2018', '\'')
                .replace('\u2019', '\'')
                .replaceAll("\\s+", " ")
                .trim();
    }
}
