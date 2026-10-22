package net.jevring.jetbrainsjobsstatus.services.jobs;

import java.time.*;

public class TestStateMachine {
	private volatile TestState state = TestState.KNOWN;
	private volatile Instant lastUpdatedAt = Instant.now();
	private volatile Instant startedAt;
	private volatile Instant endedAt;
	private volatile String stackTrace;
	private volatile String message;
	private volatile String name;
	private volatile String locationUrl;

	public TestState getState() {
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

	public String getLocationUrl() {
		return locationUrl;
	}

	public void started() {
		if (state == TestState.KNOWN) {
			state = TestState.STARTED;
			lastUpdatedAt = Instant.now();
			startedAt = Instant.now();
		} else {
			System.err.printf("Tried to transition from %s to STARTED. Ignoring%n", state);
		}
	}

	public void finished() {
		if (state != TestState.FAILED && state != TestState.IGNORED) {
			state = TestState.SUCCEEDED;
			lastUpdatedAt = Instant.now();
			endedAt = lastUpdatedAt;
		} else {
			System.err.printf("Tried to transition from %s to IGNORED. Ignoring%n", state);
		}
	}

	public void failed(String message, String stackTrace) {
		if (state != TestState.SUCCEEDED && state != TestState.IGNORED) {
			state = TestState.FAILED;
			this.message = message;
			this.stackTrace = stackTrace;
			lastUpdatedAt = Instant.now();
			endedAt = lastUpdatedAt;
		} else {
			System.err.printf("Tried to transition from %s to FAILED. Ignoring%n", state);
		}
	}

	public void ignored() {
		if (state != TestState.FAILED && state != TestState.SUCCEEDED) {
			state = TestState.IGNORED;
			lastUpdatedAt = Instant.now();
		} else {
			System.err.printf("Tried to transition from %s to IGNORED. Ignoring%n", state);
		}
	}

	public TestStateMachine info(String name, String locationUrl) {
		// Unfortunately we don't get all the callbacks for parameterized tests, so we don't know
		// until we get a status what their names and urls are, so this has to always be called
		this.name = name;
		this.locationUrl = locationUrl;
		return this;
	}

	public void known() {
		lastUpdatedAt = Instant.now();
	}
}
