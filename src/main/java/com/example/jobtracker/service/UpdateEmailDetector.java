package com.example.jobtracker.service;

import com.example.jobtracker.dto.GmailMessageDto;
import org.springframework.stereotype.Service;

@Service
public class UpdateEmailDetector {

    public boolean isUpdateRelated(GmailMessageDto email) {

        String text = (
                email.subject() + " " +
                email.snippet() + " " +
                email.from()
        ).toLowerCase();

        return text.contains("next stage") ||
                text.contains("progressed") ||
                (text.contains("update") &&
                        text.contains("application"));
    }
}
