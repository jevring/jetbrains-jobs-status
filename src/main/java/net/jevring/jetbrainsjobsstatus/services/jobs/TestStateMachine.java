package net.jevring.jetbrainsjobsstatus.services.jobs;

import java.time.*;

public class TestStateMachine {
	private State state = State.KNOWN;
	private Instant firstTimestamp = Instant.now();
	private Instant lastTimestamp = firstTimestamp;
	private String stackTrace;
	private String error;

	public State getState() {
		return state;
	}

	public Instant getFirstTimestamp() {
		return firstTimestamp;
	}

	public Instant getLastTimestamp() {
		return lastTimestamp;
	}

	public String getError() {
		return error;
	}

	public String getStackTrace() {
		return stackTrace;
	}

	public void started() {
		if (state == State.KNOWN) {
			state = State.STARTED;
			lastTimestamp = Instant.now();
		} else {
			System.err.printf("Tried to transition from %s to STARTED. Ignoring%n", state);
		}
	}

	public void finished() {
		if (state != State.FAILED && state != State.IGNORED) {
			state = State.SUCCEEDED;
			lastTimestamp = Instant.now();
		} else {
			System.err.printf("Tried to transition from %s to IGNORED. Ignoring%n", state);
		}
	}

	public void failed(String error, String stackTrace) {
		if (state != State.SUCCEEDED && state != State.IGNORED) {
			state = State.FAILED;
			this.error = error;
			this.stackTrace = stackTrace;
			lastTimestamp = Instant.now();
		} else {
			System.err.printf("Tried to transition from %s to FAILED. Ignoring%n", state);
		}
	}

	public void ignored() {
		if (state != State.FAILED && state != State.SUCCEEDED) {
			state = State.IGNORED;
			lastTimestamp = Instant.now();
		} else {
			System.err.printf("Tried to transition from %s to IGNORED. Ignoring%n", state);
		}
	}

	public void known() {
		// It's already known, by virtue of existing
		lastTimestamp = Instant.now();
	}

	public String report() {
		if (state == State.FAILED) {
			return String.format("%s at %s because \r\n%s", state, lastTimestamp, error);
		} else {
			return String.format("%s at %s", state, lastTimestamp);
		}
	}

	public enum State {
		KNOWN,
		STARTED,
		SUCCEEDED,
		FAILED,
		IGNORED
	}
}
