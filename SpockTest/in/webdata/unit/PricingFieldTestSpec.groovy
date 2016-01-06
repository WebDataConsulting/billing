package in.webdata.unit

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.PricingField.Type;

import spock.lang.Shared
import spock.lang.Specification

public class PricingFieldTestSpec extends Specification {

    @Shared private Date DATE_VALUE;
    @Shared private String DATE_VALUE_STRING;
	@Shared Calendar calendar;
	
  
    	def setup() {

	    calendar = GregorianCalendar.getInstance();
        
		calendar.clear();
        
		calendar.set(2009, 11, 16);

        DATE_VALUE = calendar.getTime();
        
		DATE_VALUE_STRING = String.valueOf(DATE_VALUE.getTime());
				       
    }

		def "testGetValue"() {
        
		setup:
		
		PricingField string = new PricingField("str field", "Some String");        

        PricingField date = new PricingField("date field", DATE_VALUE);        

        PricingField integer = new PricingField("int field", 2009);        

        PricingField decimal = new PricingField("decimal field", new BigDecimal("20.63"));
        	
		expect:
		
		"Some String"                      == string.getValue();
		
		DATE_VALUE                         == date.getValue();
		
		2009                               == ((Integer) integer.getValue()).intValue();
		
		new BigDecimal("20.63").toString() == ((BigDecimal) decimal.getValue()).toString();
		}

		def "testGetStrValue"() {
        
		setup:
		
		PricingField string = new PricingField("str field", "Some String");
        
        PricingField date = new PricingField("date field", DATE_VALUE);

		PricingField integer = new PricingField("int field", 2009);
        
		PricingField decimal = new PricingField("decimal field", new BigDecimal("20.63"));
        
		
		expect:
		
		"Some String"          ==  string.getStrValue();
		
		DATE_VALUE_STRING      == date.getStrValue();
		
		"2009"                 == integer.getStrValue();
		
		"20.63"                == decimal.getStrValue();
    }

		def "testGetDateValue"() {
		
		setup:
		
        PricingField date = new PricingField("date field", DATE_VALUE);

		expect:
		
		PricingField.Type.DATE   == date.getType();
        
		DATE_VALUE               == date.getDateValue();
    }

        def "testGetCalendarValue"() {
        
		setup:
		
		PricingField date = new PricingField("date field", DATE_VALUE);

		calendar = date.getCalendarValue();
		
		expect:
		
		PricingField.Type.DATE == date.getType();
		
        2009                   == calendar.get(Calendar.YEAR);
        
		11                     == calendar.get(Calendar.MONTH);
        
		16                     == calendar.get(Calendar.DAY_OF_MONTH);  
    }

		def "testGetIntegerValue"() {
			
		setup:
		
        PricingField integer = new PricingField("int field", 2009);

		expect:
		
        PricingField.Type.INTEGER == integer.getType();
        2009                      == integer.getIntValue().intValue();
    }

		def "testGetDecimalValue"() {
			
		setup:
		
        PricingField decimal = new PricingField("decimal field", new BigDecimal("20.63"));

        expect:
		
		PricingField.Type.DECIMAL  == decimal.getType();
        new BigDecimal("20.63")    == decimal.getDecimalValue();
    }

		def "testGetBooleanValue"() {
				
        setup:
		
		PricingField bool = new PricingField("boolean field", true);

        expect:
		
		PricingField.Type.BOOLEAN == bool.getType();
        true                      == bool.getBooleanValue().booleanValue();

        bool.setBooleanValue(false);
		
        false                     == bool.getBooleanValue().booleanValue();               
    }

		def "testEncode"() {
			
		setup:
		
        PricingField string = new PricingField("str field", "Some String");
        
        PricingField date = new PricingField("date field", DATE_VALUE);
        
        PricingField integer = new PricingField("int field", 2009);
        
        PricingField decimal = new PricingField("decimal field", new BigDecimal("20.63"));
        
        PricingField bool = new PricingField("boolean field", true);
        		
		expect:
		
		"str field:1:string:Some String" == PricingField.encode(string);
		
		"date field:1:date:" + DATE_VALUE_STRING == PricingField.encode(date);
		
		"int field:1:integer:2009" == PricingField.encode(integer);
		
		"decimal field:1:float:20.63" == PricingField.encode(decimal);
		
		"boolean field:1:boolean:true" == PricingField.encode(bool);
    }

        def "testDecode"() {
			
		setup:
		
        PricingField string = new PricingField("str field:1:string:Some String");
        
        PricingField date = new PricingField("date field:1:date:" + DATE_VALUE_STRING);

        PricingField integer = new PricingField("int field:1:integer:2009");
       
        PricingField decimal = new PricingField("decimal field:1:float:20.63");
        
        PricingField bool = new PricingField("boolean field:1:boolean:true");
		
		expect:
		
		PricingField.Type.STRING  == string.getType();
		
		"Some String"             == string.getStrValue();
		
		DATE_VALUE.getTime()      == date.getDateValue().getTime();
		
		PricingField.Type.INTEGER == integer.getType();
		
		2009                      == integer.getIntValue().intValue();
		
		PricingField.Type.DECIMAL == decimal.getType();
				
		new BigDecimal("20.63")   == decimal.getDecimalValue();
		
		PricingField.Type.BOOLEAN == bool.getType();
		
		true                      == bool.getBooleanValue().booleanValue();
    }
}
 