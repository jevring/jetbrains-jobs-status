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
	private final long executionId;

	// These fields are mutated on the EDT and read by the RestService on a different thread, so they should be volatile
	private volatile Instant scheduledAt;
	private volatile Instant startedAt;
	private volatile Instant endedAt;
	private volatile String projectPath;
	private volatile String name;
	private volatile Long pid;
	private volatile Termination termination;
	private volatile Integer exitCode;

	public TestRun(long executionId) {
		this.executionId = executionId;
	}

	public TestStateMachine testForNode(String nodeId) {
		return nodeIdToTestStateMachine.computeIfAbsent(nodeId, ignored -> new TestStateMachine());
	}

	public void scheduled(String name, String projectPath) {
		this.name = name;
		this.projectPath = projectPath;
		this.scheduledAt = Instant.now();
	}

	public void started(Long pid) {
		if (this.pid == null && pid != null) {
			this.pid = pid;
		}
		this.startedAt = Instant.now();
	}

	public void finished(Termination termination, Integer exitCode) {
		endedAt = Instant.now();
		// As this is called from multiple places, we don't want to blindly overwrite what we have
		// Technically there's a check-then-act thing here, since it's volatile, not atomic,
		// but since we want to let this be null, as opposed to an int, which is primitive, 
		// we're going to let it slide.
		if (this.termination == null || termination != null) {
			this.termination = termination;
		}
		if (this.exitCode == null || exitCode != null) {
			this.exitCode = exitCode;
		}
	}

	public Instant getScheduledAt() {
		return scheduledAt;
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

	public Integer getExitCode() {
		return exitCode;
	}

	public Termination getTermination() {
		return termination;
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
			                               .orElseGet(() -> {
				                               // These could all have fallen back to a lastUpdatedAt, but since we
				                               // want to keep some of these timestamps around anyway, we may as well use them
				                               if (startedAt != null) {
					                               return Duration.between(startedAt, Instant.now());
				                               } else if (scheduledAt != null) {
					                               return Duration.between(scheduledAt, Instant.now());
				                               } else {
					                               return Duration.between(createdAt, Instant.now());
				                               }
			                               });

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

	public enum Termination {
		NOT_STARTED,
		NORMAL,
		TERMINATED
	}
}
