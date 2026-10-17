package net.jevring.jetbrainsjobsstatus.listeners;

import com.intellij.execution.testframework.sm.runner.*;
import com.intellij.execution.testframework.sm.runner.events.*;
import com.intellij.openapi.application.*;
import com.intellij.openapi.util.*;
import net.jevring.jetbrainsjobsstatus.services.*;
import org.jetbrains.annotations.*;

import javax.swing.*;
import java.util.function.*;

public class JobStatusTestStatusListener extends SMTRunnerEventsAdapter {
	@Override
	public void onTestingFinished(SMTestProxy.@NotNull SMRootTestProxy smRootTestProxy) {
		// We can use smRootTestProxy.wasTerminated() here to determine if the process or job was prematurely terminated, 
		// as opposed to if it could run to completion. We don't actually need to, though, since we're able to count the number of tests
		report(service -> service.run(smRootTestProxy.getExecutionId()).finished());
	}

	@Override
	public void onTestStarted(@NotNull SMTestProxy test, @Nullable String nodeId, @Nullable String parentNodeId) {
		report(service -> service.run(test.getRoot().getExecutionId()).testForNode(nodeId).started());
	}

	@Override
	public void onTestFinished(@NotNull SMTestProxy test, @Nullable String nodeId) {
		report(service -> service.run(test.getRoot().getExecutionId()).testForNode(nodeId).finished());
	}

	@Override
	public void onTestFailed(@NotNull SMTestProxy test, @Nullable String nodeId) {
		report(service -> service.run(test.getRoot().getExecutionId()).testForNode(nodeId).failed(test.getErrorMessage(), test.getStacktrace()));
	}

	@Override
	public void onTestIgnored(@NotNull SMTestProxy test, @Nullable String nodeId) {
		report(service -> service.run(test.getRoot().getExecutionId()).testForNode(nodeId).ignored());
	}

	@Override
	public void onSuiteTreeNodeAdded(SMTestProxy testProxy, boolean isSuite, @Nullable String nodeId, @Nullable String parentNodeId) {
		report(service -> service.run(testProxy.getRoot().getExecutionId()).testForNode(nodeId).known());
	}

	private static JobStatusApplicationService service() {
		return ApplicationManager.getApplication().getService(JobStatusApplicationService.class);
	}

	private static void report(Consumer<JobStatusApplicationService> reporter) {
		SwingUtilities.invokeLater(() -> reporter.accept(service()));
	}
}
