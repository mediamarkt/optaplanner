package org.optaplanner.examples.projectjobscheduling.solver.score;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class JobsPrioritiesWeightsProvider {

    private static HashMap<String, Integer> coefficients;

    static {
        coefficients = new HashMap();

        coefficients.put("Blocker", 1000);
        coefficients.put("Critical", 100);
        coefficients.put("Major", 30);
        coefficients.put("Minor", 15);
        coefficients.put("Trivial", 10);
        coefficients.put("Analysis", 5);
        coefficients.put("Draft", 1);
    }


    public static Integer getPriorityWeight(String priority){
        if(coefficients.containsKey(priority)) {
            return coefficients.get(priority);
        }
        return 1;
    }

    public static List<Integer> getPrioritiesForParent(String priority) {
        Integer parentPriorityWeight = getPriorityWeight(priority) * 10;
        return coefficients.values().stream().sorted((f1, f2) -> Integer.compare(f2, f1) /*desc*/).map(w -> w + parentPriorityWeight).collect(Collectors.toList());
    }

    public static List<Integer> getPrioritiesWeights() {
        return coefficients.values().stream().collect(Collectors.toList());
    }
}
