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

package org.optaplanner.examples.projectjobscheduling.persistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.optaplanner.core.api.domain.solution.Solution;
import org.optaplanner.examples.common.persistence.AbstractTxtSolutionImporter;
import org.optaplanner.examples.common.persistence.AbstractTxtSolutionImporter.TxtInputBuilder;
import org.optaplanner.examples.projectjobscheduling.domain.Allocation;
import org.optaplanner.examples.projectjobscheduling.domain.ExecutionMode;
import org.optaplanner.examples.projectjobscheduling.domain.Job;
import org.optaplanner.examples.projectjobscheduling.domain.JobType;
import org.optaplanner.examples.projectjobscheduling.domain.Project;
import org.optaplanner.examples.projectjobscheduling.domain.ResourceRequirement;
import org.optaplanner.examples.projectjobscheduling.domain.Schedule;
import org.optaplanner.examples.projectjobscheduling.domain.resource.GlobalResource;
import org.optaplanner.examples.projectjobscheduling.domain.resource.LocalResource;
import org.optaplanner.examples.projectjobscheduling.domain.resource.Resource;
import org.optaplanner.examples.projectjobscheduling.domain.resource.ResourceLeave;
import org.optaplanner.examples.projectjobscheduling.persistence.ProjectJobSchedulingImporter.EndSyncClockMarksFileInputBuilder;

public class ProjectJobSchedulingImporter extends AbstractTxtSolutionImporter {

	public static class EndSyncClockMarksFileInputBuilder extends TxtInputBuilder {
		private Schedule schedule;

		public EndSyncClockMarksFileInputBuilder(Schedule schedule) {
			this.schedule = schedule;
		}

		@Override
		public Solution readSolution() throws IOException {
			int markersCount = readIntegerValue("Total markers: ");
			for (int i = 0; i < markersCount; i++) {
				String[] tokens = splitBySpacesOrTabs(readStringValue());
				this.schedule.getJobList().stream().filter(j -> j.getOriginalJobId() == Integer.parseInt(tokens[1]))
						.forEach(fj -> fj.incrementEndSyncClockStartMarks());
				this.schedule.getJobList().stream().filter(j -> j.getOriginalJobId() == Integer.parseInt(tokens[2]))
						.forEach(fj -> fj.incrementEndSyncClockEndMarks());
			}
			return null;
		}

	}

	public static class TimingClockMarksFileInputBuilder extends TxtInputBuilder {
		private Schedule schedule;

		public TimingClockMarksFileInputBuilder(Schedule schedule) {
			this.schedule = schedule;
		}

		@Override
		public Solution readSolution() throws IOException {
			int markersCount = readIntegerValue("Total markers: ");
			for (int i = 0; i < markersCount; i++) {
				String[] tokens = splitBySpacesOrTabs(readStringValue());
				this.schedule.getJobList().stream().filter(j -> j.getOriginalJobId() == Integer.parseInt(tokens[1]))
						.forEach(fj -> fj.incrementTimingClockStartMarks());
				this.schedule.getJobList().stream().filter(j -> j.getOriginalJobId() == Integer.parseInt(tokens[2]))
						.forEach(fj -> fj.incrementTimingClockEndMarks());
			}
			return null;
		}

	}

	public static class CommitmentsFileInputBuilder extends TxtInputBuilder {
		private Schedule schedule;

		public CommitmentsFileInputBuilder(Schedule schedule) {
			this.schedule = schedule;
		}

		@Override
		public Solution readSolution() throws IOException {
			int prioritiesCount = readIntegerValue("Total committed dates: ");
			for (int i = 0; i < prioritiesCount; i++) {
				String[] tokens = splitBySpacesOrTabs(readStringValue());
				int committedJobId = Integer.parseInt(tokens[0]);
				int committedProjectDay = Integer.parseInt(tokens[1]);
				this.schedule.getJobList().stream().filter(j -> j.getOriginalJobId() == committedJobId)
						.forEach(j -> j.setCommittedDay(committedProjectDay));
			}
			return null;
		}

	}

	public static class PrioritiesFileInputBuilder extends TxtInputBuilder {
		private Schedule schedule;

		public PrioritiesFileInputBuilder(Schedule schedule) {
			this.schedule = schedule;
		}

		@Override
		public Solution readSolution() throws IOException {
			int prioritiesCount = readIntegerValue("Total priority jobs: ");
			for (int i = 0; i < prioritiesCount; i++) {
				String[] tokens = splitBySpacesOrTabs(readStringValue());
				int priorityJobId = Integer.parseInt(tokens[0]);
				String priority = tokens[1];
				String parentPriority = tokens[2];
				
				if(parentPriority.equals("Blocker"))
				{
					int tmp = 1;
					tmp = 2;
				}
				
				this.schedule.getJobList().stream().filter(j -> j.getOriginalJobId() == priorityJobId)
						.forEach(j -> {
							j.setPriority(priority);
							j.setParentPriority(parentPriority);});
			}
			return null;
		}

	}

	public static class FixedStartDateFileInputBuilder extends TxtInputBuilder {
		private Schedule schedule;

		public FixedStartDateFileInputBuilder(Schedule schedule) {
			this.schedule = schedule;
		}

		@Override
		public Solution readSolution() throws IOException {
			int fixedJobsCount = readIntegerValue("Total fixed jobs: ");
			for (int i = 0; i < fixedJobsCount; i++) {
				String[] tokens = splitBySpacesOrTabs(readStringValue());
				int jobId = Integer.parseInt(tokens[1]);
				int fixedStartDate = Integer.parseInt(tokens[2]);
				this.schedule.getJobList().stream()
						.filter(j -> j.getOriginalJobId() == jobId && j.getProject().getId() == 0)
						.forEach(j -> j.setFixedStartDate(fixedStartDate));
			}
			return null;
		}

	}	

	public static void main(String[] args) {
		new ProjectJobSchedulingImporter().convertAll();
	}

	public ProjectJobSchedulingImporter() {
		super(new ProjectJobSchedulingDao());
	}

	public TxtInputBuilder createTxtInputBuilder() {
		return new ProjectJobSchedulingInputBuilder();
	}

	public static class ProjectJobSchedulingInputBuilder extends TxtInputBuilder {

		private Schedule schedule;

		private int projectListSize;
		private int resourceListSize;
		private int globalResourceListSize;

		private long projectId = 0L;
		private long resourceId = 0L;
		private long jobId = 0L;
		private long executionModeId = 0L;
		private long resourceRequirementId = 0L;

		private Map<Project, File> projectFileMap;

		public Solution readSolution() throws IOException {
			schedule = new Schedule();
			schedule.setId(0L);
			readProjectList();
			readResourceList();
			for (Map.Entry<Project, File> entry : projectFileMap.entrySet()) {
				readProjectFile(entry.getKey(), entry.getValue());
			}
			readTimingClockingMarkers();
			readEndSyncClockingMarkers();
			readPriorities();
			readCommitments();
			removePointlessExecutionModes();
			readFixedStartDates();
			createAllocationList();
			logger.info(
					"Schedule {} has {} projects, {} jobs, {} execution modes, {} resources"
							+ " and {} resource requirements.",
					getInputId(), schedule.getProjectList().size(), schedule.getJobList().size(),
					schedule.getExecutionModeList().size(), schedule.getResourceList().size(),
					schedule.getResourceRequirementList().size());			
			return schedule;
		}

		private void readCommitments() {
			String commitmentsFilePath = FilenameUtils.removeExtension(inputFile.getAbsolutePath())
					+ FilenameUtils.EXTENSION_SEPARATOR_STR + "commitments";
			File commitmentsFile = new File(commitmentsFilePath);
			if (!commitmentsFile.exists()) {
				logger.warn("The expected priorities marks file (" + commitmentsFilePath
						+ ") does not exist. Proceeding without clock marks.");
				return;
			}
			BufferedReader bufferedReader = null;
			try {
				bufferedReader = new BufferedReader(
						new InputStreamReader(new FileInputStream(commitmentsFile), "UTF-8"));

				CommitmentsFileInputBuilder commitmentsFileInputBuilder = new CommitmentsFileInputBuilder(schedule);
				commitmentsFileInputBuilder.setInputFile(commitmentsFile);
				commitmentsFileInputBuilder.setBufferedReader(bufferedReader);
				try {
					commitmentsFileInputBuilder.readSolution();
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException("Exception in commitments file (" + commitmentsFilePath + ")",
							e);
				} catch (IllegalStateException e) {
					throw new IllegalStateException("Exception in commitments file (" + commitmentsFilePath + ")", e);
				}
			} catch (IOException e) {
				throw new IllegalArgumentException("Could not read the commitments file (" + commitmentsFilePath + ")",
						e);
			} finally {
				IOUtils.closeQuietly(bufferedReader);
			}

		}

		private void readPriorities() {
			String prioritiesFilePath = FilenameUtils.removeExtension(inputFile.getAbsolutePath())
					+ FilenameUtils.EXTENSION_SEPARATOR_STR + "prio";
			File prioritiesFile = new File(prioritiesFilePath);
			if (!prioritiesFile.exists()) {
				logger.warn("The expected priorities marks file (" + prioritiesFilePath
						+ ") does not exist. Proceeding without clock marks.");
				return;
			}
			BufferedReader bufferedReader = null;
			try {
				bufferedReader = new BufferedReader(
						new InputStreamReader(new FileInputStream(prioritiesFile), "UTF-8"));

				PrioritiesFileInputBuilder prioritiesFileInputBuilder = new PrioritiesFileInputBuilder(schedule);
				prioritiesFileInputBuilder.setInputFile(prioritiesFile);
				prioritiesFileInputBuilder.setBufferedReader(bufferedReader);
				try {
					prioritiesFileInputBuilder.readSolution();
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException("Exception in priorities file (" + prioritiesFilePath + ")", e);
				} catch (IllegalStateException e) {
					throw new IllegalStateException("Exception in priorities file (" + prioritiesFilePath + ")", e);
				}
			} catch (IOException e) {
				throw new IllegalArgumentException("Could not read the priorities file (" + prioritiesFilePath + ")",
						e);
			} finally {
				IOUtils.closeQuietly(bufferedReader);
			}

		}

		private void readEndSyncClockingMarkers() {
			String EndSyncClockMarkersFilePath = FilenameUtils.removeExtension(inputFile.getAbsolutePath())
					+ FilenameUtils.EXTENSION_SEPARATOR_STR + "syncclocks";
			File endSyncClockMarkersFile = new File(EndSyncClockMarkersFilePath);
			if (!endSyncClockMarkersFile.exists()) {
				logger.warn("The expected end sync clock marks file (" + EndSyncClockMarkersFilePath
						+ ") does not exist. Proceeding without end sync clock marks.");
				return;
			}
			BufferedReader bufferedReader = null;
			try {
				bufferedReader = new BufferedReader(
						new InputStreamReader(new FileInputStream(endSyncClockMarkersFile), "UTF-8"));

				EndSyncClockMarksFileInputBuilder endSyncClockMarksFileInputBuilder = new EndSyncClockMarksFileInputBuilder(
						schedule);
				endSyncClockMarksFileInputBuilder.setInputFile(endSyncClockMarkersFile);
				endSyncClockMarksFileInputBuilder.setBufferedReader(bufferedReader);
				try {
					endSyncClockMarksFileInputBuilder.readSolution();
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException(
							"Exception in end sync clock marks file (" + EndSyncClockMarkersFilePath + ")", e);
				} catch (IllegalStateException e) {
					throw new IllegalStateException(
							"Exception in end sync clock marks file (" + EndSyncClockMarkersFilePath + ")", e);
				}
			} catch (IOException e) {
				throw new IllegalArgumentException(
						"Could not read the end sync clock marks file (" + EndSyncClockMarkersFilePath + ")", e);
			} finally {
				IOUtils.closeQuietly(bufferedReader);
			}

		}

		private void readTimingClockingMarkers() {
			String TimingClockMarkersFilePath = FilenameUtils.removeExtension(inputFile.getAbsolutePath())
					+ FilenameUtils.EXTENSION_SEPARATOR_STR + "timingclocks";
			File timingClockMarkersFile = new File(TimingClockMarkersFilePath);
			if (!timingClockMarkersFile.exists()) {
				logger.warn("The expected timing clock marks file (" + TimingClockMarkersFilePath
						+ ") does not exist. Proceeding without timing clock marks.");
				return;
			}
			BufferedReader bufferedReader = null;
			try {
				bufferedReader = new BufferedReader(
						new InputStreamReader(new FileInputStream(timingClockMarkersFile), "UTF-8"));

				TimingClockMarksFileInputBuilder timingClockMarksFileInputBuilder = new TimingClockMarksFileInputBuilder(
						schedule);
				timingClockMarksFileInputBuilder.setInputFile(timingClockMarkersFile);
				timingClockMarksFileInputBuilder.setBufferedReader(bufferedReader);
				try {
					timingClockMarksFileInputBuilder.readSolution();
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException(
							"Exception in timing clock marks file (" + TimingClockMarkersFilePath + ")", e);
				} catch (IllegalStateException e) {
					throw new IllegalStateException(
							"Exception in timing clock marks file (" + TimingClockMarkersFilePath + ")", e);
				}
			} catch (IOException e) {
				throw new IllegalArgumentException(
						"Could not read the timing clock marks file (" + TimingClockMarkersFilePath + ")", e);
			} finally {
				IOUtils.closeQuietly(bufferedReader);
			}

		}

		private void readFixedStartDates() {
			String fixedStartDatesFilePath = FilenameUtils.removeExtension(inputFile.getAbsolutePath())
					+ FilenameUtils.EXTENSION_SEPARATOR_STR + "fixeddates";
			File fixedStartDatesFile = new File(fixedStartDatesFilePath);
			if (!fixedStartDatesFile.exists()) {
				logger.warn("The expected fixed jobs file (" + fixedStartDatesFilePath
						+ ") does not exist. Proceeding without fixed jobs.");
				return;
			}
			BufferedReader bufferedReader = null;
			try {
				bufferedReader = new BufferedReader(
						new InputStreamReader(new FileInputStream(fixedStartDatesFile), "UTF-8"));

				FixedStartDateFileInputBuilder fixedStartDatesFileInputBuilder = new FixedStartDateFileInputBuilder(
						schedule);
				fixedStartDatesFileInputBuilder.setInputFile(fixedStartDatesFile);
				fixedStartDatesFileInputBuilder.setBufferedReader(bufferedReader);
				try {
					fixedStartDatesFileInputBuilder.readSolution();
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException("Exception in fixed jobs file (" + fixedStartDatesFilePath + ")",
							e);
				} catch (IllegalStateException e) {
					throw new IllegalStateException("Exception in fixed jobs file (" + fixedStartDatesFilePath + ")",
							e);
				}
			} catch (IOException e) {
				throw new IllegalArgumentException(
						"Could not read the fixed jobs file (" + fixedStartDatesFilePath + ")", e);
			} finally {
				IOUtils.closeQuietly(bufferedReader);
			}
		}	

		private void readProjectList() throws IOException {
			projectListSize = readIntegerValue();
			List<Project> projectList = new ArrayList<Project>(projectListSize);
			projectFileMap = new LinkedHashMap<Project, File>(projectListSize);
			for (int i = 0; i < projectListSize; i++) {
				Project project = new Project();
				project.setId(projectId);
				project.setReleaseDate(readIntegerValue());
				project.setCriticalPathDuration(readIntegerValue());
				File projectFile = new File(inputFile.getParentFile(), readStringValue());
				if (!projectFile.exists()) {
					throw new IllegalArgumentException("The projectFile (" + projectFile + ") does not exist.");
				}
				projectFileMap.put(project, projectFile);
				projectList.add(project);
				projectId++;
			}
			schedule.setProjectList(projectList);
			schedule.setJobList(new ArrayList<Job>(projectListSize * 10));
			schedule.setExecutionModeList(new ArrayList<ExecutionMode>(projectListSize * 10 * 5));
		}

		private void readResourceList() throws IOException {
			resourceListSize = readIntegerValue();
			String[] tokens = splitBySpacesOrTabs(readStringValue(), resourceListSize);
			List<Resource> resourceList = new ArrayList<Resource>(resourceListSize * projectListSize * 10);
			for (int i = 0; i < resourceListSize; i++) {
				int capacity = Integer.parseInt(tokens[i]);
				if (capacity != -1) {
					GlobalResource resource = new GlobalResource();
					resource.setId(resourceId);
					resource.setCapacity(capacity);
					resourceList.add(resource);
					resourceId++;
				}
			}
			
			for (int i = 0; i < resourceListSize; i++) {
				int leavesCount = readIntegerValue("Leaves: ");
				
				List<ResourceLeave> resourceLeaves = new ArrayList<ResourceLeave>();
				
				for(int j = 0; j < leavesCount; j++) {
					String[] leavesTokens = splitBySpacesOrTabs(readStringValue(), 3);
					int start = Integer.parseInt(leavesTokens[0]);
					int end = Integer.parseInt(leavesTokens[1]);
					int requirement = Integer.parseInt(leavesTokens[2]);
					
					ResourceLeave resourceLeave = new ResourceLeave();
					resourceLeave.setStart(start);
					resourceLeave.setEnd(end);
					resourceLeave.setRequirement(requirement);
					
					resourceLeaves.add(resourceLeave);
				}
				
				resourceList.get(i).setResourceLeaves(resourceLeaves);
			}
			
			globalResourceListSize = resourceList.size();
			schedule.setResourceList(resourceList);
			schedule.setResourceRequirementList(
					new ArrayList<ResourceRequirement>(projectListSize * 10 * 5 * resourceListSize));
		}

		private void readProjectFile(Project project, File projectFile) {
			BufferedReader bufferedReader = null;
			try {
				bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(projectFile), "UTF-8"));
				ProjectFileInputBuilder projectFileInputBuilder = new ProjectFileInputBuilder(schedule, project);
				projectFileInputBuilder.setInputFile(projectFile);
				projectFileInputBuilder.setBufferedReader(bufferedReader);
				try {
					projectFileInputBuilder.readSolution();
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException("Exception in projectFile (" + projectFile + ")", e);
				} catch (IllegalStateException e) {
					throw new IllegalStateException("Exception in projectFile (" + projectFile + ")", e);
				}
			} catch (IOException e) {
				throw new IllegalArgumentException("Could not read the projectFile (" + projectFile + ").", e);
			} finally {
				IOUtils.closeQuietly(bufferedReader);
			}
		}

		public class ProjectFileInputBuilder extends TxtInputBuilder {

			private Schedule schedule;
			private Project project;

			private int jobListSize;
			private int renewableLocalResourceSize;
			private int nonrenewableLocalResourceSize;

			public ProjectFileInputBuilder(Schedule schedule, Project project) {
				this.schedule = schedule;
				this.project = project;
			}

			public Solution readSolution() throws IOException {
				readHeader();
				readResourceList();
				readProjectInformation();
				readPrecedenceRelations();
				readRequestDurations();
				readResourceAvailabilities();
				detectPointlessSuccessor();
				return null; // Hack so the code can reuse read methods from
								// TxtInputBuilder
			}

			private void readHeader() throws IOException {
				readConstantLine("\\*+");
				readStringValue("file with basedata *:");
				readStringValue("initial value random generator *:");
				readConstantLine("\\*+");
				int projects = readIntegerValue("projects *:");
				if (projects != 1) {
					throw new IllegalArgumentException("The projects value (" + projects + ") should always be 1.");
				}
				jobListSize = readIntegerValue("jobs \\(incl\\. supersource/sink *\\) *:");
				int horizon = readIntegerValue("horizon *:");
				// Ignore horizon
			}

			private void readResourceList() throws IOException {
				readConstantLine("RESOURCES");
				int renewableResourceSize = readIntegerValue("\\- renewable *:", "R");
				if (renewableResourceSize < globalResourceListSize) {
					throw new IllegalArgumentException("The renewableResourceSize (" + renewableResourceSize
							+ ") can not be less than globalResourceListSize (" + globalResourceListSize + ").");
				}
				renewableLocalResourceSize = renewableResourceSize - globalResourceListSize;
				nonrenewableLocalResourceSize = readIntegerValue("\\- nonrenewable *:", "N");
				int doublyConstrainedResourceSize = readIntegerValue("\\- doubly constrained *:", "D");
				if (doublyConstrainedResourceSize != 0) {
					throw new IllegalArgumentException("The doublyConstrainedResourceSize ("
							+ doublyConstrainedResourceSize + ") should always be 0.");
				}
				List<LocalResource> localResourceList = new ArrayList<LocalResource>(
						globalResourceListSize + renewableLocalResourceSize + nonrenewableLocalResourceSize);
				for (int i = 0; i < renewableLocalResourceSize; i++) {
					LocalResource localResource = new LocalResource();
					localResource.setId(resourceId);
					localResource.setProject(project);
					localResource.setRenewable(true);
					resourceId++;
					localResourceList.add(localResource);
				}
				for (int i = 0; i < nonrenewableLocalResourceSize; i++) {
					LocalResource localResource = new LocalResource();
					localResource.setId(resourceId);
					localResource.setProject(project);
					localResource.setRenewable(false);
					resourceId++;
					localResourceList.add(localResource);
				}
				project.setLocalResourceList(localResourceList);
				schedule.getResourceList().addAll(localResourceList);
				readConstantLine("\\*+");
			}

			private void readProjectInformation() throws IOException {
				readConstantLine("PROJECT INFORMATION:");
				readConstantLine("pronr\\. +\\#jobs +rel\\.date +duedate +tardcost +MPM\\-Time");
				String[] tokens = splitBySpacesOrTabs(readStringValue(), 6);
				if (Integer.parseInt(tokens[0]) != 1) {
					throw new IllegalArgumentException(
							"The project information tokens (" + Arrays.toString(tokens) + ") index 0 should be 1.");
				}
				if (Integer.parseInt(tokens[1]) != jobListSize - 2) {
					throw new IllegalArgumentException("The project information tokens (" + Arrays.toString(tokens)
							+ ") index 1 should be " + (jobListSize - 2) + ".");
				}
				// Ignore releaseDate, dueDate, tardinessCost and mpmTime
				readConstantLine("\\*+");
			}

			private void readPrecedenceRelations() throws IOException {
				readConstantLine("PRECEDENCE RELATIONS:");
				readConstantLine("jobnr\\. +type +\\#modes +\\#successors +successors");
				List<Job> jobList = new ArrayList<Job>(jobListSize);
				for (int i = 0; i < jobListSize; i++) {
					Job job = new Job();
					job.setId(jobId);
					job.setProject(project);
					
					jobList.add(job);
					jobId++;
				}
				project.setJobList(jobList);
				schedule.getJobList().addAll(jobList);
				for (int i = 0; i < jobListSize; i++) {
					Job job = jobList.get(i);
					String[] tokens = splitBySpacesOrTabs(readStringValue());
					if (tokens.length < 3) {
						throw new IllegalArgumentException(
								"The tokens (" + Arrays.toString(tokens) + ") should be at least 3 in length.");
					}
					if (Integer.parseInt(tokens[0]) != i + 1) {
						throw new IllegalArgumentException(
								"The tokens (" + Arrays.toString(tokens) + ") index 0 should be " + (i + 1) + ".");
					}
					job.setoriginalJobId(Integer.parseInt(tokens[0]));
					
					int jobType = Integer.parseInt(tokens[1]);
					if(jobType == 0) {
						job.setJobType(JobType.SUPER_SOURCE);
					} else if(jobType == 1) {
						job.setJobType(JobType.SUPER_SINK);
					} else if(jobType == 2) {
						job.setJobType(JobType.BREQ_SOURCE);
					} else if(jobType == 3) {
						job.setJobType(JobType.BREQ_SINK);
					} else if(jobType == 4) {
						job.setJobType(JobType.PREQ_SOURCE);
					} else if(jobType == 5) {
						job.setJobType(JobType.PREQ_SINK);
					} else {
						job.setJobType(JobType.STANDARD);
					}
					
					int executionModeListSize = Integer.parseInt(tokens[2]);
					List<ExecutionMode> executionModeList = new ArrayList<ExecutionMode>(executionModeListSize);
					for (int j = 0; j < executionModeListSize; j++) {
						ExecutionMode executionMode = new ExecutionMode();
						executionMode.setId(executionModeId);
						executionMode.setJob(job);
						executionModeList.add(executionMode);
						executionModeId++;
					}
					job.setExecutionModeList(executionModeList);
					schedule.getExecutionModeList().addAll(executionModeList);
					int successorJobListSize = Integer.parseInt(tokens[3]);
					if (tokens.length != 4 + successorJobListSize) {
						throw new IllegalArgumentException("The tokens (" + Arrays.toString(tokens) + ") should be "
								+ (4 + successorJobListSize) + " in length.");
					}
					List<Job> successorJobList = new ArrayList<Job>(successorJobListSize);
					for (int j = 0; j < successorJobListSize; j++) {
						int successorIndex = Integer.parseInt(tokens[4 + j]);
						Job successorJob = project.getJobList().get(successorIndex - 1);
						successorJobList.add(successorJob);
					}
					job.setSuccessorJobList(successorJobList);
				}
				readConstantLine("\\*+");
			}

			private void readRequestDurations() throws IOException {
				readConstantLine("REQUESTS/DURATIONS:");
				splitBySpacesOrTabs(readStringValue());
				readConstantLine("\\-+");
				int resourceSize = globalResourceListSize + renewableLocalResourceSize + nonrenewableLocalResourceSize;
				for (int i = 0; i < jobListSize; i++) {
					Job job = project.getJobList().get(i);
					int executionModeSize = job.getExecutionModeList().size();
					for (int j = 0; j < executionModeSize; j++) {
						ExecutionMode executionMode = job.getExecutionModeList().get(j);
						boolean first = j == 0;
						String[] tokens = splitBySpacesOrTabs(readStringValue(), (first ? 3 : 2) + resourceSize);
						if (first && Integer.parseInt(tokens[0]) != i + 1) {
							throw new IllegalArgumentException(
									"The tokens (" + Arrays.toString(tokens) + ") index 0 should be " + (i + 1) + ".");
						}
						if (Integer.parseInt(tokens[first ? 1 : 0]) != j + 1) {
							throw new IllegalArgumentException("The tokens (" + Arrays.toString(tokens) + ") index "
									+ (first ? 1 : 0) + " should be " + (j + 1) + ".");
						}
						int duration = Integer.parseInt(tokens[first ? 2 : 1]);
						executionMode.setDuration(duration);
						List<ResourceRequirement> resourceRequirementList = new ArrayList<ResourceRequirement>(
								resourceSize);
						for (int k = 0; k < resourceSize; k++) {
							int requirement = Integer.parseInt(tokens[(first ? 3 : 2) + k]);
							if (requirement != 0) {
								ResourceRequirement resourceRequirement = new ResourceRequirement();
								resourceRequirement.setId(resourceRequirementId);
								resourceRequirement.setExecutionMode(executionMode);
								Resource resource;
								if (k < globalResourceListSize) {
									resource = schedule.getResourceList().get(k);
								} else {
									resource = project.getLocalResourceList().get(k - globalResourceListSize);
								}
								resourceRequirement.setResource(resource);
								resourceRequirement.setRequirement(requirement);
								resourceRequirementList.add(resourceRequirement);
								resourceRequirementId++;
							}
						}
						executionMode.setResourceRequirementList(resourceRequirementList);
						schedule.getResourceRequirementList().addAll(resourceRequirementList);
					}
				}
				readConstantLine("\\*+");
			}

			private void readResourceAvailabilities() throws IOException {
				readConstantLine("RESOURCEAVAILABILITIES:");
				splitBySpacesOrTabs(readStringValue());
				int resourceSize = globalResourceListSize + renewableLocalResourceSize + nonrenewableLocalResourceSize;
				String[] tokens = splitBySpacesOrTabs(readStringValue(), resourceSize);
				for (int i = 0; i < resourceSize; i++) {
					int capacity = Integer.parseInt(tokens[i]);
					if (i < globalResourceListSize) {
						// Overwritten by global resource
					} else {
						Resource resource = project.getLocalResourceList().get(i - globalResourceListSize);
						resource.setCapacity(capacity);
					}
				}
				readConstantLine("\\*+");
			}

			private void detectPointlessSuccessor() {
				for (Job baseJob : project.getJobList()) {
					Set<Job> baseSuccessorJobSet = new HashSet<Job>(baseJob.getSuccessorJobList());
					Set<Job> checkedSuccessorSet = new HashSet<Job>(project.getJobList().size());
					Queue<Job> uncheckedSuccessorQueue = new ArrayDeque<Job>(project.getJobList().size());
					for (Job baseSuccessorJob : baseJob.getSuccessorJobList()) {
						uncheckedSuccessorQueue.addAll(baseSuccessorJob.getSuccessorJobList());
					}
					while (!uncheckedSuccessorQueue.isEmpty()) {
						Job uncheckedJob = uncheckedSuccessorQueue.remove();
						if (checkedSuccessorSet.contains(uncheckedJob)) {
							continue;
						}
						if (baseSuccessorJobSet.contains(uncheckedJob)) {
							// throw new IllegalStateException("The baseJob (" +
							// baseJob.getOriginalJobId()
							logger.warn(("The baseJob (" + baseJob.getOriginalJobId() + ") has a direct successor ("
									+ uncheckedJob.getOriginalJobId()
									+ ") that is also an indirect successor. That's pointless."));
						} else {
							uncheckedSuccessorQueue.addAll(uncheckedJob.getSuccessorJobList());
						}
					}
				}
			}

		}

		private void removePointlessExecutionModes() {
			// TODO iterate through schedule.getJobList(), find pointless
			// ExecutionModes
			// and delete them both from the job and from
			// schedule.getExecutionModeList()
		}

		private void createAllocationList() {
			List<Job> jobList = schedule.getJobList();
			List<Allocation> allocationList = new ArrayList<Allocation>(jobList.size());
			Map<Job, Allocation> jobToAllocationMap = new HashMap<Job, Allocation>(jobList.size());
			Map<Project, Allocation> projectToSourceAllocationMap = new HashMap<Project, Allocation>(projectListSize);
			Map<Project, Allocation> projectToSinkAllocationMap = new HashMap<Project, Allocation>(projectListSize);
			for (Job job : jobList) {
				Allocation allocation = new Allocation();
				allocation.setId(job.getId());
				allocation.setJob(job);
				allocation.setPredecessorAllocationList(new ArrayList<Allocation>(job.getSuccessorJobList().size()));
				allocation.setSuccessorAllocationList(new ArrayList<Allocation>(job.getSuccessorJobList().size()));
				// Uninitialized allocations take no time, but don't break the
				// predecessorsDoneDate cascade to sink.
				allocation.setPredecessorsDoneDate(job.getProject().getReleaseDate());
				if (job.getFixedStartDate() != null) {
					allocation.setDelay(job.getFixedStartDate());// TODO
																	// subtract
																	// project
																	// start
																	// date
					allocation.setExecutionMode(job.getExecutionModeList().get(0));
				}

				if (job.getJobType() == JobType.SUPER_SOURCE) {
					allocation.setDelay(0);
					if (job.getExecutionModeList().size() != 1) {
						throw new IllegalArgumentException("The job (" + job + ")'s executionModeList ("
								+ job.getExecutionModeList() + ") is expected to be a singleton.");
					}
					allocation.setExecutionMode(job.getExecutionModeList().get(0));
					projectToSourceAllocationMap.put(job.getProject(), allocation);
				} else if (job.getJobType() == JobType.SUPER_SINK) {
					allocation.setDelay(0);
					if (job.getExecutionModeList().size() != 1) {
						throw new IllegalArgumentException("The job (" + job + ")'s executionModeList ("
								+ job.getExecutionModeList() + ") is expected to be a singleton.");
					}
					allocation.setExecutionMode(job.getExecutionModeList().get(0));
					projectToSinkAllocationMap.put(job.getProject(), allocation);
				} else if (job.getJobType() == JobType.BREQ_SOURCE || job.getJobType() == JobType.BREQ_SINK || job.getJobType() == JobType.PREQ_SOURCE || job.getJobType() == JobType.PREQ_SINK) {
					allocation.setDelay(0);
					if (job.getExecutionModeList().size() != 1) {
						throw new IllegalArgumentException("The job (" + job + ")'s executionModeList ("
								+ job.getExecutionModeList() + ") is expected to be a singleton.");
					}
					allocation.setExecutionMode(job.getExecutionModeList().get(0));					
				}
				
				allocationList.add(allocation);
				jobToAllocationMap.put(job, allocation);
			}
			for (Allocation allocation : allocationList) {
				Job job = allocation.getJob();
				allocation.setSourceAllocation(projectToSourceAllocationMap.get(job.getProject()));
				allocation.setSinkAllocation(projectToSinkAllocationMap.get(job.getProject()));
				for (Job successorJob : job.getSuccessorJobList()) {
					Allocation successorAllocation = jobToAllocationMap.get(successorJob);
					allocation.getSuccessorAllocationList().add(successorAllocation);
					successorAllocation.getPredecessorAllocationList().add(allocation);
				}
			}

			// Set predecessorDoneDate which might be affected by fixed-date
			// predecessors
			allocationList
				.stream()
				.filter(a -> a.getJob().getFixedStartDate() != null)
				.forEach(this::CascadedDoneDateUpdate);


			for (Allocation sourceAllocation : projectToSourceAllocationMap.values()) {
				for (Allocation allocation : sourceAllocation.getSuccessorAllocationList()) {
					allocation.setPredecessorsDoneDate(sourceAllocation.getEndDate());
				}
			}
			schedule.setAllocationList(allocationList);
		}

		private void CascadedDoneDateUpdate(Allocation allocation) {
			if (allocation.getJobType() != JobType.SUPER_SOURCE) {
				allocation.setPredecessorsDoneDate(allocation.getPredecessorAllocationList().stream()
						.map(Allocation::getEndDate).reduce(0, (a, b) -> Math.max(a, b)));
			}
			if (allocation.getJobType() != JobType.SUPER_SINK) {
				for (Allocation successorAllocation : allocation.getSuccessorAllocationList()) {
					CascadedDoneDateUpdate(successorAllocation);
				}
			}
		}

	}

}
