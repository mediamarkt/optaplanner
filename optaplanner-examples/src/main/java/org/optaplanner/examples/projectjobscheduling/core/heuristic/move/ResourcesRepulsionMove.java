package org.optaplanner.examples.projectjobscheduling.core.heuristic.move;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.optaplanner.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import org.optaplanner.core.impl.heuristic.move.AbstractMove;
import org.optaplanner.core.impl.heuristic.move.Move;
import org.optaplanner.core.impl.score.director.ScoreDirector;
import org.optaplanner.core.impl.score.director.incremental.IncrementalScoreDirector;
import org.optaplanner.examples.projectjobscheduling.domain.Allocation;
import org.optaplanner.examples.projectjobscheduling.domain.resource.Resource;
import org.optaplanner.examples.projectjobscheduling.solver.score.ProjectJobSchedulingIncrementalScoreCalculator;
import org.optaplanner.examples.projectjobscheduling.solver.score.capacity.ResourceCapacityTracker;

import com.google.common.collect.Lists;

public class ResourcesRepulsionMove extends AbstractMove {
	
	private Change initialChange;
	private List<Change> changes;
		
	public ResourcesRepulsionMove(Object entity, GenuineVariableDescriptor variableDescriptor,
            Object toPlanningValue, ScoreDirector scoreDirector) {
		
		changes = new ArrayList<Change>();
		initialChange = new Change(entity, variableDescriptor, toPlanningValue);
		
		Collection<ResourceCapacityTracker> resourceCapacityTrackers = ((ProjectJobSchedulingIncrementalScoreCalculator)((IncrementalScoreDirector)scoreDirector).getIncrementalScoreCalculator()).getResourceCapacityTrackerMap().values();
		
		if(resourceCapacityTrackers.stream().anyMatch(tracker -> tracker.getHardScore() != 0)) {
			changes.add(initialChange);
		} else {
			RecursivlyAddChanges((Allocation)entity, variableDescriptor, toPlanningValue, scoreDirector, resourceCapacityTrackers);			
		}		
	}
	
	private void RecursivlyAddChanges(Allocation entity, GenuineVariableDescriptor variableDescriptor,
            Object toPlanningValue, ScoreDirector scoreDirector, Collection<ResourceCapacityTracker> resourceCapacityTrackers) {
		changes.add(new Change(entity, variableDescriptor, toPlanningValue));
		Object oldValue = variableDescriptor.getValue(entity);
		
		//Do move for see result
		scoreDirector.beforeVariableChanged(variableDescriptor, entity);
		variableDescriptor.setValue(entity, toPlanningValue);
        scoreDirector.afterVariableChanged(variableDescriptor, entity);
        
        if(resourceCapacityTrackers.stream().anyMatch(tracker -> tracker.getHardScore() != 0)) {
        	//TODO find and move earliest issue for fix capacity problem, recursive add new change
        	
        	
        }
        
        //Do undo move
        scoreDirector.beforeVariableChanged(variableDescriptor, entity);
		variableDescriptor.setValue(entity, oldValue);
        scoreDirector.afterVariableChanged(variableDescriptor, entity);        
	}
	
	
	private ResourcesRepulsionMove(Change initialChange, List<Change> changes) {
		Object oldValue = initialChange.getVariableDescriptor().getValue(initialChange.getEntity());
		this.initialChange = new Change(initialChange.getEntity(), initialChange.getVariableDescriptor(), oldValue);
		
		this.changes = Lists.reverse(changes.stream().map(c -> {
			Object ov = c.getVariableDescriptor().getValue(c.getEntity());
			return new Change(c.getEntity(), c.getVariableDescriptor(), ov);
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
		
		public Change(Object entity, GenuineVariableDescriptor variableDescriptor, Object toPlanningValue) {
			this.entity = entity;
			this.variableDescriptor = variableDescriptor;
			this.toPlanningValue = toPlanningValue;
		}		
		
		private Object entity;
		private GenuineVariableDescriptor variableDescriptor;
        private Object toPlanningValue;
        
        public Object getEntity() {
        	return entity;
        }
        
        public GenuineVariableDescriptor getVariableDescriptor() {
        	return variableDescriptor;
        }
        
        public Object getToPlanningValue() {
        	return toPlanningValue;
        }
	}
}
