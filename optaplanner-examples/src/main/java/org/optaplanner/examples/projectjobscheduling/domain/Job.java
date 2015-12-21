/*
 * Copyright 2010 JBoss Inc
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

package org.optaplanner.examples.projectjobscheduling.domain;

import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.optaplanner.examples.common.domain.AbstractPersistable;

@XStreamAlias("PjsJob")
public class Job extends AbstractPersistable {

    private Project project;
    private JobType jobType;
    private int originalJobId;
    private List<ExecutionMode> executionModeList;
    private int clockingStartMarks = 0;
    private int clockingEndMarks = 0;

    private List<Job> successorJobList;
	private boolean priority = false;

    
    public int getClockingStartMarks() {
    	return clockingStartMarks;
    }
    
    public void incrementClockingStartMarks()
    {
    	this.clockingStartMarks++;
    }
 
    public int getClockingEndMarks() {
    	return clockingEndMarks;
    }
    
    public void incrementClockingEndMarks()
    {
    	this.clockingEndMarks++;
    }
    
    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }
    public int getOriginalJobId() {
        return originalJobId;
    }

    public void setoriginalJobId(int originalJobId) {
        this.originalJobId = originalJobId;
    }

    public List<ExecutionMode> getExecutionModeList() {
        return executionModeList;
    }

    public void setExecutionModeList(List<ExecutionMode> executionModeList) {
        this.executionModeList = executionModeList;
    }

    public List<Job> getSuccessorJobList() {
        return successorJobList;
    }

    public void setSuccessorJobList(List<Job> successorJobList) {
        this.successorJobList = successorJobList;
    }

	public void setPriorityMark() {
		this.priority = true;
	}
	
	public boolean getPriroityMark()
	{
		return this.priority;
	}


    // ************************************************************************
    // Complex methods
    // ************************************************************************

}
