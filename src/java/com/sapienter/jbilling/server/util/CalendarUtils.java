/*
 jBilling - The Enterprise Open Source Billing System
 Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

 This file is part of jbilling.

 jbilling is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 jbilling is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with jbilling.  If not, see <http://www.gnu.org/licenses/>.

 This source was modified by Web Data Technologies LLP (www.webdatatechnologies.in) since 15 Nov 2015.
 You may download the latest source from webdataconsulting.github.io.

 */

package com.sapienter.jbilling.server.util;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.process.IBillingProcessSessionBean;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;

import com.sapienter.jbilling.server.user.db.UserDTO;
import org.joda.time.*;
import org.joda.time.base.BaseSingleFieldPeriod;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created with IntelliJ IDEA.
 *
 * @author: Panche.Isajeski
 * @since: 12/06/12
 */
public class CalendarUtils {

    private static final FormatLogger LOG = new FormatLogger(CalendarUtils.class);

    public static Date findNearestTargetDateInPast(Date sourceDate, Date targetDate,
                                                   Integer nextInvoiceDaysOfPeriod,
                                                   Integer periodUnit, Integer periodValue) {

        DateTime sourceDatetime = new DateTime(sourceDate);
        DateTime targetDatetime = new DateTime(targetDate);

        Period datePeriod = getPeriodBetweenDates(sourceDatetime, targetDatetime, periodUnit, periodValue);

        LOG.debug("Past: Period between source date: %s and target date %s is %s ", sourceDatetime, targetDatetime, datePeriod);

        sourceDatetime = sourceDatetime.plus(datePeriod);

        // this would execute only once
        while (sourceDatetime.isAfter(targetDatetime)) {
            // get single period
            datePeriod = addUnitToPeriod(null, periodUnit, periodValue);
            sourceDatetime = sourceDatetime.minus(datePeriod);
        }

        // check if the source datetime is matching the nextInvoiceDaysOfPeriod for month unit
        if (periodUnit.compareTo(ServerConstants.PERIOD_UNIT_MONTH) == 0) {
            if (sourceDatetime.getDayOfMonth() < nextInvoiceDaysOfPeriod
                    && sourceDatetime.dayOfMonth().getMaximumValue() >= nextInvoiceDaysOfPeriod) {
                sourceDatetime = sourceDatetime.withDayOfMonth(nextInvoiceDaysOfPeriod);
            }
        }

        return sourceDatetime.toDate();

    }

    public static Date findNearestTargetDateInFuture(Date sourceDate, Date targetDate,
                                                     Integer nextInvoiceDaysOfPeriod,
                                                     Integer periodUnit, Integer periodValue) {

        DateTime sourceDatetime = new DateTime(sourceDate);
        DateTime targetDatetime = new DateTime(targetDate);

        Period datePeriod = getPeriodBetweenDates(sourceDatetime, targetDatetime, periodUnit, periodValue);

        LOG.debug("Future: Period between source date: %s and target date %s is %s ", sourceDatetime, targetDatetime, datePeriod);

        sourceDatetime = sourceDatetime.plus(datePeriod);

        // this would execute only once
        while (sourceDatetime.isBefore(targetDatetime)) {
            // get single period
            datePeriod = addUnitToPeriod(null, periodUnit, periodValue);
            sourceDatetime = sourceDatetime.plus(datePeriod);
        }

        // check if the source datetime is matching the nextInvoiceDaysOfPeriod for month unit
        if (periodUnit.compareTo(ServerConstants.PERIOD_UNIT_MONTH) == 0) {
            if (sourceDatetime.getDayOfMonth() < nextInvoiceDaysOfPeriod
                    && sourceDatetime.dayOfMonth().getMaximumValue() >= nextInvoiceDaysOfPeriod) {
                sourceDatetime = sourceDatetime.withDayOfMonth(nextInvoiceDaysOfPeriod);
            }
        }

        return sourceDatetime.toDate();
    }

	public static Date getNextInvoiceDateWithEOMAdjustment(Date sourceDate,
			Date targetDate, Integer nextInvoiceDaysOfPeriod,
			Integer periodUnit, Integer periodValue) {

		DateTime sourceDatetime = new DateTime(sourceDate);
		DateTime targetDatetime = new DateTime(targetDate);

		 // this would execute only once
        while (sourceDatetime.isBefore(targetDatetime) || sourceDatetime.equals(targetDatetime)) {
			Period datePeriod = addUnitToPeriod(null, periodUnit, periodValue);
			sourceDatetime = sourceDatetime.plus(datePeriod);
			LOG.debug("Future: source date: %s and target date %s is %s ",
					sourceDatetime, targetDatetime, datePeriod);
        }

		// check if the source date-time is matching the nextInvoiceDaysOfPeriod
		// for month unit
		if ( periodUnit.compareTo(ServerConstants.PERIOD_UNIT_MONTH) == 0 ) {
			if (sourceDatetime.getDayOfMonth() < nextInvoiceDaysOfPeriod) {
				if (sourceDatetime.dayOfMonth().getMaximumValue() >= nextInvoiceDaysOfPeriod) {
					sourceDatetime = sourceDatetime.withDayOfMonth(nextInvoiceDaysOfPeriod);
				} else {
					sourceDatetime = sourceDatetime.withDayOfMonth(
							sourceDatetime.dayOfMonth().getMaximumValue());
				}
			}
		}

		return sourceDatetime.toDate();
	}
    
    
    public static Period getPeriodBetweenDates(DateTime sourceDate, DateTime targetDate,
                                               Integer periodUnit, Integer periodValue) {

        BaseSingleFieldPeriod retValue;

        if (periodUnit == null) {
            throw new SessionInternalError("Can't get a period that is null");
        }
        if (periodUnit.compareTo(ServerConstants.PERIOD_UNIT_DAY) == 0) {
            retValue = Days.daysBetween(sourceDate, targetDate).dividedBy(periodValue).multipliedBy(periodValue);
        } else if (periodUnit.compareTo(ServerConstants.PERIOD_UNIT_MONTH) == 0) {
            retValue = Months.monthsBetween(sourceDate, targetDate).dividedBy(periodValue).multipliedBy(periodValue);
        } else if (periodUnit.compareTo(ServerConstants.PERIOD_UNIT_WEEK) == 0) {
            retValue = Weeks.weeksBetween(sourceDate, targetDate).dividedBy(periodValue).multipliedBy(periodValue);
        } else if (periodUnit.compareTo(ServerConstants.PERIOD_UNIT_YEAR) == 0) {
            retValue = Years.yearsBetween(sourceDate, targetDate).dividedBy(periodValue).multipliedBy(periodValue);
        } else if (periodUnit.compareTo(ServerConstants.PERIOD_UNIT_SEMI_MONTHLY) == 0) {
            retValue = Days.daysBetween(sourceDate, targetDate).dividedBy(periodValue).multipliedBy(periodValue);
        } else { // error !
            throw new SessionInternalError("Period not supported:" + periodUnit);
        }

        return retValue.toPeriod();
    }

    @Deprecated
    private static Period addUnitToPeriod(Period sourcePeriod, Integer periodUnit, Integer periodValue) {

        ReadablePeriod retValue;

        if (periodUnit == null) {
            throw new SessionInternalError("Can't add to a period that is null");
        }
        if (periodUnit.compareTo(ServerConstants.PERIOD_UNIT_DAY) == 0) {
            retValue = sourcePeriod == null ? Period.days(periodValue) :  sourcePeriod.plusDays(periodValue);
        } else if (periodUnit.compareTo(ServerConstants.PERIOD_UNIT_MONTH) == 0) {
            retValue = sourcePeriod == null ? Period.months(periodValue) :  sourcePeriod.plusMonths(periodValue);
        } else if (periodUnit.compareTo(ServerConstants.PERIOD_UNIT_WEEK) == 0) {
            retValue = sourcePeriod == null ? Period.weeks(periodValue) :  sourcePeriod.plusWeeks(periodValue);
        } else if (periodUnit.compareTo(ServerConstants.PERIOD_UNIT_YEAR) == 0) {
            retValue = sourcePeriod == null ? Period.years(periodValue) :  sourcePeriod.plusYears(periodValue);
        } else if (periodUnit.compareTo(ServerConstants.PERIOD_UNIT_SEMI_MONTHLY) == 0) {
            retValue = sourcePeriod == null ? Period.days(15) :  sourcePeriod.plusDays(15);
        } else { // error !
            throw new SessionInternalError("Period not supported:" + periodUnit);
        }

        return retValue.toPeriod();
    }
    
    /**
     * To calculate the date in case of Semi-Monthly period based on the source date
     * @param sourceDate
     * @return retValue
     */
    public static Date addSemiMonthyPeriod(Date sourceDate) {
       	
       	Date retValue = null;
       	
       	GregorianCalendar sourceCal = new GregorianCalendar();
       	sourceCal.setTime(sourceDate);
       	Integer sourceDay = sourceCal.get(Calendar.DAY_OF_MONTH);
       	
       	// The billingCycleDay should be always between 1 to 15.
    	Integer billingCycleDay = sourceCal.get(Calendar.DAY_OF_MONTH);
		if (sourceCal.getActualMaximum(Calendar.DAY_OF_MONTH) == billingCycleDay) {
			// when the source date in month end date, set billing cycle day to 15.
			billingCycleDay = new Integer(15);
			
		} else if (sourceCal.get(Calendar.DAY_OF_MONTH) > new Integer(15)) {
			// when source date is greater than 15 but not month end, 
			// then subtract 15 days to get billingCycleDay to be <= 15.
			billingCycleDay = (sourceCal.get(Calendar.DAY_OF_MONTH) - new Integer(15));
		}
       	
       	// retValue day start being equal to source value
       	Integer retValueDay = sourceCal.get(Calendar.DAY_OF_MONTH);
       	Integer sourceMonth = sourceCal.get(Calendar.MONTH);
   		
   		if (billingCycleDay <= 14 && sourceDay >= billingCycleDay) {
   			retValueDay = billingCycleDay + 15;
   			if (sourceDay >= retValueDay) {
   				retValueDay = billingCycleDay;
   				sourceCal.add(Calendar.MONTH, 1);
   			}
   			// FEBRUARY End of month condition in case of retValueDay is getter that actual maximum Day of month.
   			if (sourceMonth.equals(Calendar.FEBRUARY) && retValueDay > sourceCal.getActualMaximum(Calendar.DAY_OF_MONTH)) {
   				retValueDay = sourceCal.getActualMaximum(Calendar.DAY_OF_MONTH);
   			}
   			
   		} else if (billingCycleDay == 15 && sourceDay >= billingCycleDay) {
   				DateTime sourceDatetime = new DateTime(sourceCal.getTime());
   				sourceDatetime = sourceDatetime.withDayOfMonth(
   					sourceDatetime.dayOfMonth().getMaximumValue());
   				retValueDay = sourceDatetime.getDayOfMonth();
   			if (sourceDay == retValueDay) {
   				// Lets say today is month end date and add 15 days in nextInvoiceDay and month increment by 1
   				// then next invoice date should be 15th of next month
   				retValueDay = billingCycleDay;
   				sourceCal.add(Calendar.MONTH, 1);
   			} else if (sourceDay > billingCycleDay){
   				// source day is 30th but not month end
   				retValueDay = billingCycleDay;
   				sourceCal.add(Calendar.MONTH, 1);
   			}
   		}
   		sourceCal.set(Calendar.DAY_OF_MONTH, retValueDay);
   		
   		retValue = sourceCal.getTime();
   		
   		return retValue;
    }
    
    /**
     * Convenience method to check if the given period unit is semi-monthly or not.
     * @param periodUnit
     * @return
     */
    public static boolean isSemiMonthlyPeriod(PeriodUnitDTO periodUnit) {
    	if (null != periodUnit && ServerConstants.PERIOD_UNIT_SEMI_MONTHLY.intValue() == periodUnit.getId()) {
    		return true;
    	}
    	return false;
    }
    
    /**
     * Convenience method to check if the given period unit id is semi-monthly or not.
     * @param periodUnit
     * @return
     */
    public static boolean isSemiMonthlyPeriod(Integer periodUnitId) {
    	if (null != periodUnitId && ServerConstants.PERIOD_UNIT_SEMI_MONTHLY.intValue() == periodUnitId.intValue()) {
    		return true;
    	}
    	return false;
    }

    /**
     * Given the nextInvoiceDate and nextInvoiceDay between 1 and 15 (both inclusive),
     * this method would give the other possible next invoice day between 16 and end of month (both inclusive).
     * If the nextInvoiceDay is greater than 15, it would give the other next invoice day between 1 and 15.
     * @param nextInvoiceDate
     * @param nextInvoiceDay
     * @return
     */
    public static Integer getSemiMonthlyOtherPossibleNextInvoiceDay(Date nextInvoiceDate, Integer nextInvoiceDay) {
    	
    	GregorianCalendar nextInvoiceDateCal = new GregorianCalendar();
       	nextInvoiceDateCal.setTime(nextInvoiceDate);
       	Integer otherPossibleNextInvoiceDay = nextInvoiceDay;
       	Integer thisMonthsMaximum = nextInvoiceDateCal.getActualMaximum(Calendar.DAY_OF_MONTH);
       	
       	if (nextInvoiceDay <= 15) {
	       	otherPossibleNextInvoiceDay = nextInvoiceDay + 15;
	       	
	       	if (otherPossibleNextInvoiceDay > thisMonthsMaximum) {
	       		otherPossibleNextInvoiceDay = thisMonthsMaximum;
	       	}
	       	
	       	if (nextInvoiceDay == 15) {
	       		otherPossibleNextInvoiceDay = thisMonthsMaximum;
	       	}
       	} else {
       		if (nextInvoiceDay >= thisMonthsMaximum) {
       			otherPossibleNextInvoiceDay = 15;
       		} else {
       			otherPossibleNextInvoiceDay = nextInvoiceDay - 15;
       		}
       	}
    	return otherPossibleNextInvoiceDay;
    }
    
    
    /**
 	 * Subract semi monthly period to given date, to get Nearest expected start date in case pro-rating
 	 * @param cal
 	 * @param customerDayOfInvoice
 	 * @return
 	 */
    public static Date findNearestTargetDateInPastForSemiMonthly(Calendar cal, Integer customerDayOfInvoice) {
     	Integer nextInvoiceDay = cal.get(Calendar.DAY_OF_MONTH);
     	Integer sourceDay = cal.get(Calendar.DAY_OF_MONTH);

     	if (sourceDay < customerDayOfInvoice) {
     		nextInvoiceDay = customerDayOfInvoice + 15;
     		cal.add(Calendar.MONTH, -1);
     	} else if (customerDayOfInvoice <= 14 && sourceDay >= customerDayOfInvoice) {
     		nextInvoiceDay = customerDayOfInvoice + 15;
 	    	if (sourceDay >= nextInvoiceDay) {
 	    		// Lets say today is 30th and nextInvoiceDay is 29th after adding 15 days.
 	    		// then next invoice date should be 14th of the next month
 	    		cal.set(Calendar.DAY_OF_MONTH, nextInvoiceDay);
 	    	} else {
 	    		nextInvoiceDay = customerDayOfInvoice;
 	    	}
     	} else if (customerDayOfInvoice == 15 && sourceDay >= customerDayOfInvoice) {
     		DateTime sourceDatetime = new DateTime(cal.getTime());
     		sourceDatetime = sourceDatetime.withDayOfMonth(
     			sourceDatetime.dayOfMonth().getMaximumValue());
     		nextInvoiceDay = sourceDatetime.getDayOfMonth();

     		if (sourceDay == nextInvoiceDay) {
     			// Lets say today is 31st and nextInvoiceDay is 30 after adding 15 days
     			// then next invoice date should be 15th of next month
     			nextInvoiceDay = customerDayOfInvoice;
     		} else if (sourceDay > customerDayOfInvoice){
    				// source day is 30th but not month end
     			nextInvoiceDay = customerDayOfInvoice;
    			}
     	}
     	cal.set(Calendar.DAY_OF_MONTH, nextInvoiceDay);
     	return cal.getTime();
     }
    
}
