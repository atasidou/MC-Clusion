/** * Copyright (C) 2021 Aimilia Tasidou
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

//***********************************************************************************************//

// This file contains the step-by-step local benchmarking of the IEX-2Lev. The encrypted data structure remains in the RAM.
// This file also gathers stats useful to give some insights about the scheme implementation
// One needs to wait until the complete creation of the encrypted data structures of IEX-2Lev in order to issue queries.
// Queries need to be in the form of CNF. Follow on-line instructions
//***********************************************************************************************//
package org.crypto.sse;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.joda.time.DateTime;
import org.joda.time.Duration;


public class TestDataGen {

	public static void main(String[] args) throws Exception {

		Printer.addPrinter(new Printer(Printer.LEVEL.EXTRA));
		
		if(args.length != 2) {
			System.out.println("Syntax: program <docs-count> <random-seed>");
			System.out.println("Example: TestDataGen 10000 1");
			System.exit(-1);
		}
		int docsCount = Integer.parseInt(args[0]);
		int randomSeed= Integer.parseInt(args[1]);
		
		
		Random rng = new Random(randomSeed);
		int totalGeneratedTransactions = docsCount;
		int parkingPriceInCentsPerMinute = 2;
		File datasetDirectory = new File("gen-data-"+docsCount+"-"+randomSeed);
		Path path = datasetDirectory.toPath();
		Files.createDirectories(path);
		List<String> vehicleClassifications = Arrays.asList("L1", "L2", "L3", "L4", "M1", "M2", "M3", "N1", "N2", "N3", "O1", "O2", "O3", "O4");		
		List<String> vehicleTypes = Arrays.asList("Gas", "Electric", "GPL");
		List<String> parkingSpotTypes = Arrays.asList("disability", "premium", "shortduration", "longduration", "secure");
		List<String> serviceTypes = Arrays.asList("", "washing", "charging");
		List<String> userAffiliations = Arrays.asList("", "IBM", "Google", "Amazon");
		for(int i=0; i<totalGeneratedTransactions; i++) {

			String vehicleClassification = vehicleClassifications.get(rng.nextInt(vehicleClassifications.size())); 	
			String vehicleType  = vehicleTypes.get(rng.nextInt(vehicleTypes.size()));
			String parkingSpotType = parkingSpotTypes.get(rng.nextInt(parkingSpotTypes.size()));
			String[] servicesRequired = new String[] {serviceTypes.get(rng.nextInt(serviceTypes.size()))};
			String userAffiliation = userAffiliations.get(rng.nextInt(userAffiliations.size()));
			
			DateTime parkingStart = between(rng, DateTime.parse("2020-01-01"), DateTime.parse("2021-04-01"));
		    DateTime parkingEnd = parkingStart.plusMinutes(1 + rng.nextInt(60*24*5));
		    int parkingDurationInMins = (int) new Duration(parkingStart, parkingEnd).getStandardMinutes();
		    
			int parkingCostInCents = parkingDurationInMins*parkingPriceInCentsPerMinute;
			ParkingTransaction parkingTransaction = new ParkingTransaction(vehicleClassification, vehicleType, parkingSpotType, servicesRequired, userAffiliation, parkingStart, parkingEnd, parkingCostInCents);
			
			parkingTransaction.writeTransaction(datasetDirectory, i);			
		}

	}

	public static DateTime between(Random rng, DateTime startInclusive, DateTime endExclusive) {
	    long startMillis = startInclusive.getMillis();
	    long endMillis = endExclusive.getMillis();
	    long randomMillisSinceEpoch = startMillis + rng.nextLong()%(endMillis - startMillis);

	    return new DateTime(randomMillisSinceEpoch);
	}	
	
}