package net.jevring.jetbrainsjobsstatus.services.jobs;

import java.time.*;

public final class Build implements Job {
	private final long id;
	private String projectPath;
	private State state = State.STARTED;
	private Instant lastUpdatedAt = Instant.now();

	public Build(long id) {
		this.id = id;
	}

	public void complete(State state) {
		this.state = state;
		this.lastUpdatedAt = Instant.now();
	}

	public long getId() {
		return id;
	}

	public State getState() {
		return state;
	}

	public String getProjectPath() {
		return projectPath;
	}

	public Instant getLastUpdatedAt() {
		return lastUpdatedAt;
	}

	public void started(String projectPath) {
		this.projectPath = projectPath;
		this.lastUpdatedAt = Instant.now();
	}

	public enum State {
		STARTED,
		COMPLETED,
		FAILED
	}
}

