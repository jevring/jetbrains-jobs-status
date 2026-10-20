package net.jevring.jetbrainsjobsstatus.services;

import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.*;
import net.jevring.jetbrainsjobsstatus.services.jobs.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * This tracks all the jobs for all the open projects in the application
 */
@Service(Service.Level.APP)
public final class JobStatusApplicationService {
	private static final Logger log = Logger.getInstance(JobStatusApplicationService.class);
	private static final Duration CLEAN_UP_AGE = Duration.ofMinutes(10);
	private final Map<Long, TestRun> testRuns = new ConcurrentHashMap<>();
	private final Map<Long, Build> builds = new ConcurrentHashMap<>();

	public TestRun testRun(long executionId) {
		return testRuns.computeIfAbsent(executionId, TestRun::new);
	}

	public Optional<TestRun> getTestRun(long executionId) {
		return Optional.ofNullable(testRuns.get(executionId));
	}

	public Build build(long buildId) {
		return builds.computeIfAbsent(buildId, Build::new);
	}

	public Optional<Build> getBuild(long buildId) {
		return Optional.ofNullable(builds.get(buildId));
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
	void cleanUp() {
		try {
			cleanUp(testRuns);
			cleanUp(builds);
		} catch (Exception e) {
			log.error("Could not clean up jobs", e);
		}
	}

	private <T extends Job> void cleanUp(Map<Long, T> jobs) {
		List<T> inactiveJobsInIdleOrder = jobs.values().stream().filter(job -> !job.isActive()).sorted(Comparator.comparing(Job::idleFor)).toList();
		// We're starting from 1, since we always want to keep the most recent job, no matter what.
		// sorting by idle means that we're sorting from lowest idle time to highest, which means 
		// that lowest idle time is first, and lowest idle time means most recent
		for (int i = 1; i < inactiveJobsInIdleOrder.size(); i++) {
			T job = inactiveJobsInIdleOrder.get(i);
			if (job.idleFor().compareTo(CLEAN_UP_AGE) > 0) {
				log.info("Cleaning up job " + job + " which has been inactive for " + job.idleFor() + " which is longer than " + CLEAN_UP_AGE);
				jobs.remove(job.id());
			}
		}
	}
}
