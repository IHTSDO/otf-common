package org.snomed.otf.scheduler.domain;

import java.util.*;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class Job {
	//TODO Make testing jobs appear in Dev environment
	public enum ProductionStatus {TESTING, PROD_READY, HIDEME}
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@JsonIgnore
	private long id;
	
	String name;
	
	@Column(columnDefinition="TEXT")
	String description;
	ProductionStatus productionStatus;
	
	@ManyToOne
	@JsonIgnore //Will be evident in JSON from structure, causes infinite recursion if included explicitly.
	JobCategory category;
	
	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	JobParameters parameters;
	
	@OneToMany
	List<JobSchedule> schedules;
	
	public Job() {
		this.parameters = new JobParameters();
	};
	
	public Job(JobCategory category, String name, String description, JobParameters params, ProductionStatus prodStatus) {
		this();
		this.category = category;
		this.name = name;
		this.description = description;
		this.parameters = params;
		this.productionStatus = prodStatus;
	}
	
	public Job(JobCategory category, String name, String description, JobParameters params) {
		this(category, name, description, params, ProductionStatus.PROD_READY);
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public JobCategory getCategory() {
		return category;
	}
	public void setCategory(JobCategory category) {
		this.category = category;
	}
	public List<JobSchedule> getSchedules() {
		return schedules;
	}
	public void setSchedules(List<JobSchedule> schedules) {
		this.schedules = schedules;
	}
	@Override
	public boolean equals (Object other) {
		//Job may be missing a category as we hide that in the json
		if (other instanceof Job) {
			Job otherJob = (Job)other;
			if (category == null || otherJob.getCategory() == null || category.equals(otherJob.getCategory())) {
				return name.equals(otherJob.getName());
			}
		}
		return false;
	}
	
	@Override
	public String toString() {
		return getCategory() + "/" + getName();
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public ProductionStatus getProductionStatus() {
		return productionStatus;
	}
	public void setProductionStatus(ProductionStatus productionStatus) {
		this.productionStatus = productionStatus;
	}
	public JobParameters getParameters() {
		return parameters;
	}
	public void setParamters(JobParameters paramaters) {
		//Don't allow JobParameters to be set to null, wipe if required
		if (parameters == null) {
			this.parameters = new JobParameters();
		} else {
			this.parameters = paramaters;
		}
	}
}
