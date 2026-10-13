package com.example.jobtracker.service;

import com.example.jobtracker.dto.GmailMessageDto;
import com.example.jobtracker.model.ApplicationStatus;
import com.example.jobtracker.model.EmailClassification;
import com.example.jobtracker.model.EmailOutcome;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobEmailClassifierTest {

    private final JobEmailClassifier classifier = new JobEmailClassifier();

    @Test
    void classifiesExplicitRejectionInsteadOfRoleClosure() {
        EmailClassificationResult result = classifier.classify(
                "An update on your application",
                "The position has been filled. We won't be moving forward with your candidacy."
        );

        assertThat(result.classification()).isEqualTo(EmailClassification.REJECTED);
        assertThat(result.matchedPhrase()).isEqualTo("won't be moving forward");
        assertThat(classifier.detectOutcome(
                "An update on your application",
                "We won't be moving forward with your candidacy."
        )).isEqualTo(EmailOutcome.REJECTION);
    }

    @Test
    void doesNotRejectUsingAmbiguousNegativeWords() {
        assertThat(classifier.classify(
                "Interview update",
                "Unfortunately, your interview has moved to Tuesday."
        ).classification()).isEqualTo(EmailClassification.REVIEW_REQUIRED);

        assertThat(classifier.classify(
                "Application update",
                "The role has now closed and we will be in touch."
        ).classification()).isEqualTo(EmailClassification.PENDING);
    }

    @Test
    void detectsOfferInterviewAssessmentAndProgression() {
        assertClassification(
                "We are pleased to offer you the position.",
                EmailClassification.OFFER
        );
        assertClassification(
                "Please select an interview slot.",
                EmailClassification.INTERVIEW
        );
        assertClassification(
                "We would like to invite you to the first stage pre-screening call.",
                EmailClassification.INTERVIEW
        );
        assertClassification(
                "You are invited to complete an assessment.",
                EmailClassification.ONLINE_ASSESSMENT
        );
        assertClassification(
                "Your application has progressed to the next stage.",
                EmailClassification.PROGRESSED
        );
    }

    @Test
    void distinguishesNeutralAndIgnoredMessages() {
        assertClassification(
                "We have received your application.",
                EmailClassification.APPLICATION_RECEIVED
        );
        assertClassification(
                "Your application is currently under review.",
                EmailClassification.PENDING
        );
        assertClassification(
                "The company cancelled this opening.",
                EmailClassification.ROLE_CLOSED
        );
        assertClassification(
                "Your withdrawal has been confirmed.",
                EmailClassification.WITHDRAWN
        );
        assertClassification(
                "Here is your weekly digest of new job openings.",
                EmailClassification.OTHER
        );
    }

    @Test
    void treatsConflictingDecisionsAsReviewRequired() {
        assertClassification(
                "We regret to inform you that your previous offer letter was sent in error.",
                EmailClassification.REVIEW_REQUIRED
        );
    }

    @Test
    void ignoresAQuotedOlderDecisionWhenClassifyingAReply() {
        EmailClassificationResult result = classifier.classify(
                "Interview invitation",
                "Please schedule an interview with our team.\n\n" +
                        "On Monday, Recruiter wrote:\n" +
                        "We will not be moving forward."
        );

        assertThat(result.classification())
                .isEqualTo(EmailClassification.INTERVIEW);
    }

    @Test
    void mapsOnlyActionableClassificationsToApplicationStatuses() {
        GmailMessageDto interview = message(
                "We would like to schedule an interview."
        );
        GmailMessageDto received = message(
                "We have received your application."
        );

        assertThat(classifier.detectStatus(interview))
                .contains(ApplicationStatus.INTERVIEW);
        assertThat(classifier.detectStatus(received)).isEmpty();
    }

    private void assertClassification(
            String text,
            EmailClassification expected
    ) {
        assertThat(classifier.classify("Application update", text).classification())
                .isEqualTo(expected);
    }

    private GmailMessageDto message(String text) {
        return new GmailMessageDto(
                "gmail-id",
                "jobs@example.com",
                "Application update",
                text,
                text,
                1L,
                null
        );
    }
}
