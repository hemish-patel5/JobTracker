package com.example.jobtracker.service;

import com.example.jobtracker.model.EmailClassification;

public record EmailClassificationResult(
        EmailClassification classification,
        String matchedPhrase
) {
}
