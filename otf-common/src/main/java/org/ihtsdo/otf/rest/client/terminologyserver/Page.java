package org.ihtsdo.otf.rest.client.terminologyserver;

import java.util.List;

public class Page<T> {

	private List<T> items;
	private Long total;
	private Long offset;
	private String searchAfter;

	public Page() {
	}

	public Page(List<T> items, Long total) {
		this.items = items;
		this.total = total;
	}

	public Page(List<T> allItems) {
		offset = 0L;
		total = (long) allItems.size();
		this.items = allItems;
	}

	public Page(org.springframework.data.domain.Page<T> springPage) {
		items = springPage.getContent();
		total = springPage.getTotalElements();
		offset = springPage.getNumber() * (long) springPage.getSize();
	}

	public List<T> getItems() {
		return items;
	}

	public Long getTotal() {
		return total;
	}

	public Long getOffset() {
		return offset;
	}

	public String getSearchAfter() {
		return searchAfter;
	}
}
