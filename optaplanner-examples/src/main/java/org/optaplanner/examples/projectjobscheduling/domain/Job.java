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
    private int endSyncClockStartMarks = 0;
    private int endSyncClockEndMarks = 0;
    private int timingClockStartMarks = 0;
    private int timingClockEndMarks = 0;
    private Integer fixedStartDate; 

    private List<Job> successorJobList;
	private String priority;
	private int committedProjectDay;
	
	private String jobStatus;

	public int getTimingClockStartMarks() {
    	return timingClockStartMarks;
    }
    
    public void incrementTimingClockStartMarks()
    {
    	this.timingClockStartMarks++;
    }
 
    public int getTimingClockEndMarks() {
    	return timingClockEndMarks;
    }
    
    public void incrementTimingClockEndMarks()
    {
    	this.timingClockEndMarks++;
    }
	
    public int getEndSyncClockStartMarks() {
    	return endSyncClockStartMarks;
    }
    
    public void incrementEndSyncClockStartMarks()
    {
    	this.endSyncClockStartMarks++;
    }
 
    public int getEndSyncClockEndMarks() {
    	return endSyncClockEndMarks;
    }
    
    public void incrementEndSyncClockEndMarks()
    {
    	this.endSyncClockEndMarks++;
    }
    
    public Integer getFixedStartDate() {
    	return fixedStartDate;
    }
    
    public void setFixedStartDate(Integer fixedStartDate) {
    	this.fixedStartDate = fixedStartDate;
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

	public void setPriorityMark(String priority) {
		this.priority = priority;
	}
	
	public String getPriorityMark()
	{
		return this.priority;
	}

	public void setCommittedDay(int committedProjectDay) {
		this.committedProjectDay = committedProjectDay;
	}
	
	public int getCommittedDay(){
		return this.committedProjectDay;
	}

	public String getJobStatus() {
		return jobStatus;
	}

	public void setJobStatus(String jobStatus) {
		this.jobStatus = jobStatus;
	}
	
	public boolean isVerified()	{
		return jobStatus != null && (jobStatus.equals("Verified") || jobStatus.equals("InProgress"));
	}

    // ************************************************************************
    // Complex methods
    // ************************************************************************

}
