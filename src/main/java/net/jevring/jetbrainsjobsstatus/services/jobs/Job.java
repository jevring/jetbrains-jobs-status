package net.jevring.jetbrainsjobsstatus.services.jobs;

public sealed interface Job permits TestRun, Build {}
