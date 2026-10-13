package com.example.jobtracker.service;

import com.example.jobtracker.dto.GmailMessageDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobEmailDetectorTest {

    private final JobEmailDetector detector =
            new JobEmailDetector(new JobEmailClassifier());

    @Test
    void includesRelevantJobRecommendations() {
        GmailMessageDto email = message(
                "Junior Data Scientist + 11 new jobs",
                "We have new job recommendations for you. " +
                        "These roles could be a match."
        );

        assertThat(detector.isJobRelated(email)).isTrue();
    }

    @Test
    void filtersPromotionalOffers() {
        GmailMessageDto email = message(
                "Beat the heat this summer - special offer",
                "Cool down sale on now. Shop hot deals and buy now."
        );

        assertThat(detector.isJobRelated(email)).isFalse();
    }

    @Test
    void usesTheFullBodyToDetectApplicationMessages() {
        GmailMessageDto email = message(
                "Application update",
                "We have decided not to move forward with your application."
        );

        assertThat(detector.isJobRelated(email)).isTrue();
    }

    private GmailMessageDto message(
            String subject,
            String fullBody
    ) {
        return new GmailMessageDto(
                "gmail-id",
                "jobs@example.com",
                subject,
                "Short Gmail preview",
                fullBody,
                1L,
                null
        );
    }
}
