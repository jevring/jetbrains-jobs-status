package net.jevring.jetbrainsjobsstatus.services.jobs;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

public final class TestRun implements Job {
	private final Map<String, TestStateMachine> nodeIdToTestStateMachine = new ConcurrentHashMap<>();
	private final long executionId;

	private Instant startedAt;
	private Instant endedAt;
	private String projectPath;
	private String name;
	private Long pid;
	private boolean wasTerminated;

	public TestRun(long executionId) {
		this.executionId = executionId;
	}

	public TestStateMachine testForNode(String nodeId) {
		return nodeIdToTestStateMachine.computeIfAbsent(nodeId, ignored -> new TestStateMachine());
	}

	public void setNameAndPath(String name, String projectPath) {
		this.name = name;
		this.projectPath = projectPath;
	}

	public void started(Long pid) {
		this.pid = pid;
		this.startedAt = Instant.now();
	}

	public void finished(boolean wasTerminated) {
		endedAt = Instant.now();
		this.wasTerminated = wasTerminated;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public long getExecutionId() {
		return executionId;
	}

	public Instant getEndedAt() {
		return endedAt;
	}

	public String getProjectPath() {
		return projectPath;
	}

	public String getName() {
		return name;
	}

	public Long getPid() {
		return pid;
	}

	public boolean wasTerminated() {
		return wasTerminated;
	}

	@Override
	public Duration idleFor() {
		if (endedAt != null) {
			return Duration.between(endedAt, Instant.now());
		} else {
			return nodeIdToTestStateMachine.values()
			                               .stream()
			                               .max(Comparator.comparing(TestStateMachine::getLastUpdatedAt))
			                               .map(tsm -> Duration.between(tsm.getLastUpdatedAt(), Instant.now()))
			                               .orElse(Duration.ZERO);

		}
	}

	@Override
	public boolean isActive() {
		return endedAt == null;
	}

	@Override
	public long id() {
		return executionId;
	}

	public Map<String, TestStateMachine> getStateMachines() {
		return Map.copyOf(nodeIdToTestStateMachine);
	}
}
