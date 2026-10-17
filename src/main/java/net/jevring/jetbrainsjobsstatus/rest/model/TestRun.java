package net.jevring.jetbrainsjobsstatus.rest.model;

import java.util.*;

/**
 * @param lastUpdateAt This is a string because Gson doesn't automatically convert time classes to strings
 */
public record TestRun(long executionId,
                      long totalTests,
                      long successfulTests,
                      long failedTests,
                      long ignoredTests,
                      String lastUpdateAt,
                      Status status,
                      List<Failure> failures) {

	public enum Status {
		RUNNING,
		ABORTED,
		COMPLETED_SUCCESSFULLY,
		COMPLETED_WITH_FAILURES,
	}

	public record Failure(String nodeId, String error, String stackTrace) {}
}
