package com.example.jobtracker.service;

import com.example.jobtracker.dto.GmailMessageDto;
import com.example.jobtracker.model.ApplicationStatus;
import com.example.jobtracker.model.EmailClassification;
import com.example.jobtracker.model.EmailOutcome;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class JobEmailClassifier {

    private static final List<String> REJECTION_PHRASES = List.of(
            "not moving forward",
            "will not be moving forward",
            "won't be moving forward",
            "will not be proceeding",
            "will not be progressing",
            "not progressing your application",
            "not selected for further consideration",
            "not selected",
            "not been shortlisted",
            "application was unsuccessful",
            "application has been unsuccessful",
            "your application has not been successful",
            "not successful",
            "decided not to move forward",
            "decided not to proceed",
            "decided not to progress",
            "unable to offer you the position",
            "cannot offer you the role",
            "cannot proceed with your application",
            "selected another candidate",
            "chosen another candidate",
            "chosen another applicant",
            "chosen a different candidate",
            "proceed with other candidates",
            "move forward with other candidates",
            "moving forward with another candidate",
            "moving forward with other applicants",
            "pursuing other applicants",
            "no longer being considered",
            "no longer under consideration",
            "not taking your application further",
            "will not be taking your application forward",
            "will not be taking your candidacy further",
            "did not meet the selection criteria",
            "declined your application",
            "not inviting you to interview",
            "not progressing to the next stage",
            "not advancing your candidacy",
            "unable to advance your application",
            "unable to advance you",
            "we regret to inform you",
            "after careful consideration, we regret"
    );

    private static final List<String> OFFER_PHRASES = List.of(
            "pleased to offer you the position",
            "pleased to offer you",
            "delighted to offer you",
            "excited to offer you",
            "we would like to offer you the role",
            "would like to offer you",
            "extending an offer",
            "offer of employment",
            "formal job offer",
            "formal offer of employment",
            "conditional offer of employment",
            "your offer letter",
            "offer letter",
            "employment agreement for your review",
            "employment agreement",
            "employment contract",
            "review and sign your offer",
            "please review and sign your offer",
            "accept our job offer",
            "selected for employment",
            "congratulations on securing the position",
            "approved an offer for you",
            "pleased to make you a job offer",
            "we are offering you the position",
            "attached contract confirms our offer",
            "please sign your offer letter"
    );

    private static final List<String> INTERVIEW_PHRASES = List.of(
            "invited to interview",
            "invite you to interview",
            "interview invitation",
            "invitation to interview",
            "invite you to the first stage",
            "schedule an interview",
            "schedule a video interview",
            "arrange an interview",
            "book an interview",
            "book an interview time",
            "provide your interview availability",
            "interview availability",
            "select an interview slot",
            "shortlisted for an interview",
            "selected for an interview",
            "technical interview",
            "live technical interview",
            "video interview",
            "virtual interview",
            "online interview",
            "phone interview",
            "phone screening",
            "telephone interview",
            "panel interview",
            "hiring manager interview",
            "second-round interview",
            "second round interview",
            "second interview",
            "final interview invitation",
            "final-round interview",
            "interview booking",
            "choose an interview date",
            "meet with our hiring manager",
            "inviting you to a live interview",
            "please book your interview"
    );

    private static final List<String> ASSESSMENT_PHRASES = List.of(
            "invited to complete an assessment",
            "invitation to complete an assessment",
            "complete your online assessment",
            "complete your assessment",
            "complete the coding challenge",
            "coding assessment invitation",
            "assessment invitation",
            "online assessment link",
            "online assessment portal",
            "assessment link",
            "assessment deadline",
            "coding challenge",
            "coding test",
            "timed coding test",
            "technical test",
            "technical assessment",
            "technical quiz",
            "online aptitude test",
            "numerical reasoning assessment",
            "logical reasoning assessment",
            "psychometric assessment",
            "behavioural assessment",
            "one-way video interview",
            "recorded video interview",
            "take-home assignment",
            "submit the take-home assignment",
            "programming exercise",
            "technical exercise",
            "work sample exercise",
            "assessment centre invitation",
            "assessment centre",
            "pre-interview task",
            "complete the following task",
            "complete your pre-work"
    );

    private static final List<String> PROGRESSION_PHRASES = List.of(
            "progressed to the next stage",
            "progressing to the next stage",
            "advanced to the next round",
            "advancing to the next round",
            "successful in screening",
            "passed initial screening",
            "passed initial review",
            "passed the screening round",
            "shortlisted for further consideration",
            "advanced your application",
            "moving forward with your application",
            "next stage of recruitment",
            "selected to progress",
            "successful in progressing",
            "passed the initial screening stage",
            "application has progressed",
            "progressed in the selection process",
            "proceeding to the next stage",
            "progressed to another stage",
            "shortlisted for the next stage"
    );

    private static final List<String> APPLICATION_RECEIVED_PHRASES = List.of(
            "application received",
            "thank you for applying",
            "submitted successfully",
            "application confirmation",
            "confirm receipt",
            "received your cv",
            "received your application",
            "application submitted",
            "submission successful",
            "application acknowledgement",
            "application acknowledgment",
            "application logged",
            "application successfully submitted",
            "thank you for your application",
            "we have received your application",
            "your application has been received",
            "application submission confirmed",
            "successfully applied"
    );

    private static final List<String> PENDING_PHRASES = List.of(
            "under review",
            "under consideration",
            "still reviewing",
            "reviewing applications",
            "no decision has been made",
            "recruitment delay",
            "shortlisting ongoing",
            "awaiting feedback",
            "results being reviewed",
            "interview postponed",
            "decision pending",
            "will be in touch",
            "recruitment timeline has changed",
            "currently reviewing applications",
            "application being reviewed",
            "selection process ongoing",
            "awaiting hiring manager feedback",
            "recruitment process delayed",
            "interview rescheduled",
            "position temporarily on hold",
            "we will update you shortly",
            "still considering your application",
            "review process is ongoing"
    );

    private static final List<String> ROLE_CLOSED_PHRASES = List.of(
            "vacancy cancelled",
            "position cancelled",
            "role withdrawn",
            "hiring freeze",
            "recruitment suspended",
            "position no longer available",
            "requisition closed",
            "funding withdrawn",
            "will not be filling the role",
            "hiring on hold indefinitely",
            "vacancy has been withdrawn",
            "position has been closed",
            "role has been cancelled",
            "recruitment has been cancelled",
            "opening has been cancelled",
            "we will not be hiring anyone",
            "company cancelled this opening",
            "position has been filled",
            "role has been filled"
    );

    private static final List<String> WITHDRAWN_PHRASES = List.of(
            "withdrawal confirmed",
            "application withdrawn",
            "withdrawal request processed",
            "withdrawn as requested",
            "removed from consideration at your request",
            "no longer an active applicant",
            "cancelled your application at your request",
            "your withdrawal has been confirmed",
            "application withdrawal successful",
            "we have processed your withdrawal",
            "withdrawal request confirmed",
            "removed your name from consideration"
    );

    private static final List<String> REVIEW_REQUIRED_PHRASES = List.of(
            "check your candidate portal",
            "status changed",
            "sent in error",
            "corrected decision",
            "no final decision",
            "await verification",
            "decision reconsidered",
            "conflicting status",
            "please contact recruitment",
            "details to follow",
            "incorrect status notification",
            "previous message was incorrect",
            "awaiting clarification",
            "status under investigation",
            "decision remains uncertain",
            "message may have been sent in error",
            "recruitment status requires confirmation",
            "contradictory status",
            "please disregard the previous email",
            "an earlier status notice will be corrected",
            "no hiring decision has been made"
    );

    private static final List<String> JOB_RECOMMENDATION_PHRASES = List.of(
            "job recommendations",
            "new job recommendations",
            "jobs based on your profile",
            "roles could be a match",
            "recommended jobs",
            "weekly job alert",
            "new jobs for you",
            "jobs you may be interested in",
            "new job openings",
            "new positions available",
            "jobs newsletter"
    );

    private static final List<String> OTHER_PHRASES = List.of(
            "newsletter",
            "apply now",
            "new vacancies",
            "career fair",
            "webinar",
            "discount offer",
            "promotional offer",
            "weekly digest",
            "career opportunities",
            "recruitment advertising",
            "upcoming careers event",
            "shop hot deals",
            "shop now",
            "limited time deal",
            "discount code",
            "free shipping",
            "rewards members",
            "clearance sale",
            "buy now"
    );

    private static final List<String> AMBIGUOUS_PHRASES = List.of(
            "unfortunately",
            "successful",
            "congratulations",
            "offer",
            "interview",
            "assessment",
            "thank you for your interest",
            "after careful consideration",
            "high volume of applications",
            "competitive applicant pool",
            "wish you all the best",
            "future opportunities",
            "pleased",
            "excited"
    );

    public EmailClassificationResult classify(GmailMessageDto email) {
        String body = email.fullBody() == null || email.fullBody().isBlank()
                ? email.snippet()
                : email.fullBody();

        return classify(email.subject(), body);
    }

    public EmailClassificationResult classify(
            String subject,
            String fullBody
    ) {
        String text = normalize(subject, fullBody);

        Optional<String> reviewPhrase = firstMatch(text, REVIEW_REQUIRED_PHRASES);
        if (reviewPhrase.isPresent()) {
            return result(EmailClassification.REVIEW_REQUIRED, reviewPhrase.get());
        }

        Optional<String> rejectionPhrase = firstMatch(text, REJECTION_PHRASES);
        Optional<String> offerPhrase = firstMatch(text, OFFER_PHRASES);

        if (rejectionPhrase.isPresent() && offerPhrase.isPresent()) {
            return result(EmailClassification.REVIEW_REQUIRED,
                    rejectionPhrase.get() + " / " + offerPhrase.get());
        }
        if (rejectionPhrase.isPresent()) {
            return result(EmailClassification.REJECTED, rejectionPhrase.get());
        }
        if (offerPhrase.isPresent()) {
            return result(EmailClassification.OFFER, offerPhrase.get());
        }

        Optional<String> withdrawnPhrase = firstMatch(text, WITHDRAWN_PHRASES);
        if (withdrawnPhrase.isPresent()) {
            return result(EmailClassification.WITHDRAWN, withdrawnPhrase.get());
        }

        Optional<String> interviewPhrase = firstMatch(text, INTERVIEW_PHRASES);
        if (interviewPhrase.isPresent()) {
            return result(EmailClassification.INTERVIEW, interviewPhrase.get());
        }

        Optional<String> assessmentPhrase = firstMatch(text, ASSESSMENT_PHRASES);
        if (assessmentPhrase.isPresent()) {
            return result(EmailClassification.ONLINE_ASSESSMENT, assessmentPhrase.get());
        }

        Optional<String> progressionPhrase = firstMatch(text, PROGRESSION_PHRASES);
        if (progressionPhrase.isPresent()) {
            return result(EmailClassification.PROGRESSED, progressionPhrase.get());
        }

        Optional<String> roleClosedPhrase = firstMatch(text, ROLE_CLOSED_PHRASES);
        if (roleClosedPhrase.isPresent()) {
            return result(EmailClassification.ROLE_CLOSED, roleClosedPhrase.get());
        }

        Optional<String> receivedPhrase = firstMatch(text, APPLICATION_RECEIVED_PHRASES);
        if (receivedPhrase.isPresent()) {
            return result(EmailClassification.APPLICATION_RECEIVED, receivedPhrase.get());
        }

        Optional<String> pendingPhrase = firstMatch(text, PENDING_PHRASES);
        if (pendingPhrase.isPresent()) {
            return result(EmailClassification.PENDING, pendingPhrase.get());
        }

        Optional<String> recommendationPhrase =
                firstMatch(text, JOB_RECOMMENDATION_PHRASES);
        if (recommendationPhrase.isPresent()) {
            return result(
                    EmailClassification.JOB_RECOMMENDATION,
                    recommendationPhrase.get()
            );
        }

        Optional<String> otherPhrase = firstMatch(text, OTHER_PHRASES);
        if (otherPhrase.isPresent()) {
            return result(EmailClassification.OTHER, otherPhrase.get());
        }

        Optional<String> ambiguousPhrase = firstMatch(text, AMBIGUOUS_PHRASES);
        if (ambiguousPhrase.isPresent()) {
            return result(EmailClassification.REVIEW_REQUIRED, ambiguousPhrase.get());
        }

        return result(EmailClassification.OTHER, null);
    }

    public Optional<ApplicationStatus> detectStatus(GmailMessageDto email) {
        return switch (classify(email).classification()) {
            case REJECTED -> Optional.of(ApplicationStatus.REJECTED);
            case OFFER -> Optional.of(ApplicationStatus.OFFER);
            case INTERVIEW -> Optional.of(ApplicationStatus.INTERVIEW);
            case ONLINE_ASSESSMENT -> Optional.of(ApplicationStatus.ONLINE_ASSESSMENT);
            case WITHDRAWN -> Optional.of(ApplicationStatus.WITHDRAWN);
            default -> Optional.empty();
        };
    }

    public EmailOutcome detectOutcome(GmailMessageDto email) {
        return toOutcome(classify(email).classification());
    }

    public EmailOutcome detectOutcome(String subject, String fullBody) {
        return toOutcome(classify(subject, fullBody).classification());
    }

    public boolean isUpdateClassification(EmailClassification classification) {
        return switch (classification) {
            case REJECTED, OFFER, INTERVIEW, ONLINE_ASSESSMENT,
                    PROGRESSED -> true;
            default -> false;
        };
    }

    public boolean isJobClassification(EmailClassification classification) {
        return classification != EmailClassification.OTHER;
    }

    private EmailOutcome toOutcome(EmailClassification classification) {
        return classification == EmailClassification.REJECTED
                ? EmailOutcome.REJECTION
                : EmailOutcome.SUCCESS;
    }

    private Optional<String> firstMatch(String text, List<String> phrases) {
        return phrases.stream().filter(text::contains).findFirst();
    }

    private EmailClassificationResult result(
            EmailClassification classification,
            String matchedPhrase
    ) {
        return new EmailClassificationResult(classification, matchedPhrase);
    }

    private String normalize(String... values) {
        StringBuilder normalized = new StringBuilder();

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                normalized.append(' ')
                        .append(stripQuotedHistory(value));
            }
        }

        return normalized.toString()
                .toLowerCase(Locale.ROOT)
                .replace('\u2018', '\'')
                .replace('\u2019', '\'')
                .replace('\u201c', '"')
                .replace('\u201d', '"')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String stripQuotedHistory(String value) {
        return value
                .replaceFirst(
                        "(?is)\\R\\s*-{2,}\\s*original message\\s*-{2,}.*$",
                        ""
                )
                .replaceFirst(
                        "(?is)\\R\\s*on\\s.{1,300}\\swrote:\\s*.*$",
                        ""
                )
                .replaceFirst(
                        "(?is)\\R\\s*begin forwarded message:\\s*.*$",
                        ""
                );
    }
}
