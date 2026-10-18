package net.jevring.jetbrainsjobsstatus.listeners;

import com.intellij.openapi.application.*;
import com.intellij.openapi.diagnostic.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.*;
import com.intellij.task.*;
import com.intellij.task.impl.*;
import com.intellij.util.keyFMap.*;
import net.jevring.jetbrainsjobsstatus.services.*;
import net.jevring.jetbrainsjobsstatus.services.jobs.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.atomic.*;

public class JobStatusProjectTaskListener implements ProjectTaskListener {
	private static final Logger log = Logger.getInstance(JobStatusProjectTaskListener.class);
	private static final AtomicLong ID = new AtomicLong();
	private static final String ID_DATA_KEY_NAME = "job-status-build-id";
	private static final Key<Long> ID_KEY = Key.create(ID_DATA_KEY_NAME);

	@Override
	public void started(@NotNull ProjectTaskContext context) {
		ProjectTaskScope projectTaskScope = context.getUserData(ProjectTaskScope.KEY);
		if (projectTaskScope != null) {
			// We're making some assumptions here. Once that the thing we're looking for is a ModuleBuildTask, and that there is only one task
			// per context. We're making these assumptions because 
			// a) I got this from just the debugger, so it might be wrong. Maybe there's better docs, but I haven't found it
			// b) We only get a single finished() callback, and it has the whole context. So if there were more tasks, they wouldn't get independent ids
			// These assumptions are based on observations, and might be wrong. Let's see if it turns out to be wrong in the future
			List<ModuleBuildTask> moduleBuildTasks = projectTaskScope.getRequestedTasks(ModuleBuildTask.class);
			if (!moduleBuildTasks.isEmpty()) {
				if (moduleBuildTasks.size() > 1) {
					log.info("We got more than 1 build task per project task. Found " + moduleBuildTasks.size());
				}
				Long buildId = null;

				for (ModuleBuildTask buildTask : moduleBuildTasks) {
					if (buildId != null) {
						log.info("We got more than 1 build task per project task. Skipping this one " + buildTask);
						continue; // to the next build task
					}
					Module module = buildTask.getModule();
					String projectBasePath = module.getProject().getBasePath();
					buildId = ID.getAndIncrement();
					context.putUserData(ID_KEY, buildId);
					build(buildId).started(projectBasePath);
				}
			}
		}
	}

	@Override
	public void finished(ProjectTaskManager.@NotNull Result result) {
		Long id = result.getContext().getUserData(ID_KEY);
		if (id != null) {
			build(id).complete(result.hasErrors() ? Build.State.FAILED : Build.State.COMPLETED);
		}
	}

	private static Build build(long buildId) {
		return ApplicationManager.getApplication().getService(JobStatusApplicationService.class).build(buildId);
	}

}
