package org.snomed.otf.scheduler.domain;

import java.io.Serializable;
import java.util.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
public class JobCategory implements Serializable {

	private static final long serialVersionUID = -3051153469536899317L;
	
	public static final String GENERAL_QA = "General QA";
	public static final String ADHOC_QUERIES = "Ad-Hoc Queries";
	public static final String QI = "Quality Improvement";
	public static final String DRUGS = "Drugs and Substances";
	public static final String RELEASE_VALIDATION = "Release Validation";
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;
	
	@Column(length=50)
	String name;

	@ManyToOne
	JobType type;
	
	@OneToMany(mappedBy = "category")
	List<Job> jobs = new ArrayList<>();
	
	public JobCategory() {}
	
	public JobCategory(@JsonProperty("name") String name) {
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
		return jobs;
	}
	public void setJobs(List<Job> jobs) {
		this.jobs = jobs;
	}
	public JobCategory addJob (Job job) {
		jobs.add(job);
		return this;
	}
	@Override
	public boolean equals (Object other) {
		if (other instanceof JobCategory) {
			JobCategory otherCat = (JobCategory)other;
			if (type.equals(otherCat.getType())) {
				return name.equals(otherCat.getName());
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
