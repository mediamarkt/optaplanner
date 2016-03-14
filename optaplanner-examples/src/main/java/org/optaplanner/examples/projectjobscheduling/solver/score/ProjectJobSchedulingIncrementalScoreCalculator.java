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

public class ProjectJobSchedulingIncrementalScoreCalculator extends AbstractIncrementalScoreCalculator<Schedule> {

	private Map<Resource, ResourceCapacityTracker> resourceCapacityTrackerMap;
	private Map<Project, Integer> projectEndDateMap;
	private int maximumProjectEndDate;

	private int resourceCapcityViolations;
	private int totalProjectDelay;
	private int totalJobDelay;
	private int totalMakeSpan;
	private int totalEndSyncGap;
	private HashMap<String, Integer> priorityJobDelays;
	private int totalCommitmentOverrun;
	private int totalTimedJobMakespan;

	public void resetWorkingSolution(Schedule schedule) {
		List<Resource> resourceList = schedule.getResourceList();
		resourceCapacityTrackerMap = new HashMap<Resource, ResourceCapacityTracker>(resourceList.size());
		for (Resource resource : resourceList) {
			resourceCapacityTrackerMap.put(resource,
					resource.isRenewable() ? new RenewableResourceCapacityTracker(resource)
							: new NonrenewableResourceCapacityTracker(resource));
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
		priorityJobDelays = new HashMap();
		int minimumReleaseDate = Integer.MAX_VALUE;
		for (Project p : projectList) {
			minimumReleaseDate = Math.min(p.getReleaseDate(), minimumReleaseDate);
		}
		totalMakeSpan += minimumReleaseDate;
		for (Allocation allocation : schedule.getAllocationList()) {
			insert(allocation);
		}
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
		if (allocation.getJob().getJobType() == JobType.SINK) {
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
		
		// Priority jobs delay
		if (allocation.getJob().getPriorityMark() != null) {
			if (!priorityJobDelays.containsKey(allocation.getJob().getPriorityMark())) {
				priorityJobDelays.put(allocation.getJob().getPriorityMark(), 0);
			}
			priorityJobDelays.compute(allocation.getJob().getPriorityMark(), (a, b) -> b - allocation.getEndDate());
		}

		// Committed date overruns
		if (allocation.getJob().getCommittedDay() != 0) {
			totalCommitmentOverrun -= (allocation.getEndDate() > allocation.getJob().getCommittedDay())
					? allocation.getEndDate() - allocation.getJob().getCommittedDay() : 0;
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
		if (allocation.getJob().getJobType() == JobType.SINK) {
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
		
		// Priority jobs delay
		if (allocation.getJob().getPriorityMark() != null) {
			if (!priorityJobDelays.containsKey(allocation.getJob().getPriorityMark())) {
				priorityJobDelays.put(allocation.getJob().getPriorityMark(), 0);
			}
			priorityJobDelays.compute(allocation.getJob().getPriorityMark(), (a, b) -> b + allocation.getEndDate());
		}

		// Committed date overruns
		if (allocation.getJob().getCommittedDay() != 0) {
			totalCommitmentOverrun += (allocation.getEndDate() > allocation.getJob().getCommittedDay())
					? allocation.getEndDate() - allocation.getJob().getCommittedDay() : 0;
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

	public Score calculateScore() {
		return BendableScore.valueOf(new int[] { resourceCapcityViolations },
				new int[] { totalCommitmentOverrun,
						(priorityJobDelays.containsKey("Blocker") ? priorityJobDelays.get("Blocker") : 0)
								+ (priorityJobDelays.containsKey("Critical") ? priorityJobDelays.get("Critical") : 0),
						totalProjectDelay,
						priorityJobDelays.containsKey("Major") ? priorityJobDelays.get("Major") : 0,
						totalTimedJobMakespan,
						totalEndSyncGap,
						totalJobDelay });
	}

}
