package in.webdata.geb;

import geb.Page;


import java.util.List;

import org.openqa.selenium.Keys
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import spock.lang.Stepwise;
import in.webdata.geb.pageobject.*
import in.webdata.geb.pageobject.OrderLineChangePage
import geb.Browser
import geb.spock.GebReportingSpec

@Stepwise
class BillingProcessAndCollectionsSpec extends GebReportingSpec {

    def "Creating orders belonging to different periods"() {//11.1


        when:
        def fieldsMap = [loginName: "Billing Customer1", emailId: "billing_customer1@gmail.com", mainSubscriptionPeriodId: "Monthly", nextInvoiceDayOfPeriodId: "1",
						paymentMethod: "Credit Card1", processingOrder: "1", cardHolderName: "Ashish", cardNumber: "4111111111111152", cardExpiryDate: "02/2020" ]
        CustomerPage customerPage = to(CustomerPage).clickAddNewCustomer()
                .clickCustomerForm("Prancing Pony", "Direct Customer")
                .clickCustomerFormSubmit(fieldsMap)

        then:
        assert $("div.msg-box.successfully >strong").text() == "Done"



        when:
        fieldsMap.clear()
        fieldsMap = [loginName: "Billing Customer2", emailId: "billing_customer2@gmail.com", mainSubscriptionPeriodId: "Monthly", nextInvoiceDayOfPeriodId: "1",
					paymentMethod: "Credit Card1", processingOrder: "1", cardHolderName: "Ashish", cardNumber: "4111111111111152", cardExpiryDate: "02/2020" ]
        customerPage = to(CustomerPage).clickAddNewCustomer()
                .clickCustomerForm("Prancing Pony", "Direct Customer")
                .clickCustomerFormSubmit(fieldsMap)

        then:
        assert $("div.msg-box.successfully >strong").text() == "Done"




        when:
        fieldsMap.clear()
        fieldsMap = [nextRunDate: "01/01/2008"]
        customerPage = to(CustomerPage).clickCustomer("Billing Customer1")
                .clickEditCustomer()
                .clickCustomerFormSubmit(fieldsMap)

        then:
        assert $("div.msg-box.successfully >strong").text() == "Done"




        when:
        fieldsMap.clear()
        fieldsMap = [nextRunDate: "01/01/2008", dueDate: "15"]
        customerPage = to(CustomerPage).clickCustomer("Billing Customer2")
                .clickEditCustomer()
                .clickCustomerFormSubmit(fieldsMap)

        then:
        assert $("div.msg-box.successfully >strong").text() == "Done"



        when:
        go "product/index"


        $("div.btn-box > a.submit.add").find("span").find { it.text() == "ADD CATEGORY" }.click()
        $('#description') << "Billing Category"
        $("select", id: "company-select").find("option").find { it.text() == "Prancing Pony" }.click()
        $("div.buttons > ul > li > a.submit.save").find("span").find { it.text() == "SAVE CHANGES" }.click()


        then:
        assert $("div.msg-box.successfully >strong").text() == "Done"



        when:

        $("a.cell.double").find("strong").find{it.text() == "Billing Category"}.click()
        waitFor { $("div.btn-box > a.submit.add").find("span").find { it.text() == "ADD PRODUCT" }.click() }
        $('img[alt=\"remove\"]').click()
        $("input", id: "product.number") << "Billing Flat"
        $("input", id: "product.descriptions[0].content") << "Flat Pricing"
		$("select", id: "company-select") << "Prancing Pony"
		waitFor { $("input", id: "mydate").value("01/01/2008") }
		$("select", id: "entitySelect") << "Prancing Pony"
		$("select", id: "currencySelect") << "Unites State Dollars"
		$("input", name: "product.rate") << "20"
		waitFor { $("a").find{it.text().contains("ADD PRICE")}.click() }
        $("div.buttons > ul > li > a.submit.save").find("span").find { it.text() == "SAVE CHANGES" }.click()


        then:
        assert $("div.msg-box.successfully >strong").text() == "Done"




        when:
        //$("a.cell.double").find("strong").find{it.text() == "Billing Category"}.click()
        $("div.btn-box > a.submit.add").find("span").find { it.text() == "ADD PRODUCT" }.click()
        $('img[alt=\"remove\"]').click()
        $("input", id: "product.number") << "Billing Graduated"
        $("input", id: "product.descriptions[0].content") << "Graduated Pricing"
		$("select", id: "company-select") << "Prancing Pony"
		waitFor { $("input", id: "mydate").value("01/01/2008") }
		$("select", id: "entitySelect") << "Prancing Pony"
		$("select", id: "currencySelect") << "Unites State Dollars"
		$("input", name: "product.rate") << "5"
		waitFor { $("a").find{it.text().contains("ADD PRICE")}.click() }
        $("div.buttons > ul > li > a.submit.save").find("span").find { it.text() == "SAVE CHANGES" }.click()


        then:
        assert $("div.msg-box.successfully >strong").text() == "Done"




        when:
        fieldsMap.clear()
        fieldsMap = [orderPeriods: "One time", billingTypes: "post paid", activeSinceDate: "01/01/2008"]
        ShowCustomerPage show = to(CustomerPage).clickCustomer("Billing Customer2")
		OrderDetailsPage orderDetailsPage = show .clickCreateOrder()
		OrderProductPage productPage = waitFor { orderDetailsPage.orderDetails(fieldsMap)}
        OrderLineChangePage lineChange = waitFor { productPage.clickNonAssetProduct("Graduated Pricing") } //.updateOrderLine()


        $("input").find {
            it.attr('name').startsWith('change--') && it.attr('name').endsWith('quantityAsDecimal')
        }.each {
            it.value(Keys.BACK_SPACE + "14")
        }



        $("input").find {
            it.attr('name').startsWith('change--') && it.attr('name').endsWith('startDate')

        }.each {
            it.value(Keys.BACK_SPACE + "01/01/2008")
        }

        waitFor { $("a.submit.save").find("span").find { it.text() == "UPDATE" }.click() }

        waitFor {
            $("div.btn-box.order-btn-box > a.submit.save").find("span").find { it.text() == "SAVE CHANGES" }.click()
        }


        then:
        assert $("div.msg-box.successfully > strong").text() == "Done"




        when:
        fieldsMap.clear()
        fieldsMap = [orderPeriods: "Monthly", billingTypes: "pre paid", activeSinceDate: "01/01/2008"]
        OrderLineChangePage orderPage = waitFor {
            to(CustomerPage).clickCustomer("Billing Customer2")
                    .clickCreateOrder()
                    .orderDetails(fieldsMap)
                    .clickNonAssetProduct("Flat Pricing")
        }

        $("input").find {
            it.attr('name').startsWith('change--') && it.attr('name').endsWith('quantityAsDecimal')
        }.each {
            it.value(Keys.BACK_SPACE + "3")
        }


        $("input").find {
            it.attr('name').startsWith('change--') && it.attr('name').endsWith('startDate')

        }.each {
            it.value(Keys.BACK_SPACE + "01/01/2008")
        }



        $("a.submit.save").find("span").find { it.text() == "UPDATE" }.click()

        waitFor {
            $("div.btn-box.order-btn-box > a.submit.save").find("span").find { it.text() == "SAVE CHANGES" }.click()
        }


        then:
        assert $("div.msg-box.successfully > strong").text() == "Done"




        when:
        fieldsMap.clear()
        fieldsMap = [orderPeriods: "Monthly", billingTypes: "post paid", activeSinceDate: "01/01/2008"]
        orderPage = to(CustomerPage).clickCustomer("Billing Customer1")
                .clickCreateOrder()
                .orderDetails(fieldsMap)
                .clickNonAssetProduct("Graduated Pricing")//.updateOrderLine()


        $("input").find {
            it.attr('name').startsWith('change--') && it.attr('name').endsWith('quantityAsDecimal')
        }.each {
            it.value(Keys.BACK_SPACE + "14")
        }



        $("input").find {
            it.attr('name').startsWith('change--') && it.attr('name').endsWith('startDate')

        }.each {
            it.value(Keys.BACK_SPACE + "01/01/2008")
        }



        $("a.submit.save").find("span").find { it.text() == "UPDATE" }.click()

        waitFor {
            $("div.btn-box.order-btn-box > a.submit.save").find("span").find { it.text() == "SAVE CHANGES" }.click()
        }

        then:
        assert $("div.msg-box.successfully > strong").text() == "Done"




        when:
        fieldsMap.clear()
        fieldsMap = [orderPeriods: "Monthly", billingTypes: "post paid", activeSinceDate: "01/01/2008"]
        orderPage = waitFor {
            to(CustomerPage).clickCustomer("Billing Customer1")
                    .clickCreateOrder()
                    .orderDetails(fieldsMap)
                    .clickNonAssetProduct("Flat Pricing")
        }


        $("input").find {
            it.attr('name').startsWith('change--') && it.attr('name').endsWith('quantityAsDecimal')
        }.each {
            it.value(Keys.BACK_SPACE + "2")
        }


        waitFor { $("input").find {
            it.attr('name').startsWith('change--') && it.attr('name').endsWith('startDate')

        }.each {
            it.value(Keys.BACK_SPACE + "01/01/2008")
        }
        }

        $("a.submit.save").find("span").find { it.text() == "UPDATE" }.click()

        waitFor { $("a.submit.save").find("span").find { it.text() == "SAVE CHANGES" }.click() }

        then:
        assert $("div.msg-box.successfully > strong").text() == "Done"
    }


    def "Approving review report for real billing run"() { //11.2

        given:
        def var1
        go "billingconfiguration/index"


        when:
        $("input", id: "nextRunDate").value(Keys.BACK_SPACE + "01/01/2008")
        waitFor { $("div.btn-box > a.submit.save").find("span").find { it.text() == "SAVE CHANGES" }.click() }
        waitFor { $("a.submit").find("span").find { it.text() == "RUN BILLING" }.click() }
		sleep(10000)

        then:
        assert $("div.msg-box.info > p").text() == "The Billing Process is currently running"

        /*when:
            go "billing/index"

            $("td.medium > a.cell").find("em").find{it.text() == "Multi-currency"}.click()
            $("a.submit.apply").find("span").find{it.text() == "SHOW ORDERS"}.click()


        and:
            $("a.double.cell").find("strong").each{it.text().contains("gandalf")}.click()
            sleep(3000)
            $("tr >td.value").find {
                println  "it text **********" +it.text()
                 if(it.text().equals("pre paid")) {
                     var1 = it.text()
                }
            }

        then:
                println "order is present"
                //assert var1 == "pre paid"
            */



        when:
        go "billing/index"
        $("td.medium > a.cell").find("em").find { it.text() == "Multi-currency" }.click()
        waitFor { $("a.submit.show").find("span").find { it.text() == "SHOW INVOICES" }.click() }


        then:
        println "No invoice"





        when:
        go "billing/index"

        $("td.medium > a.cell").find("em").find { it.text() == "Multi-currency" }.click()
        waitFor { $("a.submit.apply").find("span").find { it.text() == "APPROVE" }.click() }
        waitFor { $("div.ui-dialog-buttonset").find("span").find { it.text() == "Yes" }.click() }
        then:
        assert $("div.msg-box.successfully > p").text() == "Process Review Approved"

    }

    def "Running Billing Process for the orders created"() {//11.3

        given:
        def var2
        go "billingconfiguration/index"


        when:
        $("input", id: "nextRunDate").value(Keys.BACK_SPACE + "01/01/2008")
        $('#generateReport').click()
        $("div.btn-box > a.submit.save").find("span").find { it.text() == "SAVE CHANGES" }.click()
        $("a.submit", text: "RUN BILLING").click()

        then:
        assert $("div.msg-box.info > p").text() == "The Billing Process is currently running"



        when:
        go "billing/index"

        $("td.medium > a.cell").find { it.text() == "09/26/2006" }.click()
        $("a.submit.apply").find("span").find { it.text() == "SHOW ORDERS" }.click()

        and:
        waitFor { $("a.double.cell").find("strong").each { it.text().contains("gandalf") }.click() }
        waitFor {
            $("tr >td.value").find {
                //println  "it text **********" +it.text()
                if (it.text().equals("pre paid")) {
                    var2 = it.text()
                }
            }
        }

        then:

        assert var2 == "pre paid"


        when:
        go "billing/index"
        $("td.medium > a.cell").find { it.text() == "09/26/2006" }.click()
        waitFor { $("a.submit.show").find("span").find { it.text() == "SHOW INVOICES" }.click() }

        then:
        assert $("a.double.cell > strong").text() == "gandalf"

    }


    def "Invoice periods and due dates"() {

        given:
        def var3
        go "billing/index"

        when:
        $("td.medium > a.cell").find { it.text() == "09/26/2006" }.click()
        waitFor { $("a.submit.show").find("span").find { it.text() == "SHOW INVOICES" }.click() }

        waitFor { $("a.double.cell").find("strong").each { it.text().contains("gandalf") }.click() }

        waitFor {
            $("tr >td.value").find {
                //println  "it text **********" +it.text()
                if (it.text().equals("09/26/2006")) {
                    var3 = it.text()
                }
            }
        }

        then:

        assert var3 == "09/26/2006"
    }
}
