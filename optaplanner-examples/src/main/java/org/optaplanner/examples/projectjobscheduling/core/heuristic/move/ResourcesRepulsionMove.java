package org.optaplanner.examples.projectjobscheduling.core.heuristic.move;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import org.optaplanner.core.impl.heuristic.move.AbstractMove;
import org.optaplanner.core.impl.heuristic.move.Move;
import org.optaplanner.core.impl.score.director.ScoreDirector;
import org.optaplanner.core.impl.score.director.incremental.IncrementalScoreDirector;
import org.optaplanner.examples.projectjobscheduling.domain.Allocation;
import org.optaplanner.examples.projectjobscheduling.solver.score.ProjectJobSchedulingIncrementalScoreCalculator;
import org.optaplanner.examples.projectjobscheduling.solver.score.capacity.CapacityProblem;
import org.optaplanner.examples.projectjobscheduling.solver.score.capacity.CapacityProblemsDetector;
import org.optaplanner.examples.projectjobscheduling.solver.score.capacity.ResourceCapacityTracker;
import org.optaplanner.examples.projectjobscheduling.solver.score.capacity.TeamResourceCapacityTracker;

import com.google.common.collect.Lists;

public class ResourcesRepulsionMove extends AbstractMove {
	
	private Change initialChange;
	private List<Change> changes;
		
	public ResourcesRepulsionMove(Object entity, GenuineVariableDescriptor variableDescriptor,
            Object toPlanningValue, ScoreDirector scoreDirector) {
		
		changes = new ArrayList<Change>();

		Object oldValue = variableDescriptor.getValue(entity);
		initialChange = new Change(entity, variableDescriptor, toPlanningValue, oldValue);

		Collection<ResourceCapacityTracker> resourceCapacityTrackers = ((ProjectJobSchedulingIncrementalScoreCalculator)((IncrementalScoreDirector)scoreDirector).getIncrementalScoreCalculator()).getResourceCapacityTrackerMap().values();
		List<TeamResourceCapacityTracker> teamResourceCapacityTrackers = (List<TeamResourceCapacityTracker>)(List<?>) new ArrayList(resourceCapacityTrackers);

		AddChanges((Allocation)entity, variableDescriptor, toPlanningValue, scoreDirector, teamResourceCapacityTrackers);
	}
	
	private void AddChanges(Allocation entity, GenuineVariableDescriptor variableDescriptor,
            Object toPlanningValue, ScoreDirector scoreDirector, List<TeamResourceCapacityTracker> resourceCapacityTrackers) {

		//till before all changes
		CapacityProblemsDetector capacityProblemsDetector = new CapacityProblemsDetector(resourceCapacityTrackers);

		Object oldValue = variableDescriptor.getValue(entity);
		changes.add(new Change(entity, variableDescriptor, toPlanningValue, oldValue));

		//Do move for see result
		scoreDirector.beforeVariableChanged(variableDescriptor, entity);
		variableDescriptor.setValue(entity, toPlanningValue);
        scoreDirector.afterVariableChanged(variableDescriptor, entity);

		for (CapacityProblem capacityProblem : capacityProblemsDetector) {

			Object old = variableDescriptor.getValue(capacityProblem.getAllocation());
			Change newChange = new Change(capacityProblem.getAllocation(), variableDescriptor, capacityProblem.getNewDelay(), old);
			changes.add(newChange);

			scoreDirector.beforeVariableChanged(newChange.getVariableDescriptor(), newChange.getEntity());
			variableDescriptor.setValue(newChange.getEntity(), newChange.getToPlanningValue());
			scoreDirector.afterVariableChanged(newChange.getVariableDescriptor(), newChange.getEntity());
		}

		//Do undo move
		List<Change> back = Lists.reverse(changes.stream().map(c -> {
			return new Change(c.getEntity(), c.getVariableDescriptor(), c.getOldValue(), c.getToPlanningValue());
		}).collect(Collectors.toList()));

		for (Change change : back) {
			scoreDirector.beforeVariableChanged(change.getVariableDescriptor(), change.getEntity());
			variableDescriptor.setValue(change.getEntity(), change.getToPlanningValue());
			scoreDirector.afterVariableChanged(change.getVariableDescriptor(), change.getEntity());
		}
	}
	
	
	private ResourcesRepulsionMove(Change initialChange, List<Change> changes) {
		this.initialChange = new Change(initialChange.getEntity(), initialChange.getVariableDescriptor(), initialChange.getOldValue(), initialChange.getToPlanningValue());
		
		this.changes = Lists.reverse(changes.stream().map(c -> {
			return new Change(c.getEntity(), c.getVariableDescriptor(), c.getOldValue(), c.getToPlanningValue());
		}).collect(Collectors.toList()));		
	}	
	
	@Override
	public boolean isMoveDoable(ScoreDirector scoreDirector) {
		Object oldValue = initialChange.getVariableDescriptor().getValue(initialChange.getEntity());
        return !ObjectUtils.equals(oldValue, initialChange.getToPlanningValue());
	}

	@Override
	public Move createUndoMove(ScoreDirector scoreDirector) {
		return new ResourcesRepulsionMove(initialChange, changes);
	}

	@Override
	public Collection<? extends Object> getPlanningEntities() {
		return changes.stream().map(x -> x.getEntity()).collect(Collectors.toList());
	}

	@Override
	public Collection<? extends Object> getPlanningValues() {
		return changes.stream().map(x -> x.getToPlanningValue()).collect(Collectors.toList());
	}

	@Override
	protected void doMoveOnGenuineVariables(ScoreDirector scoreDirector) {
		for(Change change : changes) {
			scoreDirector.beforeVariableChanged(change.getVariableDescriptor(), change.getEntity());
	        change.getVariableDescriptor().setValue(change.getEntity(), change.getToPlanningValue());
	        scoreDirector.afterVariableChanged(change.getVariableDescriptor(), change.getEntity());			
		}
	}
	
	private class Change {
		
		public Change(Object entity, GenuineVariableDescriptor variableDescriptor, Object toPlanningValue, Object oldValue) {
			this.entity = entity;
			this.variableDescriptor = variableDescriptor;
			this.toPlanningValue = toPlanningValue;
			this.oldValue = oldValue;
		}
		
		private Object entity;
		private GenuineVariableDescriptor variableDescriptor;
        private Object toPlanningValue;
		private Object oldValue;
        
        public Object getEntity() {
        	return entity;
        }
        
        public GenuineVariableDescriptor getVariableDescriptor() {
        	return variableDescriptor;
        }
        
        public Object getToPlanningValue() {
        	return toPlanningValue;
        }

        public Object getOldValue() { return oldValue; }
	}
}
