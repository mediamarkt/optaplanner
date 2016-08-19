package org.optaplanner.examples.projectjobscheduling.app;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.poi.openxml4j.exceptions.InvalidOperationException;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.examples.projectjobscheduling.domain.Schedule;
import org.optaplanner.examples.projectjobscheduling.persistence.ProjectJobSchedulingImporter;
import org.optaplanner.persistence.xstream.impl.domain.solution.XStreamSolutionFileIO;

public class ProjectJobSchedulingConsoleApp {
	
	
	public static void main(String[] args) {
		
		String loggerName;
		if(args.length > 0) {
			loggerName = args[0];//Plan version
		} else {
			loggerName = "Default";
		}
		
		Logger logger = Logger.getLogger(ProjectJobSchedulingConsoleApp.class.getName());
		try{
			File dir = new File("data/Logs");
			
			if(!dir.exists()) {
				dir.mkdirs();
			}
			
			FileHandler fh = new FileHandler("data/Logs/"+ loggerName + ".txt", true);			
	        logger.addHandler(fh);
	        SimpleFormatter formatter = new SimpleFormatter();  
	        fh.setFormatter(formatter);
	        
	        try{
	        	if(args.length < 4)
	    			throw new InvalidOperationException("Not enouth arguments.");
	    			        	
	        	String configFilePath = args[1];
	    		String inputFilePath = args[2];
	    		String outputFilePath = args[3];
	    		
	    		logger.info("Planning start with arguments: " + configFilePath + " " + inputFilePath + " " + outputFilePath);
	    		
	    		SolverFactory solverFactory = SolverFactory.createFromXmlFile(new File(configFilePath));
	    			
	    		Solver solver = solverFactory.buildSolver();
	    			
	    		ProjectJobSchedulingImporter importer = new ProjectJobSchedulingImporter();
	    		Schedule schedule = (Schedule)importer.readSolution(new File(inputFilePath));
	    			
	    		solver.solve(schedule);
	    			
	    		Schedule bestSolution = (Schedule)solver.getBestSolution();			
	    			
	    		XStreamSolutionFileIO xstream = new XStreamSolutionFileIO(Schedule.class);

				xstream.write(bestSolution, new File(outputFilePath));
	    		
	    		logger.info("Planning ended.");
	        } catch(Exception e) {
	        	logger.log(Level.SEVERE, "Planning error: " + ExceptionUtils.getStackTrace(e));
	        	logger.info("Planning ended with error.");
	        }
		} catch (SecurityException e) {  
	        e.printStackTrace();  
	    } catch (IOException e) {  
	        e.printStackTrace();  
	    }
	}
}
