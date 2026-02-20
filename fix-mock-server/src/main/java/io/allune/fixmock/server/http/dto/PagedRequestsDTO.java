/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * Copyright 2025-2026 the original author or authors.
 */
package io.allune.fixmock.server.http.dto;

import java.util.List;

/**
 * Simple container for paged journal results.
 */
public class PagedRequestsDTO {
	private long total;
	private int page;
	private int size;
	private List<JournalEntryDTO> items;

	public PagedRequestsDTO() {}

	public PagedRequestsDTO(long total, int page, int size, List<JournalEntryDTO> items) {
		this.total = total;
		this.page = page;
		this.size = size;
		this.items = items;
	}

	public long getTotal() { return total; }
	public void setTotal(long total) { this.total = total; }
	public int getPage() { return page; }
	public void setPage(int page) { this.page = page; }
	public int getSize() { return size; }
	public void setSize(int size) { this.size = size; }
	public List<JournalEntryDTO> getItems() { return items; }
	public void setItems(List<JournalEntryDTO> items) { this.items = items; }
}
