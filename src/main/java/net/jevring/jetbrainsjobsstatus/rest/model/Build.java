package net.jevring.jetbrainsjobsstatus.rest.model;

import net.jevring.jetbrainsjobsstatus.services.jobs.*;

public record Build(long id, String projectPath, BuildState state, String startedAt, String endedAt, String duration, String idleFor) {}
