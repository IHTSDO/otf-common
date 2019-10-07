package org.snomed.otf.scheduler.domain;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Entity;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

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
	
	public static class Serialize extends JsonSerializer<JobScheduleParameters> {
		public Serialize() {
			super();
		}
		
		@Override
		public void serialize(JobScheduleParameters value, JsonGenerator gen, SerializerProvider serializers)
				throws IOException, JsonProcessingException {
			gen.writeObject(value.getParameterMap());
		}
	}
	
	public static class Deserialize extends JsonDeserializer<JobParameters>{
		public Deserialize() {
			super();
		}

		@Override
		public JobScheduleParameters deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			JobScheduleParameters jobParameters = new JobScheduleParameters();
			ObjectMapper mapper = (ObjectMapper) p.getCodec();
			JsonNode node = mapper.readTree(p);
			TypeReference<HashMap<String, JobParameter>> typeRef = new TypeReference<HashMap<String, JobParameter>>() {};
			Map<String,JobParameter> map = mapper.readValue(node.toString(), typeRef);
			jobParameters.setParameterMap(map);
			return jobParameters;
		}
	}

}
