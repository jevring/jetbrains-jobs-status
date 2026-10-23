package net.jevring.jetbrainsjobsstatus.listeners;

import com.intellij.execution.*;
import com.intellij.execution.configurations.*;
import com.intellij.execution.process.*;
import com.intellij.execution.runners.*;
import com.intellij.execution.testframework.sm.runner.*;
import com.intellij.openapi.application.*;
import net.jevring.jetbrainsjobsstatus.services.*;
import net.jevring.jetbrainsjobsstatus.services.jobs.*;
import org.jetbrains.annotations.*;

public class JobStatusExecutionListener implements ExecutionListener {
	@Override
	public void processStartScheduled(@NotNull String executorId, @NotNull ExecutionEnvironment env) {
		recordStartUnlessRecorded(env, null);
	}

	@Override
	public void processStarting(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler) {
		recordStartUnlessRecorded(env, handler);
	}

	@Override
	public void processStarted(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler) {
		// We need this, because launching via MCP means that processStarting() doesn't get called
		recordStartUnlessRecorded(env, handler);
	}

	@Override
	public void processNotStarted(@NotNull String executorId, @NotNull ExecutionEnvironment env, Throwable cause) {
		RunProfile runProfile = env.getRunProfile();
		if (runProfile instanceof SMRunnerConsolePropertiesProvider) {
			service().testRun(env.getExecutionId()).process().notStarted();
		}
	}

	@Override
	public void processTerminated(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler, int exitCode) {
		RunProfile runProfile = env.getRunProfile();
		if (runProfile instanceof SMRunnerConsolePropertiesProvider) {
			service().testRun(env.getExecutionId()).process().terminated(exitCode);
		}
	}

	private static JobStatusApplicationService service() {
		return ApplicationManager.getApplication().getService(JobStatusApplicationService.class);
	}

	private static void recordStartUnlessRecorded(@NotNull ExecutionEnvironment env, ProcessHandler handler) {
		RunProfile runProfile = env.getRunProfile();
		if (runProfile instanceof SMRunnerConsolePropertiesProvider) {
			// We're assuming this is a test, because we're listening to the tests with a similarly named class

			long executionId = env.getExecutionId();
			if (executionId <= 0) {
				// In the MCP case, we get a 0 here at the start, which is almost certainly wrong.
				// When launching manually, the first execution id observed is 2.
				// We're going to assume this is reliable. Mostly because this is a hacky work-around anyway
				// Even when launching via MCP, we never see executionId=1. It's always 2+

				// This unfortunately means that any MCP job that fails, due to something like a compilation error, 
				// will not have a name or project path, since we can't assign it to the correct test run.
				// We could potentially keep state somewhere and guestimate, but that doesn't seem right. It might also fail under concurrency.

				return;
			}
			TestRun testRun = service().testRun(executionId);
			// Unfortunately we have to do them *all* here to work around bugs in the MCP implementation.
			// When you launch a test by clicking the button in intellij, you get an ordered sequence of scheduled() -> starting() -> started(), with consistent execution ids.
			// When you launch it via the MCP endpoint execute_run_configuration, you get scheduled() with execution id 0 always, followed by started() with the correct 
			// execution id
			String projectPath = env.getProject().getBasePath();
			String name = runProfile.getName();
			testRun.setNameAndProjectPath(name, projectPath);
			testRun.process().scheduled();
			if (handler != null) {
				testRun.process().starting(Utils.pidOf(handler));
			}
		}
	}
}
