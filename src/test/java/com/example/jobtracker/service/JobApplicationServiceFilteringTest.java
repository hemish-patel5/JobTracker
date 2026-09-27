package com.example.jobtracker.service;

import com.example.jobtracker.model.ApplicationStatus;
import com.example.jobtracker.model.JobApplication;
import com.example.jobtracker.repository.JobApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JobApplicationServiceFilteringTest {

    @Autowired
    private JobApplicationService service;

    @Autowired
    private JobApplicationRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        repository.saveAll(List.of(
                new JobApplication(
                        "Air New Zealand",
                        "Java Developer",
                        "Auckland",
                        ApplicationStatus.INTERVIEW,
                        LocalDate.of(2026, 9, 20),
                        "https://example.com/java",
                        "Technical interview booked"
                ),
                new JobApplication(
                        "Canva",
                        "Product Designer",
                        "Sydney",
                        ApplicationStatus.APPLIED,
                        LocalDate.of(2026, 9, 21),
                        "https://example.com/design",
                        "Waiting for a response"
                )
        ));
    }

    @Test
    void filtersTextFieldsUsingCaseInsensitivePartialMatches() {
        List<JobApplication> results = service.getAllApplications(
                "AIR", null, "land", null, null, null, null
        );

        assertThat(results)
                .extracting(JobApplication::getCompany)
                .containsExactly("Air New Zealand");
    }

    @Test
    void combinesStatusAndDateFilters() {
        List<JobApplication> results = service.getAllApplications(
                null,
                null,
                null,
                ApplicationStatus.INTERVIEW,
                LocalDate.of(2026, 9, 20),
                null,
                null
        );

        assertThat(results)
                .extracting(JobApplication::getCompany)
                .containsExactly("Air New Zealand");
    }

    @Test
    void returnsAllApplicationsWhenNoFiltersAreProvided() {
        List<JobApplication> results = service.getAllApplications(
                null, null, null, null, null, null, null
        );

        assertThat(results).hasSize(2);
    }
}
