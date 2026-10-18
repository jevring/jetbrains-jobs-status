package net.jevring.jetbrainsjobsstatus.services.jobs;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

public final class TestRun implements Job {
	private final Map<String, TestStateMachine> nodeIdToTestStateMachine = new ConcurrentHashMap<>();
	private final Instant startedAt;
	private final long executionId;
	private Instant endedAt;
	private String projectPath;
	private String name;

	public TestRun(long executionId) {
		this.startedAt = Instant.now();
		this.executionId = executionId;
	}

	public TestStateMachine testForNode(String nodeId) {
		return nodeIdToTestStateMachine.computeIfAbsent(nodeId, ignored -> new TestStateMachine());
	}

	public void setNameAndPath(String name, String projectPath) {
		this.name = name;
		this.projectPath = projectPath;
	}

	public void finished() {
		endedAt = Instant.now();
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

	@Override
	public Duration idleFor() {
		if (endedAt != null) {
			return Duration.between(endedAt, Instant.now());
		} else {
			return nodeIdToTestStateMachine.values()
			                               .stream()
			                               .min(Comparator.comparing(TestStateMachine::getLastTimestamp))
			                               .map(tsm -> Duration.between(tsm.getLastTimestamp(), Instant.now()))
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
