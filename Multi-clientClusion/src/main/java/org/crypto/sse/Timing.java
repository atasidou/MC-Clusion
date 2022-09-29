package org.crypto.sse;

import java.time.Duration;
import java.time.Instant;

public final class Timing {
	public interface FailableSupplier <T>{
	    /**
	     * Gets a result.
	     *
	     * @return a result
	     */
	    T get() throws Exception;
	}
	public static <T> T time(int minTimeInMs, int minInitialReps, int minRestReps, String name, FailableSupplier<T> func) throws Exception {
		final long deadline = System.currentTimeMillis() + minTimeInMs;

		T result = null;
		int rep;
		int totalReps = 0;

		Instant start = Instant.now();
		do {
			for(rep=0; rep < minInitialReps; rep++)
				result = func.get();
			totalReps += minInitialReps;
		    if (deadline - System.currentTimeMillis() > 0) // more time needed
		    	minInitialReps = minRestReps;
		    else
		    	break;
		}while(true);
		Instant end = Instant.now();
		
		System.out.println(name+": " + Duration.between(start, end).toNanos() + "ns in "+totalReps+" iterations. Per iteration: "+Duration.between(start, end).toNanos()/totalReps+"ns");
		return result;
	}
}
