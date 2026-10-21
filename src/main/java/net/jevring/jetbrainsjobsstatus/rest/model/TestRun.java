package net.jevring.jetbrainsjobsstatus.rest.model;

import java.util.*;

/**
 * @param lastUpdateAt This is a string because Gson doesn't automatically convert time classes to strings
 */
public record TestRun(long executionId,
                      String name,
                      String projectPath,
                      long totalTests,
                      long successfulTests,
                      long failedTests,
                      long ignoredTests,
                      String scheduledAt,
                      String startedAt,
                      String lastUpdateAt,
                      String duration,
                      Status status,
                      String idleFor,
                      Long pid,
                      Integer exitCode,
                      List<Failure> failures) {

	public enum Status {
		RUNNING,
		ABORTED,
		COMPLETED_SUCCESSFULLY,
		COMPLETED_WITH_FAILURES,
	}

	public record Failure(String nodeId, String name, String message, String stackTrace) {}
}
