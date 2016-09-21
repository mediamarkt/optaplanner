/*
 * Copyright 2015 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.examples.projectjobscheduling.domain.solver;

import org.optaplanner.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import org.optaplanner.core.impl.score.director.ScoreDirector;
import org.optaplanner.examples.projectjobscheduling.domain.Allocation;
import org.optaplanner.examples.projectjobscheduling.domain.Job;
import org.optaplanner.examples.projectjobscheduling.domain.JobType;

public class NotSourceSinkOrFixedAllocationFilter implements SelectionFilter<Allocation> {

    public boolean accept(ScoreDirector scoreDirector, Allocation allocation) {
    	Job job = allocation.getJob();
        JobType jobType = job.getJobType();
        return jobType != JobType.BREQ_SOURCE && jobType != JobType.BREQ_SINK && jobType != JobType.PREQ_SOURCE && jobType != JobType.PREQ_SINK && jobType != JobType.SUPER_SOURCE && jobType != JobType.SUPER_SINK && job.getFixedStartDate() == null;
    }

}