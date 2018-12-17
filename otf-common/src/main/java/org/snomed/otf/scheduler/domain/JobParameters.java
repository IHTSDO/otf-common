package org.snomed.otf.scheduler.domain;

import java.util.*;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
public class JobParameters {
	
	@Id
	@JsonIgnore
	@GeneratedValue(strategy = GenerationType.AUTO)
	Long id;

	//See https://stackoverflow.com/questions/2327971/how-do-you-map-a-map-in-hibernate-using-annotations
	//Also https://thoughts-on-java.org/hibernate-tips-how-to-delete-child-entities/
	@ElementCollection(fetch = FetchType.EAGER)
	@OneToMany(mappedBy="parentParams", orphanRemoval = true, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	Map<String, JobParameter> parameterMap;
	
	public JobParameters (@JsonProperty("parameters") Map<String, JobParameter> parameterMap) {
		this();
	}
	
	public JobParameters(String[] keys) {
		this();
		for (String key : keys) {
			setValue(key, null);
		}
	}

	public JobParameters() {
		parameterMap = new LinkedHashMap<>();
	}

	public JobParameter withValue(String key, Object value) {
		return setValue(key, value);
	}
	
	public JobParameter setValue(String key, Object value) {
		return add(key).setValue(value);
	}
	
	public String getValue(String key) {
		JobParameter param = getParameterMap().get(key);
		if (param != null) {
			return param.getValue();
		}
		return null;
	}
	
	public String getDefaultValue(String key) {
		JobParameter param = getParameterMap().get(key);
		if (param != null) {
			return param.getDefaultValue();
		}
		return null;
	}
	
	public String getMandatory(String key) {
		String value = getValue(key);
		if (value == null) {
			throw new IllegalArgumentException("Mandatory parameter value for '" + key + "' was not supplied");
		}
		return value;
	}
	
	public boolean getMandatoryBoolean(String key) {
		String value = getValue(key);
		if (value == null) {
			throw new IllegalArgumentException("Mandatory parameter value for '" + key + "' was not supplied");
		}
		value = value.toUpperCase();
		if (value.equals("Y") || value.equals("N")) {
			return value.equals("Y");
		}
		throw new IllegalArgumentException("Mandatory boolean parameter value for '" + key + "' was not recognised: '" + value + "'");
	}

	// Private so not to expose the internal collection
	// Ah, can't be private or doesn't show up in JSON
	public Map<String, JobParameter> getParameterMap() {
		if (parameterMap == null) {
			parameterMap = new LinkedHashMap<>();
		}
		return parameterMap;
	}
	
	public void setParameterMap(Map<String, JobParameter> parameterMap) {
		this.parameterMap = parameterMap;
	}
	
	public Set<String> keySet() {
		return parameterMap.keySet();
	}
	public void remove(String key) {
		parameterMap.remove(key);
	}
	public JobParameter add(String key) {
		JobParameter param = getParameterMap().get(key);
		if (param == null) {
			param = new JobParameter(this, getParameterMap().size());
			getParameterMap().put(key, param);
		}
		return param;
	}
	public JobParameter get(String key) {
		return getParameterMap().get(key);
	}
	
	public JobParameters clone() {
		JobParameters clone = new JobParameters(new HashMap<>(this.getParameterMap()));
		return clone;
	}
	
	public String toString() {
		return getParameterMap().toString();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
