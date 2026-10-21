package net.jevring.jetbrainsjobsstatus.listeners;

import com.intellij.execution.process.*;
import com.intellij.execution.testframework.sm.runner.*;
import com.intellij.openapi.application.*;
import com.intellij.openapi.diagnostic.*;
import net.jevring.jetbrainsjobsstatus.services.*;
import net.jevring.jetbrainsjobsstatus.services.jobs.*;
import org.jetbrains.annotations.*;

import javax.swing.*;
import java.util.function.*;

public class JobStatusTestStatusListener extends SMTRunnerEventsAdapter {
	private static final Logger log = Logger.getInstance(JobStatusTestStatusListener.class);

	@Override
	public void onTestingStarted(SMTestProxy.@NotNull SMRootTestProxy testsRoot) {
		report(service -> service.testRun(testsRoot.getExecutionId()).started(pidOf(testsRoot)));
	}

	@Override
	public void onTestingFinished(SMTestProxy.@NotNull SMRootTestProxy smRootTestProxy) {
		TestRun.Termination termination = smRootTestProxy.wasTerminated() ? TestRun.Termination.TERMINATED : TestRun.Termination.NORMAL;
		report(service -> service.testRun(smRootTestProxy.getExecutionId()).finished(termination, 0));
	}

	@Override
	public void onTestStarted(@NotNull SMTestProxy test, @Nullable String nodeId, @Nullable String parentNodeId) {
		if (nodeId == null) {
			log.warn("Skipping onTestStarted for null nodeId for test " + test.getPresentableName());
			return;
		}
		report(service -> service.testRun(test.getRoot().getExecutionId()).testForNode(nodeId).started(test.getPresentableName()));
	}

	@Override
	public void onTestFinished(@NotNull SMTestProxy test, @Nullable String nodeId) {
		if (nodeId == null) {
			log.warn("Skipping onTestFinished for null nodeId for test " + test.getPresentableName());
			return;
		}
		report(service -> service.testRun(test.getRoot().getExecutionId()).testForNode(nodeId).finished());
	}

	@Override
	public void onTestFailed(@NotNull SMTestProxy test, @Nullable String nodeId) {
		if (nodeId == null) {
			log.warn("Skipping onTestFailed for null nodeId for test " + test.getPresentableName());
			return;
		}
		report(service -> service.testRun(test.getRoot().getExecutionId()).testForNode(nodeId).failed(test.getErrorMessage(), test.getStacktrace()));
	}

	@Override
	public void onTestIgnored(@NotNull SMTestProxy test, @Nullable String nodeId) {
		if (nodeId == null) {
			log.warn("Skipping onTestIgnored for null nodeId for test " + test.getPresentableName());
			return;
		}
		report(service -> service.testRun(test.getRoot().getExecutionId()).testForNode(nodeId).ignored());
	}

	@Override
	public void onSuiteTreeNodeAdded(SMTestProxy test, boolean isSuite, @Nullable String nodeId, @Nullable String parentNodeId) {
		if (nodeId == null) {
			log.warn("Skipping onSuiteTreeNodeAdded for null nodeId for test " + test.getPresentableName());
			return;
		}
		report(service -> service.testRun(test.getRoot().getExecutionId()).testForNode(nodeId).known());
	}

	private static JobStatusApplicationService service() {
		return ApplicationManager.getApplication().getService(JobStatusApplicationService.class);
	}

	private static void report(Consumer<JobStatusApplicationService> reporter) {
		SwingUtilities.invokeLater(() -> reporter.accept(service()));
	}

	private static Long pidOf(SMTestProxy.SMRootTestProxy testsRoot) {
		return Utils.pidOf(testsRoot.getHandler());
	}
}
