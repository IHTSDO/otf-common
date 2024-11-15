package org.snomed.otf.scheduler.domain;

import java.io.IOException;
import java.util.*;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Entity
@JsonSerialize(using = JobParameters.Serialize.class)
@JsonDeserialize(using = JobParameters.Deserialize.class)
public class JobParameters {

	@Id
	@JsonIgnore
	@GeneratedValue(strategy = GenerationType.AUTO)
	Long id;

	//See https://stackoverflow.com/questions/2327971/how-do-you-map-a-map-in-hibernate-using-annotations
	//Also https://thoughts-on-java.org/hibernate-tips-how-to-delete-child-entities/
	@ElementCollection(fetch = FetchType.EAGER)
	@OneToMany(mappedBy="parentParams", orphanRemoval = true, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@OrderBy("displayOrder ASC")
	@JsonIgnore
	Map<String, JobParameter> parameterMap;
	
	public JobParameters (Map<String, JobParameter> parameterMap) {
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
	
	public JobParameter setValues(String key, List<String> values) {
		JobParameter p = add(key);
		p.setValues(values);
		return p;
	}
	
	public JobParameter setOptions(String key, List<String> options) {
		JobParameter p = add(key);
		p.setOptions(options);
		return p;
	}
	
	public String getValue(String key) {
		JobParameter param = getParameterMap().get(key);
		if (param != null) {
			return param.getValue();
		}
		return null;
	}
	
	public List<String> getValues(String key) {
		JobParameter param = getParameterMap().get(key);
		if (param != null) {
			return param.getValues();
		} 
		return new ArrayList<>();
	}
	
	public List<String> getOptions(String key) {
		JobParameter param = getParameterMap().get(key);
		if (param != null) {
			return param.getOptions();
		} 
		return new ArrayList<>();
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
		if (value.equals("Y") || value.equals("N") || value.equals("TRUE") || value.equals("FALSE")) {
			return value.equals("Y") || value.equals("TRUE");
		}
		throw new IllegalArgumentException("Mandatory boolean parameter value for '" + key + "' was not recognised: '" + value + "'");
	}
	
	public boolean getBoolean(String key) {
		String value = getValue(key);
		if (value != null && (value.equalsIgnoreCase("Y") ||
				value.equalsIgnoreCase("TRUE"))) {
			return true;
		}
		return false;
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
			param = new JobParameter(this, key, getParameterMap().size());
			getParameterMap().put(key, param);
		}
		return param;
	}
	
	public JobParameter add(JobParameter param) {
		getParameterMap().remove(param.getParamKey());
		param.setParentParams(this);
		param.setDisplayOrder(getParameterMap().size());
		getParameterMap().put(param.getParamKey(), param);
		return param;
	}
	
	//Adds a new parameter to the top of the list
	public JobParameter addFirst(String paramKey) {
		Map<String, JobParameter> originalParams = this.parameterMap;
		this.parameterMap = new LinkedHashMap<>();
		JobParameter first = add(paramKey);
		for (JobParameter existingParam : originalParams.values()) {
			add (existingParam);
		}
		return first;
	}
	
	
	public JobParameter get(String key) {
		return getParameterMap().get(key);
	}
	
	public JobParameters clone() {
		return new JobParameters(new HashMap<>(this.getParameterMap()));
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
	
	public static class Serialize extends JsonSerializer<JobParameters> {
		public Serialize() {
			super();
		}
		
		@Override
		public void serialize(JobParameters value, JsonGenerator gen, SerializerProvider serializers)
				throws IOException {
			gen.writeObject(value.getParameterMap());
		}
	}
	
	public static class Deserialize extends JsonDeserializer<JobParameters>{
		public Deserialize() {
			super();
		}

		@Override
		public JobParameters deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException {
			JobParameters jobParameters = new JobParameters();
			ObjectMapper mapper = (ObjectMapper) p.getCodec();
			JsonNode node = mapper.readTree(p);
			TypeReference<HashMap<String, JobParameter>> typeRef = new TypeReference<>() {
            };
			Map<String,JobParameter> map = mapper.readValue(node.toString(), typeRef);
			jobParameters.setParameterMap(map);
			return jobParameters;
		}
	}
}
