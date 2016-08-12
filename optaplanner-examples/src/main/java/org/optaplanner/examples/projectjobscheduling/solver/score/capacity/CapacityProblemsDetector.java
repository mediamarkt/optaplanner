package org.optaplanner.examples.projectjobscheduling.solver.score.capacity;

import java.util.Iterator;
import java.util.List;

public class CapacityProblemsDetector implements Iterable<CapacityProblem> {

    private List<TeamResourceCapacityTracker> trackerList;

    public CapacityProblemsDetector(List<TeamResourceCapacityTracker> trackerList){
        this.trackerList = trackerList;

        //Save current score (if hard score is not null for fixed jobs)
        trackerList.forEach(teamResourceCapacityTracker -> {
            teamResourceCapacityTracker.saveMaxHardScore();
        });
    }

    @Override
    public Iterator<CapacityProblem> iterator() {
        return new CapacityProblemsIterator(trackerList);
    }
}

