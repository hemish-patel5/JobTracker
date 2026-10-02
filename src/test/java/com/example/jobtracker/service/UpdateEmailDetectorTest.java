package com.example.jobtracker.service;

import com.example.jobtracker.dto.GmailMessageDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateEmailDetectorTest {

    private final UpdateEmailDetector detector =
            new UpdateEmailDetector();

    @Test
    void detectsNextStageAsAnUpdate() {
        assertThat(detector.isUpdateRelated(message(
                "Your application has progressed to the next stage"
        ))).isTrue();
    }

    @Test
    void detectsProgressedAsAnUpdate() {
        assertThat(detector.isUpdateRelated(message(
                "You have progressed in the selection process"
        ))).isTrue();
    }

    @Test
    void detectsUpdateOnlyWhenApplicationIsAlsoPresent() {
        assertThat(detector.isUpdateRelated(message(
                "An update about your application"
        ))).isTrue();
        assertThat(detector.isUpdateRelated(message(
                "A general account update"
        ))).isFalse();
    }

    @Test
    void doesNotTreatApplicationAloneAsAnUpdate() {
        assertThat(detector.isUpdateRelated(message(
                "Thank you for your application"
        ))).isFalse();
    }

    private GmailMessageDto message(String text) {
        return new GmailMessageDto(
                "gmail-id",
                "jobs@example.com",
                text,
                "",
                1L,
                null
        );
    }
}
