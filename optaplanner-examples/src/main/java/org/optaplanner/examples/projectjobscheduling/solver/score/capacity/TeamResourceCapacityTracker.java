package org.optaplanner.examples.projectjobscheduling.solver.score.capacity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.optaplanner.examples.projectjobscheduling.domain.Allocation;
import org.optaplanner.examples.projectjobscheduling.domain.ResourceRequirement;
import org.optaplanner.examples.projectjobscheduling.domain.resource.Resource;
import org.optaplanner.examples.projectjobscheduling.domain.resource.ResourceLeave;

public class TeamResourceCapacityTracker extends ResourceCapacityTracker {

	protected int capacityEveryDay;

    protected Map<Integer, TeamPerDayUsage> usedPerDay;

    protected Map<Integer, TeamPerDayUsage> problemsDays;

    protected int hardScore;
    
    
    public TeamResourceCapacityTracker(Resource resource) {
        super(resource);
        if (!resource.isRenewable()) {
            throw new IllegalArgumentException("The resource (" + resource + ") is expected to be renewable.");
        }
        capacityEveryDay = resource.getCapacity();

        usedPerDay = new HashMap<Integer, TeamPerDayUsage>();
        problemsDays = new HashMap<Integer, TeamPerDayUsage>();

        hardScore = 0;
    }

    @Override
    public void insert(ResourceRequirement resourceRequirement, Allocation allocation) {
    	    	
        int startDate = allocation.getStartDate();
        int endDate = allocation.getEndDate();
        for (int i = startDate; i < endDate; i++) {

            TeamPerDayUsage teamUsed = usedPerDay.get(i);
        	if (teamUsed == null) {
        		teamUsed = new TeamPerDayUsage();
            }        	
        	
        	int used = teamUsed.getUsedPerDay(); 
            
            if (used > capacityEveryDay) {
                hardScore += (used - capacityEveryDay);

                problemsDays.remove(i);
            }
            
            teamUsed.insert(resourceRequirement, allocation);
            used = teamUsed.getUsedPerDay();
            
            if (used > capacityEveryDay) {
                hardScore -= (used - capacityEveryDay);

                problemsDays.put(i, teamUsed);
            }
            
            usedPerDay.put(i, teamUsed);
        }
    }
    
    @Override
    public void retract(ResourceRequirement resourceRequirement, Allocation allocation) {       	
    	
        int startDate = allocation.getStartDate();
        int endDate = allocation.getEndDate();
        for (int i = startDate; i < endDate; i++) {

            TeamPerDayUsage teamUsed = usedPerDay.get(i);

            int used = teamUsed.getUsedPerDay();
            
            if (used > capacityEveryDay) {
                hardScore += (used - capacityEveryDay);

                problemsDays.remove(i);
            }
            
            teamUsed.retract(resourceRequirement, allocation);
            used = teamUsed.getUsedPerDay();

            if (used > capacityEveryDay) {
                hardScore -= (used - capacityEveryDay);

                problemsDays.put(i, teamUsed);
            }

            usedPerDay.put(i, teamUsed);
        }
    }

	public void setLeaves(List<ResourceLeave> resourceLeaves) {
    	if(resourceLeaves == null) return;
    	
    	for(ResourceLeave leave : resourceLeaves) {
    		int startDate = leave.getStart();
    		int endDate = leave.getEnd();
    		int requirement = leave.getRequirement();
    		
    		if(startDate > endDate) continue;
    		
    		for (int i = startDate; i < endDate; i++) {
    			
    			TeamPerDayUsage teamUsed = usedPerDay.get(i);
    			if (teamUsed == null) {
    				teamUsed = new TeamPerDayUsage();
                }
    			
                int used = teamUsed.getUsedPerDay();
                
                if (used > capacityEveryDay) {
                    hardScore += (used - capacityEveryDay);
                }
                
                teamUsed.leave(requirement);
                used = teamUsed.getUsedPerDay();
                
                if (used > capacityEveryDay) {
                    hardScore -= (used - capacityEveryDay);
                }
                
                usedPerDay.put(i, teamUsed);
            }
    	}
    }

	@Override
	public int getHardScore() {
		return hardScore;
	}

	public CapacityProblem GetFirstProblem(){

        Iterator<Integer> iterator = problemsDays.keySet().stream().sorted().iterator();

        Integer startDay = iterator.next();

        while (!problemsDays.get(startDay).hasMovedAllocations()) {
            startDay = iterator.next();
        }

        Integer endDay = startDay;

        while (iterator.hasNext()) {

            Integer next = iterator.next();

            if (next != endDay + 1) break;

            endDay = next;
        }

        Allocation allocation = problemsDays.get(startDay).getNotFixedWithMinPriority();

        int addingDelay =  endDay - allocation.getStartDate() + 1;
        int newDelay =  allocation.getDelay() + addingDelay;

	    return new CapacityProblem(allocation, newDelay);
    }

    private int maxHardScore;

    public int getMaxHardScore() {
        return maxHardScore;
    }

    public void saveMaxHardScore() {
        maxHardScore = hardScore;
    }
}
