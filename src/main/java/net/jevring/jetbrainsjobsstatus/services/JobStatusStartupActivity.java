package net.jevring.jetbrainsjobsstatus.services;

import com.intellij.openapi.application.*;
import com.intellij.openapi.project.*;
import com.intellij.openapi.startup.*;
import com.intellij.util.concurrency.*;
import kotlin.*;
import kotlin.coroutines.*;
import org.jetbrains.annotations.*;

import java.util.concurrent.*;

public class JobStatusStartupActivity implements ProjectActivity {
	@Override
	public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
		JobStatusApplicationService service = ApplicationManager.getApplication().getService(JobStatusApplicationService.class);
		AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(service::cleanup, 1, 1, TimeUnit.MINUTES);
	}
}
