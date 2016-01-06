package in.webdata.geb

import geb.spock.GebReportingSpec
import geb.Browser

class TestLoginSpec extends GebReportingSpec {

    def "Test jBilling Login"() {
        given:
           go "login/auth/"

        when:
            $("#j_username") << "admin"
            $("#j_password") << "123qwe"
            $("#j_client_id") << "Prancing Pony"
            $("a.submit.save").click()

        then:
            assert $("h1").text() == "jBilling"
    }

}

