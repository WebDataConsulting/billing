package in.webdata.geb

import geb.Page;
import java.util.List;
import org.openqa.selenium.Keys
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import geb.spock.GebReportingSpec
import geb.Browser

class CategoryProductsSpec extends GebReportingSpec {

				def "Creating_editing a product"(){//_3.1
					
					given:
						go "product/index"
					
					and:
						go "product/editCategory?add=true"
					
						$("input", name:"description") 			<< "New Test Category1"
						$("#global-checkbox").click()
						
						waitFor { $("a.submit.save").click() }
					
					expect:
						assert  $("div.msg-box.successfully > strong").text() == "Done" 
					}
	
		
				def "Creating Asset Category1"(){//_3.2
					given:
						go "product/index"
						
						
					and:
						go "product/editCategory?add=true"
						
						$("input", name:"description") 			<< "Asset Category1"
						$("#global-checkbox").click()
						$("#allowAssetManagement").click()
						
						$("#lastStatusName") 					<< "Available"
						$("#lastStatusAvailable").click()
						$("#lastStatusDefault").click()
						$("img[alt=\"add\"]").click()
						
						$("#lastStatusName") 					<< "In use"
						$("#lastStatusOrderSaved").click()
						
						waitFor { $("span.type-metafield-menu > a > img[alt=\"add\"]").click() }
						$("input", name: "metaField2.name") 	<< "Tax ID"
						waitFor { $("a.submit.save > span").click() }
						
					expect:
						assert  $("a.cell.double > strong").text() == "Asset Category1"
					}
	
				def "Creating_editing Product"(){//_3.3
					given:
						go "product/index"
						
					when:
						waitFor { $("tr > td > a").find{it.text().contains("New Test Category1")}.click() }
						//Add Product
						waitFor { $("a").find{it.text().contains("ADD PRODUCT")}.click() }
						$('img[alt="remove"]').click()
						$("input", name: "product.descriptions[0].content") << "Product Code1 Description"
						$("input", name: "product.number" ) 				<< "Product Code1"
						$("input", id:"product.standardAvailability").value('false')
						$("select", id: 'product.accountTypes') << "Direct Customer"
						$("select", id: "company-select").find("option").find { it.text() == "Prancing Pony"}.click();
						$("select", id: "entitySelect") << "Prancing Pony"
						$("select", id: "currencySelect") << "Unites State Dollars"
						$("input", name: "product.rate") << "500"
						waitFor { $("a").find{it.text().contains("ADD PRICE")}.click() }
						waitFor { $("a.submit.save > span").click() }
						
						//Add Product
						waitFor { $("div.btn-box > a.submit.add > span").click() }
						waitFor { $('img[alt=\"remove\"]').click() }
						$("input", name: "product.descriptions[0].content") << "Test Code Description"
						$("input", name: "product.number" ) 				<< "Test Code"
						$("input", id:"product.standardAvailability").value('false')
						$("select", id: 'product.accountTypes') << "Direct Customer"
						$("select", id: "company-select").find("option").find { it.text() == "Prancing Pony"}.click();
						$("select", id: "entitySelect") << "Prancing Pony"
						$("select", id: "currencySelect") << "Unites State Dollars"
						$("input", name: "product.rate") << "10"
						waitFor { $("a").find{it.text().contains("ADD PRICE")}.click() }
						waitFor { $("a.submit.save > span").click() }
						
						//Edit Product
						waitFor { $("a.cell.double > strong", text: "Test Code Description").click() }
						waitFor { $("a.submit.edit > span").click() }
						$("select", id: "entitySelect") << "Prancing Pony"
						$("select", id: "currencySelect") << "Australian Dollar"
						$("input", name: "product.rate") << "15"
						waitFor { $("a").find{it.text().contains("ADD PRICE")}.click() }
						waitFor { $("a.submit.save > span").click() }
						
					then:
						assert  $("div.msg-box.successfully >strong").text() == "Done"
						
					}
	
				def "Creating Products with Assets"(){//_3.4
					given:
						def var1
						go "plugin/index"
						
						
					when:
						waitFor { $("#column1 a > strong ").find{it.text().equals("Generic internal events listener")}.click() }
						waitFor { $("a.submit.add > span").click() }
						waitFor { $('#typeId').find("option").find{ it.value() == "111" }.click() }
						$("input", name: "processingOrder")			<< "10"
						waitFor { $("a.submit.save > span").click() }
						
						go "product/index"
						waitFor { $("tr > td > a").find{it.text().contains("Asset Category1")}.click() }
						waitFor { $("#column2 > div.btn-box > a.submit.add > span").click() }
						waitFor { $("img[alt=\"remove\"]").click() }
						$("input", name: "product.descriptions[0].content") << "SIM-card1"
						$("input", name: "product.number") 					<< "S001"
						$("#assetManagementEnabled").click()
						$("#global-checkbox").click()
						$("select", id: "entitySelect") << "Prancing Pony"
						$("select", id: "currencySelect") << "Unites State Dollars"
						$("input", name: "product.rate") << "2"
						waitFor { $("a").find{it.text().contains("ADD PRICE")}.click() }
						waitFor { $("a.submit.save > span").click() }
						
						waitFor { $("tr > td > a").find{it.text().contains("SIM-card1")}.click() }
						waitFor { $("div.btn-box > a").find{it.text().contains("ADD ASSET")}.click() }
						waitFor { $("#global-checkbox").click() }
						$("input", name:"identifier") 						<< "SIM-101"
						$("div.inp-bg").find("input").find{it.value() == ""} << "T-101"
						$("a.submit.save > span").click()
						
						waitFor { $("a.submit.add").find("span").find{it.text() == "ADD NEW"}.click() }
						
						waitFor { $('#company-select').find("option").find{ it.text() == "Prancing Pony" }.click() }
						$("input", name:"identifier")                          << "SIM-201"
						$("div.inp-bg").find("input").find{it.value() == ""}   << "T-201"
						$("a.submit.save > span").click()
						
						
						waitFor { $("a.cell.double").find("strong").each{it.text() == "SIM-201"}.click() 
						$("#column2 .heading").find {
							//println  "it text **********" +it.text()
							 	if(it.text().equals("SIM-201") ){
									 var1 = it.text()
								 }
							}
						}
						
						then:
							assert var1 == "SIM-201"
						
				}	
	}