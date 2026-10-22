package net.jevring.jetbrainsjobsstatus.listeners;

import com.intellij.execution.testframework.sm.runner.*;
import com.intellij.execution.testframework.sm.runner.events.*;
import com.intellij.openapi.application.*;
import com.intellij.openapi.diagnostic.*;
import net.jevring.jetbrainsjobsstatus.services.*;
import net.jevring.jetbrainsjobsstatus.services.jobs.*;
import org.jetbrains.annotations.*;

import javax.swing.*;
import java.util.*;
import java.util.function.*;

public class JobStatusTestStatusListener extends SMTRunnerEventsAdapter {
	private static final Logger log = Logger.getInstance(JobStatusTestStatusListener.class);

	@Override
	public void onTestingStarted(SMTestProxy.@NotNull SMRootTestProxy testsRoot) {
		report(service -> service.testRun(testsRoot.getExecutionId()).started());
	}

	@Override
	public void onTestingFinished(SMTestProxy.@NotNull SMRootTestProxy smRootTestProxy) {
		report(service -> service.testRun(smRootTestProxy.getExecutionId()).finished(smRootTestProxy.wasTerminated()));
	}

	@Override
	public void onTestStarted(@NotNull SMTestProxy test, @Nullable String nodeId, @Nullable String parentNodeId) {
		test(test, nodeId, TestStateMachine::started);
	}

	@Override
	public void onTestFinished(@NotNull SMTestProxy test, @Nullable String nodeId) {
		test(test, nodeId, TestStateMachine::finished);
	}

	@Override
	public void onTestFailed(@NotNull SMTestProxy test, @Nullable String nodeId) {
		test(test, nodeId, testStateMachine -> testStateMachine.failed(test.getErrorMessage(), test.getStacktrace()));
	}

	@Override
	public void onTestIgnored(@NotNull SMTestProxy test, @Nullable String nodeId) {
		test(test, nodeId, TestStateMachine::ignored);
	}

	@Override
	public void onSuiteTreeNodeAdded(SMTestProxy test, boolean isSuite, @Nullable String nodeId, @Nullable String parentNodeId) {
		// This is not called for @ParameterizedTest, so we have to set the name and the URL in all the calls =(
		test(test, nodeId, TestStateMachine::known);
	}

	private static JobStatusApplicationService service() {
		return ApplicationManager.getApplication().getService(JobStatusApplicationService.class);
	}

	private static void report(Consumer<JobStatusApplicationService> reporter) {
		SwingUtilities.invokeLater(() -> reporter.accept(service()));
	}

	private static void test(@NotNull SMTestProxy test, @Nullable String nodeId, Consumer<TestStateMachine> tsmc) {
		if (nodeId == null) {
			log.warn("Skipping callback for null nodeId for test " + test.getPresentableName());
			return;
		}
		report(service -> tsmc.accept(service.testRun(test.getRoot().getExecutionId())
		                                     .testForNode(nodeId)
		                                     .info(test.getPresentableName(), test.getLocationUrl())));
	}

}
