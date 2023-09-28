package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class RelationshipPojoTest {
	
	@Test
	public void testSnowstormFormat() throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		RelationshipPojo relationshipPojo = objectMapper
				.readValue("{ 'type': {'conceptId': '123', 'fsn': { 'term': 'My type', 'lang':'en', 'conceptId':'okay'}, 'pt': { 'term': 'My type', 'lang':'en'} }}"
				.replace("'", "\""), RelationshipPojo.class);
		assertEquals("My type", relationshipPojo.getType().getFsn().getTerm());
		assertEquals("en", relationshipPojo.getType().getFsn().getLang());
		
		assertEquals("My type", relationshipPojo.getType().getPt().getTerm());
		assertEquals("en", relationshipPojo.getType().getPt().getLang());
	}
}
