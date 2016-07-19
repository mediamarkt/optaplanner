package org.optaplanner.examples.projectjobscheduling.solver.score;

import java.lang.reflect.InvocationTargetException;

import org.optaplanner.core.config.util.ConfigUtils;
import org.optaplanner.core.impl.score.director.incremental.IncrementalScoreCalculator;
import org.optaplanner.core.impl.score.director.incremental.IncrementalScoreDirector;
import org.optaplanner.core.impl.score.director.incremental.IncrementalScoreDirectorFactory;

public class ProjectJobShedulingIncrementalScoreDirectorFactory extends IncrementalScoreDirectorFactory {

	protected int unallowedWeeksCountForDraft;
	private Class<? extends IncrementalScoreCalculator> incrementalScoreCalculatorClass;
	
	public ProjectJobShedulingIncrementalScoreDirectorFactory(
			Class<? extends IncrementalScoreCalculator> incrementalScoreCalculatorClass, int unallowedWeeksCountForDraft) {
		super(incrementalScoreCalculatorClass);
		
		this.incrementalScoreCalculatorClass = incrementalScoreCalculatorClass; 
		this.unallowedWeeksCountForDraft = unallowedWeeksCountForDraft;
	}
	
	public IncrementalScoreDirector buildScoreDirector(boolean constraintMatchEnabledPreference) {
        IncrementalScoreCalculator incrementalScoreCalculator;
		try {
			incrementalScoreCalculator = incrementalScoreCalculatorClass.getDeclaredConstructor(int.class).newInstance(unallowedWeeksCountForDraft);
		} catch (Exception e) {
			throw new IllegalArgumentException("The " + this.getClass().getSimpleName() + "'s " +  "incrementalScoreCalculatorClass" + " ("
                    + incrementalScoreCalculatorClass.getName() + ") was not created.", e);
		}        
      
        return new IncrementalScoreDirector(this, constraintMatchEnabledPreference, incrementalScoreCalculator);
    }
}
