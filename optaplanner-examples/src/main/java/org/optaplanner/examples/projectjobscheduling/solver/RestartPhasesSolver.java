package org.optaplanner.examples.projectjobscheduling.solver;

import java.util.Iterator;

import org.optaplanner.core.impl.phase.Phase;
import org.optaplanner.core.impl.solver.DefaultSolver;;

public class RestartPhasesSolver extends DefaultSolver {
	@Override	
	protected void runPhases() {
		while (!termination.isSolverTerminated(solverScope)) {
			Iterator<Phase> it = phaseList.iterator();
			while (!termination.isSolverTerminated(solverScope) && it.hasNext()) {
				Phase phase = it.next();
				phase.solve(solverScope);
				if (it.hasNext()) {
					solverScope.setWorkingSolutionFromBestSolution();
				}
			}
		}
	}	
}
