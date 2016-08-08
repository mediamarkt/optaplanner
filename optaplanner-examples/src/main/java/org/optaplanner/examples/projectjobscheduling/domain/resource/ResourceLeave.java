package org.optaplanner.examples.projectjobscheduling.domain.resource;

public class ResourceLeave {
	private int start;
	private int end;
	private int requirement;
	
	public int getStart() {
		return start;
	}
	
	public void setStart(int start) {
		this.start = start;
	}
	
	public int getEnd() {
		return end;
	}
	
	public void setEnd(int end) {
		this.end = end;
	}

	public int getRequirement() {
		return requirement;
	}

	public void setRequirement(int requirement) {
		this.requirement = requirement;
	}	
}
