package net.jevring.jetbrainsjobsstatus.listeners;

import com.intellij.execution.*;
import com.intellij.execution.configurations.*;
import com.intellij.execution.process.*;
import com.intellij.execution.runners.*;
import com.intellij.execution.testframework.sm.runner.*;
import com.intellij.openapi.application.*;
import net.jevring.jetbrainsjobsstatus.services.*;
import org.jetbrains.annotations.*;

import javax.print.attribute.standard.*;
import javax.swing.*;
import java.util.function.*;

public class JobStatusExecutionListener implements ExecutionListener {
	@Override
	public void processStarting(@NotNull String executorId, @NotNull ExecutionEnvironment env) {
		System.out.println("JobStatusExecutionListener.processStarting executorId = " +
		                   executorId +
		                   ", env = " +
		                   env +
		                   ", execution id = " +
		                   env.getExecutionId());
		RunProfile runProfile = env.getRunProfile();
		if (runProfile instanceof SMRunnerConsolePropertiesProvider) {
			// We're assuming this is a test, because we're listening to the tests with a similarly named class
			String projectPath = env.getProject().getBasePath();
			String name = runProfile.getName();
			report(service -> service.testRun(env.getExecutionId()).setNameAndPath(name, projectPath));
		}
	}

	private static JobStatusApplicationService service() {
		return ApplicationManager.getApplication().getService(JobStatusApplicationService.class);
	}

	private static void report(Consumer<JobStatusApplicationService> reporter) {
		SwingUtilities.invokeLater(() -> reporter.accept(service()));
	}
}
