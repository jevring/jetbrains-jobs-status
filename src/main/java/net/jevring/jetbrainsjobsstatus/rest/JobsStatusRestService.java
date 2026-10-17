package net.jevring.jetbrainsjobsstatus.rest;

import com.intellij.openapi.application.*;
import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import net.jevring.jetbrainsjobsstatus.rest.model.*;
import net.jevring.jetbrainsjobsstatus.rest.model.TestRun;
import net.jevring.jetbrainsjobsstatus.services.*;
import net.jevring.jetbrainsjobsstatus.services.jobs.*;
import org.jetbrains.annotations.*;
import org.jetbrains.ide.*;

import java.nio.charset.*;
import java.time.*;
import java.time.format.*;
import java.time.temporal.*;
import java.util.*;

/**
 * By default, the port is 63342, but if there's already something running on that, the next one will be chosen, etc.
 * It would be nice if this could be pinned, and maybe it can me. Might be worth checking out.
 *
 * <p>This endpoint can be found at <a href="http://127.0.0.1:63342/api/job-status">http://127.0.0.1:63342/api/job-status</a> (depending on port, as per above)</p>
 */
public class JobsStatusRestService extends RestService {

	@Override
	protected @NotNull String getServiceName() {
		// This is the endpoint name
		return "job-status";
	}

	@Override
	public @Nullable String execute(@NotNull QueryStringDecoder queryStringDecoder,
	                                @NotNull FullHttpRequest fullHttpRequest,
	                                @NotNull ChannelHandlerContext channelHandlerContext) {

		JobStatusApplicationService service = ApplicationManager.getApplication().getService(JobStatusApplicationService.class);
		
		List<TestRun> testRuns = service.getTestRuns().stream().map(testRun -> {
			Map<String, TestStateMachine> stateMachines = testRun.getStateMachines();
			long totalTests = stateMachines.size();
			long successfulTests = 0;
			long failedTests = 0;
			long ignoredTests = 0;
			Instant lastUpdateAt = Instant.MIN;
			List<TestRun.Failure> failures = new ArrayList<>();
			// todo: it might be a bit iffy to expose the state machines like this. Maybe we should have a TestRunReport record or something that it can produce.
			for (Map.Entry<String, TestStateMachine> entry : stateMachines.entrySet()) {
				TestStateMachine stateMachine = entry.getValue();
				switch (stateMachine.getState()) {
					case IGNORED -> ignoredTests++;
					case SUCCEEDED -> successfulTests++;
					case FAILED -> {
						failedTests++;
						String nodeId = entry.getKey();

						failures.add(new TestRun.Failure(nodeId, stateMachine.getError(), stateMachine.getStackTrace()));
					}
				}
				Instant stateMachineLastTimestamp = stateMachine.getLastTimestamp();
				if (stateMachineLastTimestamp.isAfter(lastUpdateAt)) {
					lastUpdateAt = stateMachineLastTimestamp;
				}
			}
			TestRun.Status status;
			Instant runEndedAt = testRun.getEndedAt();
			if (runEndedAt != null) {
				if (runEndedAt.isAfter(lastUpdateAt)) {
					lastUpdateAt = runEndedAt;
				}
				if (totalTests > successfulTests + failedTests + ignoredTests) {
					status = TestRun.Status.ABORTED;
				} else if (failedTests > 0) {
					status = TestRun.Status.COMPLETED_WITH_FAILURES;
				} else {
					status = TestRun.Status.COMPLETED_SUCCESSFULLY;
				}
			} else {
				status = TestRun.Status.RUNNING;
			}
			return new TestRun(testRun.getExecutionId(),
			                   totalTests,
			                   successfulTests,
			                   failedTests,
			                   ignoredTests,
			                   DateTimeFormatter.ISO_ZONED_DATE_TIME.format(lastUpdateAt.atZone(ZoneId.of("Z")).truncatedTo(ChronoUnit.SECONDS)),
			                   status,
			                   failures);
		}).toList();
		Jobs jobs = new Jobs(testRuns);
		// todo: clean up test runs older than 30 minutes if there is more than 1
		String responseBody = getGson().toJson(jobs);
		byte[] responseBodyBytes = responseBody.getBytes(StandardCharsets.UTF_8);
		DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(responseBodyBytes));
		response.headers().add(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8");
		response.headers().add(HttpHeaderNames.CONTENT_LENGTH, responseBodyBytes.length);
		sendResponse(fullHttpRequest, channelHandlerContext, response);
		return null;
	}
}
