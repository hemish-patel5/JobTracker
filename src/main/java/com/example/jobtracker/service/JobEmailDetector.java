package com.example.jobtracker.service;

import com.example.jobtracker.dto.GmailMessageDto;
import org.springframework.stereotype.Service;

@Service
public class JobEmailDetector {

    private final JobEmailClassifier classifier;

    public JobEmailDetector(JobEmailClassifier classifier) {
        this.classifier = classifier;
    }

    public boolean isJobRelated(GmailMessageDto email) {

        if (!classifier.isJobClassification(
                classifier.classify(email).classification()
        )) {
            return false;
        }

        String text = (
                email.subject() + " " +
                email.snippet() + " " +
                email.fullBody() + " " +
                email.from()
        ).toLowerCase();

        return JobEmailKeywords.VALUES.stream()
                .anyMatch(text::contains);
    }
}
