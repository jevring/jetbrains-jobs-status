package net.jevring.jetbrainsjobsstatus.rest.model;

import net.jevring.jetbrainsjobsstatus.services.jobs.*;

import java.util.*;

/**
 * @param lastUpdateAt This is a string because Gson doesn't automatically convert time classes to strings
 */
public record TestRun(long executionId,
                      String name,
                      String projectPath,
                      Status status,
                      Process process,
                      long totalTests,
                      long successfulTests,
                      long failedTests,
                      long ignoredTests,
                      String startedAt,
                      String lastUpdateAt,
                      String duration,
                      String idleFor,
                      List<Test> tests,
                      List<Failure> failures) {

	public enum Status {
		RUNNING,
		ABORTED,
		COMPLETED_SUCCESSFULLY,
		COMPLETED_WITH_FAILURES,
		COMPLETED_WITHOUT_TESTS
	}

	public record Failure(String nodeId, String name, String locationUrl, String message, String stackTrace) {}

	public record Test(String nodeId, String name, String locationUrl, TestState state) {}
}
