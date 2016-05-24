package org.optaplanner.examples.projectjobscheduling.core.heuristic.move;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.optaplanner.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import org.optaplanner.core.impl.heuristic.move.AbstractMove;
import org.optaplanner.core.impl.heuristic.move.Move;
import org.optaplanner.core.impl.score.director.ScoreDirector;



import com.google.common.collect.Lists;

public class ResourcesRepulsionMove extends AbstractMove {
	
	private Change initialChange;
	private List<Change> changes;
		
	public ResourcesRepulsionMove(Object entity, GenuineVariableDescriptor variableDescriptor,
            Object toPlanningValue) {
		
		changes = new ArrayList<Change>();		
		initialChange = new Change(entity, variableDescriptor, toPlanningValue);
		changes.add(initialChange);
		//TODO get resources repulsion moves.
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
