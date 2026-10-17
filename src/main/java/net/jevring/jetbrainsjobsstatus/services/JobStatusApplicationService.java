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
	private final Map<Long, TestRun> testRuns = new ConcurrentHashMap<>();

	public TestRun run(long executionId) {
		return testRuns.computeIfAbsent(executionId, TestRun::new);
	}

	public List<TestRun> getTestRuns() {
		return testRuns.values().stream().sorted(Comparator.comparing(TestRun::getExecutionId)).toList();
	}
}
