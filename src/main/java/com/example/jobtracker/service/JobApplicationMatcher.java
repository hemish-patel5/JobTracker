package com.example.jobtracker.service;

import com.example.jobtracker.dto.GmailMessageDto;
import com.example.jobtracker.model.JobApplication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobApplicationMatcher {

    public JobApplication findBestMatch(
            GmailMessageDto email,
            List<JobApplication> applications
    ) {
        JobApplication bestMatch = null;
        int bestScore = 0;

        String emailText = (
                email.from() + " " +
                email.subject() + " " +
                email.snippet()
        ).toLowerCase();

        for (JobApplication application : applications) {

            int score = 0;

            if (emailText.contains(
                    application.getCompany().toLowerCase()
            )) {
                score += 5;
            }

            if (emailText.contains(
                    application.getRole().toLowerCase()
            )) {
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
}
