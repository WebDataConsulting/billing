package com.sapienter.jbilling.server.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class MainSubscriptionWS implements Serializable {

	@NotNull(message = "validation.error.is.required")
	private Integer periodId;

	@NotNull(message = "validation.error.is.required")
	@Min(value = 1, message = "validation.error.min,1")
	private Integer nextInvoiceDayOfPeriod;
	private Integer nextInvoiceDayOfPeriodOfYear;
	private String templateName;
	public static  HashMap<Integer, String> weekDaysMap = new HashMap<Integer, String>() {
		{
			put(1, "Sunday");
			put(2, "Monday");
			put(3, "Tuesday");
			put(4, "Wednesday");
			put(5, "Thursday");
			put(6, "Friday");
			put(7, "Saturday");
		}
	};
	public static  ArrayList<Integer> monthDays = new ArrayList<Integer>() {
		{
			for (int i = 1; i <= 31; i++) {
				add(i);
			}
		}
	};
	public static  HashMap<Integer, String> yearMonthsMap = new HashMap<Integer, String>() {
		{
			put(1, "January");
			put(2, "February");
			put(3, "March");
			put(4, "April");
			put(5, "May");
			put(6, "June");
			put(7, "July");
			put(8, "August");
			put(9, "September");
			put(10, "October");
			put(11, "November");
			put(12, "December");
		}
	};
	public static  ArrayList<Integer> yearMonthDays = new ArrayList<Integer>() {
		{
			for (int i = 1; i <= 31; i++) {
				add(i);
			}
		}
	};

	public static  HashMap<Integer, String> semiMonthlyDaysMap = new HashMap<Integer, String>() {
		{
			put(1, "1 & 16");
			put(2, "2 & 17");
			put(3, "3 & 18");
			put(4, "4 & 19");
			put(5, "5 & 20");
			put(6, "6 & 21");
			put(7, "7 & 22");
			put(8, "8 & 23");
			put(9, "9 & 24");
			put(10, "10 & 25");
			put(11, "11 & 26");
			put(12, "12 & 27");
			put(13, "13 & 28");
			put(14, "14 & 29");
			put(15, "15 & End of Month");
		}
	};

	public MainSubscriptionWS() {
		super();
	}

	public MainSubscriptionWS(Integer periodId, Integer nextInvoiceDayOfPeriod) {
		super();
		this.periodId = periodId;
		this.nextInvoiceDayOfPeriod = nextInvoiceDayOfPeriod;
	}

	public Integer getPeriodId() {
		return periodId;
	}

	public void setPeriodId(Integer periodId) {
		this.periodId = periodId;
	}

	public Integer getNextInvoiceDayOfPeriod() {
		return nextInvoiceDayOfPeriod;
	}

	public void setNextInvoiceDayOfPeriod(Integer nextInvoiceDayOfPeriod) {
		this.nextInvoiceDayOfPeriod = nextInvoiceDayOfPeriod;
	}

	public Integer getNextInvoiceDayOfPeriodOfYear() {
		return nextInvoiceDayOfPeriodOfYear;
	}

	public void setNextInvoiceDayOfPeriodOfYear(
			Integer nextInvoiceDayOfPeriodOfYear) {
		this.nextInvoiceDayOfPeriodOfYear = nextInvoiceDayOfPeriodOfYear;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((nextInvoiceDayOfPeriod == null) ? 0
						: nextInvoiceDayOfPeriod.hashCode());
		result = prime * result
				+ ((periodId == null) ? 0 : periodId.hashCode());
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
		MainSubscriptionWS other = (MainSubscriptionWS) obj;
		if (nextInvoiceDayOfPeriod == null) {
			if (other.nextInvoiceDayOfPeriod != null)
				return false;
		} else if (!nextInvoiceDayOfPeriod.equals(other.nextInvoiceDayOfPeriod))
			return false;
		if (periodId == null) {
			if (other.periodId != null)
				return false;
		} else if (!periodId.equals(other.periodId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "MainSubscriptionWS [periodId=" + periodId
				+ ", nextInvoiceDayOfPeriod=" + nextInvoiceDayOfPeriod + "]";
	}

	public String getTemplateName() {
		return templateName;
	}

	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}
	
	
}
