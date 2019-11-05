package in.webdata.geb

import geb.Page;
import java.util.List;

import spock.lang.Stepwise;

import geb.spock.GebReportingSpec
import in.webdata.geb.pageobject.CustomerPage
@Stepwise
class CompanySpec extends GebReportingSpec{
	
		def "Test for child company creation_1.3"() {
			
			when:
				waitFor { $("#impersonate").click() }
			
			then:
				assert $("select", name: "entityId").text().contains("Reseller Organization") 
		}
	
		def "Creating child company with invoice reseller"(){//_1.4
			
			given:
				go "signup"
			
			and:
				waitFor { $("input", 	name: "contact.postalCode")     << "111123" }
				$("input", 	name: "user.userName") 						<< "TestChild"
				$("input", 	name: "contact.firstName") 					<< "Child2"
				$("input", 	name: "contact.lastName") 					<< "entity"
				$("input", 	name: "contact.phoneCountryCode1") 			<< "112"
				$("input", 	name: "contact.phoneAreaCode") 				<< "141"
				$("input", 	name: "contact.phoneNumber") 				<< "8585"
				$("input", 	name: "contact.email") 						<< "ravi_singhal30@yahoo.co.in"
				$("input", 	name: "contact.organizationName") 			<< "WDT Child3"
				$("input", 	name: "contact.invoiceAsReseller").click()
				
				$("input", 	name: "contact.address1") 					<< "Elements Mall"
				$("input", 	name: "contact.stateProvince") 				<< "Rajasthan"
				$("select", name: "contact.countryCode") 				<< "India"
				
			
				waitFor { $("a.submit.save").click() }
				to(CustomerPage)
				
			expect:
				at CustomerPage
				assert $("a.cell.double > strong").text().contains("Child2 entity") == true 
		}
}
