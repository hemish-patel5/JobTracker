package com.example.jobtracker.service;

import com.example.jobtracker.dto.GmailMessageDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobEmailDetector {

    private final List<String> jobKeywords = List.of(
            "application",
            "interview",
            "assessment",
            "candidate",
            "recruitment",
            "graduate",
            "internship",
            "developer",
            "unfortunately",
            "next stage",
            "next step"
    );

    public boolean isJobRelated(GmailMessageDto email) {

        String text = (
                email.subject() + " " +
                email.snippet() + " " +
                email.from()
        ).toLowerCase();

        return jobKeywords.stream()
                .anyMatch(text::contains);
    }
}