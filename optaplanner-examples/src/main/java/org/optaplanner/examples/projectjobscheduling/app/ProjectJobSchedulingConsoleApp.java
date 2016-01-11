package org.optaplanner.examples.projectjobscheduling.app;

import java.io.File;

import org.apache.poi.openxml4j.exceptions.InvalidOperationException;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.examples.projectjobscheduling.domain.Schedule;
import org.optaplanner.examples.projectjobscheduling.persistence.ProjectJobSchedulingImporter;
import org.optaplanner.persistence.xstream.impl.domain.solution.XStreamSolutionFileIO;

public class ProjectJobSchedulingConsoleApp {
	
	
	public static void main(String[] args) {
		
		if(args.length < 3)
			throw new InvalidOperationException("Not enouth arguments.");
		
		String configFilePath = args[0];
		String inputFilePath = args[1];
		String outputFilePath = args[2];
		
		SolverFactory solverFactory = SolverFactory.createFromXmlFile(new File(configFilePath));
			
		Solver solver = solverFactory.buildSolver();
			
		ProjectJobSchedulingImporter importer = new ProjectJobSchedulingImporter();
		Schedule schedule = (Schedule)importer.readSolution(new File(inputFilePath));
			
		solver.solve(schedule);
			
		Schedule bestSolution = (Schedule)solver.getBestSolution();			
			
		XStreamSolutionFileIO xstream = new XStreamSolutionFileIO(Schedule.class);
		xstream.write(bestSolution, new File(outputFilePath));
	}
}
