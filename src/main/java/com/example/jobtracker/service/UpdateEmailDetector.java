package com.example.jobtracker.service;

import com.example.jobtracker.dto.GmailMessageDto;
import org.springframework.stereotype.Service;

@Service
public class UpdateEmailDetector {

    private final JobEmailClassifier classifier;

    public UpdateEmailDetector(JobEmailClassifier classifier) {
        this.classifier = classifier;
    }

    public boolean isUpdateRelated(GmailMessageDto email) {
        return classifier.isUpdateClassification(
                classifier.classify(email).classification()
        );
    }
}
