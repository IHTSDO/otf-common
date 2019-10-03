package org.ihtsdo.otf.rest.client.terminologyserver.pojo;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


@JsonPropertyOrder({"items", "total" })
@JsonIgnoreProperties(ignoreUnknown = false)
public class SimpleDescriptionResponse {
	
	private SimpleDescriptionResponse() {
		
	}
	private long total;

	private Set<SimpleDescriptionPojo> items;

	public void setTotal(long total) {
		this.total = total;
	}
	
	public long getTotal() {
		return total;
	}
	
	public Set<SimpleDescriptionPojo> getItems() {
		return items;
	}
	
	public void setItems(Set<SimpleDescriptionPojo> items) {
		this.items = items;
	}
}