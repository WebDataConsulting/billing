package in.webdata.geb.pageobject

import geb.Page

import org.openqa.selenium.By
import geb.Browser
import in.webdata.geb.pageobject.OrderDetailsPage


class CustomerPage extends Page {

    static url = "customer/index"

    static at = {
        waitFor { js.('document.readyState') == 'complete' }
       // $("div.btn-box > a.submit.add > span").text() == "ADD NEW"
    }


    static content = {
        addNewCustomer(wait: true) { $("div.btn-box > a.submit.add > span", text: "ADD NEW") }
        selectCustomer(wait: true) { $("a.cell.double > strong") }

    }

    CustomerEditPage clickAddNewCustomer() {
        waitFor { addNewCustomer.click() }
        browser.page(CustomerEditPage)
        return browser.page
    }

    ShowCustomerPage clickCustomer(def customerName) {
        waitFor {
            selectCustomer.find {
                it.text().equals(customerName)
            }.click()
        }
        browser.page(ShowCustomerPage)
        return browser.page
    }
}

class CustomerEditPage extends Page {
		static at = {
			waitFor {js.('document.readyState') == 'complete'}
			
			$("div.form-edit > div.heading > strong ").text() == "SELECT ACCOUNT TYPE"
		}
		
		static content = {
		
		userCompany (wait:true) { $("select", id: "user.entityId")}
		accountType (wait:true) { $("select", id: "accountTypeId")}
		customerForm (wait:true) { 
			$(".submit", text: "SELECT")
		}
	}
		
	CustomerNewFormPage clickCustomerForm(String entitTyName, String accountTypes) {
		userCompany = entitTyName 
		accountType = accountTypes 
		
		customerForm.click()
		
		browser.page(CustomerNewFormPage)
		return browser.page
	}
}

class CustomerNewFormPage  extends Page {

	static at = {
		waitFor {js.('document.readyState') == 'complete'}
		$("div.heading > strong ").text() == "NEW CUSTOMER"
	}
	
	static content = {
		loginName { $("input", id: "user.userName") } 
		emailId {$("div.row > label", text: iContains("Email"))} 
		companyBillingCycle { $('a', id: "companyBillingCycle") }
		mainSubscriptionPeriodId {$("select", name: "mainSubscription.periodId")} 
		nextInvoiceDayOfPeriodId {$("select", name: "mainSubscription.nextInvoiceDayOfPeriod")} 
		isParent { $("input", id: "user.isParent")} 
		agentId {$('input', id: "user.partnerId")}
		nextRunDate {$("input", id: "user.nextInvoiceDate")} 
		checkSubaccountType 	 { $("input", id: "user.isParent")} 
		
		// processing payment
		
		paymentMethod {$("select", id: endsWith(".paymentMethodTypeId"))}
		processingOrder  {$("input").find { it.attr('name').endsWith('processingOrder')}}
		cardHolderName   {$("div.row > label")}
		cardNumber   	 {$("div.row > label")}
		cardExpiryDate   {$("div.row > label")}
		// redirect to customer page
		customerFormSubmit (wait: true) { $("a", text: "SAVE CHANGES") }
	}
		
	CustomerNewFormPage clickCompanyBillingCycle() {
		companyBillingCycle.click()
		
		browser.page(CustomerNewFormPage)
		return browser.page
	}
	
	CustomerPage clickCustomerFormSubmit(def fieldsMap) {
		if(fieldsMap['loginName']) loginName = fieldsMap['loginName']
		if(fieldsMap['emailId']) emailId << fieldsMap['emailId']
		if(fieldsMap['mainSubscriptionPeriodId']) mainSubscriptionPeriodId = fieldsMap['mainSubscriptionPeriodId']
		if(fieldsMap['nextInvoiceDayOfPeriodId']) nextInvoiceDayOfPeriodId = fieldsMap['nextInvoiceDayOfPeriodId']
		if(fieldsMap['isParent']) isParent = fieldsMap['isParent']
		if(fieldsMap['agentId']) agentId = fieldsMap['agentId']
		if(fieldsMap['nextRunDate']) nextRunDate = fieldsMap['nextRunDate']
		if(fieldsMap['checkSubaccountType'])      checkSubaccountType      = fieldsMap['checkSubaccountType']
		if(fieldsMap['processingOrder']) processingOrder << fieldsMap['processingOrder']
		println "paymentMethodType: " + paymentMethod.find('option').find{it.text() != "--"}.click()
		
		if(fieldsMap['cardHolderName']) {
			def cardHolderNameId = waitFor { cardHolderName.find{it.text() == "cc.cardholder.name*"}.attr('for') }
			waitFor { $('input',id:endsWith (cardHolderNameId)) << fieldsMap['cardHolderName'] }
		}
		
		if(fieldsMap['cardNumber']) {
			def cardNumberId = cardNumber.find{it.text() == "cc.number*"}.attr('for')
			waitFor { $('input',id:endsWith (cardNumberId)) << fieldsMap['cardNumber'] }
		}	
		
		if(fieldsMap['cardExpiryDate']) {
			def cardExpiryDateId = cardNumber.find{it.text() == "cc.expiry.date*"}.attr('for')
			waitFor { $('input',id:endsWith (cardExpiryDateId)) << fieldsMap['cardExpiryDate'] }
		}
		
		customerFormSubmit.click()
		
		browser.page(CustomerPage)
		return browser.page
	}
	
}


class ShowCustomerPage extends Page {
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
		
	}

	static content = {
		subAccount (wait: true) { $('.submit', text: "ADD SUB-ACCOUNT")}
		parentOrChildId(wait:true) {$("tr > td.value > a")} 
		createOrder(wait: true) { $('.submit', text: "CREATE ORDER") }
		makePayment(wait: true) {$('.submit', text: "MAKE PAYMENT")}
		editCustomer(wait: true) {$('.submit', text: "EDIT")}
	}
	
	CustomerEditPage clickAddChildCusomer() {
		waitFor  { subAccount.click() }
		
		browser.page(CustomerEditPage)
		return browser.page
	}
	
	OrderDetailsPage clickCreateOrder() {
		waitFor { createOrder.click() }
		browser.page(OrderDetailsPage)
		return browser.page
	}
	
	EditPaymentPage clickMakePayment() {
		waitFor { makePayment.click() }
		
		browser.page(EditPaymentPage)
		return browser.page
	}
	
	CustomerNewFormPage clickEditCustomer() {
		waitFor { editCustomer.click() }
		
		browser.page(CustomerNewFormPage)
		return browser.page
	}
	
}



