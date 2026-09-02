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
@JsonSerialize(using = JobScheduleParameters.Serialize.class)
@JsonDeserialize(using = JobScheduleParameters.Deserialize.class)
public class JobScheduleParameters extends JobParameters {
	
	public JobScheduleParameters() {
		super();
	}

	public JobScheduleParameters(Map<String, JobParameter> parameterMap) {
		super(parameterMap);
	}
	
	public static class Serialize extends ValueSerializer<JobScheduleParameters> {
		public Serialize() {
			super();
		}
		
		@Override
		public void serialize(JobScheduleParameters value, JsonGenerator gen, SerializationContext ctxt)
				throws JacksonException {
			gen.writePOJO(value.getParameterMap());
		}
	}
	
	public static class Deserialize extends ValueDeserializer<JobScheduleParameters>{
		public Deserialize() {
			super();
		}

		@Override
		public JobScheduleParameters deserialize(JsonParser p, DeserializationContext ctxt)
				throws JacksonException {
			JobScheduleParameters jobParameters = new JobScheduleParameters();
			TypeReference<HashMap<String, JobParameter>> typeRef = new TypeReference<>() {
            };
			Map<String,JobParameter> map = ctxt.readValue(p, typeRef);
			jobParameters.setParameterMap(map);
			return jobParameters;
		}
	}

}
