package in.webdata.geb.pageobject

import geb.Page

class AgentPage extends Page {

	static url = "partner/index"
	static at = {
		
		waitFor {js.('jQuery.active') == 0}
		
		driver.currentUrl == System.getProperty("baseUrl").concat(url)
	}
	
	static content = {
		addAgent {$('.submit', text: "ADD NEW")}
		showCommission {$('.submit', text: "SHOW COMMISSION")}
		agents {$("#column1 .double")}
	}
	
	EditAgentPage clickAddNewAgent() {
		addAgent.click()
		
		browser.page(EditAgentPage)
		return browser.page
	}
	
	ShowAgentPage clickAgent(def agentName) {
		agents.find(text: agentName).click()
		
		browser.page(ShowAgentPage)
		return browser.page
	}
	
	CommissionPage clickShowCommission() {
		showCommission.click()
		
		browser.page(CommissionPage)
		return browser.page
	}
}

class EditAgentPage extends Page {
	
	static at = {
		waitFor {js.('jQuery.active') == 0}
		
		$('.heading').find { it.text().equalsIgnoreCase("NEW AGENT") }.text() != null 
	}
	static content = {
		agentName { $("input", id: "user.userName")}
		agentType { $("select", id: "type")}
		commissionType { $("select", id: "commissionType")}
		parentId { $("input", id: "parentId")}
		emailId { $("input", id: "contact-null.email")}
		
		saveAgent { $(".submit", text: "SAVE CHANGES")}
	}
	
	AgentPage clickSaveAgent(def fieldsMap) {
		
			if(fieldsMap['agentName']) agentName = fieldsMap['agentName']
			if(fieldsMap['agentType']) agentType = fieldsMap['agentType']
			if(fieldsMap['commissionType']) commissionType = fieldsMap['commissionType']
			if(fieldsMap['parentId']) parentId = fieldsMap['parentId']
			if(fieldsMap['emailId']) emailId = fieldsMap['emailId']
			
			saveAgent.click()
			
		browser.page(AgentPage)
		return browser.page
	}
}

class ShowAgentPage extends Page {

	static at = {
		waitFor {js.('jQuery.active') == 0}
		
	}
	
	static content = {
		editAgent (wait: true){$('.submit', text: "EDIT")}
		userCode (wait: true){$('.submit', text: "USER CODE")}
		deleteAgent (wait: true) {$('.submit', text: "DELETE")}
		addSubAgent (wait: true) {$('.submit', text: "ADD SUB AGENT")}
		showCommission (wait: true) {$('#column2 .submit', text: "SHOW COMMISSIONS")}
		clickYes(wait: true ) {$(".ui-button-text", text: "Yes")}
	}
	
	CommissionPage clickShowCommission() {
		showCommission.click() 
		
		browser.page(CommissionPage)
		return browser.page
	}
	
	EditAgentPage clickEditAgent() {
		editAgent.click() 
		
		browser.page(EditAgentPage)
		return browser.page
	}
	
	EditAgentPage clickAddSubAgent() {
		addSubAgent.click() 
		
		browser.page(EditAgentPage)
		return browser.page
	}
	
	EditAgentPage clickDeleteAgent() {
			deleteAgent.click()
			clickYes().click()

		browser.page(AgentPage)
		return browser.page
	}
	
}


class CommissionPage extends Page {
	
	static at = {
	
		waitFor {js.('jQuery.active') == 0}
	}

	static content = {
		runDate (wait:  true) {$('#column1 a.cell')}
	}
	
	ShowCommissionPage clickRunDate(def agentRunDate) {
		runDate.find {
			it.text().equals(agentRunDate)
		}.click()
		
		browser.page(ShowCommissionPage)
		return browser.page
	}
}

class ShowCommissionPage extends Page {
	
	static at = {
		waitFor {js.('jQuery.active') == 0}
	}
}	
