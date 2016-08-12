package org.optaplanner.examples.projectjobscheduling.solver.score.capacity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.optaplanner.examples.projectjobscheduling.domain.Allocation;
import org.optaplanner.examples.projectjobscheduling.domain.ResourceRequirement;

public class TeamPerDayUsage {

	private int usedPerDay;
	
	private ArrayList<Allocation> allocations;
	
	public TeamPerDayUsage() {
		allocations = new ArrayList<Allocation>();
		usedPerDay = 0;
	}
	
	public int getUsedPerDay() {
		return usedPerDay;	
	}
	
	public void insert(ResourceRequirement resourceRequirement, Allocation allocation) {
		int requirement = resourceRequirement.getRequirement();
        usedPerDay += requirement;

		allocations.add(allocation);
	}
	
	public void retract(ResourceRequirement resourceRequirement, Allocation allocation) {
		int requirement = resourceRequirement.getRequirement();
        usedPerDay -= requirement;

		allocations.remove(allocation);
	}
	
	public void leave(int requirement) {
		usedPerDay += requirement;
	}

	public Allocation getNotFixedWithMinPriority() {

		int minPriority = allocations.stream().filter(allocation -> allocation.getJob().getFixedStartDate() == null).min((o1, o2) -> Integer.compare(o1.calculatePriorityWeight(), o2.calculatePriorityWeight())).get().calculatePriorityWeight();
		List<Allocation> minPriorityAllocations = allocations.stream().filter(allocation -> allocation.getJob().getFixedStartDate() == null && allocation.calculatePriorityWeight() == minPriority).collect(Collectors.toList());

		return minPriorityAllocations.stream().max((o1, o2) -> Integer.compare(o1.getStartDate(), o2.getStartDate())).get();
	}

	public boolean hasMovedAllocations() {
		return allocations.stream().anyMatch(allocation -> allocation.getJob().getFixedStartDate() == null);
	}


}
