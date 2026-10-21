package net.jevring.jetbrainsjobsstatus.listeners;

import com.intellij.execution.process.*;
import org.jetbrains.annotations.*;

import java.util.concurrent.*;

public class Utils {
	static @Nullable Long pidOf(ProcessHandler handler) {
		CompletableFuture<@Nullable Long> pidFuture = handler.getNativePid();
		if (pidFuture != null) {
			try {
				// This is a CompletableFuture.completedFuture() whenever it isn't null.
				// Not sure why it isn't just a long, but let's make some assumptions about 
				// the implementation here
				return pidFuture.get();
			} catch (InterruptedException | ExecutionException e) {
				return null;
			}
		} else {
			return null;
		}
	}

}
