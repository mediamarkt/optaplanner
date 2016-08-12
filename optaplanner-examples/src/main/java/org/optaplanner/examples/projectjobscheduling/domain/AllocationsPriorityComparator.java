package org.optaplanner.examples.projectjobscheduling.domain;

import java.util.Comparator;

public class AllocationsPriorityComparator implements Comparator<Allocation>  {
	@Override
	public int compare(Allocation arg0, Allocation arg1) {
		return arg0.calculatePriorityWeight() - arg1.calculatePriorityWeight();
	}
	
}
