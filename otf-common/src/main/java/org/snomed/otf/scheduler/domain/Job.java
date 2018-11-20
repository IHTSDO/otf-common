package org.snomed.otf.scheduler.domain;

import java.util.*;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class Job {
	
	public enum ProductionStatus {TESTING, PROD_READY, HIDEME}
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@JsonIgnore
	private long id;
	
	String name;
	String description;
	ProductionStatus productionStatus;
	
	@ManyToOne
	@JsonIgnore //Will be evident in JSON from structure, causes infinite recursion if included explicitly.
	JobCategory category;
	
	@ElementCollection
	@CollectionTable(name="ParameterNames", joinColumns=@JoinColumn(name="job_id"))
	@Column(name="parameterName")
	List<String> parameterNames;
	
	@OneToMany
	List<JobSchedule> schedules;
	public Job() {};
	public Job(JobCategory category, String name, String description, String[] params, ProductionStatus prodStatus) {
		this.category = category;
		this.name = name;
		this.description = description;
		parameterNames = Arrays.asList(params);
		this.productionStatus = prodStatus;
	}
	
	public Job(JobCategory category, String name, String description, String[] params) {
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
	public List<String> getParameterNames() {
		return parameterNames;
	}
	public void setParameterNames(List<String> parameterNames) {
		this.parameterNames = parameterNames;
	}
	public List<JobSchedule> getSchedules() {
		return schedules;
	}
	public void setSchedules(List<JobSchedule> schedules) {
		this.schedules = schedules;
	}
	@Override
	public boolean equals (Object other) {
		if (other instanceof Job) {
			Job otherJob = (Job)other;
			if (category.equals(otherJob.getCategory())) {
				return name.equals(otherJob.getName());
			}
		}
		return false;
	}
	
	@Override
	public String toString() {
		return getCategory() + "/" + getName();
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
}
