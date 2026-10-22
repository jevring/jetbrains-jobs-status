package net.jevring.jetbrainsjobsstatus.rest.model;

import java.util.*;

public record Jobs(List<TestRun> testRuns, List<Build> builds) {}
