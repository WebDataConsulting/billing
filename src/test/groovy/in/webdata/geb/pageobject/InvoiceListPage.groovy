package in.webdata.geb.pageobject
import geb.Page

public class InvoiceListPage extends Page {
	
	static url = "invoice/index"
			
	static at = {
		waitFor {js.('jQuery.active') == 0}
		
		driver.currentUrl == System.getProperty("baseUrl").concat(url)
	}
	
	static content = {
		invoices{ $("a.double.cell > strong ")}
	}
	
	ShowInvoicePage clickInvoice(def invoice) {
		invoices.find {
					it.text().equals(invoice)
				}.click()
		browser.page(ShowInvociePage)
		return browser.page
	}
	
}

class ShowInvoicePage extends Page {
	
			
	static at = {
		waitFor {js.('jQuery.active') == 0}
		
	}
	
	static content = {
		payInvoice 		(wait: true) {$(".submit", text: "PAY INVOICE")}
		downloadPDF 	(wait: true) {$(".submit", text: "DOWNLOAD PDF")}
		sendEmail 		(wait: true) {$(".submit", text: "SEND AS EMAIL")}
		deleteInvoice 	(wait: true) {$(".submit", text: "DELETE INVOICE")}
		clickYes		(wait: true) {$(".ui-button-text", text: "Yes")}
	}
	
	EditPaymentPage clickPayInvoice() {
		payInvoice.click() 
		browser.page(EditPaymentPage)
		return browser.page
	}
	
	InvoiceListPage clickSendEmail() {
		
	}
	
	InvoiceListPage clickDeleteInvoice () {
		deleteInvoice.click()
		clickYes().click()
			
		browser.page(InvoiceListPage)
		return browser.page
	}
}