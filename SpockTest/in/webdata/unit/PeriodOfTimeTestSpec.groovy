package in.webdata.unit

import com.sapienter.jbilling.server.process.PeriodOfTime
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import spock.lang.Specification

public class PeriodOfTimeTestSpec extends Specification {

    private static final TimeZone tz = TimeZone.getTimeZone("America/Edmonton");
	 
    private static final Calendar calendar = GregorianCalendar.getInstance(tz);

    	def "testDaysInPeriodDST"()  {
        
		setup:
		calendar.clear();
        calendar.set(2009, 2, 1);  // Start Date before DST switchover (March 8th, 2009)
        Date start = calendar.getTime();
        
        calendar.clear();
        calendar.set(2009, 2, 10); // End Date after DST switchover
        Date end = calendar.getTime();

        PeriodOfTime period = new PeriodOfTime(start, end, 0, 0);
		
		expect:
         9        == period.getDaysInPeriod();
    }

		def "testDaysInPeriod31Days"() {
        
		setup:
		calendar.clear();
        calendar.set(2009, 0, 1);  // Start January 01
        Date start = calendar.getTime();

        calendar.clear();
        calendar.set(2009, 0, 31); // End January 31 
        Date end = calendar.getTime();

        PeriodOfTime period = new PeriodOfTime(start, end, 0, 0);
		
		expect:
        30           == period.getDaysInPeriod();
    }

    	def "testDaysInPeriod30Days"() {
        
		setup:
		calendar.clear();
        calendar.set(2009, 3, 1);  // Start April 01
        Date start = calendar.getTime();

        calendar.clear();
        calendar.set(2009, 3, 30); // End April 30 
        Date end = calendar.getTime();

        PeriodOfTime period = new PeriodOfTime(start, end, 0, 0);
  
		expect:
		 29               == period.getDaysInPeriod();
    }

    	def "testDaysInPeriodMidMonthDays"() {
        calendar.clear();
        calendar.set(2009, 0, 5);  // Start January 05
        Date start = calendar.getTime();

        calendar.clear();
        calendar.set(2009, 0, 14); // End January 14
        Date end = calendar.getTime();


        PeriodOfTime period = new PeriodOfTime(start, end, 0, 0);
        
		expect
		  9                  == period.getDaysInPeriod();
    }

		def "testDaysInPeriodSpanMonths"() {
        
		setup:
		calendar.clear();
        calendar.set(2009, 0, 1);  // Start January 01
        Date start = calendar.getTime();

        calendar.clear();
        calendar.set(2009, 1, 14); // End February 14
        Date end = calendar.getTime();

        // 31 days + 13 days = 44 days
        PeriodOfTime period = new PeriodOfTime(start, end, 0, 0);
        
		expect:
		44           == period.getDaysInPeriod();
    }


    	  def "testDaysInPeriodAddMonths"() {
	
		  setup:
        calendar.clear();
        calendar.set(2009, 0, 1);        // Start January 01
        Date start = calendar.getTime();

        calendar.add(Calendar.MONTH, 1); // End February 01
        Date end = calendar.getTime();

        PeriodOfTime period = new PeriodOfTime(start, end, 0, 0);
  		
		  expect:
		  31          == period.getDaysInPeriod();
    }

		def "testDaysInPeriodEndBeforeStart"()  {
        
		setup:
		calendar.clear();
        calendar.set(2009, 1, 1); // Start February 01
        Date start = calendar.getTime();

        calendar.clear();
        calendar.set(2009, 0, 1); // End January 01
        Date end = calendar.getTime();

        // Start date occurs before end date, default to 0 days (not a negative value!)
        PeriodOfTime period = new PeriodOfTime(start, end, 0, 0);
		
		expect:
		
        0             == period.getDaysInPeriod();
    }
}
 