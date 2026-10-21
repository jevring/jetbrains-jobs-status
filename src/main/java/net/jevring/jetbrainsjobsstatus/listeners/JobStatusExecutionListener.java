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
			service().testRun(env.getExecutionId()).scheduled(name, projectPath);
		}
	}

	@Override
	public void processStarting(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler) {
		RunProfile runProfile = env.getRunProfile();
		if (runProfile instanceof SMRunnerConsolePropertiesProvider) {
			service().testRun(env.getExecutionId()).started(Utils.pidOf(handler));
		}
	}

	@Override
	public void processNotStarted(@NotNull String executorId, @NotNull ExecutionEnvironment env, Throwable cause) {
		RunProfile runProfile = env.getRunProfile();
		if (runProfile instanceof SMRunnerConsolePropertiesProvider) {
			// If the process wasn't even started, it's the same, semantically, as if it was terminated in the middle.
			// It's still aborted. The only thing that differs is the number of test registered (0 in this case)
			service().testRun(env.getExecutionId()).finished(TestRun.Termination.NOT_STARTED, null);
		}
	}

	@Override
	public void processTerminated(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler, int exitCode) {
		RunProfile runProfile = env.getRunProfile();
		if (runProfile instanceof SMRunnerConsolePropertiesProvider) {
			// We're not going to make assumptions about the termination based on the exit code. 
			// We're going to rely on something else making that call
			service().testRun(env.getExecutionId()).finished(null, exitCode);
		}
	}

	private static JobStatusApplicationService service() {
		return ApplicationManager.getApplication().getService(JobStatusApplicationService.class);
	}
}
