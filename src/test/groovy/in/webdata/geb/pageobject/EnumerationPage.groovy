package in.webdata.geb.pageobject
import javax.swing.text.html.FormView.BrowseFileAction;

import java.util.Map

import geb.Page
import geb.PageChangeListener

class EnumerationPage extends Page {
	
				static url = "enumerations/index"
		
				static at = {
					driver.currentUrl == System.getProperty("baseUrl").concat(url)
				}
		
				
				static content = {
						
					addNewEnumeration (wait: true){$("#column1 .submit", text: "ADD NEW")}
					//editEnum					  {waitFor{$("a.submit.edit", text: "EDIT")}}
					selectEnum                    {waitFor{$("a.cell.double")} }
				}

				AddEnumerationPage clickAddNewEnumeration(){
					addNewEnumeration.click()
					
					browser.page(AddEnumerationPage)
					return browser.page
				}
				
				
				EnumerationListPage chooseEnum(def enumName){
					selectEnum.find("strong").find{it.text().equalsIgnoreCase(enumName)}.click()
					
					browser.page(EnumerationListPage)
					return browser.page
				}
				
	}
		


class AddEnumerationPage extends Page{
	
				static url	= "enumerations/edit"
		
				static at 	= {
					waitFor { driver.currentUrl == System.getProperty("baseUrl").concat(url) }
				}
	
		
		static content = {
				addName 					    	{$("input", id: "name")}
				addNewValues 	                    {waitFor{$('img[alt=\"Add more values\"]')} }
				enterNewValues  					{waitFor{$("span > input.field").find{it.value() == ""}} }
				enterValues							{$("span > input.field")}
				saveChanges							{$("a.submit.save > span")} 
				
				
		}
		
		
		EnumerationPage clickSaveEnumeration(def fieldsMap){
			
				if (fieldsMap['addName']) 		 	 addName         	= fieldsMap['addName']
				if (fieldsMap['enterValues']) 		 enterValues       	= fieldsMap['enterValues']
				addNewValues.click()
				if (fieldsMap['enterNewValues']) 	 enterNewValues    	= fieldsMap['enterNewValues']

				saveChanges.click()
				browser.page(EnumerationPage)
				return browser.page
		}
			
		
		EnumerationPage clickSaveEditedEnum(def fieldsMap){
					if (fieldsMap['enterValues']) 		 enterValues       	= fieldsMap['enterValues']
					//if (fieldsMap['enterNewValues']) 	 enterNewValues    	= fieldsMap['enterNewValues']
					
					enterValues.find{it.value() == "1"}.click()
					//enterValues.find{it.value() == "2"}.value(Keys.BACK_SPACE +"fieldsMap")
					
					saveChanges.click()
					browser.page(EnumerationPage)
					return browser.page
				}
		
}


class EnumerationListPage extends Page{
	
				static url = "enumerations/list"
		
				static at  = {
					waitFor { driver.currentUrl == System.getProperty("baseUrl").concat(url) }
				}
		
				static content = {
					editEnum 						{waitFor{$("a.submit.edit", text: "EDIT")} }
				}
				
				AddEnumerationPage clickEditEnum(){
					
					editEnum.click()
					browser.page(AddEnumerationPage)
					return browser.page
				}
				
					
}