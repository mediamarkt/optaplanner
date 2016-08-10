package org.optaplanner.examples.projectjobscheduling.solver.score;

import org.optaplanner.core.config.score.director.ScoreDirectorFactoryConfig;
import org.optaplanner.core.impl.score.director.AbstractScoreDirectorFactory;
import org.optaplanner.core.impl.score.director.incremental.IncrementalScoreCalculator;

import com.thoughtworks.xstream.annotations.XStreamAlias;

public class ProjectJobSchedulingScoreDirectorFactoryConfig extends ScoreDirectorFactoryConfig {
	
	protected int unallowedWeeksCountForDraft;
	protected int unallowedWeeksCountForAnalysis;
	
	@Override
	protected AbstractScoreDirectorFactory buildIncrementalScoreDirectorFactory(){
		if (incrementalScoreCalculatorClass != null) {
            if (!IncrementalScoreCalculator.class.isAssignableFrom(incrementalScoreCalculatorClass)) {
                throw new IllegalArgumentException(
                        "The incrementalScoreCalculatorClass (" + incrementalScoreCalculatorClass
                        + ") does not implement " + IncrementalScoreCalculator.class.getSimpleName() + ".");
            }
            return new ProjectJobShedulingIncrementalScoreDirectorFactory(incrementalScoreCalculatorClass, unallowedWeeksCountForDraft, unallowedWeeksCountForAnalysis);
        } else {
            return null;
        }
	}

	public int getUnallowedWeeksCountForDraft() {
		return unallowedWeeksCountForDraft;
	}

	public void setUnallowedWeeksCountForDraft(int unallowedWeeksCountForDraft) {
		this.unallowedWeeksCountForDraft = unallowedWeeksCountForDraft;
	}
	
	public int getUnallowedWeeksCountForAnalysis() {
		return unallowedWeeksCountForAnalysis;
	}

	public void setUnallowedWeeksCountForAnalysis(int unallowedWeeksCountForAnalysis) {
		this.unallowedWeeksCountForAnalysis = unallowedWeeksCountForAnalysis;
	}
}
