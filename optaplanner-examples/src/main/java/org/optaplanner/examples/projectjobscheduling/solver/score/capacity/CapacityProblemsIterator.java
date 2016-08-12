package org.optaplanner.examples.projectjobscheduling.solver.score.capacity;

import java.util.Iterator;
import java.util.List;

public class CapacityProblemsIterator implements Iterator<CapacityProblem> {

    private List<TeamResourceCapacityTracker> trackerList;

    public CapacityProblemsIterator(List<TeamResourceCapacityTracker> trackerList) {
        this.trackerList = trackerList;
    }

    @Override
    public boolean hasNext() {
        return trackerList.stream().anyMatch(tracker -> tracker.getHardScore() < tracker.getMaxHardScore());
    }

    @Override
    public CapacityProblem next() {
        return trackerList.stream().filter(tracker -> tracker.getHardScore() < tracker.getMaxHardScore()).findFirst().get().GetFirstProblem();
    }
}
