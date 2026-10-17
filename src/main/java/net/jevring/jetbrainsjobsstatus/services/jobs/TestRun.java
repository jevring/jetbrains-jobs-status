package net.jevring.jetbrainsjobsstatus.services.jobs;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

public final class TestRun implements Job {
	private final Map<String, TestStateMachine> nodeIdToTestStateMachine = new ConcurrentHashMap<>();
	private final Instant startedAt;
	private final long executionId;
	private Instant endedAt;

	public TestRun(long executionId) {
		this.startedAt = Instant.now();
		this.executionId = executionId;
	}

	public TestStateMachine testForNode(String nodeId) {
		return nodeIdToTestStateMachine.computeIfAbsent(nodeId, ignored -> new TestStateMachine());
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

	public Map<String, TestStateMachine> getStateMachines() {
		return Map.copyOf(nodeIdToTestStateMachine);
	}
}
