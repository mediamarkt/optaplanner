package org.optaplanner.examples.projectjobscheduling.solver;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.config.heuristic.policy.HeuristicConfigPolicy;
import org.optaplanner.core.config.score.director.ScoreDirectorFactoryConfig;
import org.optaplanner.core.config.solver.EnvironmentMode;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.termination.TerminationConfig;
import org.optaplanner.core.impl.domain.solution.descriptor.SolutionDescriptor;
import org.optaplanner.core.impl.score.director.InnerScoreDirectorFactory;
import org.optaplanner.core.impl.solver.BasicPlumbingTermination;
import org.optaplanner.core.impl.solver.recaller.BestSolutionRecaller;
import org.optaplanner.core.impl.solver.termination.Termination;
import org.optaplanner.examples.projectjobscheduling.solver.score.ProjectJobSchedulingScoreDirectorFactoryConfig;

public class RestartPhasesSolverConfig extends SolverConfig {
	@Override
	public Solver buildSolver(ClassLoader classLoader) {
		RestartPhasesSolver solver = new RestartPhasesSolver();
        EnvironmentMode environmentMode_ = determineEnvironmentMode();
        solver.setEnvironmentMode(environmentMode_);
        boolean daemon_ = defaultIfNull(daemon, false);
        BasicPlumbingTermination basicPlumbingTermination = new BasicPlumbingTermination(daemon_);
        solver.setBasicPlumbingTermination(basicPlumbingTermination);

        solver.setRandomFactory(buildRandomFactory(environmentMode_));
        SolutionDescriptor solutionDescriptor = buildSolutionDescriptor();
        ScoreDirectorFactoryConfig scoreDirectorFactoryConfig_
                = scoreDirectorFactoryConfig == null ? new ScoreDirectorFactoryConfig()
                : scoreDirectorFactoryConfig;
        InnerScoreDirectorFactory scoreDirectorFactory = scoreDirectorFactoryConfig_.buildScoreDirectorFactory(
                classLoader, environmentMode_, solutionDescriptor);
        solver.setConstraintMatchEnabledPreference(environmentMode_.isAsserted());
        solver.setScoreDirectorFactory(scoreDirectorFactory);

        HeuristicConfigPolicy configPolicy = new HeuristicConfigPolicy(environmentMode_, scoreDirectorFactory);
        TerminationConfig terminationConfig_ = getTerminationConfig() == null ? new TerminationConfig()
                : getTerminationConfig();
        Termination termination = terminationConfig_.buildTermination(configPolicy, basicPlumbingTermination);
        solver.setTermination(termination);
        BestSolutionRecaller bestSolutionRecaller = buildBestSolutionRecaller(environmentMode_);
        solver.setBestSolutionRecaller(bestSolutionRecaller);
        solver.setPhaseList(buildPhaseList(configPolicy, bestSolutionRecaller, termination));
        return solver;
	}
	
	protected ProjectJobSchedulingScoreDirectorFactoryConfig scoreDirectorFactoryConfig = null;
}
