import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxProfile

println "LOADING tests..."

baseUrl = "http://localhost:8080/jbilling/"
reportsDir ="test-reports/geb"
autoClearCookies = false



System.setProperty("baseUrl", baseUrl)
	
waiting {
	timeout = 120
	retryInterval = 1.0
}