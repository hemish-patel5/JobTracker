package com.example.jobtracker.service;

import com.example.jobtracker.model.JobApplication;
import com.example.jobtracker.repository.JobApplicationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class JobApplicationService {

    private final JobApplicationRepository repository;

    public JobApplicationService(JobApplicationRepository repository) {
        this.repository = repository;
    }

    public List<JobApplication> getAllApplications() {
        return repository.findAll();
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