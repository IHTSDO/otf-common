package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import java.util.List;

public class ItemsPage<T> {

	private List<T> items;
	private long total;
	private long limit;
	private Long offset;

	public List<T> getItems() {
		return items;
	}

	public long getTotal() {
		return total;
	}

	public long getLimit() {
		return limit;
	}

	public Long getOffset() {
		return offset;
	}
}
