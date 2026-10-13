package com.example.jobtracker.service;

import com.example.jobtracker.dto.GmailMessageDto;
import com.example.jobtracker.model.JobApplication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class JobApplicationMatcher {

    public JobApplication findBestMatch(
            GmailMessageDto email,
            List<JobApplication> applications
    ) {
        JobApplication bestMatch = null;
        int bestScore = 0;

        String emailText = normalize(
                email.from(),
                email.subject(),
                email.snippet(),
                email.fullBody()
        );

        for (JobApplication application : applications) {

            int score = 0;

            String company = normalize(application.getCompany());
            String role = normalize(application.getRole());

            if (!company.isBlank() && emailText.contains(company)) {
                score += 5;
            }

            if (!role.isBlank() && emailText.contains(role)) {
                score += 5;
            }

            if (score > bestScore) {
                bestScore = score;
                bestMatch = application;
            }
        }

        return bestScore >= 5
                ? bestMatch
                : null;
    }

    private String normalize(String... values) {
        StringBuilder text = new StringBuilder();

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                text.append(' ').append(value);
            }
        }

        return text.toString()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }
}
