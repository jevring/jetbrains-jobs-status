package net.jevring.jetbrainsjobsstatus.services.jobs;

import org.jetbrains.annotations.*;

import java.time.*;

public class Process {
	private volatile Long pid;
	private volatile Instant scheduledAt;
	private volatile Instant startedAt;
	private volatile Instant endedAt;
	private volatile Integer exitCode;

	public void scheduled() {
		if (this.scheduledAt == null) {
			this.scheduledAt = Instant.now();
		}
	}

	public void starting(@Nullable Long pid) {
		if (this.startedAt == null) {
			this.startedAt = Instant.now();
		}
		if (this.pid == null) {
			this.pid = pid;
		}
	}

	public void notStarted() {
		this.endedAt = Instant.now();
	}

	public void terminated(int exitCode) {
		this.endedAt = Instant.now();
		this.exitCode = exitCode;
	}

	public Long getPid() {
		return pid;
	}

	public Instant getScheduledAt() {
		return scheduledAt;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public Instant getEndedAt() {
		return endedAt;
	}

	public Integer getExitCode() {
		return exitCode;
	}
}
