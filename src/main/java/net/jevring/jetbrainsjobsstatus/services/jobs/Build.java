package net.jevring.jetbrainsjobsstatus.services.jobs;

import java.time.*;
import java.util.*;

public final class Build implements Job {
	private final long id;
	private volatile String projectPath;
	private volatile BuildState state = BuildState.STARTED;

	private volatile Instant startedAt;
	private volatile Instant endedAt;

	public Build(long id) {
		this.id = id;
	}

	public void complete(BuildState state) {
		this.state = state;
		this.endedAt = Instant.now();
	}

	public long getId() {
		return id;
	}

	public BuildState getState() {
		return state;
	}

	public String getProjectPath() {
		return projectPath;
	}

	public Instant getEndedAt() {
		return endedAt;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public void started(String projectPath) {
		this.projectPath = projectPath;
		this.startedAt = Instant.now();
	}

	public Optional<Duration> getDuration() {
		if (endedAt != null) {
			return Optional.of(Duration.between(startedAt, endedAt));
		} else {
			return Optional.empty();
		}
	}

	@Override
	public Duration idleFor() {
		if (endedAt != null) {
			return Duration.between(endedAt, Instant.now());
		} else {
			return Duration.between(startedAt, Instant.now());
		}
	}

	@Override
	public boolean isActive() {
		return state == BuildState.STARTED;
	}

	@Override
	public long id() {
		return id;
	}

}

