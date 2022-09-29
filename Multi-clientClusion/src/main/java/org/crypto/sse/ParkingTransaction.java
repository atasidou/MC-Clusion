package org.crypto.sse;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import org.joda.time.DateTime;
import org.joda.time.Duration;

public class ParkingTransaction {
	public static String vehicleClassificationPrefix = "vehicleClassification";
	public static String vehicleTypePrefix = "vehicleType";
	public static String parkingSpotTypePrefix = "parkingSpotType";
	public static String servicesRequiredPrefix = "servicesRequired";
	public static String userAffiliationPrefix = "userAffiliation";
	public static String parkingStartPrefix = "!parkingStart";
	public static String parkingStartYearPrefix = "parkingStartYear";
	public static String parkingStartMonthPrefix = "parkingStartMonth";
	public static String parkingStartDayPrefix = "parkingStartDay";
	public static String parkingStartWeekDayPrefix = "parkingStartWeekDay";
	public static String parkingStartHourPrefix = "parkingStartHour";
	public static String parkingEndPrefix = "!parkingEnd";
	public static String parkingEndYearPrefix = "parkingEndYear";
	public static String parkingEndMonthPrefix = "parkingEndMonth";
	public static String parkingEndDayPrefix = "parkingEndDay";
	public static String parkingEndWeekDayPrefix = "parkingEndWeekDay";
	public static String parkingEndHourPrefix = "parkingEndHour";
	public static String parkingDurationInMinsPrefix = "parkingDurationInMins";
	public static String parkingDurationIn10MinsPrefix = "parkingDurationIn10Mins";
	public static String parkingDurationInHoursPrefix = "parkingDurationInHours";
	public static String parkingCostInCentsPrefix = "parkingCostInCents";
	public static String parkingCostIn1EURPrefix = "parkingCostIn1EUR";
	public static String parkingCostIn10EURPrefix = "parkingCostIn10EUR";
	public static String parkingCostIn100EURPrefix = "parkingCostIn100EUR";
	
	String vehicleClassification;
	String vehicleType;
	String parkingSpotType;
	String[] servicesRequired;
	String userAffiliation;
	DateTime parkingStart;
	DateTime parkingEnd;
	int parkingCostInCents;
	
	public void writeTransaction(File datasetDirectory, int i) {
		try {
			  File myObj = new File(datasetDirectory.getPath()+datasetDirectory.separator+String.valueOf(i)+".txt");
		      FileWriter myWriter = new FileWriter(myObj);
		      
		      myWriter.write(vehicleClassificationPrefix+vehicleClassification+"\n");
		      myWriter.write(vehicleTypePrefix+vehicleType+"\n");
		      myWriter.write(parkingSpotTypePrefix+parkingSpotType+"\n");
		      myWriter.write(servicesRequiredPrefix+servicesRequired[0]+"\n");
		      myWriter.write(userAffiliationPrefix+userAffiliation+"\n");

		      myWriter.write(parkingStartPrefix+parkingStart+"\n");
		      myWriter.write(parkingStartYearPrefix+parkingStart.getYear()+"\n");
		      myWriter.write(parkingStartMonthPrefix+parkingStart.getMonthOfYear()+"\n");
		      myWriter.write(parkingStartDayPrefix+parkingStart.getDayOfMonth()+"\n");
		      myWriter.write(parkingStartWeekDayPrefix+parkingStart.getDayOfWeek()+"\n");
		      myWriter.write(parkingStartHourPrefix+parkingStart.getHourOfDay()+"\n");
		      
		      
		      myWriter.write(parkingEndPrefix+parkingEnd+"\n");
		      myWriter.write(parkingEndYearPrefix+parkingEnd.getYear()+"\n");
		      myWriter.write(parkingEndMonthPrefix+parkingEnd.getMonthOfYear()+"\n");
		      myWriter.write(parkingEndDayPrefix+parkingEnd.getDayOfMonth()+"\n");
		      myWriter.write(parkingEndWeekDayPrefix+parkingEnd.getDayOfWeek()+"\n");
		      myWriter.write(parkingEndHourPrefix+parkingEnd.getHourOfDay()+"\n");

              int parkingDurationInMins = (int) new Duration(parkingStart, parkingEnd).getStandardMinutes();
              int parkingDurationIn10Mins = (int) new Duration(parkingStart, parkingEnd).getStandardMinutes()/10;
              int parkingDurationInHours = (int) new Duration(parkingStart, parkingEnd).getStandardHours();
		      
		      myWriter.write(parkingDurationIn10MinsPrefix+parkingDurationIn10Mins+"\n");
		      myWriter.write(parkingDurationInHoursPrefix+parkingDurationInHours+"\n");
		      myWriter.write(parkingCostInCentsPrefix+parkingCostInCents+"\n");
		      myWriter.write(parkingCostIn1EURPrefix+parkingCostInCents/100+"\n");
		      myWriter.write(parkingCostIn10EURPrefix+parkingCostInCents/1000+"\n");
		      myWriter.write(parkingCostIn100EURPrefix+parkingCostInCents/10000+"\n");
		      
		      myWriter.close();
		    } catch (IOException e) {
		      System.out.println("An error occurred.");
		      e.printStackTrace();
		    }
	}
	
	
	public ParkingTransaction(String vehicleClassification, String vehicleType, String parkingSpotType,
			String[] servicesRequired, String userAffiliation, DateTime parkingStart, DateTime parkingEnd,
			int parkingCostInCents) {
		super();
		this.vehicleClassification = vehicleClassification;
		this.vehicleType = vehicleType;
		this.parkingSpotType = parkingSpotType;
		this.servicesRequired = servicesRequired;
		this.userAffiliation = userAffiliation;
		this.parkingStart = parkingStart;
		this.parkingEnd = parkingEnd;
		this.parkingCostInCents = parkingCostInCents;
	}



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + parkingCostInCents;
		result = prime * result + ((parkingEnd == null) ? 0 : parkingEnd.hashCode());
		result = prime * result + ((parkingSpotType == null) ? 0 : parkingSpotType.hashCode());
		result = prime * result + ((parkingStart == null) ? 0 : parkingStart.hashCode());
		result = prime * result + Arrays.hashCode(servicesRequired);
		result = prime * result + ((userAffiliation == null) ? 0 : userAffiliation.hashCode());
		result = prime * result + ((vehicleClassification == null) ? 0 : vehicleClassification.hashCode());
		result = prime * result + ((vehicleType == null) ? 0 : vehicleType.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ParkingTransaction other = (ParkingTransaction) obj;
		if (parkingCostInCents != other.parkingCostInCents)
			return false;
		if (parkingEnd == null) {
			if (other.parkingEnd != null)
				return false;
		} else if (!parkingEnd.equals(other.parkingEnd))
			return false;
		if (parkingSpotType == null) {
			if (other.parkingSpotType != null)
				return false;
		} else if (!parkingSpotType.equals(other.parkingSpotType))
			return false;
		if (parkingStart == null) {
			if (other.parkingStart != null)
				return false;
		} else if (!parkingStart.equals(other.parkingStart))
			return false;
		if (!Arrays.equals(servicesRequired, other.servicesRequired))
			return false;
		if (userAffiliation == null) {
			if (other.userAffiliation != null)
				return false;
		} else if (!userAffiliation.equals(other.userAffiliation))
			return false;
		if (vehicleClassification == null) {
			if (other.vehicleClassification != null)
				return false;
		} else if (!vehicleClassification.equals(other.vehicleClassification))
			return false;
		if (vehicleType == null) {
			if (other.vehicleType != null)
				return false;
		} else if (!vehicleType.equals(other.vehicleType))
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "ParkingTransaction [vehicleClassification=" + vehicleClassification + ", vehicleType=" + vehicleType
				+ ", parkingSpotType=" + parkingSpotType + ", servicesRequired=" + Arrays.toString(servicesRequired)
				+ ", userAffiliation=" + userAffiliation + ", parkingStart=" + parkingStart + ", parkingEnd="
				+ parkingEnd + ", parkingCostInCents=" + parkingCostInCents + "]";
	}
}
