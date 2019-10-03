package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class RelationshipPojoTest {

	@Test
	public void testSO6Format() throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		RelationshipPojo relationshipPojo = objectMapper.readValue("{ 'type': {'conceptId': '123', 'fsn': 'My type'} }".replace("'", "\""), RelationshipPojo.class);
		assertEquals("My type", relationshipPojo.getType().getFsn());
	}

	@Test
	public void testSnowstormFormat() throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		RelationshipPojo relationshipPojo = objectMapper.readValue("{ 'type': {'conceptId': '123', 'fsn': { 'term': 'My type', 'lang':'en', 'conceptId':'okay'}} }".replace("'", "\""), RelationshipPojo.class);
		assertEquals("My type", relationshipPojo.getType().getFsn());
	}

}
