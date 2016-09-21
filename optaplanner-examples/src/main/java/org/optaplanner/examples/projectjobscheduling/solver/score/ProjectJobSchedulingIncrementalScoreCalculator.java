/*
 * Copyright 2015 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.examples.projectjobscheduling.solver.score;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.score.buildin.bendable.BendableScore;
import org.optaplanner.core.impl.score.director.incremental.AbstractIncrementalScoreCalculator;
import org.optaplanner.examples.projectjobscheduling.domain.Allocation;
import org.optaplanner.examples.projectjobscheduling.domain.ExecutionMode;
import org.optaplanner.examples.projectjobscheduling.domain.JobType;
import org.optaplanner.examples.projectjobscheduling.domain.Project;
import org.optaplanner.examples.projectjobscheduling.domain.ResourceRequirement;
import org.optaplanner.examples.projectjobscheduling.domain.Schedule;
import org.optaplanner.examples.projectjobscheduling.domain.resource.Resource;
import org.optaplanner.examples.projectjobscheduling.solver.score.capacity.NonrenewableResourceCapacityTracker;
import org.optaplanner.examples.projectjobscheduling.solver.score.capacity.RenewableResourceCapacityTracker;
import org.optaplanner.examples.projectjobscheduling.solver.score.capacity.ResourceCapacityTracker;
import org.optaplanner.examples.projectjobscheduling.solver.score.capacity.TeamResourceCapacityTracker;

public class ProjectJobSchedulingIncrementalScoreCalculator extends AbstractIncrementalScoreCalculator<Schedule> {

	public ProjectJobSchedulingIncrementalScoreCalculator(int unallowedWeeksCountForDraft, int unallowedWeeksCountForAnalysis) {
		this.unallowedWeeksCountForDraft = unallowedWeeksCountForDraft;
		this.unallowedWeeksCountForAnalysis = unallowedWeeksCountForAnalysis;
	}	
	
	private Map<Resource, ResourceCapacityTracker> resourceCapacityTrackerMap;
	private Map<Project, Integer> projectEndDateMap;
	private int maximumProjectEndDate;

	private int resourceCapcityViolations;
	private int totalProjectDelay;
	private int totalJobDelay;
	private int totalMakeSpan;
	private int totalEndSyncGap;
	private HashMap<String, Integer> priorityBreqJobDelays;
	private HashMap<String, Integer> priorityPreqJobDelays;

	private HashMap<Integer, Integer> preqsJobDalaysByPriorityMarks;

	private int totalCommitmentOverrun;
	private int totalTimedJobMakespan;
	
	private int draftEarlyStarted;
	
	private int unallowedWeeksCountForDraft;
	private int unallowedWeeksCountForAnalysis;

	public void resetWorkingSolution(Schedule schedule) {
		List<Resource> resourceList = schedule.getResourceList();
		resourceCapacityTrackerMap = new HashMap<Resource, ResourceCapacityTracker>(resourceList.size());
		for (Resource resource : resourceList) {
			ResourceCapacityTracker tracker;
			if(resource.isRenewable()) {
				TeamResourceCapacityTracker renewableResourceCapacityTracker = new TeamResourceCapacityTracker(resource);
				renewableResourceCapacityTracker.setLeaves(resource.getResourceLeaves());
				tracker = renewableResourceCapacityTracker;
			} else {
				tracker = new NonrenewableResourceCapacityTracker(resource);
			}
						
			resourceCapacityTrackerMap.put(resource, tracker);
		}
		List<Project> projectList = schedule.getProjectList();
		projectEndDateMap = new HashMap<Project, Integer>(projectList.size());
		maximumProjectEndDate = 0;
		resourceCapcityViolations = 0;
		totalProjectDelay = 0;
		totalJobDelay = 0;
		totalMakeSpan = 0;
		totalEndSyncGap = 0;
		totalCommitmentOverrun = 0;
		totalTimedJobMakespan = 0;
		draftEarlyStarted = 0;
		priorityBreqJobDelays = new HashMap();
		priorityPreqJobDelays = new HashMap();
		preqsJobDalaysByPriorityMarks = new HashMap();
		int minimumReleaseDate = Integer.MAX_VALUE;
		for (Project p : projectList) {
			minimumReleaseDate = Math.min(p.getReleaseDate(), minimumReleaseDate);
		}
		totalMakeSpan += minimumReleaseDate;
		for (Allocation allocation : schedule.getAllocationList()) {
			insert(allocation);
		}
	}
	
	public Map<Resource, ResourceCapacityTracker> getResourceCapacityTrackerMap() {
		return resourceCapacityTrackerMap;
	}

	public void beforeEntityAdded(Object entity) {
		// Do nothing
	}

	public void afterEntityAdded(Object entity) {
		insert((Allocation) entity);
	}

	public void beforeVariableChanged(Object entity, String variableName) {
		retract((Allocation) entity);
	}

	public void afterVariableChanged(Object entity, String variableName) {
		insert((Allocation) entity);
	}

	public void beforeEntityRemoved(Object entity) {
		retract((Allocation) entity);
	}

	public void afterEntityRemoved(Object entity) {
		// Do nothing
	}

	private void insert(Allocation allocation) {
		// Job precedence is build-in
		// Resource capacity
		ExecutionMode executionMode = allocation.getExecutionMode();
		if (executionMode != null && allocation.getJob().getJobType() == JobType.STANDARD) {
			for (ResourceRequirement resourceRequirement : executionMode.getResourceRequirementList()) {
				ResourceCapacityTracker tracker = resourceCapacityTrackerMap.get(resourceRequirement.getResource());
				resourceCapcityViolations -= tracker.getHardScore();
				tracker.insert(resourceRequirement, allocation);
				resourceCapcityViolations += tracker.getHardScore();
			}
		}
		// Total project delay and total make span
		if (allocation.getJob().getJobType() == JobType.SUPER_SINK) {
			Integer endDate = allocation.getEndDate();
			if (endDate != null) {
				Project project = allocation.getProject();
				projectEndDateMap.put(project, endDate);
				// Total project delay
				totalProjectDelay -= endDate - project.getCriticalPathEndDate();
				// Total make span
				if (endDate > maximumProjectEndDate) {
					totalMakeSpan -= endDate - maximumProjectEndDate;
					maximumProjectEndDate = endDate;
				}
			}
		}

		// Total job delay
		if (allocation.getJob().getJobType() == JobType.STANDARD) {
			totalJobDelay -= allocation.getDelay() == null ? 0 : allocation.getDelay();
		}

		// Total work end sync gap
		if (allocation.getJob().getEndSyncClockStartMarks() != 0) {
			totalEndSyncGap += allocation.getEndDate() * allocation.getJob().getEndSyncClockStartMarks();

		}
		if (allocation.getJob().getEndSyncClockEndMarks() != 0) {
			totalEndSyncGap -= allocation.getStartDate() * allocation.getJob().getEndSyncClockEndMarks();
		}

		// Total timed job make span
		if (allocation.getJob().getTimingClockStartMarks() != 0) {
			totalTimedJobMakespan += allocation.getStartDate() * allocation.getJob().getTimingClockStartMarks();

		}
		if (allocation.getJob().getTimingClockEndMarks() != 0) {
			totalTimedJobMakespan -= allocation.getEndDate() * allocation.getJob().getTimingClockEndMarks();
		}
		
		//Priority BREQ jobs delay
		if(allocation.getJob().getJobType() == JobType.BREQ_SINK){
			if (allocation.getJob().getPriority() != null) {
				if (!priorityBreqJobDelays.containsKey(allocation.getJob().getPriority())) {
					priorityBreqJobDelays.put(allocation.getJob().getPriority(), 0);
				}
				priorityBreqJobDelays.compute(allocation.getJob().getPriority(), (a, b) -> b - allocation.getEndDate());
			}
		}

		//Priority PREQ jobs delay
		if(allocation.getJob().getJobType() == JobType.PREQ_SINK){
			if (allocation.getJob().getPriority() != null) {
				if (!priorityPreqJobDelays.containsKey(allocation.getJob().getPriority())) {
					priorityPreqJobDelays.put(allocation.getJob().getPriority(), 0);
				}
				priorityPreqJobDelays.compute(allocation.getJob().getPriority(), (a, b) -> b - allocation.getEndDate());
			}
		}

		//New priorities
		if(allocation.getJob().getJobType() == JobType.PREQ_SINK){
			Integer weight = allocation.calculatePriorityWeight();
			if (!preqsJobDalaysByPriorityMarks.containsKey(weight)) {
				preqsJobDalaysByPriorityMarks.put(weight, 0);
				}
			preqsJobDalaysByPriorityMarks.compute(weight, (a, b) -> b - allocation.getEndDate());
		}

		// Committed date overruns
		if (allocation.getJob().getCommittedDay() != 0) {
			totalCommitmentOverrun -= (allocation.getEndDate() > allocation.getJob().getCommittedDay())
					? allocation.getEndDate() - allocation.getJob().getCommittedDay() : 0;
		}
		
		if(allocation.getJob().getJobType() == JobType.STANDARD) {
			if("Draft".equals(allocation.getJob().getPriority())) {
				int delay = allocation.getDelay() != null ? allocation.getDelay() : 0;
				draftEarlyStarted -= Math.max(unallowedWeeksCountForDraft * 5 - (delay + allocation.getPredecessorsDoneDate()), 0);
			} else if("Analysis".equals(allocation.getJob().getPriority())) {
				int delay = allocation.getDelay() != null ? allocation.getDelay() : 0;
				draftEarlyStarted -= Math.max(unallowedWeeksCountForAnalysis * 5 - (delay + allocation.getPredecessorsDoneDate()), 0);
			}
		}


	}

	private void retract(Allocation allocation) {
		// Job precedence is build-in
		// Resource capacity
		ExecutionMode executionMode = allocation.getExecutionMode();
		if (executionMode != null && allocation.getJob().getJobType() == JobType.STANDARD) {
			for (ResourceRequirement resourceRequirement : executionMode.getResourceRequirementList()) {
				ResourceCapacityTracker tracker = resourceCapacityTrackerMap.get(resourceRequirement.getResource());
				resourceCapcityViolations -= tracker.getHardScore();
				tracker.retract(resourceRequirement, allocation);
				resourceCapcityViolations += tracker.getHardScore();
			}
		}
		// Total project delay and total make span
		if (allocation.getJob().getJobType() == JobType.SUPER_SINK) {
			Integer endDate = allocation.getEndDate();
			if (endDate != null) {
				Project project = allocation.getProject();
				projectEndDateMap.remove(project);
				// Total project delay
				totalProjectDelay += endDate - project.getCriticalPathEndDate();
				// Total make span
				if (endDate == maximumProjectEndDate) {
					updateMaximumProjectEndDate();
					totalMakeSpan += endDate - maximumProjectEndDate;
				}
			}
		}

		// Total job delay
		if (allocation.getJob().getJobType() == JobType.STANDARD) {
			totalJobDelay += allocation.getDelay() == null ? 0 : allocation.getDelay();
		}

		// Total work end sync gap
		if (allocation.getJob().getEndSyncClockStartMarks() != 0) {
			totalEndSyncGap -= allocation.getEndDate() * allocation.getJob().getEndSyncClockStartMarks();

		}
		if (allocation.getJob().getEndSyncClockEndMarks() != 0) {
			totalEndSyncGap += allocation.getStartDate() * allocation.getJob().getEndSyncClockEndMarks(); 
		}

		// Total timed job make span
		if (allocation.getJob().getTimingClockStartMarks() != 0) {
			totalTimedJobMakespan -= allocation.getStartDate() * allocation.getJob().getTimingClockStartMarks();

		}
		if (allocation.getJob().getTimingClockEndMarks() != 0) {
			totalTimedJobMakespan += allocation.getEndDate() * allocation.getJob().getTimingClockEndMarks();
		}
		
		// Priority BREQ jobs delay
		if(allocation.getJob().getJobType() == JobType.BREQ_SINK){
			if (allocation.getJob().getPriority() != null) {
				if (!priorityBreqJobDelays.containsKey(allocation.getJob().getPriority())) {
					priorityBreqJobDelays.put(allocation.getJob().getPriority(), 0);
				}
				priorityBreqJobDelays.compute(allocation.getJob().getPriority(), (a, b) -> b + allocation.getEndDate());
			}
		}

		// Priority PREQ jobs delay
		if(allocation.getJob().getJobType() == JobType.PREQ_SINK){
			if (allocation.getJob().getPriority() != null) {
				if (!priorityPreqJobDelays.containsKey(allocation.getJob().getPriority())) {
					priorityPreqJobDelays.put(allocation.getJob().getPriority(), 0);
				}
				priorityPreqJobDelays.compute(allocation.getJob().getPriority(), (a, b) -> b + allocation.getEndDate());
			}
		}

		//New priorities
		if(allocation.getJob().getJobType() == JobType.PREQ_SINK){
			Integer weight = allocation.calculatePriorityWeight();
			if (!preqsJobDalaysByPriorityMarks.containsKey(weight)) {
				preqsJobDalaysByPriorityMarks.put(weight, 0);
			}
			preqsJobDalaysByPriorityMarks.compute(weight, (a, b) -> b + allocation.getEndDate());
		}
		
		// Committed date overruns
		if (allocation.getJob().getCommittedDay() != 0) {
			totalCommitmentOverrun += (allocation.getEndDate() > allocation.getJob().getCommittedDay())
					? allocation.getEndDate() - allocation.getJob().getCommittedDay() : 0;
		}
				
		if(allocation.getJobType() == JobType.STANDARD) {
			if("Draft".equals(allocation.getJob().getPriority())) {
				int delay = allocation.getDelay() != null ? allocation.getDelay() : 0;
				draftEarlyStarted += Math.max(unallowedWeeksCountForDraft * 5 - (delay + allocation.getPredecessorsDoneDate()), 0);
			} else if("Analysis".equals(allocation.getJob().getPriority())) {
				int delay = allocation.getDelay() != null ? allocation.getDelay() : 0;
				draftEarlyStarted += Math.max(unallowedWeeksCountForAnalysis * 5 - (delay + allocation.getPredecessorsDoneDate()), 0);
			}
		}
	}

	private void updateMaximumProjectEndDate() {
		int maximum = 0;
		for (Integer endDate : projectEndDateMap.values()) {
			if (endDate > maximum) {
				maximum = endDate;
			}
		}
		maximumProjectEndDate = maximum;
	}

	private int getCombinedScoreByPriorityForPreq(String priority) {
		Integer parentWeight = JobsPrioritiesWeightsProvider.getPriorityWeight(priority);
		List<Integer> weights = JobsPrioritiesWeightsProvider.getPrioritiesWeights();

		Integer score = 0;

		for (Integer w : weights) {
			Integer key = parentWeight * 10 + w;
			Integer ends = preqsJobDalaysByPriorityMarks.containsKey(key) ? preqsJobDalaysByPriorityMarks.get(key) : 0;

			score += ends * w;
		}

		return score;
	}
	
	private int getScoreByPriorityForBreq(String priority) {
		return priorityBreqJobDelays.containsKey(priority) ? priorityBreqJobDelays.get(priority) : 0;
 	}

	public Score calculateScore() {
		return BendableScore.valueOf(
				new int[] { 
						resourceCapcityViolations, draftEarlyStarted						
				},
				new int[] {
						totalCommitmentOverrun,
						getCombinedScoreByPriorityForPreq("Blocker"),
						getScoreByPriorityForBreq("Blocker"),
						getCombinedScoreByPriorityForPreq("Critical"),
						getScoreByPriorityForBreq("Critical"),
						totalProjectDelay,
						getCombinedScoreByPriorityForPreq("Major"),
						getScoreByPriorityForBreq("Major"),
						getCombinedScoreByPriorityForPreq("Minor"),
						getScoreByPriorityForBreq("Minor"),
						getCombinedScoreByPriorityForPreq("Trivial"),
						getScoreByPriorityForBreq("Trivial"),
						getCombinedScoreByPriorityForPreq("Analysis"),
						getScoreByPriorityForBreq("Analysis"),
						getCombinedScoreByPriorityForPreq("Draft"),
						getScoreByPriorityForBreq("Draft"),
						totalTimedJobMakespan,
						totalEndSyncGap,
						totalJobDelay
				});
	}

}
