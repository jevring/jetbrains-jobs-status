package net.jevring.jetbrainsjobsstatus.listeners;

import com.intellij.execution.*;
import com.intellij.execution.configurations.*;
import com.intellij.execution.executors.*;
import com.intellij.execution.impl.*;
import com.intellij.execution.runners.*;
import com.intellij.execution.testframework.sm.runner.*;
import com.intellij.execution.testframework.sm.runner.ui.*;
import com.intellij.openapi.application.*;
import com.intellij.openapi.project.*;
import com.intellij.testFramework.fixtures.*;
import com.intellij.util.messages.*;
import net.jevring.jetbrainsjobsstatus.services.*;
import net.jevring.jetbrainsjobsstatus.services.jobs.*;
import org.jetbrains.annotations.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;
import org.mockito.*;

import java.util.*;

@RunWith(JUnit4.class)
public class JobStatusExecutionListenerTest extends BasePlatformTestCase {

	@Test
	public void shouldRegisterStartOfTestExecution() {
		RunProfile jUnitRunProfiler = Mockito.mock(RunProfile.class, Mockito.withSettings().extraInterfaces(SMRunnerConsolePropertiesProvider.class));
		Project project = getProject();
		long executionId = 22;
		ExecutionEnvironment env = Mockito.mock();
		Mockito.when(env.getExecutionId()).thenReturn(executionId);
		Mockito.when(env.getRunProfile()).thenReturn(jUnitRunProfiler);
		Mockito.when(env.getProject()).thenReturn(project);
		Mockito.when(jUnitRunProfiler.getName()).thenReturn("my name");

		// It would be nicer if we could trigger something *else* and then see that our listener got invoked correctly.
		// That would also test that we had wired everything up correctly, but there doesn't seem to be a way to do that, 
		// so calling our listener directly is the best we've got.
		project.getMessageBus().syncPublisher(ExecutionManager.EXECUTION_TOPIC).processStarting("Run", env);
		JobStatusApplicationService service = ApplicationManager.getApplication().getService(JobStatusApplicationService.class);
		Optional<TestRun> testRun = service.getTestRun(executionId);
		assertTrue(testRun.isPresent());
		assertEquals("my name", testRun.get().getName());
		assertEquals(project.getBasePath(), testRun.get().getProjectPath());
	}

	@Test
	public void shouldNotRegisterStartOfTestExecutionWhenExecutionIsNotATest() {
		RunProfile normalRunProfiler = Mockito.mock(RunProfile.class);
		Project project = getProject();
		long executionId = 23;
		ExecutionEnvironment env = Mockito.mock();
		Mockito.when(env.getExecutionId()).thenReturn(executionId);
		Mockito.when(env.getRunProfile()).thenReturn(normalRunProfiler);
		Mockito.when(env.getProject()).thenReturn(project);
		Mockito.when(normalRunProfiler.getName()).thenReturn("my name");

		// It would be nicer if we could trigger something *else* and then see that our listener got invoked correctly.
		// That would also test that we had wired everything up correctly, but there doesn't seem to be a way to do that, 
		// so calling our listener directly is the best we've got.
		project.getMessageBus().syncPublisher(ExecutionManager.EXECUTION_TOPIC).processStarting("Run", env);
		JobStatusApplicationService service = ApplicationManager.getApplication().getService(JobStatusApplicationService.class);
		Optional<TestRun> testRun = service.getTestRun(executionId);
		// As this is trivially true if everything is broken, only run *all* the tests in this class together, so that shouldRegisterStartOfTestExecution()
		// shows that everything is correctly wired up, *and* that this test, then, shows that we're calling the thing, but we're not registering the run
		assertFalse(testRun.isPresent());
	}

}