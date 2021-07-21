package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import java.util.Set;

public class ConceptMiniResponse {
	
	private long total;

	private Set<ConceptMiniPojo> items;

	private ConceptMiniResponse() {
		
	}
	
	public long getTotal() {
		return total;
	}

	public void setTotal(long total) {
		this.total = total;
	}

	public Set<ConceptMiniPojo> getItems() {
		return items;
	}

	public void setItems(Set<ConceptMiniPojo> items) {
		this.items = items;
	}

}
