package org.uma.jmetal.util.evaluator.impl;

import java.util.List;
import java.util.UUID;

public class SeverAndId {
	private UUID id;
	private List <String>  url;
	public SeverAndId(UUID id, List<String> url) {
		super();
		this.id = id;
		this.url = url;
	}
	public UUID getId() {
		return id;
	}
	public void setId(UUID id) {
		this.id = id;
	}
	public List<String> getUrl() {
		return url;
	}
	public void setUrl(List<String> url) {
		this.url = url;
	}


}
