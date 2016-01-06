package in.webdata.unit

import spock.lang.Specification;
import java.math.BigDecimal;
import java.math.RoundingMode;
import spock.lang.Shared;

 class BigDecimalTestCaseSpec extends Specification { 

 
	 @Shared Integer COMPARISON_SCALE              = 2;
	 
	 @Shared RoundingMode COMPARISON_ROUNDING_MODE = RoundingMode.HALF_UP;
	 
	 public  def "Checking for equality"() {
      
		    setup:
			
			BigDecimal actual   = 5;
			   			   
			BigDecimal expected = 5;
			
			test2(4,5,"String doesn't match.")
						
		    expect:
			
			expected == actual
	    }
	 
	  public def "checking for equality"(){
	  		  
	            setup:
			  
			  BigDecimal expected = 12.12;
			  BigDecimal actual = 12.12341;
			  
		    expect:
						
	// Providing null in case it is to be tested in TestCase and literal is not used
			
			((Object) expected  == null ? null : expected.setScale(COMPARISON_SCALE, COMPARISON_ROUNDING_MODE)) == 
					((Object) expected  == null ? null : expected.setScale(COMPARISON_SCALE, COMPARISON_ROUNDING_MODE));
	 }
	  
	//Sir we can use Junit anotation or groovy function in a Spock test class so using it here.
	// Sir I'm using groovy method  Because when a method is called having spock blocks (when: then:) in it show the no such method error that's why I have used groovy method.
	// I have seached almost completely on internet.But I haven't found any way to call spock method with arguments from spoke method.If you find any sir then please tell me.

	  public def "test2"(BigDecimal expectedValue, BigDecimal actualValue,String messageToShow) {
		  
		  if(expectedValue   != actualValue)
		  	
		  println(messageToShow)		  
		  
	  }
}