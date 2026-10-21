package net.jevring.jetbrainsjobsstatus.services.jobs;

import com.intellij.openapi.util.*;
import org.jetbrains.annotations.*;

import java.time.*;

public class TestStateMachine {
	private volatile State state = State.KNOWN;
	private volatile Instant lastUpdatedAt;
	private volatile Instant startedAt;
	private volatile Instant endedAt;
	private volatile String stackTrace;
	private volatile String message;
	private volatile String name;

	public State getState() {
		return state;
	}

	public Instant getLastUpdatedAt() {
		return lastUpdatedAt;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public Instant getEndedAt() {
		return endedAt;
	}

	public String getMessage() {
		return message;
	}

	public String getStackTrace() {
		return stackTrace;
	}

	public String getName() {
		return name;
	}

	public void started(String name) {
		if (state == State.KNOWN) {
			state = State.STARTED;
			lastUpdatedAt = Instant.now();
			startedAt = Instant.now();
			this.name = name;
		} else {
			System.err.printf("Tried to transition from %s to STARTED. Ignoring%n", state);
		}
	}

	public void finished() {
		if (state != State.FAILED && state != State.IGNORED) {
			state = State.SUCCEEDED;
			lastUpdatedAt = Instant.now();
			endedAt = lastUpdatedAt;
		} else {
			System.err.printf("Tried to transition from %s to IGNORED. Ignoring%n", state);
		}
	}

	public void failed(String message, String stackTrace) {
		if (state != State.SUCCEEDED && state != State.IGNORED) {
			state = State.FAILED;
			this.message = message;
			this.stackTrace = stackTrace;
			lastUpdatedAt = Instant.now();
			endedAt = lastUpdatedAt;
		} else {
			System.err.printf("Tried to transition from %s to FAILED. Ignoring%n", state);
		}
	}

	public void ignored() {
		if (state != State.FAILED && state != State.SUCCEEDED) {
			state = State.IGNORED;
			lastUpdatedAt = Instant.now();
		} else {
			System.err.printf("Tried to transition from %s to IGNORED. Ignoring%n", state);
		}
	}

	public void known() {
		// It's already known, by virtue of existing
		lastUpdatedAt = Instant.now();
	}

	public enum State {
		KNOWN,
		STARTED,
		SUCCEEDED,
		FAILED,
		IGNORED
	}
}
