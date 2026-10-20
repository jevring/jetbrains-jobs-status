package net.jevring.jetbrainsjobsstatus.listeners;

import com.intellij.execution.testframework.sm.runner.*;
import com.intellij.openapi.application.*;
import com.intellij.openapi.diagnostic.*;
import net.jevring.jetbrainsjobsstatus.services.*;
import org.jetbrains.annotations.*;

import javax.swing.*;
import java.util.concurrent.*;
import java.util.function.*;

public class JobStatusTestStatusListener extends SMTRunnerEventsAdapter {
	private static final Logger log = Logger.getInstance(JobStatusTestStatusListener.class);

	@Override
	public void onTestingStarted(SMTestProxy.@NotNull SMRootTestProxy testsRoot) {
		report(service -> service.testRun(testsRoot.getExecutionId()).started(pidOf(testsRoot)));
	}

	private Long pidOf(SMTestProxy.SMRootTestProxy testsRoot) {
		CompletableFuture<@Nullable Long> pidFuture = testsRoot.getHandler().getNativePid();
		if (pidFuture != null) {
			try {
				// This is a CompletableFuture.completedFuture() whenever it isn't null.
				// Not sure why it isn't just a long, but let's make some assumptions about 
				// the implementation here
				return pidFuture.get();
			} catch (InterruptedException | ExecutionException e) {
				return null;
			}
		} else {
			return null;
		}
	}

	@Override
	public void onTestingFinished(SMTestProxy.@NotNull SMRootTestProxy smRootTestProxy) {
		report(service -> service.testRun(smRootTestProxy.getExecutionId()).finished(smRootTestProxy.wasTerminated()));
	}

	@Override
	public void onTestStarted(@NotNull SMTestProxy test, @Nullable String nodeId, @Nullable String parentNodeId) {
		if (nodeId == null) {
			log.warn("Skipping onTestStarted for null nodeId for test " + test.getPresentableName());
			return;
		}
		report(service -> service.testRun(test.getRoot().getExecutionId()).testForNode(nodeId).started());
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
}
