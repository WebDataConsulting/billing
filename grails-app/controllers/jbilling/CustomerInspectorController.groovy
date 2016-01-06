/*
 jBilling - The Enterprise Open Source Billing System
 Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

 This file is part of jbilling.

 jbilling is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 jbilling is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with jbilling.  If not, see <http://www.gnu.org/licenses/>.

 This source was modified by Web Data Technologies LLP (www.webdatatechnologies.in) since 15 Nov 2015.
You may download the latest source from webdataconsulting.github.io.

 */

package jbilling

import com.sapienter.jbilling.server.invoice.db.InvoiceDTO
import com.sapienter.jbilling.server.item.CurrencyBL
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue
import com.sapienter.jbilling.server.order.OrderWS
import com.sapienter.jbilling.server.order.db.OrderDAS
import com.sapienter.jbilling.server.payment.blacklist.BlacklistBL
import com.sapienter.jbilling.server.payment.db.PaymentDTO
import com.sapienter.jbilling.server.user.UserBL
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.db.CustomerNoteDTO
import com.sapienter.jbilling.server.user.db.UserDAS
import com.sapienter.jbilling.server.user.db.UserDTO
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import grails.plugin.springsecurity.annotation.Secured

@Secured(["isAuthenticated()"])
class CustomerInspectorController {

    static pagination = [max: 10, offset: 0]
	static scope = "prototype"
	IWebServicesSessionBean webServicesSession
    def viewUtils

    def breadcrumbService
    def productService

	def index () {
		redirect action: 'inspect', params: params
	}

	def inspect () {

        params.max=params.max?:pagination.max
        params.offset=params.offset?:pagination.offset

		def user = params.id ? UserDTO.get(params.int('id')) : null

        if (!user) {
            flash.error = 'no.user.found'
            flash.args = [ params.id as String ]
            return // todo: show generic error page
        }
        def customerNotes=CustomerNoteDTO.createCriteria().list(max: params.max,offset: params.offset){
            and{
                eq('customer.id',UserBL.getUserEntity(params.int('id'))?.getCustomer()?.getId())
                order("creationTime","desc")
            }
        }
        def revenue =  webServicesSession.getTotalRevenueByUser(user.id)
        def subscriptions = webServicesSession.getUserSubscriptions(user.id)

        // last invoice
        def invoiceIds = webServicesSession.getLastInvoices(user.id, 1)
        def invoice = invoiceIds ? InvoiceDTO.get(invoiceIds.first()) : null

        // last payment
        def paymentIds = webServicesSession.getLastPayments(user.id, 1)
        def payment = paymentIds ? PaymentDTO.get(paymentIds.first()) : null

        // blacklist matches
        def blacklistMatches = BlacklistBL.getBlacklistMatches(user.id)

        // used to find the next invoice date
        def cycle = new OrderDAS().findEarliestActiveOrder(user.id)

        // all customer prices and products
        def company = CompanyDTO.get(session['company_id'])

        def itemTypes = productService.getItemTypes(user.company.id, null)

        //initialize pagination parameters
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset

        def accountType = user?.customer?.accountType
        //use customer's company to fetch products
		def products = productService.getFilteredProductsForCustomer(company, null, params, null, false, true, user.company)
        def infoTypes = accountType?.informationTypes?.sort { it.displayOrder }

		List<MetaFieldValue> values =  new ArrayList<MetaFieldValue>()
		values.addAll(user?.customer?.metaFields)
		new UserBL().getCustomerEffectiveAitMetaFieldValues(values, user?.customer?.getAitTimelineMetaFieldsMap())
		
		// find all the subscription accounts and orders
		def subscriptionAccounts = new ArrayList<Integer>()
		def internalSubscriptions = new ArrayList<OrderWS>()
		
		UserDAS userDas = new UserDAS()
		for(def child : user?.customer?.children) {
			if(userDas.isSubscriptionAccount(child.baseUser.id)) {
				subscriptionAccounts.add(child.baseUser.id)
				
				internalSubscriptions.addAll(webServicesSession.getUserSubscriptions(child.baseUser.id))
			}
		}
		
        breadcrumbService.addBreadcrumb(controllerName, actionName, null, params.int('id'))

        [
                user: user,
                blacklistMatches: blacklistMatches,
                invoice: invoice,
                payment: payment,
                subscriptions: subscriptions,
                company: company,
				typeId: params.typeId,
                itemTypes: itemTypes,
                products: products,
                currencies: retrieveCurrencies(false),
                cycle: cycle,
                revenue: revenue,
                accountInformationTypes: infoTypes,
                customerNotes:customerNotes,
                customerNotesTotal:customerNotes?.totalCount,
                metaFields : values,
				subscriptionAccounts : subscriptionAccounts,
				internalSubscriptions : internalSubscriptions
        ]
    }

    def subNotes (){
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        def user = params.id ? UserDTO.get(params.int('id')) : null
        def customerNotes=CustomerNoteDTO.createCriteria().list(max: params.max,offset: params.offset){
            and{
                eq('customer.id',UserBL.getUserEntity(params.int('id'))?.getCustomer()?.getId())
                order("creationTime","desc")
            }
        }
        render template: 'customerNotes', model: [customerNotes: customerNotes, customerNotesTotal: customerNotes?.totalCount, user:user]
    }

    def retrieveCurrencies(def inUse) {
        def currencies = new CurrencyBL().getCurrencies(session['language_id'].toInteger(), session['company_id'].toInteger())
        return inUse ? currencies.findAll { it.inUse } : currencies;
    }

}
