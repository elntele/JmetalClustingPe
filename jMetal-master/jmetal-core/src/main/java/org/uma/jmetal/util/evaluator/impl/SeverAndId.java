package org.uma.jmetal.util.evaluator.impl;

import java.util.List;
import java.util.UUID;

public class SeverAndId implements Comparable  {
	private UUID id;
	private List <String>  url;
	private long executionTime;
	private int slice;
	private double velocity;
	private int lastEvaluateSize;
	private boolean StatusOnLine;
	
	public long getExecutionTime() {
		return executionTime;
	}
	public void setExecutionTime(long executionTime) {
		this.executionTime = executionTime;
	}
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
		
	public double getVelocity() {
		return velocity;
	}
	public void setVelocity(double velocity) {
		this.velocity = velocity;
	}
	public int getSlice() {
		return slice;
	}
	public void setSlice(int slice) {
		this.slice = slice;
	}
	
	
	public int getLastEvaluateSize() {
		return lastEvaluateSize;
	}
	public void setLastEvaluateSize(int lastEvaluateSize) {
		this.lastEvaluateSize = lastEvaluateSize;
	}
	
	
	public boolean isStatusOnLine() {
		return StatusOnLine;
	}
	public void setStatusOnLine(boolean statusOnLine) {
		StatusOnLine = statusOnLine;
	}
	/**
	 * metodo inserido para ordenação de listas de objetos severAndId
	 */
	@Override
	public int compareTo(Object sever) {
		int compareExecutionTime=(int)((SeverAndId)sever).getExecutionTime();
		return (int) (this.executionTime-compareExecutionTime);
	}
	


}
