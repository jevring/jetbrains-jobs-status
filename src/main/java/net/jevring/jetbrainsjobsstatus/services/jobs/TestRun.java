package net.jevring.jetbrainsjobsstatus.services.jobs;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

public final class TestRun implements Job {
	private final Map<String, TestStateMachine> nodeIdToTestStateMachine = new ConcurrentHashMap<>();
	/**
	 * This is a fallback in case all other timestamps are null.
	 */
	private final Instant createdAt = Instant.now();
	private final Process process = new Process();
	private final long executionId;

	// These fields are mutated on the EDT and read by the RestService on a different thread, so they should be volatile
	private volatile Instant startedAt;
	private volatile Instant finishedAt;
	private volatile String projectPath;
	private volatile String name;
	private volatile boolean terminated;

	public TestRun(long executionId) {
		this.executionId = executionId;
	}

	public Process process() {
		return this.process;
	}

	public TestStateMachine testForNode(String nodeId) {
		return nodeIdToTestStateMachine.computeIfAbsent(nodeId, ignored -> new TestStateMachine());
	}

	public void setNameAndProjectPath(String name, String projectPath) {
		if (this.name == null) {
			this.name = name;
		}
		if (this.projectPath == null) {
			this.projectPath = projectPath;
		}
	}

	public void started() {
		this.startedAt = Instant.now();
	}

	public void finished(boolean wasTerminated) {
		this.terminated = wasTerminated;
		this.finishedAt = Instant.now();
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getFinishedAt() {
		return finishedAt;
	}

	public boolean wasTerminated() {
		return terminated;
	}

	public long getExecutionId() {
		return executionId;
	}

	public String getProjectPath() {
		return projectPath;
	}

	public String getName() {
		return name;
	}

	@Override
	public Duration idleFor() {
		// We assume that we get callbacks in such an order that the suite is terminated after each test in the suite.
		// As all events that require the execution id have to go through the EDT (why this is I don't know, but whatever),
		// they would get linearized, assuming the callbacks happen in order.
		if (finishedAt != null) {
			return Duration.between(finishedAt, Instant.now());
		} else {
			return nodeIdToTestStateMachine.values()
			                               .stream()
			                               .max(Comparator.comparing(TestStateMachine::getLastUpdatedAt))
			                               .map(tsm -> Duration.between(tsm.getLastUpdatedAt(), Instant.now()))
			                               .orElseGet(() -> {
				                               // These could all have fallen back to a lastUpdatedAt, but since we
				                               // want to keep some of these timestamps around anyway, we may as well use them
				                               if (startedAt != null) {
					                               return Duration.between(startedAt, Instant.now());
				                               } else {
					                               return Duration.between(createdAt, Instant.now());
				                               }
			                               });

		}
	}

	@Override
	public boolean isActive() {
		return finishedAt == null;
	}

	@Override
	public long id() {
		return executionId;
	}

	public Map<String, TestStateMachine> getStateMachines() {
		return Map.copyOf(nodeIdToTestStateMachine);
	}
}
