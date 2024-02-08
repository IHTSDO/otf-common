package org.snomed.otf.scheduler.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
public class JobRunBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @OneToMany(mappedBy = "runBatch")
    @JsonIgnore
    Set<JobRun> jobRuns = new HashSet<>();
    private Date batchRequestTime;
    private Boolean international;
    private Boolean managedService;
    private String projectName;
    private String user;

    public JobRunBatch() {
    }

    public JobRunBatch(boolean international, boolean managedService, String projectName, String user) {
        this.batchRequestTime = new Date();
        this.international = international;
        this.managedService = managedService;
        this.projectName = projectName;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<JobRun> getJobRuns() {
        return jobRuns;
    }

    public void setJobRuns(Set<JobRun> jobRuns) {
        this.jobRuns = jobRuns;
    }

    public Date getBatchRequestTime() {
        return batchRequestTime;
    }

    public void setBatchRequestTime(Date batchRequestTime) {
        this.batchRequestTime = batchRequestTime;
    }

    public Boolean getInternational() {
        return international;
    }

    public void setInternational(Boolean international) {
        this.international = international;
    }

    public Boolean getManagedService() {
        return managedService;
    }

    public void setManagedService(Boolean managedService) {
        this.managedService = managedService;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
