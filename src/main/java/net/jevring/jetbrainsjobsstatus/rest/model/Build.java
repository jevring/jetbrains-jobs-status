package net.jevring.jetbrainsjobsstatus.rest.model;

public record Build(long id, String projectPath, String lastUpdateAt, String idleFor) {}
