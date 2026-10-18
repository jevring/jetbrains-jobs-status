package net.jevring.jetbrainsjobsstatus.rest.model;

import net.jevring.jetbrainsjobsstatus.services.jobs.*;

import java.util.*;

public record Jobs(List<TestRun> testRuns, List<Build> builds) {}
