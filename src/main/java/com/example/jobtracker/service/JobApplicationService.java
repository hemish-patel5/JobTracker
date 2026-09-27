package com.example.jobtracker.service;

import com.example.jobtracker.model.JobApplication;
import com.example.jobtracker.model.ApplicationStatus;
import com.example.jobtracker.repository.JobApplicationRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class JobApplicationService {

    private final JobApplicationRepository repository;

    public JobApplicationService(JobApplicationRepository repository) {
        this.repository = repository;
    }

    public List<JobApplication> getAllApplications(
            String company,
            String role,
            String location,
            ApplicationStatus status,
            LocalDate dateApplied,
            String jobUrl,
            String notes
    ) {
        Specification<JobApplication> filters = (root, query, builder) ->
                builder.conjunction();

        filters = addContainsFilter(filters, "company", company);
        filters = addContainsFilter(filters, "role", role);
        filters = addContainsFilter(filters, "location", location);
        filters = addEqualFilter(filters, "status", status);
        filters = addEqualFilter(filters, "dateApplied", dateApplied);
        filters = addContainsFilter(filters, "jobUrl", jobUrl);
        filters = addContainsFilter(filters, "notes", notes);

        return repository.findAll(filters);
    }

    private Specification<JobApplication> addContainsFilter(
            Specification<JobApplication> filters,
            String field,
            String value
    ) {
        if (value == null || value.isBlank()) {
            return filters;
        }

        String searchTerm = "%" + value.trim().toLowerCase() + "%";
        return filters.and((root, query, builder) ->
                builder.like(builder.lower(root.get(field)), searchTerm));
    }

    private <T> Specification<JobApplication> addEqualFilter(
            Specification<JobApplication> filters,
            String field,
            T value
    ) {
        if (value == null) {
            return filters;
        }

        return filters.and((root, query, builder) ->
                builder.equal(root.get(field), value));
    }

    public Optional<JobApplication> getApplicationById(Long id) {
        return repository.findById(id);
    }

    public JobApplication createApplication(JobApplication application) {
        return repository.save(application);
    }

    public Optional<JobApplication> updateApplication(
            Long id,
            JobApplication updatedApplication
    ) {
        return repository.findById(id)
                .map(existingApplication -> {

                    existingApplication.setCompany(
                            updatedApplication.getCompany()
                    );

                    existingApplication.setRole(
                            updatedApplication.getRole()
                    );

                    existingApplication.setLocation(
                            updatedApplication.getLocation()
                    );

                    existingApplication.setStatus(
                            updatedApplication.getStatus()
                    );

                    existingApplication.setDateApplied(
                            updatedApplication.getDateApplied()
                    );

                    existingApplication.setJobUrl(
                            updatedApplication.getJobUrl()
                    );

                    existingApplication.setNotes(
                            updatedApplication.getNotes()
                    );

                    return repository.save(existingApplication);
                });
    }

    public void deleteApplication(Long id) {
        repository.deleteById(id);
    }
}
