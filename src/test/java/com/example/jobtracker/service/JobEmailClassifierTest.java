package com.example.jobtracker.service;

import com.example.jobtracker.dto.GmailMessageDto;
import com.example.jobtracker.model.ApplicationStatus;
import com.example.jobtracker.model.EmailOutcome;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobEmailClassifierTest {

    private final JobEmailClassifier classifier =
            new JobEmailClassifier();

    @Test
    void classifiesTheTrimbleEmailAsARejection() {
        EmailOutcome outcome = classifier.detectOutcome(
                "An update on your application",
                "The position has been filled. " +
                        "We won’t be moving forward with your candidacy."
        );

        assertThat(outcome).isEqualTo(EmailOutcome.REJECTION);
    }

    @Test
    void detectsCommonRejectionWording() {
        assertThat(classifier.detectOutcome(
                "Application outcome",
                "We regret to inform you that another candidate was selected."
        )).isEqualTo(EmailOutcome.REJECTION);

        assertThat(classifier.detectOutcome(
                "Your application",
                "Your application is no longer under consideration."
        )).isEqualTo(EmailOutcome.REJECTION);

        assertThat(classifier.detectOutcome(
                "Application update",
                "We will not be taking your application further."
        )).isEqualTo(EmailOutcome.REJECTION);
    }

    @Test
    void doesNotTreatAmbiguousWordsAsRejectionByThemselves() {
        assertThat(classifier.detectOutcome(
                "Interview update",
                "Unfortunately, your interview has moved to Tuesday."
        )).isEqualTo(EmailOutcome.SUCCESS);

        assertThat(classifier.detectOutcome(
                "Application received",
                "We will contact you after the role has closed."
        )).isEqualTo(EmailOutcome.SUCCESS);
    }

    @Test
    void usesTheSameRejectionRulesForApplicationStatus() {
        GmailMessageDto email = new GmailMessageDto(
                "gmail-id",
                "jobs@example.com",
                "Application outcome",
                "The role has been filled.",
                1L,
                null
        );

        assertThat(classifier.detectStatus(email))
                .contains(ApplicationStatus.REJECTED);
    }
}
