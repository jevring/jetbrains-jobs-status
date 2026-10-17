package net.jevring.jetbrainsjobsstatus.startup;

import com.intellij.openapi.diagnostic.*;
import com.intellij.openapi.project.*;
import com.intellij.openapi.startup.*;
import kotlin.*;
import kotlin.coroutines.*;
import org.jetbrains.annotations.*;

public class JobStatusStartup implements ProjectActivity {
	private static final Logger LOG = Logger.getInstance(JobStatusStartup.class);
	
	@Override
	public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
		// todo: this is called PER PROJECT. This needs to register the project with some kind of central thing that owns the rest service, which is for everything
		// This has to be referenced in plugin.xml
		// todo: start the REST server on a known port. Read this port from config somewhere, eventually
		LOG.info("XXX The startup just executed automatically");
		return "job status execute response";
	}
}
