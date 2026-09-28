package com.example.jobtracker.service;

import com.example.jobtracker.dto.GmailMessageDto;
import com.example.jobtracker.model.ApplicationStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class JobEmailClassifier {

    public Optional<ApplicationStatus> detectStatus(
            GmailMessageDto email
    ) {

        String text = (
                email.subject() + " " +
                email.snippet()
        ).toLowerCase();

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

        if (
                text.contains("unfortunately") ||
                text.contains("not progressing") ||
                text.contains("not be progressing") ||
                text.contains("other candidates") ||
                text.contains("unsuccessful")
        ) {
            return Optional.of(
                    ApplicationStatus.REJECTED
            );
        }

        return Optional.empty();
    }
}
