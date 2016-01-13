package in.webdata.geb

import geb.Page;

import java.util.List;

import org.openqa.selenium.Keys
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import in.webdata.geb.pageobject.*
import spock.lang.Stepwise;
import geb.spock.GebReportingSpec
import geb.Browser

@Stepwise
class CompanySetupConfigurationSpec extends GebReportingSpec {
	
			def "Impersonating a child company"(){//_2
				
				when:
					waitFor { $("#impersonate").click() }
					$("#impersonation-button").click() 
					$("ul.top-nav > a.dissimulate").click() 
		
				then:
					assert $(".top-nav > li").text() == "Prancing Pony" 
			}
			
	
			def "Setting up Account Type"(){//_2.1
				
				given:
					go "accountType/list"
					
				when:
					waitFor { $("a.submit.add > span").click() }
					waitFor { $("input", name:"invoiceDesign") 	<< "invoice_design" }
					$("input", name:"description") 		<< "Direct Customer" 
					waitFor { $("a.submit.save > span").click() }
					
					waitFor { $("a.submit.add > span").click() }
					waitFor { $("input", name:"invoiceDesign") 	<< "invoice_design" }
					$("input", name:"description") 		<< "Distributor" 
					
					waitFor { $("a.submit.save > span").click() }
					
					go "accountType/list"
					
					waitFor { $("a.cell.double").find("strong").find{it.text().contains("Distributor")}.click() }
					waitFor { $("a.submit.edit").find("span").find{it.text() == "EDIT"}.click() }
					waitFor { $("input", name:"description").value(Keys.BACK_SPACE + "Distributor Account") }
					waitFor { $("a.submit.save > span").click() }
					
				then:
					assert  $("div.msg-box.successfully > strong").text() == "Done" 
			}
	
			def "Creating Payment method to account type"() {//_2.1.1
				
				given:
					go "config/index"
				
				
				when:
					waitFor { $("li > a").find{it.text().contains("Payment Method")}.click() }
					waitFor { $("div.btn-box > a.submit.add").click() }
					$('#templateId').find("option").find{ it.text() == "Payment Card" }.click() 
					waitFor { $("a.submit.save").click() }
					
					waitFor { $('#accountTypes').find("option").find{ it.text() == "Distributor Account" }.click() }
					waitFor { $("input", name: "isRecurring").click() }
					$("input", name: "methodName") 		<< "Debit Card" 
					
					waitFor { $(".submit.save").find{it.text() == "SAVE CHANGES" }.click()}
				then:
					assert $("div.msg-box.successfully >strong").text() == "Done" 
			}

			def "Adding MetaFiled Info-Type"(){//_2.2
				given:
					to AccountTypePage
				
				expect: 
					at AccountTypePage
					
				when:
					ShowAccountTypePage showAccountTypePage= to(AccountTypePage).clickAccountType("Direct Customer")
					AITDetailsPage aitDetailsPage = showAccountTypePage.clickAddInformationType()
					AddMetafieldPage addMetaFieldPage= waitFor { aitDetailsPage.fillAccountInformationtDetails([name:'Email', displayOrder: 1]).clickAddMetafield() }
					waitFor { addMetaFieldPage.metaFieldName << "Email" }
					waitFor { addMetaFieldPage.updateMetafield.click()}
					//waitFor { $(".submit.save").find{it.text() == "SAVE CHANGES" }.click()}
					waitFor { addMetaFieldPage.saveChangesButton.click() } 
				then:
					assert  $("div.msg-box.successfully >p").text()=="Account Information Type created successfully"
			}
			
			def "Configuring Credit-card payment method to Direct Customer_2.3"(){
				
				given:
					go "config/index"
				
				
				when:
					$("li > a").find{it.text().contains("Payment Method")}.click()
					$("div.btn-box > a.submit.add").click()
					$('#templateId').find("option").find{ it.text() == "Payment Card" }.click()
					$("a.submit.save").click()
					
					waitFor { $('#accountTypes').find("option").find{ it.text() == "Direct Customer" }.click() }
					waitFor { $("input", name: "isRecurring").click() }
					waitFor { $("input", name: "methodName") 		<< "Credit Card1" }
					
					if ( waitFor { $(".submit.save").find{it.text() == "SAVE CHANGES" }.click()} == null)
						$(".submit.save").find{it.text() == "SAVE CHANGES" }.click()
						
				then:
					 assert  $("div.msg-box.successfully >strong").text()=="Done"
				
			}
			
			def "Configure Order Periods"(){//_2.4
				given:
					go "config/index"
				
				
				when:
					$("li > a").find{it.text().contains("Order Periods")}.click()
					$("div.btn-box > a.submit.add > span").find{it.text().contains("ADD NEW")}.click()
					waitFor { $("input", name: "description") } 		<< "Semi monthly"
					
					$('#periodUnitId').find("option").find{ it.text() == "Semi-Monthly" }.click()
					waitFor { $("input", name: "value") 				<< "1"}
					$("a.submit.save > span").click()
					
					waitFor { $("div.btn-box > a.submit.add > span").find{it.text().contains("ADD NEW")}.click() }
					
					waitFor { $("input", id: "description") 		<< "Weekly"}
					$('#periodUnitId').find("option").find{ it.text() == "Week" }.click()
					$("input", name: "value") 				<< "1"
					$("a.submit.save > span").click()
					
					waitFor { $("div.btn-box > a.submit.add > span").find{it.text().contains("ADD NEW")}.click()}
					
					waitFor { $("input", name: "description") 		<< "Daily"}
					$('#periodUnitId').find("option").find{ it.text() == "Day" }.click()
					$("input", name: "value") 				<< "1"
					$("a.submit.save > span").click()
					
				then:
					assert $("div.msg-box.successfully > p").text() == "Order Period created successfully"
			}

			
			def "Configuring Collections"(){//2.5
				given:
					go "config/index"
						
				when:
					$("li > a").find{it.text().contains("Collections")}.click()
					waitFor { $('img[alt=\"add\"]').click() }
					waitFor { $("tr > td.medium2 > input").find{it.value()==""}.click().value(Keys.BACK_SPACE +"Payment due") }
					def element1 = $("tr > td.medium > input.numericOnly").find{it.value()==""}
					element1.click().value(Keys.BACK_SPACE +"0")
					println "***a*** " + element1.attr('id')
					def n_id1 = element1.attr('id').substring(0, element1.attr('id')?.indexOf('.'))
					def str1 = ".paymentRetry"
					str1=n_id1.concat(str1)
					$("input", name:str1).click()
					
					
					waitFor { $('img[alt=\"add\"]').click() }
					waitFor { $("tr > td.medium2 > input").find{it.value()==""}.click().value(Keys.BACK_SPACE +"Grace Period") }
					$("tr > td.medium > input.numericOnly").find{it.value()==""}//.click().value(Keys.BACK_SPACE +"2")
					def element2 = $("tr > td.medium > input.numericOnly").find{it.value()==""} 
					element2.value(Keys.BACK_SPACE +"2")
					println "***b*** " + element2.attr('id')
					def n_id2 = element2.attr('id').substring(0, element2.attr('id')?.indexOf('.'))
					def str2 = ".sendNotification"
					str2=n_id2.concat(str2)
					$("input", name:str2).click()
					
					
					
					waitFor { $('img[alt=\"add\"]').click() }
					waitFor { $("tr > td.medium2 > input").find{it.value()==""}.click().value(Keys.BACK_SPACE +"First Retry") }
					$("tr > td.medium > input.numericOnly").find{it.value()==""}//.click().value(Keys.BACK_SPACE +"3")
					def element3 = $("tr > td.medium > input.numericOnly").find{it.value()==""}
					element3.click().value(Keys.BACK_SPACE +"3")
					println "***c*** " + element3.attr('id')
					def n_id3 = element3.attr('id').substring(0, element3.attr('id')?.indexOf('.'))
					def str3 = ".paymentRetry"
					str3=n_id3.concat(str3)
					$("input", name:str3).click()
					
					
					waitFor { $('img[alt=\"add\"]').click() }
					waitFor { $("tr > td.medium2 > input").find{it.value()==""}.click().value(Keys.BACK_SPACE +"Suspended") }
					$("tr > td.medium > input.numericOnly").find{it.value()==""}//.click().value(Keys.BACK_SPACE +"7")
					def element4 = $("tr > td.medium > input.numericOnly").find{it.value()==""}
					element4.click().value(Keys.BACK_SPACE +"7")
					println "***d*** " + element4.attr('id')
					def n_id4 = element4.attr('id').substring(0, element4.attr('id')?.indexOf('.'))
					def str4 = ".suspended"
					str4=n_id4.concat(str4)
					$("input", name:str4).click()
					
					$("a.submit.save").find("span").find{it.text() == "SAVE CHANGES"}.click()
					
					
				then:
					assert $("div.msg-box.successfully > p").text() == "Collection configuration steps updated successfully"
			}

						
			def "configuring Collections plugin"(){//_2.6
				given:
					go "config/index"
					
				when:
					$("li > a").find{it.text().contains("Plug-ins")}.click()
					$("td > a > strong").find{it.text().contains("Generic internal events listener")}.click()
					waitFor { $("a.submit.add >span").click() }
					waitFor { $("#typeId").find("option").find{ it.text() == "com.sapienter.jbilling.server.user.tasks.UserAgeingNotificationTask" }.click() }
					$("input", name: "processingOrder") 	<< "9"
					$("input", name: "plgDynamic.1.name") 	<< "3"
					$("input", name: "plgDynamic.1.value") 	<< "901"
					$('img[alt=\"add\"]').click()
					$("a.submit.save > span").click()
				then:
					 assert $("div.msg-box.successfully > strong").text() == "Done" 
			}


			def "Configure Billing Process"(){//_2.7
				given:
					go "config/index"
					
				when:
					$("li > a").find{it.text().contains("Billing Process")}.click()
					$("input", name: "nextRunDate").value(Keys.BACK_SPACE +"10/26/2006")
					$("input", name: "maximumPeriods")value(Keys.BACK_SPACE +"1")
					$("a.submit.save > span").click()
					
				then:
					$("div.msg-box.successfully >strong").text() == "Done"
			}

}
