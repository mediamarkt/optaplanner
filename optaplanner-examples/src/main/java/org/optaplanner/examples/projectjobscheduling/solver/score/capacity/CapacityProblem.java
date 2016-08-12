package org.optaplanner.examples.projectjobscheduling.solver.score.capacity;


import org.optaplanner.examples.projectjobscheduling.domain.Allocation;

public class CapacityProblem {

    private Allocation allocation;
    private int newDelay;

    public CapacityProblem(Allocation allocation, int newDelay) {
        this.allocation = allocation;
        this.newDelay = newDelay;
    }

    public Allocation getAllocation() {
        return allocation;
    }

    public int getNewDelay() {
        return newDelay;
    }
}
