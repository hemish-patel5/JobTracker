package com.example.jobtracker.service;

import com.example.jobtracker.dto.GmailMessageDto;
import com.example.jobtracker.model.JobApplication;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JobApplicationMatcherTest {

    private final JobApplicationMatcher matcher =
            new JobApplicationMatcher();

    @Test
    void matchesAnApplicationUsingTheFullEmailBody() {
        JobApplication application = new JobApplication();
        application.setCompany("Fujitsu");
        application.setRole("Graduate Software Engineer");

        GmailMessageDto email = message(
                "Application update",
                "Your Fujitsu Graduate Program application has progressed."
        );

        assertThat(matcher.findBestMatch(email, List.of(application)))
                .isSameAs(application);
    }

    @Test
    void doesNotMatchAnUnrelatedRecruitmentEmail() {
        JobApplication application = new JobApplication();
        application.setCompany("Canonical");
        application.setRole("Graduate Software Engineer");

        GmailMessageDto email = message(
                "Application update",
                "Your data analyst application at Example Limited has progressed."
        );

        assertThat(matcher.findBestMatch(email, List.of(application)))
                .isNull();
    }

    private GmailMessageDto message(
            String subject,
            String fullBody
    ) {
        return new GmailMessageDto(
                "gmail-id",
                "recruitment@example.com",
                subject,
                "Short Gmail preview",
                fullBody,
                1L,
                null
        );
    }
}
