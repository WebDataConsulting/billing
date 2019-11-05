import geb.Page;
import java.util.List;
import org.openqa.selenium.Keys
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import spock.lang.Stepwise;

import geb.spock.GebReportingSpec
import geb.Browser


@Stepwise
class InvoiceManualSpec extends GebReportingSpec {
	
		def GeneratingInvoiceManually () {//10.1
			
			given:	
				go "order/index"
			
			
			when:
				$("tr > td > a > strong").find{it.text().contains("mediation-batch-test-01")}.click()
				waitFor { $("div.btn-box >div.row > a.submit.order > span").find{it.text().contains("GENERATE INVOICE")}.click() }
			
			then:
				assert  $("div.msg-box.successfully > strong").text() == "Done" 
				
				
			when:
				$("tr > td > a > strong").find{it.text().contains("mediation-batch-test-01")}.click()
				waitFor { $("div.btn-box >div.row > a.submit.save > span").find{it.text().contains("DOWNLOAD PDF")}.click() }
				$("div.btn-box >div.row > a.submit.payment > span").find{it.text().contains("PAY INVOICE")}.click()
		
			then:
				assert  $("div.form-edit >div.heading > strong").text() == "NEW PAYMENT"
		}
	
	
		def "ValidatingInvoice"(){//10.1.1
		 
		 given:
			 go "order/index"
			 def var1
			 
		 when:
			 $("tr > td > a > strong").find{it.text().contains("mediation-batch-test-01")}.click()
			 waitFor { $("div.btn-box > div.row > a > span").find{it.text().contains("GENERATE INVOICE")}.click() }
			 
		 then:
			 assert $("div.msg-box.successfully > strong").text() == "Done" 
			 
			 
		 when:
			 waitFor { $("tr > td > a > strong").find{it.text().contains("mediation-batch-test-01")}.click() }
			 
			 $("tr > td > a > strong").find {
				 if(it.text().equals("mediation-batch-test-01")){
				 var1 = it.text()
				 } 
			 }
		 then:
			assert   var1 != null
				
		 }
		
		def "creating Payment from generated invoices"(){
			given:
				go "invoice/index"
				
			when:
				$("tr > td > a > strong").find{it.text().contains("mediation-batch-test-01")}.click()
				waitFor { $("div.btn-box >div.row > a.submit.payment > span").find{it.text().contains("PAY INVOICE")}.click() }
				waitFor { $("select", name: "paymentMethod_0.paymentMethodTypeId").find("option").find{it.text() == "Payment Card"}.click() }
				
				waitFor{ $("div.row > div.inp-bg > input.field").find{it.value()==""}.click().value(Keys.BACK_SPACE +"1") }
				$("div.row > div.inp-bg > input.field.text").find{it.value()==""}.click().value(Keys.BACK_SPACE +"Ashish")
				$("div.row > div.inp-bg > input.field.text").find{it.value()==""}.click().value(Keys.BACK_SPACE +"4111111111111152")
				$("div.row > div.inp-bg > input.field.text").find{it.value()==""}.click().value(Keys.BACK_SPACE +"12/2020")
				
				
				$("a.submit.payment > span").find{it.text().contains("REVIEW PAYMENT")}.click()
				waitFor { $("a.submit.payment > span").find{it.text().contains("MAKE PAYMENT")}.click() }
				$(".msg-box.info>strong").each{ println "dcfdsf" +it.text() }
				
			then:
				assert  $(".msg-box.info>strong").find{ it.text() == "Info:"} != null
		}

}
