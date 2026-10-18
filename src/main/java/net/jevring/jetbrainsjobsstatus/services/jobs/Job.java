package net.jevring.jetbrainsjobsstatus.services.jobs;

import java.time.*;

public sealed interface Job permits TestRun, Build {
	public long id();
	public Duration idleFor();
	public boolean isActive();
}
