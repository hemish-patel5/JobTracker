package com.example.jobtracker.service;

import com.example.jobtracker.dto.GmailMessageDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UpdateEmailDetector {

    public static final List<String> KEYWORDS = List.of(
            "update",
            "application"
    );

    public boolean isUpdateRelated(GmailMessageDto email) {

        String text = (
                email.subject() + " " +
                email.snippet() + " " +
                email.from()
        ).toLowerCase();

        return KEYWORDS.stream()
                .allMatch(text::contains);
    }
}
