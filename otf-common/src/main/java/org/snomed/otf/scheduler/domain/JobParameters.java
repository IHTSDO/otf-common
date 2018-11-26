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

	@OneToMany(mappedBy="parentParams", cascade = CascadeType.ALL)
	@MapKey(name="paramKey")
	Map<String, JobParameter> parameterMap;
	
	public JobParameters (@JsonProperty("parameters") Map<String, JobParameter> parameterMap) {
		this.parameterMap = new HashMap<>();
	}
	
	public JobParameters(String[] keys) {
		parameterMap = new HashMap<>();
		for (String key : keys) {
			setValue(key, null);
		}
	}

	public JobParameters() {
		parameterMap = new HashMap<>();
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
	
	public String getMandatory(String key) {
		String value = getValue(key);
		if (value == null) {
			throw new IllegalArgumentException("Mandatory parameter value for '" + key + "' was not supplied");
		}
		return value;
	}

	// Private so not to expose the internal collection
	// Ah, can't be private or doesn't show up in JSON
	public Map<String, JobParameter> getParameterMap() {
		if (parameterMap == null) {
			parameterMap = new HashMap<>();
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
			param = new JobParameter(this, key);
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

	protected void setParameters(JobParameters params) {
		parameterMap = params.getParameterMap();
	}
	
}
