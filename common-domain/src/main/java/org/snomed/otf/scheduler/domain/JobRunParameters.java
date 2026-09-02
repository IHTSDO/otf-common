package org.snomed.otf.scheduler.domain;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.Entity;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

@Entity
@JsonSerialize(using = JobRunParameters.Serialize.class)
@JsonDeserialize(using = JobRunParameters.Deserialize.class)
public class JobRunParameters extends JobParameters {
	
	public JobRunParameters() {
		super();
	}

	public JobRunParameters(Map<String, JobParameter> parameterMap) {
		super(parameterMap);
	}
	
	public static class Serialize extends ValueSerializer<JobRunParameters> {
		
		public Serialize() {
			super();
		}
		
		@Override
		public void serialize(JobRunParameters value, JsonGenerator gen, SerializationContext ctxt)
				throws JacksonException {
			gen.writePOJO(value.getParameterMap());
		}
	}
	
	public static class Deserialize extends ValueDeserializer<JobRunParameters>{
		
		public Deserialize() {
			super();
		}

		@Override
		public JobRunParameters deserialize(JsonParser p, DeserializationContext ctxt)
				throws JacksonException {
			JobRunParameters jobParameters = new JobRunParameters();
			TypeReference<HashMap<String, JobParameter>> typeRef = new TypeReference<>() {
            };
			Map<String,JobParameter> map = ctxt.readValue(p, typeRef);
			jobParameters.setParameterMap(map);
			return jobParameters;
		}

	}

}
