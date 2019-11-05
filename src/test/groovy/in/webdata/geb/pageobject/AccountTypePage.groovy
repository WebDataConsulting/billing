package in.webdata.geb.pageobject

import javax.swing.text.html.FormView.BrowseFileAction;
import java.util.Map 
import geb.Page
import geb.PageChangeListener

class AccountTypePage extends Page {
	
	static url = "accountType/list"
	static at = {
		waitFor {js.('jQuery.active') == 0}
		
		driver.currentUrl == System.getProperty("baseUrl").concat(url)
	}

	static content = {
		createAccountType { $("div.btn-box > a.submit.add > span") }
		selectAccountType (wait:true) {$("a.cell.double > strong")}
	}
	
	EditAccountTypePage clickAddNewAccountType() {
		createAccountType.click()
		
		browser.page(EditAccountTypePage)
		return browser.page
	}
	
	ShowAccountTypePage clickAccountType(def accountTypeName) {
		selectAccountType.each  {
			if(it.text().equals(accountTypeName)) { it.click() }
		}
		
		browser.page(ShowAccountTypePage)
		return browser.page
	}
}

class EditAccountTypePage extends Page {
	
	static url = "accountType/edit"
	static accountTypeId
	
	static at = {
		waitFor {js.('jQuery.active') == 0}
		
		if(null != accountTypeId) {
			driver.currentUrl == System.getProperty("baseUrl").concat(url).concat("/").concat(languageId.toString())
		} else {
			driver.currentUrl == System.getProperty("baseUrl").concat(url)
		}	
	}

	static content = {
		accountTypeName { $("input", id: "description") }
		nextInvoiceDayOfPeriod { $("input", id: "mainSubscription.nextInvoiceDayOfPeriod") }
		periodId { $("input", id: "mainSubscription.periodId") }
		invoiceDesign { $("input", id: "invoiceDesign") }
		paymentMethod { $("select", id: "payment-method-select") }
		saveButton (wait : true ) { $("div.btn-box.buttons > ul > li > a.submit.save > span") }
	}

    //TODO Rohit - To implement Map as an input type to all saves/edit calls
	AccountTypePage clickSaveAccountType(def fieldsMap) {
		
        if (fieldsMap['accountTypeName']) 		 accountTypeName         = fieldsMap['accountTypeName']
		if (fieldsMap['nextInvoiceDayOfPeriod']) nextInvoiceDayOfPeriod  = fieldsMap['nextInvoiceDayOfPeriod']
		if (fieldsMap['periodId']) 				 periodId                = fieldsMap['periodId']
		if (fieldsMap['invoiceDesign']) 		 invoiceDesign           = fieldsMap['invoiceDesign']
		if (fieldsMap['paymentMethod']) 		 paymentMethod           = fieldsMap['paymentMethod']
		
		saveButton.click()
		
		browser.page(AccountTypePage)
		return browser.page
	}
	
}


class ShowAccountTypePage extends Page {
	
	static at = {
		waitFor {js.('jQuery.active') == 0}
	}
	
	static content = {
		accountTypeDescription (wait: true){ $("div#column2.column-hold > div.column-hold > div.heading > strong")}
		editAccountTypeOrAddInformationType (wait: true) {$("div.btn-box > div.row > a.submit.edit > span")}
		deleteAccountType (wait: true) {$("div.btn-box > div.row > a.submit.delete > span")}
		clickYes(wait: true ) {$("ui-button.ui-widget.ui-state-default.ui-corner-all.ui-button-text-only > span.ui-button-text")}
	}
	
	EditAccountTypePage clickEditAccountType() {
		editAccountTypeOrAddInformationType.find {
			it.text().equalsIgnoreCase("EDIT")
		}.click()
		
		browser.page(EditAccountTypePage)
		return browser.page
	}
	
	
	AITDetailsPage clickAddInformationType() {
		editAccountTypeOrAddInformationType.find {
			it.text().equalsIgnoreCase("Add Information Type")
		}.click()
		
		browser.page(AITDetailsPage)
		return browser.page
	}
	
	AccountTypePage clickDeleteAccountType() {
		deleteAccountType.find  {
			it.text().equalsIgnoreCase("DELETE")
		}.click()
	
		sleep(1000)
		
		clickYes.find {
			it.text().equalsIgnoreCase("YES")
		}.click()
		
		browser.page(AccountTypePage)
		return browser.page
	}
	
}

class AITDetailsPage extends Page {
	
	static at = {
		 waitFor {js.('jQuery.active') == 0}
		
		 $("div#builder-tabs.ui-tabs.ui-widget.ui-widget-content.ui-corner-all" + 
					"> ul.ui-tabs-nav.ui-helper-reset.ui-helper-clearfix.ui-widget-header.ui-corner-all" + 
					"> li.ui-state-default.ui-corner-top.ui-tabs-active.ui-state-active").find {
				println "it text *********" +it.text()
			it.text().equalsIgnoreCase("DETAILS")
		}.text() != null 	
	}

	static content = {
		name { $("input", id: "name") }
		displayOrder { $("input", id: "displayOrder") }
		useInNotification { $("input", id: "useForNotifications") }
		addMetafield (wait: true) { $("div#column1 > div.btn-box.ait-btn-box > a.submit.save > span") }
		saveButton (wait: true) { $("div#column2 > div.btn-box.ait-btn-box > a.submit.save > span") }
	}
	
	AITDetailsPage fillAccountInformationtDetails(def fieldsMap) {
		
		if(fieldsMap['name']) 				name 			  = fieldsMap['name']
		if(fieldsMap['displayOrder'])   	displayOrder 	  = fieldsMap['displayOrder']
		if(fieldsMap['useInNotification']) 	useInNotification = fieldsMap['useInNotification']
		//if(fieldsMap['addMetafield']) 		addMetafield 	  = fieldsMap['addMetafield']
		
		return browser.page
	}
	
	AddMetafieldPage clickAddMetafield() {
		addMetafield.click()
		
		browser.page(AddMetafieldPage)
		return browser.page
		
	}
	
	AccountInformationTypeListPage saveAccountInformationType () {
		saveButton.find {
			it.text().equalsIgnoreCase("SAVE CHANGES")
		}.click()
		
		browser.page(AccountInformationTypeListPage)
		return browser.page
	}
}

class AddMetafieldPage extends Page {
	
	static at = {
		waitFor {js.('jQuery.active') == 0}
		$("div#column2.column-hold > div#review-box > div.box.no-heading > div.sub-box > div.header > div.column > h1").text() == "Metafields"
	}

	static content = {
		updateMetafield (wait: true) { $(".submit.save").find{it.text() == "UPDATE" } }
		metaFieldName (wait:true) { $("input", id:"metaField0.name") } 
		saveChangesButton { $(".submit.save").find{it.text() == "SAVE CHANGES" } }	
	}
	
	AITDetailsPage fillAitMetaField(def fieldsMap) {
		
		metafieldDetails(fieldsMap)
		
		updateMetafield.find {
			it.text().equalsIgnoreCase("UPDATE")
		}.click()
		
		browser.page(AITDetailsPage)
		return browser.page
	}
	
	EditPaymentMethodPage fillPaymentMetaField(def fieldsMap) {
		
		metafieldDetails(fieldsMap)
		
		updateMetafield.find {
			it.text().equalsIgnoreCase("UPDATE")
		}.click()
		
		browser.page(EditPaymentMethodPage)
		return browser.page
	}
	
	AddOrderChangeTypePage fillOrdChangMetaField(def fieldsMap) {
		
		metafieldDetails(fieldsMap)
		
		updateMetafield.click()
		
		browser.page(AddOrderChangeTypePage)
		return browser.page
	}
	
	MetafieldListPage fillMetaField(def fieldsMap) {
		
		metafieldDetails(fieldsMap)
		updateMetafield.click()
		
		browser.page(MetafieldListPage)
		return browser.page
	}
	
	
	public  void metafieldDetails(def fieldsMap) {
		// content has deleted so still remain
	}
	
}

class AccountInformationTypeListPage extends Page {
	static url = "accountType/listAIT"
	
	static at = {
		waitFor {js.('jQuery.active') == 0}
		
		driver.currentUrl == System.getProperty("baseUrl").concat(url)
		$("table#periods > thead > tr > th.medium ").text() == "ACCOUNT INFORMATION TYPES"
	}
	
	static content = {
		addAccountInformationType (wait: true) { $("div.btn-box > a.submit.save > span") }
		selectAccountInformationType {$("a.cell.double > strong")}
	}
	
	ShowAccountInformationTypePage clickAccountInformationType(def accountInformationTypeName) {
		selectAccountInformationType.each  {
			if(it.text().equalsIgnoreCase(accountInformationTypeName)) { it.click()}
		}
		
		browser.page(ShowAccountInformationTypePage)
		return browser.page
	}
}

class ShowAccountInformationTypePage extends Page {
	
	def accountInformationType
	static at = {
		waitFor {js.('jQuery.active') == 0}
		
		$("div#column2.column-hold > div.column-hold > div.heading > strong").text() == accountInformationType
	}
	
	static content = {
		editAccountInfomationType (wait: true) { $("div#column2.column-hold > div.column-hold > div.btn-box > a.submit.edit > span") }
		deleteAccountInformationType (wait: true) { $("div#column2.column-hold > div.column-hold > div.btn-box > a.submit.delete > span") }
		clickYes(wait: true ) {$("ui-button.ui-widget.ui-state-default.ui-corner-all.ui-button-text-only > span.ui-button-text")}
		cloneAccountInformationType (wait: true) { $("div#column2.column-hold > div.column-hold > div.btn-box > a.submit.add > span") }
	}

	AITDetailsPage clickEditAccountInformationType() {
		editAccountInfomationType.each  {
			if(it.text().equalsIgnoreCase("EDIT")) { it.click()}
		}
		
		browser.page(AITDetailsPage)
		return browser.page
	}
	
	AccountInformationTypeListPage clickDeleteAccountInformationType () {
		deleteAccountInformationType.each  {
			if(it.text().equalsIgnoreCase("DELETE")) { it.click()}
		}
		
		sleep(1000)
		
		clickYes.find {
			it.equalsIgnoreCase("YES")
		}.click()
		
		browser.page(AccountInformationTypeListPage)
		return browser.page
	}
	
	AITDetailsPage cloneAccountInformationType() {
		cloneAccountInformationType.each  {
			if(it.text().equalsIgnoreCase("CLONE")) { it.click()}
		}
		
		browser.page(AITDetailsPage)
		return browser.page
	}
	
}

