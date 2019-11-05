package in.webdata.geb.pageobject
import java.util.Map;

import geb.Page


class AgentCommissionProcessPage extends Page{

	static url = "config/partnerCommission"
	
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
		
		driver.currentUrl == System.getProperty("baseUrl").concat(url)
	}
	
	static content = {
		nextRunDate { $("input", id: "nextRunDate")}
		periodValue { $("input", id: "periodValue")}
		periodUnitId {$("input", id: "periodUnitId")}
		saveAgentCommission (wait: true) { $("div.btn-box > a.submit.save > span") }
		runProcess (wait: true) { $("div.btn-box > a.submit.apply > span") }
	}
	
	AgentCommissionProcessPage clickRunAgentProcess (def fieldsMap) {
		
		if(fieldsMap['nextRunDate'])  nextRunDate  = fieldsMap['nextRunDate']
		if(fieldsMap['periodValue'])  periodValue  = fieldsMap['periodValue']
		if(fieldsMap['periodUnitId']) periodUnitId = fieldsMap['periodUnitId']
		
		saveAgentCommission.click()
		
		runProcess.click()
		
		return browser.page
		
	}
	
}
