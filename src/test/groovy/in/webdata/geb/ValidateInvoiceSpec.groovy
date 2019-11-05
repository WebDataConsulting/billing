package in.webdata.geb

import geb.Page;
import java.util.List;
import org.openqa.selenium.Keys
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import geb.spock.GebReportingSpec
import geb.Browser

class ValidateInvoiceSpec extends GebReportingSpec {

		def "ValidatingInvoice"(){//10.1
			
			given:
				def var1
				go "order/index"
				
			when:
				$("tr > td > a > strong").find{it.text().contains("mediation-batch-test-01")}.click()
				$("div.btn-box > div.row > a > span").find{it.text().contains("GENERATE INVOICE")}.click()
			
			then:
				$("div.msg-box.successfully > strong").text() == "Done"
				
				
			when:
				$("tr > td > a > strong").find{it.text().contains("mediation-batch-test-01")}.click()
				
				$("tr >td.value").each {
					println  "it text **********" +it.text()
 					if(it.text().equals("11/05/2015")) {
						var1 = it.text()
					}
				}
				
			then:
				assert var1 !=  null
		}	

	}
