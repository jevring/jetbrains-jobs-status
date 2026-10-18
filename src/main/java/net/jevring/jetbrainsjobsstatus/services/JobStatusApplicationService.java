package net.jevring.jetbrainsjobsstatus.services;

import com.intellij.openapi.components.*;
import net.jevring.jetbrainsjobsstatus.services.jobs.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * This tracks all the jobs for all the open projects in the application
 */
@Service(Service.Level.APP)
public final class JobStatusApplicationService {
	// todo: schedule clean-up of the things
	private final Map<Long, TestRun> testRuns = new ConcurrentHashMap<>();
	private final Map<Long, Build> builds = new ConcurrentHashMap<>();

	public TestRun testRun(long executionId) {
		return testRuns.computeIfAbsent(executionId, TestRun::new);
	}

	public Build build(long buildId) {
		return builds.computeIfAbsent(buildId, Build::new);
	}

	public List<TestRun> getTestRuns() {
		return testRuns.values().stream().sorted(Comparator.comparing(TestRun::getExecutionId)).toList();
	}

	public List<Build> getBuilds() {
		return builds.values().stream().sorted(Comparator.comparing(Build::getId)).toList();
	}
}
