package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Set;

@JsonPropertyOrder({"items", "total" })
@JsonIgnoreProperties(ignoreUnknown = false)
public final class MembersResponse {

	private long total;

	private Set<RefsetMemberPojo> items;

    public MembersResponse() {}

	public long getTotal() {
		return total;
	}

	public void setTotal(long total) {
		this.total = total;
	}

	public Set<RefsetMemberPojo> getItems() {
		return items;
	}

	public void setItems(Set<RefsetMemberPojo> items) {
		this.items = items;
	}

}
