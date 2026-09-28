package com.example.jobtracker.service;

import com.example.jobtracker.dto.GmailMessageDto;
import org.springframework.stereotype.Service;

@Service
public class JobEmailDetector {

    public boolean isJobRelated(GmailMessageDto email) {

        String text = (
                email.subject() + " " +
                email.snippet() + " " +
                email.from()
        ).toLowerCase();

        return JobEmailKeywords.VALUES.stream()
                .anyMatch(text::contains);
    }
}
