package org.ihtsdo.otf.rest.client.snowowl.pojo;

import java.util.Set;

public final class SimpleConceptResponse {

	private long total;

	private Set<SimpleConceptPojo> items;

	private SimpleConceptResponse() {
		
	}
	
	public long getTotal() {
		return total;
	}

	public void setTotal(long total) {
		this.total = total;
	}

	public Set<SimpleConceptPojo> getItems() {
		return items;
	}

	public void setItems(Set<SimpleConceptPojo> items) {
		this.items = items;
	}
}
