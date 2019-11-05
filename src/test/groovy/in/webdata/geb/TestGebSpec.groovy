package in.webdata.geb

import geb.spock.GebReportingSpec
import in.webdata.geb.pageobject.CustomerPage

class TestGebSpec extends GebReportingSpec {

		    def "Test jBilling Login"() {
		        given:
		           go "login/auth/"
		
		        when:
		            $("#j_username") 	<< "admin"
		            $("#j_password") 	<< "123qwe"
		            $("#j_client_id") 	<< "Prancing Pony"
		            $("a.submit.save").click()
		
		        then:
		            assert $(".top-nav").text().contains("Prancing Pony")
		    }
		
		   /* def "Test List Customers" () {
		        given:
		            to CustomerPage
		    }*/

}

