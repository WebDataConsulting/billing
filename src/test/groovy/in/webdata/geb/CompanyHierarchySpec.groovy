package in.webdata.geb

import geb.Page;
import java.util.List;
import org.openqa.selenium.Keys
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import geb.spock.GebReportingSpec
import geb.Browser

class CompanyHierarchySpec extends GebReportingSpec{
	
			def "Impersonate child company and view information_5.1"(){
				when:
					$("#impersonate").click()
					waitFor { $("#impersonation-button").click() }
					$("li > a > span").find{it.text().contains("Products")}.click()
					$("tr > td > a").find{it.text().contains("Asset Category1")}.click()
					waitFor { $("tr > td > a").find{it.text().contains("SIM-card1")}.click() }
					waitFor { $("div.btn-box > a").find{it.text().contains("SHOW ASSETS")}.click() }
					waitFor { $("#column1 a.cell.double > strong").find{it.text().equals("SIM-201")}.click() }
					
				then:
					assert  $("td.narrow >a > strong").text() == "SIM-201"
					
					
				when:
					$("ul.top-nav > a.dissimulate").click()
				
				
				then:
					assert $(".top-nav > li").text() == "Prancing Pony"
					
				when:
					$("li > a > span").find{it.text().contains("Customers")}.click()
					$("tr > td > a").find{it.text().contains("Child2 entity")}.click()
				
				then:
					assert $("tr > td > a > strong").text() == "Child2 entity"
			}
		
}
