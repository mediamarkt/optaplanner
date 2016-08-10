package org.optaplanner.examples.projectjobscheduling.domain;

import java.util.Comparator;

public class AllocationsPriorityComparator implements Comparator<Allocation>  {

	private int calculateWeight(Allocation allocation) {
		
		String priority = allocation.getJob().getPriority();
		String parentPriority = allocation.getJob().getParentPriority();
		
		return getPriorityCoefficient(parentPriority) * 10 + getPriorityCoefficient(priority);
	}
	
	private int getPriorityCoefficient(String priority) {
		if(priority == null)
			return 0;
		
		switch (priority)
		{
			case "Analysis": return 5;
			case "Trivial": return 10;
			case "Minor":return 15;
			case "Major": return 20;
			case "Critical": return 25;
			case "Blocker":return 30;
			default: return 0;
		}
	}
	
	@Override
	public int compare(Allocation arg0, Allocation arg1) {
		return calculateWeight(arg0) - calculateWeight(arg1);
	}
	
}
