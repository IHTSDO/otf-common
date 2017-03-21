package org.ihtsdo.otf.rest.client.snowowl.pojo;

import java.util.HashSet;
import java.util.Set;

public final class ConceptIdsResponse {

	private long total;

	private Set<ConceptResponse> items;

	private ConceptIdsResponse() {
	}

	public Set<String> getConceptIds() {
		Set<String> conceptIds = new HashSet<>();
		if (items != null) {
			for (ConceptResponse item : items) {
				conceptIds.add(item.getId());
			}
		}
		return conceptIds;
	}

	public long getTotal() {
		return total;
	}

	public void setTotal(long total) {
		this.total = total;
	}

	public Set<ConceptResponse> getItems() {
		return items;
	}

	public void setItems(Set<ConceptResponse> items) {
		this.items = items;
	}

	private static final class ConceptResponse {
		private String id;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
	}

}
