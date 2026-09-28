package com.example.jobtracker.repository;

import com.example.jobtracker.model.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface JobApplicationRepository
        extends JpaRepository<JobApplication, Long>,
        JpaSpecificationExecutor<JobApplication> {

    List<JobApplication> findByCompanyContainingIgnoreCase(
            String company
    );
}
