package com.example.jobtracker.service;

import java.util.List;
import java.util.stream.Collectors;

public final class JobEmailKeywords {

    public static final List<String> VALUES = List.of(
            "application",
            "interview",
            "assessment",
            "recruitment",
            "graduate",
            "internship",
            "developer",
            "unfortunately",
            "next stage",
            "next step",
            "software engineer",
            "software engineering",
            "software",
            "intern"
    );

    private JobEmailKeywords() {
    }

    public static String asGmailSearchGroup() {

        return VALUES.stream()
                .map(keyword ->
                        keyword.contains(" ")
                                ? "\"" + keyword + "\""
                                : keyword
                )
                .collect(
                        Collectors.joining(" ", "{", "}")
                );
    }
}
