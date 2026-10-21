package net.jevring.jetbrainsjobsstatus.rest.model;

import net.jevring.jetbrainsjobsstatus.services.jobs.*;

public record Build(long id, String projectPath, String startedAt, String endedAt, String duration, BuildState state, String idleFor) {}
