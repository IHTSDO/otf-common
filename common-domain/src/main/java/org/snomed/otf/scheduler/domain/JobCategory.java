package org.snomed.otf.scheduler.domain;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import javax.persistence.*;

import org.snomed.otf.scheduler.domain.Job.ProductionStatus;

import com.fasterxml.jackson.annotation.*;

@Entity
public class JobCategory implements Serializable {

	private static final long serialVersionUID = -3051153469536899317L;
	
	public static final String GENERAL_QA = "General QA";
	public static final String ADHOC_QUERIES = "Ad-Hoc Queries";
	public static final String QI = "Quality Improvement";
	public static final String DRUGS = "Drugs and Substances";
	public static final String RELEASE_VALIDATION = "Release Validation";
	public static final String RELEASE_STATS = "Release Statistics";
	
	public static final String REFSET_UPDATE = "Refset Update";
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@JsonIgnore
	private long id;
	
	@Column(length=50)
	String name;

	@ManyToOne
	@JsonIgnore
	JobType type;
	
	@OneToMany(mappedBy = "category")
	Set<Job> jobs = new HashSet<>();
	
	public JobCategory() {}
	
	public JobCategory(@JsonProperty("name") String name) {
		this.name = name;
	}
	
	public JobCategory( String type, String name) {
		this.type = new JobType(type);
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public JobType getType() {
		return type;
	}
	public void setType(JobType type) {
		this.type = type;
	}
	public List<Job> getJobs() {
		//This is the simpliest place to hide jobs that have been "deleted" or withdrawn, but we 
		//want them to remain in the database
		return jobs.stream()
				.filter(j -> j.getProductionStatus() != null && !j.getProductionStatus().equals(ProductionStatus.HIDEME))
				.collect(Collectors.toList());
	}
	public void setJobs(Set<Job> jobs) {
		this.jobs = jobs;
	}
	public JobCategory addJob (Job job) {
		job.setCategory(this);
		jobs.add(job);
		return this;
	}
	@Override
	public boolean equals (Object other) {
		if (other instanceof JobCategory) {
			JobCategory otherCat = (JobCategory)other;
			if (type!=null) {
				if (type.equals(otherCat.getType())) {
					return name.equals(otherCat.getName());
				}
			} else {
				throw new RuntimeException("Job Category '" + this.getName() + "' doesn't know what type it is!"); 
			}
		}
		return false;
	}
	
	@Override
	public String toString() {
		return type + ":" + name;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
}
