import geb.Page;

import java.util.List;

import org.junit.Before;
import org.openqa.selenium.Keys
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import geb.spock.GebReportingSpec
import geb.Browser
import in.webdata.geb.pageobject.ShowCustomerPage
import in.webdata.geb.pageobject.CustomerPage

class CollectionSpec extends GebReportingSpec{
	
	def "Running collection to see changed status of Customer" () {
			
		when:
			go "config/aging"
			
			waitFor { $("input", id: "collectionsRunDate").value(Keys.BACK_SPACE + "01/01/2008") }
			$('#run').click()
			waitFor { $("div.ui-dialog-buttonset").find("span").find{it.text() == "Run Collections"}.click() }
			
		then:
			assert $("div.msg-box.successfully > p").text() == "Collections process for the date 01/01/2008 was triggered" 
		
			
			
		
			
	
		when:
			ShowCustomerPage customerPage = to(CustomerPage).clickCustomer("Spencer Haas")
			
		and:
			def temp
			$("tr >td.value").each{
							println "td*** "+it.text()
				}
		
			
			
			$("tr >td.value").find {
						if(it.text().equals("Active"))  {
							temp = it.text()
						}
					}
			
				 
		then:
			assert temp == "Active"
			
			
		when:
			customerPage = to(CustomerPage).clickCustomer("ageing-test-01")
			
		and:
		   $("tr >td.value").find {
			  		if(it.text().equals("Active"))  {
						  temp = it.text()
					  }
		   		}
		then:
		   assert temp == "Active"
			
			
	
	
		when:
			go "config/aging"
			
			waitFor { $("input", id: "collectionsRunDate").value(Keys.BACK_SPACE + "01/20/2008") }
			$('#run').click()
			$("div.ui-dialog-buttonset").find("span").find{it.text() == "Run Collections"}.click()
			
		then:
			assert  $("div.msg-box.successfully > p").text() == "Collections process for the date 01/20/2008 was triggered"
		
			
			
				
		when:
			 customerPage = to(CustomerPage).clickCustomer("Spencer Haas")
		and:
			$("tr >td.value").find {
						if(it.text().equals("Active"))  {
							temp = it.text()
						}
					}
		then:
			assert temp == "Active"
			
		
			
		when:
			 customerPage = to(CustomerPage).clickCustomer("ageing-test-01")
			 
		and:
			$("tr >td.value").find {
				 		if(it.text().equals("Active"))  {
							 temp = it.text()
						 }
				}
		then:
			assert temp == "Active"
			
	
	
		when:
		go "config/aging"
		
		waitFor { $("input", id: "collectionsRunDate").value(Keys.BACK_SPACE + "01/25/2008") }
		$('#run').click()
		$("div.ui-dialog-buttonset").find("span").find{it.text() == "Run Collections"}.click()
		
		then:
			assert $("div.msg-box.successfully > p").text() == "Collections process for the date 01/25/2008 was triggered"
	
		when:
			 customerPage = to(CustomerPage).clickCustomer("Spencer Haas")
			 
		and:
			$("tr >td.value").find {
				 if(it.text().equals("Active"))  {
					 temp = it.text()
				}
		}	 
			
		then:
			assert temp == "Active"
		
		when:
			 customerPage = to(CustomerPage).clickCustomer("ageing-test-01")
		and:
			$("tr >td.value").find {
				 		if(it.text().equals("Active"))  {
							 temp = it.text()
						 }
			 }
		then:
			assert temp == "Active"
		}
}