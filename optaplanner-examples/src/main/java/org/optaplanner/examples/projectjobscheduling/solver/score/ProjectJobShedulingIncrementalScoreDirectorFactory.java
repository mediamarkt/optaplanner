package org.optaplanner.examples.projectjobscheduling.solver.score;

import java.lang.reflect.InvocationTargetException;

import org.optaplanner.core.config.util.ConfigUtils;
import org.optaplanner.core.impl.score.director.incremental.IncrementalScoreCalculator;
import org.optaplanner.core.impl.score.director.incremental.IncrementalScoreDirector;
import org.optaplanner.core.impl.score.director.incremental.IncrementalScoreDirectorFactory;

public class ProjectJobShedulingIncrementalScoreDirectorFactory extends IncrementalScoreDirectorFactory {

	protected int unallowedWeeksCountForDraft;
	protected int unallowedWeeksCountForAnalysis;
	
	private Class<? extends IncrementalScoreCalculator> incrementalScoreCalculatorClass;
	
	public ProjectJobShedulingIncrementalScoreDirectorFactory(
			Class<? extends IncrementalScoreCalculator> incrementalScoreCalculatorClass, int unallowedWeeksCountForDraft, int unallowedWeeksCountForAnalysis) {
		super(incrementalScoreCalculatorClass);
		
		this.incrementalScoreCalculatorClass = incrementalScoreCalculatorClass; 
		this.unallowedWeeksCountForDraft = unallowedWeeksCountForDraft;
		this.unallowedWeeksCountForAnalysis = unallowedWeeksCountForAnalysis;
	}
	
	public IncrementalScoreDirector buildScoreDirector(boolean constraintMatchEnabledPreference) {
        IncrementalScoreCalculator incrementalScoreCalculator;
		try {
			incrementalScoreCalculator = incrementalScoreCalculatorClass.getDeclaredConstructor(int.class, int.class).newInstance(unallowedWeeksCountForDraft, unallowedWeeksCountForAnalysis);
		} catch (Exception e) {
			throw new IllegalArgumentException("The " + this.getClass().getSimpleName() + "'s " +  "incrementalScoreCalculatorClass" + " ("
                    + incrementalScoreCalculatorClass.getName() + ") was not created.", e);
		}        
      
        return new IncrementalScoreDirector(this, constraintMatchEnabledPreference, incrementalScoreCalculator);
    }
}
