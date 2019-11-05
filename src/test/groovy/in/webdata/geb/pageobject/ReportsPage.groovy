package in.webdata.geb.pageobject

import geb.Page

class ReportTypePage extends Page {
	static url = "report/index"
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
		
		driver.currentUrl == System.getProperty("baseUrl").concat(url)
	}
	
	static content = {
		reportType (wait: true) {$('a.cell.double > strong')}
	}
	
	ReportPage clickReportType(def reportTypeName) {
			reportType.find {
				it.text().equals(reportTypeName)
			}.click()
			
		browser.page(ReportPage)
		return browser.page
	}
}


class ReportPage extends Page {
	
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
		
	}
	
	static content = {
		reports (wait:true) {$('a.cell.double > strong')}
	}
	
	RunReportPage clickReport(def reportName) {
			reports.find {
				it.text().equals(reportName)
			}.click()
			
		browser.page(RunReportPage)
		return browser.page
	}
}

class RunReportPage extends Page {
	
	static at = {
		waitFor {js.('document.readyState') == 'complete'}
		
	}
	
	static content = {
		startDate {$('input', id: "start_date")}
		endDate {$('input', id: "end_date")}
		periodBreakDown {$('select', id: "period")}
		childCompnies {$('select', id: "childs")}
		reportFormat {$('select', id: "format")}
		
		runReport(wait: true ) { $('.submit', text: "RUN REPORT")}
	}
	
	RunReportPage clickRunReport(def fieldsMap) {
			if(fieldsMap['startDate']) startDate = fieldsMap['startDate']
			if(fieldsMap['endDate']) endDate = fieldsMap['endDate']
			if(fieldsMap['periodBreakDown']) periodBreakDown = fieldsMap['periodBreakDown']
			if(fieldsMap['childCompnies']) childCompnies = fieldsMap['childCompnies']
			if(fieldsMap['reportFormat']) reportFormat = fieldsMap['reportFormat']
			
			//runReport.click()
			browser.page(RunReportPage)
			return browser.page
	}

}
