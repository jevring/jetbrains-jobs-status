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
import net.jevring.jetbrainsjobsstatus.services.jobs.Process;
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
	protected int getMaxRequestsPerMinute() {
		return Integer.MAX_VALUE;
	}

	@Override
	public @Nullable String execute(@NotNull QueryStringDecoder queryStringDecoder,
	                                @NotNull FullHttpRequest fullHttpRequest,
	                                @NotNull ChannelHandlerContext channelHandlerContext) {
		try {
			JobStatusApplicationService service = ApplicationManager.getApplication().getService(JobStatusApplicationService.class);
			List<String> verboseValues = queryStringDecoder.parameters().getOrDefault("verbose", Collections.emptyList());
			boolean verbose = verboseValues.equals(Collections.singletonList("true"));

			String[] pathParts = queryStringDecoder.path().substring("/api/jobs".length()).split("/");
			switch (pathParts.length) {
				// Because the path starts with a /, we end up with one extra token
				case 3 -> {
					// For example /tests/3 or /builds/3
					switch (pathParts[1]) {
						case "tests" -> {
							Optional<TestRun> testRun = service.getTestRun(Long.parseLong(pathParts[2])).map(testRun1 -> mapTestRun(testRun1, verbose));
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
							List<TestRun> testRuns = service.getTestRuns().stream().map(testRun -> mapTestRun(testRun, verbose)).toList();
							sendResponse(HttpResponseStatus.OK, fullHttpRequest, channelHandlerContext, testRuns);
							return "ok";
						}
						case "builds" -> {
							List<Build> builds = service.getBuilds().stream().map(JobsStatusRestService::mapBuild).toList();
							sendResponse(HttpResponseStatus.OK, fullHttpRequest, channelHandlerContext, builds);
							return "ok";
						}
						default -> {
							sendNotFound(fullHttpRequest, channelHandlerContext);
							return "failed";
						}
					}
				}
				case 1 -> {
					List<TestRun> testRuns = service.getTestRuns().stream().map(testRun -> mapTestRun(testRun, verbose)).toList();
					List<Build> builds = service.getBuilds().stream().map(JobsStatusRestService::mapBuild).toList();
					Jobs jobs = new Jobs(testRuns, builds);
					sendResponse(HttpResponseStatus.OK, fullHttpRequest, channelHandlerContext, jobs);
					return "ok";
				}
				default -> {
					sendNotFound(fullHttpRequest, channelHandlerContext);
					return "failed";
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
		                 build.getState(),
		                 instantToString(build.getStartedAt()),
		                 instantToString(build.getEndedAt()),
		                 build.getDuration().map(String::valueOf).orElse(null),
		                 build.idleFor().toString());
	}

	private static TestRun mapTestRun(net.jevring.jetbrainsjobsstatus.services.jobs.TestRun testRun, boolean verbose) {
		Map<String, TestStateMachine> stateMachines = testRun.getStateMachines();
		long totalTests = stateMachines.size();
		long successfulTests = 0;
		long failedTests = 0;
		long ignoredTests = 0;
		Instant lastUpdateAt = null;
		List<TestRun.Failure> failures = new ArrayList<>();
		List<TestRun.Test> tests = new ArrayList<>();
		for (Map.Entry<String, TestStateMachine> entry : stateMachines.entrySet()) {
			String nodeId = entry.getKey();
			TestStateMachine test = entry.getValue();
			switch (test.getState()) {
				case IGNORED -> ignoredTests++;
				case SUCCEEDED -> successfulTests++;
				case FAILED -> {
					failedTests++;

					failures.add(new TestRun.Failure(nodeId, test.getName(), test.getLocationUrl(), test.getMessage(), test.getStackTrace()));
				}
			}
			if (verbose) {
				tests.add(new TestRun.Test(nodeId, test.getName(), test.getLocationUrl(), test.getState()));
			}
			Instant stateMachineLastTimestamp = test.getLastUpdatedAt();
			if (lastUpdateAt == null || stateMachineLastTimestamp.isAfter(lastUpdateAt)) {
				lastUpdateAt = stateMachineLastTimestamp;
			}
		}
		TestRun.Status status;
		Instant testFinishedAt = testRun.getFinishedAt();
		if (testFinishedAt != null) {
			if (lastUpdateAt == null || testFinishedAt.isAfter(lastUpdateAt)) {
				lastUpdateAt = testFinishedAt;
			}
			if (testRun.wasTerminated()) {
				status = TestRun.Status.ABORTED;
			} else if (failedTests > 0) {
				status = TestRun.Status.COMPLETED_WITH_FAILURES;
			} else if (totalTests == 0) {
				status = TestRun.Status.COMPLETED_WITHOUT_TESTS;
			} else {
				status = TestRun.Status.COMPLETED_SUCCESSFULLY;
			}
		} else {
			if (testRun.process().getEndedAt() != null) {
				// If the test hasn't yet finished, but the process has, then the test was aborted.
				// Maybe the process never started in the first place
				// todo: handle this in idleFor etc
				status = TestRun.Status.ABORTED;
			} else {
				status = TestRun.Status.RUNNING;
			}
		}
		tests.sort(Comparator.comparing(TestRun.Test::nodeId));
		failures.sort(Comparator.comparing(TestRun.Failure::nodeId));
		return new TestRun(testRun.getExecutionId(),
		                   testRun.getName(),
		                   testRun.getProjectPath(),
		                   status,
		                   process(testRun),
		                   totalTests,
		                   successfulTests,
		                   failedTests,
		                   ignoredTests,
		                   instantToString(testRun.getStartedAt()),
		                   instantToString(lastUpdateAt),
		                   durationToString(testRun.getStartedAt(), lastUpdateAt),
		                   testRun.idleFor().toString(),
		                   verbose ? tests : null,
		                   failures);
	}

	private static net.jevring.jetbrainsjobsstatus.rest.model.Process process(net.jevring.jetbrainsjobsstatus.services.jobs.TestRun testRun) {
		Process process = testRun.process();
		return new net.jevring.jetbrainsjobsstatus.rest.model.Process(process.getPid(),
		                                                              instantToString(process.getScheduledAt()),
		                                                              instantToString(process.getStartedAt()),
		                                                              instantToString(process.getEndedAt()),
		                                                              process.getExitCode());
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
