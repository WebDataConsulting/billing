package in.webdata.unit

import com.sapienter.jbilling.server.BigDecimalTestCase
import com.sapienter.jbilling.server.item.CurrencyBL;
import com.sapienter.jbilling.server.util.db.CurrencyDAS
import com.sapienter.jbilling.server.util.db.CurrencyExchangeDAS
import com.sapienter.jbilling.server.util.db.CurrencyExchangeDTO

import spock.lang.Specification
import spock.lang.Shared

public class CurrencyBLTestSpec extends Specification{

    @Shared private static final Integer ENTITY_ID = 1;

	@Shared String rate   = "2";
    @Shared private CurrencyDAS mockCurrencyDas = Mock(CurrencyDAS.class);
    @Shared private CurrencyExchangeDAS mockExchangeDas = Mock(CurrencyExchangeDAS.class);

	     def _mockCurrencyExchangeDTO() {
        
	     setup:

	//Combined 2 method in this spock method which is genreally done in spock methods
		 		 
		     CurrencyExchangeDTO dto = new CurrencyExchangeDTO();
			 
			 CurrencyExchangeDTO dto2 = new CurrencyExchangeDTO();
			 
			 CurrencyBL bl = new CurrencyBL(mockCurrencyDas, mockExchangeDas);
			 
		  and:
			 
			 dto.setRate(new BigDecimal("0.98"));
			 
			 dto2.setRate(new BigDecimal("1.20"));
			 
		  and:
		  
			 BigDecimal amount = bl.convert(200, 200, new BigDecimal("20.00"), ENTITY_ID);
		 
		   expect:
		   
		   	 new BigDecimal("20.000") ==  amount;
		 
      
    }

		 def "testConvertRepeatingDecimal"()  {
			
			 setup:

			 //   removing Easy mocks notations like verify(), reset(), andReturn()
 
				CurrencyBL bl = new CurrencyBL(mockCurrencyDas, mockExchangeDas);
		
				BigDecimal amount = bl.convert(200, 200, new BigDecimal("10.00"), ENTITY_ID);

         expect:
				new BigDecimal("10.00")  ==    amount ;
    }
}
