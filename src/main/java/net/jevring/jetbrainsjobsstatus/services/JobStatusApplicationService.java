package net.jevring.jetbrainsjobsstatus.services;

import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.*;
import com.intellij.util.concurrency.*;
import net.jevring.jetbrainsjobsstatus.listeners.*;
import net.jevring.jetbrainsjobsstatus.services.jobs.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * This tracks all the jobs for all the open projects in the application
 */
@Service(Service.Level.APP)
public final class JobStatusApplicationService {
	private static final Logger log = Logger.getInstance(JobStatusProjectTaskListener.class);
	private static final Duration CLEAN_UP_AGE = Duration.ofMinutes(10);
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

	/**
	 * Cleans up inactive jobs that are older than {@link #CLEAN_UP_AGE}.
	 */
	void cleanup() {
		try {
			cleanupEntries(testRuns.entrySet());
			cleanupEntries(builds.entrySet());
		} catch (Exception e) {
			log.error("Could not clean up jobs", e);
		}
	}

	private <T extends Job> void cleanupEntries(Set<Map.Entry<Long, T>> entries) {
		Iterator<Map.Entry<Long, T>> tri = entries.iterator();
		while (tri.hasNext()) {
			Map.Entry<Long, T> entry = tri.next();
			T job = entry.getValue();
			if (job.isActive()) {
				continue;
			}
			if (job.idleFor().compareTo(CLEAN_UP_AGE) > 0) {
				log.info("Cleaning up job " + job);
				tri.remove();
			}
		}
	}
}
