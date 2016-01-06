import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxProfile

println "LAODING tests..."

baseUrl = "http://localhost:8080/jbilling/"
reportsDir ="test-reports/geb"

driver = {
    FirefoxProfile profile = new FirefoxProfile()
    new FirefoxDriver(profile)
}

	
