package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class RefsetAggregationPage {

	private Map<String, Long> memberCountsByReferenceSet;
	private Map<String, ConceptMiniPojo> referenceSets;

	public List<ConceptMiniPojo> getRefsetsWithActiveMemberCount() {
		Collection<ConceptMiniPojo> values = referenceSets.values();
		return new ArrayList<>(values);
	}

	public Map<String, Long> getMemberCountsByReferenceSet() {
		return memberCountsByReferenceSet;
	}

	public Map<String, ConceptMiniPojo> getReferenceSets() {
		return referenceSets;
	}
}
