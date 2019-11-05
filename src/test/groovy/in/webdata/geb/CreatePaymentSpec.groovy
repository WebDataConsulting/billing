package in.webdata.geb

import geb.Page;
import java.util.List;
import org.openqa.selenium.Keys
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import in.webdata.geb.pageobject.InvoiceListPage
import geb.spock.GebReportingSpec
import geb.Browser

class CreatePaymentSpec extends GebReportingSpec {
	
	def "creating Payment from generated invoices"(){
		given:
			to InvoiceListPage
			
		expect: 
			at 	InvoiceListPage
			
		when:
			$("#column1 .double > strong").find {
						it.text().equals("carry-over-test1")
					}.click() 
					
			waitFor { $("div.btn-box >div.row > a.submit.payment > span").find{it.text().contains("PAY INVOICE")}.click() }
			$("td.innerContent > div.row > input").click()
			waitFor { $("select", name: "paymentMethod_0.paymentMethodTypeId").find("option").find{it.text() == "Payment Card"}.click() 
			$("div.row > div.inp-bg > input.field").find{it.value()==""}.click().value(Keys.BACK_SPACE +"1")
			$("div.row > div.inp-bg > input.field.text").find{it.value()==""}.click().value(Keys.BACK_SPACE +"Ashish")
			$("div.row > div.inp-bg > input.field.text").find{it.value()==""}.click().value(Keys.BACK_SPACE +"4111111111111152")
			$("div.row > div.inp-bg > input.field.text").find{it.value()==""}.click().value(Keys.BACK_SPACE +"12/2020")  }
			
			
			waitFor { $("a.submit.payment > span").find{it.text().contains("REVIEW PAYMENT")}.click() }
			waitFor { $("a.submit.payment > span").find{it.text().contains("MAKE PAYMENT")}.click() }
			
		then:
			assert  $("div.msg-box.info > strong").text() == "Info:" 
	}

}
