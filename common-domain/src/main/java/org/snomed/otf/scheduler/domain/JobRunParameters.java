package org.snomed.otf.scheduler.domain;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.Entity;

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
@JsonSerialize(using = JobRunParameters.Serialize.class)
@JsonDeserialize(using = JobRunParameters.Deserialize.class)
public class JobRunParameters extends JobParameters {
	
	public JobRunParameters() {
		super();
	}

	public JobRunParameters(Map<String, JobParameter> parameterMap) {
		super(parameterMap);
	}
	
	public static class Serialize extends JsonSerializer<JobRunParameters> {
		
		public Serialize() {
			super();
		}
		
		@Override
		public void serialize(JobRunParameters value, JsonGenerator gen, SerializerProvider serializers)
				throws IOException, JsonProcessingException {
			gen.writeObject(value.getParameterMap());
		}
	}
	
	public static class Deserialize extends JsonDeserializer<JobRunParameters>{
		
		public Deserialize() {
			super();
		}

		@Override
		public JobRunParameters deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException {
			JobRunParameters jobParameters = new JobRunParameters();
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
