package net.jevring.jetbrainsjobsstatus.listeners;

import com.intellij.openapi.application.*;
import com.intellij.openapi.project.*;
import net.jevring.jetbrainsjobsstatus.services.*;
import org.jetbrains.annotations.*;

/**
 * Listening to when a project closes allows us to clean up after ourselves, because we don't get any other lifecycle callbacks when the project is closing.
 * For example if the tests are running, and the project closes, we don't get that callback, so it would look like the tests are running forever.
 */
public class JobStatusProjectManagerListener implements ProjectManagerListener {
	@Override
	public void projectClosed(@NotNull Project project) {
		service().cancelAllRunningJobsForProject(project);
	}

	@Override
	public void projectClosing(@NotNull Project project) {
		service().cancelAllRunningJobsForProject(project);
	}

	@Override
	public void projectClosingBeforeSave(@NotNull Project project) {
		service().cancelAllRunningJobsForProject(project);
	}

	private static JobStatusApplicationService service() {
		return ApplicationManager.getApplication().getService(JobStatusApplicationService.class);
	}
}
