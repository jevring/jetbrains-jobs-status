package net.jevring.jetbrainsjobsstatus.rest;

import com.intellij.openapi.application.*;
import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import net.jevring.jetbrainsjobsstatus.rest.model.*;
import net.jevring.jetbrainsjobsstatus.rest.model.Build;
import net.jevring.jetbrainsjobsstatus.rest.model.TestRun;
import net.jevring.jetbrainsjobsstatus.services.*;
import net.jevring.jetbrainsjobsstatus.services.jobs.*;
import org.jetbrains.annotations.*;
import org.jetbrains.ide.*;
import org.slf4j.*;

import java.nio.charset.*;
import java.time.*;
import java.time.format.*;
import java.time.temporal.*;
import java.util.*;

/**
 * By default, the port is 63342, but if there's already something running on that, the next one will be chosen, etc.
 * You can pin this port with {@code -Drpc.port=<port>}, in which case it probably doesn't hunt for ports. I don't know what happens if this port is taken.
 *
 * <p>This endpoint can be found at <a href="http://127.0.0.1:63342/api/jobs">http://127.0.0.1:63342/api/jobs</a> (depending on port, as per above)</p>
 */
public class JobsStatusRestService extends RestService {

	private static final Logger log = LoggerFactory.getLogger(JobsStatusRestService.class);

	@Override
	protected @NotNull String getServiceName() {
		// This is the endpoint name
		return "jobs";
	}

	@Override
	public @Nullable String execute(@NotNull QueryStringDecoder queryStringDecoder,
	                                @NotNull FullHttpRequest fullHttpRequest,
	                                @NotNull ChannelHandlerContext channelHandlerContext) {
		try {
			JobStatusApplicationService service = ApplicationManager.getApplication().getService(JobStatusApplicationService.class);

			String[] pathParts = queryStringDecoder.path().substring("/api/jobs".length()).split("/");
			switch (pathParts.length) {
				// Because the path starts with a /, we end up with one extra token
				case 3 -> {
					// For example /tests/3 or /builds/3
					switch (pathParts[1]) {
						case "tests" -> {
							Optional<TestRun> testRun = service.getTestRun(Long.parseLong(pathParts[2])).map(JobsStatusRestService::mapTestRun);
							if (testRun.isPresent()) {
								sendResponse(HttpResponseStatus.OK, fullHttpRequest, channelHandlerContext, testRun.get());
								return "ok";
							} else {
								sendNotFound(fullHttpRequest, channelHandlerContext);
								return "failed";
							}
						}
						case "builds" -> {
							Optional<Build> build = service.getBuild(Long.parseLong(pathParts[2])).map(JobsStatusRestService::mapBuild);
							if (build.isPresent()) {
								sendResponse(HttpResponseStatus.OK, fullHttpRequest, channelHandlerContext, build.get());
								return "ok";
							} else {
								sendNotFound(fullHttpRequest, channelHandlerContext);
								return "failed";
							}
						}
						default -> {
							sendNotFound(fullHttpRequest, channelHandlerContext);
							return "failed";
						}
					}
				}
				case 2 -> {
					// For example /tests or /builds
					switch (pathParts[1]) {
						case "tests" -> {
							List<TestRun> testRuns = service.getTestRuns().stream().map(JobsStatusRestService::mapTestRun).toList();
							sendResponse(HttpResponseStatus.OK, fullHttpRequest, channelHandlerContext, testRuns);
							return null;
						}
						case "builds" -> {
							List<Build> builds = service.getBuilds().stream().map(JobsStatusRestService::mapBuild).toList();
							sendResponse(HttpResponseStatus.OK, fullHttpRequest, channelHandlerContext, builds);
							return null;
						}
						default -> {
							sendNotFound(fullHttpRequest, channelHandlerContext);
							return "failed";
						}
					}
				}
				default -> {
					List<TestRun> testRuns = service.getTestRuns().stream().map(JobsStatusRestService::mapTestRun).toList();
					List<Build> builds = service.getBuilds().stream().map(JobsStatusRestService::mapBuild).toList();
					Jobs jobs = new Jobs(testRuns, builds);
					sendResponse(HttpResponseStatus.OK, fullHttpRequest, channelHandlerContext, jobs);
					return "ok";
				}
			}
		} catch (NumberFormatException e) {
			log.warn("Could not parse input", e);
			sendResponse(HttpResponseStatus.BAD_REQUEST, fullHttpRequest, channelHandlerContext, Collections.singletonMap("error", e.getMessage()));
			return "failure";
		} catch (Exception e) {
			log.error("Could not handle request", e);
			sendResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR, fullHttpRequest, channelHandlerContext, Collections.singletonMap("error", e.getMessage()));
			return "failure";
		}
	}

	private void sendNotFound(@NotNull FullHttpRequest fullHttpRequest, @NotNull ChannelHandlerContext channelHandlerContext) {
		sendResponse(HttpResponseStatus.NOT_FOUND, fullHttpRequest, channelHandlerContext, Collections.singletonMap("status", "not found"));
	}

	private void sendResponse(HttpResponseStatus status, FullHttpRequest fullHttpRequest, ChannelHandlerContext channelHandlerContext, Object responseBody) {
		String responseBodyJson = getGson().toJson(responseBody);
		byte[] responseBodyBytes = responseBodyJson.getBytes(StandardCharsets.UTF_8);
		DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.wrappedBuffer(responseBodyBytes));
		response.headers().add(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8");
		response.headers().add(HttpHeaderNames.CONTENT_LENGTH, responseBodyBytes.length);
		sendResponse(fullHttpRequest, channelHandlerContext, response);
	}

	private static Build mapBuild(net.jevring.jetbrainsjobsstatus.services.jobs.Build build) {
		return new Build(build.getId(),
		                 build.getProjectPath(),
		                 instantToString(build.getStartedAt()),
		                 instantToString(build.getEndedAt()),
		                 build.getDuration().map(String::valueOf).orElse(null),
		                 build.getState(),
		                 build.idleFor().toString());
	}

	private static TestRun mapTestRun(net.jevring.jetbrainsjobsstatus.services.jobs.TestRun testRun) {
		Map<String, TestStateMachine> stateMachines = testRun.getStateMachines();
		long totalTests = stateMachines.size();
		long successfulTests = 0;
		long failedTests = 0;
		long ignoredTests = 0;
		Instant lastUpdateAt = null;
		List<TestRun.Failure> failures = new ArrayList<>();
		for (Map.Entry<String, TestStateMachine> entry : stateMachines.entrySet()) {
			TestStateMachine test = entry.getValue();
			switch (test.getState()) {
				case IGNORED -> ignoredTests++;
				case SUCCEEDED -> successfulTests++;
				case FAILED -> {
					failedTests++;
					String nodeId = entry.getKey();

					failures.add(new TestRun.Failure(nodeId, test.getName(), test.getMessage(), test.getStackTrace()));
				}
			}
			Instant stateMachineLastTimestamp = test.getLastUpdatedAt();
			if (lastUpdateAt == null || stateMachineLastTimestamp.isAfter(lastUpdateAt)) {
				lastUpdateAt = stateMachineLastTimestamp;
			}
		}
		TestRun.Status status;
		Instant runEndedAt = testRun.getEndedAt();
		if (runEndedAt != null) {
			if (lastUpdateAt == null || runEndedAt.isAfter(lastUpdateAt)) {
				lastUpdateAt = runEndedAt;
			}
			status = switch (testRun.getTermination()) {
				case NOT_STARTED, TERMINATED -> TestRun.Status.ABORTED;
				// You can't combine null with another value here, so we have to have two branches here
				case NORMAL -> calculateFromTests(totalTests, successfulTests, failedTests, ignoredTests);
				case null -> calculateFromTests(totalTests, successfulTests, failedTests, ignoredTests);
			};

		} else {
			status = TestRun.Status.RUNNING;
		}
		return new TestRun(testRun.getExecutionId(),
		                   testRun.getName(),
		                   testRun.getProjectPath(),
		                   totalTests,
		                   successfulTests,
		                   failedTests,
		                   ignoredTests,
		                   instantToString(testRun.getScheduledAt()),
		                   instantToString(testRun.getStartedAt()),
		                   instantToString(lastUpdateAt),
		                   durationToString(testRun.getStartedAt(), lastUpdateAt),
		                   status,
		                   testRun.idleFor().toString(),
		                   testRun.getPid(),
		                   testRun.getExitCode(),
		                   failures);
	}

	private static TestRun.Status calculateFromTests(long totalTests, long successfulTests, long failedTests, long ignoredTests) {
		if (totalTests > successfulTests + failedTests + ignoredTests) {
			// This would indicate that the test ended in the middle of things, for some reason. 
			// Or, in theory, that some test didn't classify as successful, failed, or ignored.
			// Timed out, perhaps. But then that would either be reported as failed or the process 
			// would have been terminated, which would mean that we're take the branch above. 
			// Keep this still
			log.warn("Had more total tests than finished tests. Total: {}. Successful: {}. Failed: {}. Ignored: {}",
			         totalTests,
			         successfulTests,
			         failedTests,
			         ignoredTests);
			return TestRun.Status.ABORTED;
		} else if (failedTests > 0) {
			return TestRun.Status.COMPLETED_WITH_FAILURES;
		} else {
			return TestRun.Status.COMPLETED_SUCCESSFULLY;
		}
	}

	private static String durationToString(Instant startedAt, Instant lastUpdateAt) {
		if (startedAt == null || lastUpdateAt == null) {
			return null;
		}
		return Duration.between(startedAt, lastUpdateAt).toString();
	}

	private static String instantToString(Instant instant) {
		if (instant == null) {
			return null;
		}
		return DateTimeFormatter.ISO_ZONED_DATE_TIME.format(instant.atZone(ZoneId.of("Z")).truncatedTo(ChronoUnit.SECONDS));
	}
}
