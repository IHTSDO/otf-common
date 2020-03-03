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
	
	@ElementCollection
	@CollectionTable(name="job_tags", joinColumns=@JoinColumn(name="job_id"))
	@Column(name="tag")
	Set<String> tags = new HashSet<>();
	
	@ElementCollection//(fetch = FetchType.EAGER)
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@MapKey(name = "codeSystemShortname")
	Map<String, WhiteList> whiteListMap;
	
	public Job() {
		this.parameters = new JobParameters();
	};
	
	public Job withCategory(JobCategory category) {
		this.category = category;
		return this;
	}
	
	public Job withName (String name) {
		this.name = name;
		return this;
	}
	
	public Job withDescription(String description) {
		this.description = description;
		return this;
	}
	public Job withParameters (JobParameters params) {
		this.parameters = params;
		return this;
	}
	
	public Job withProductionStatus (ProductionStatus status) {
		this.productionStatus = status;
		return this;
	}
	
	public Job withTag (String tag) {
		tags.add(tag);
		return this;
	}
	
	public Job build() {
		return this;
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
				//If neither object has a job name, compare object ids
				if (getName() == null && otherJob.getName() == null) {
					return this == other;
				}
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

	public Set<WhiteListedConcept> getWhiteListConcepts(String codeSystemShortname) {
		if (whiteListMap.containsKey(codeSystemShortname)) {
			return whiteListMap.get(codeSystemShortname).getConcepts();
		}
		return new HashSet<>();
	}
	
	public WhiteList getWhiteList(String codeSystemShortname) {
		return whiteListMap.get(codeSystemShortname);
	}

	public void setWhiteList(String codeSystemShortname, WhiteList whiteList) {
		if (whiteList != null) {
			if (this.whiteListMap == null) {
				this.whiteListMap = new HashMap<>();
			}
			this.whiteListMap.put(codeSystemShortname, whiteList);
		} else {
			this.whiteListMap.remove(codeSystemShortname);
		}
	}
	
	public Job addTag(String tag) {
		tags.add(tag);
		return this;
	}

	public Set<String> getTags() {
		return tags;
	}

	public void setTags(Set<String> tags) {
		this.tags = tags;
	}

	public Map<String, WhiteList> getWhiteListMap() {
		return whiteListMap;
	}
	
	public void setWhiteListMap(Map<String, WhiteList> whiteListMap) {
		this.whiteListMap = whiteListMap;
	}
}
