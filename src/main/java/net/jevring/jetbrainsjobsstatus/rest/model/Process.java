package net.jevring.jetbrainsjobsstatus.rest.model;

public record Process(Long pid, String scheduledAt, String startedAt, String endedAt, Integer exitCode) {
}
