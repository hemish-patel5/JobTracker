package com.example.jobtracker.service;

import com.example.jobtracker.dto.GmailMessageDto;
import com.example.jobtracker.model.EmailClassification;
import org.springframework.stereotype.Service;

@Service
public class JobEmailDetector {

    private final JobEmailClassifier classifier;

    public JobEmailDetector(JobEmailClassifier classifier) {
        this.classifier = classifier;
    }

    public boolean isJobRelated(GmailMessageDto email) {

        EmailClassification classification =
                classifier.classify(email).classification();

        if (!classifier.isJobClassification(classification)) {
            return false;
        }

        if (classification == EmailClassification.JOB_RECOMMENDATION) {
            return true;
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
