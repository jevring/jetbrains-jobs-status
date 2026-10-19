package net.jevring.jetbrainsjobsstatus.rest.model;

import net.jevring.jetbrainsjobsstatus.services.jobs.Build.*;

public record Build(long id, String projectPath, String startedAt, String endedAt, String duration, State state, String idleFor) {}
