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
		RunProfile runProfile = env.getRunProfile();
		if (runProfile instanceof SMRunnerConsolePropertiesProvider) {
			// We're assuming this is a test, because we're listening to the tests with a similarly named class
			String projectPath = env.getProject().getBasePath();
			String name = runProfile.getName();
			TestRun testRun = service().testRun(env.getExecutionId());
			testRun.setNameAndProjectPath(name, projectPath);
			testRun.process().scheduled();
		}
	}

	@Override
	public void processStarting(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler) {
		RunProfile runProfile = env.getRunProfile();
		if (runProfile instanceof SMRunnerConsolePropertiesProvider) {
			service().testRun(env.getExecutionId()).process().starting(Utils.pidOf(handler));
		}
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
}
