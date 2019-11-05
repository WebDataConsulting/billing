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

import grails.plugin.springsecurity.annotation.Secured

import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.common.SessionInternalError;

/**
* BillingController
*
* @author Vikas Bodani
* @since 11/01/11
*/
@Secured(["isAuthenticated()"])
class BillingconfigurationController {

	static scope = "prototype"
	IWebServicesSessionBean webServicesSession
	def viewUtils
	def breadcrumbService
    
    def index () {

		breadcrumbService.addBreadcrumb(controllerName, actionName, null, null)
		def configuration= webServicesSession.getBillingProcessConfiguration()
		boolean isBillingRunning= webServicesSession.isBillingRunning(webServicesSession.getCallerCompanyId())
		if (params['alreadyRunning']?.toBoolean()) {
			flash.error = 'prompt.billing.already.running'
			flash.info = 'prompt.billing.running'
		} else {
			if (isBillingRunning) {
				flash.info = 'prompt.billing.running'
			} else {
				if (params['isFutureRunAttempt']?.toBoolean()) {
					flash.error = 'billing.cannot.be.run.for.future.date'
				} else if ( params['trigger']?.toBoolean()) {
					flash.error = 'prompt.billing.trigger.fail'
				}
			}
		}
		[configuration:configuration, isBillingRunning: isBillingRunning]
	}
	
	def saveConfig () {

		log.info "${params}"
		def configuration= new BillingProcessConfigurationWS() 
		bindData(configuration, params)

		//set all checkbox values as int
		configuration.setGenerateReport params.generateReport ? 1 : 0
		configuration.setInvoiceDateProcess params.invoiceDateProcess ? 1 : 0 
		configuration.setOnlyRecurring params.onlyRecurring ? 1 : 0
		configuration.setAutoPaymentApplication params.autoPaymentApplication ? 1 : 0
		//configuration.setNextRunDate (DateTimeFormat.forPattern("dd-MMM-yyyy").parseDateTime(params.nextRunDate))
		configuration.setEntityId webServicesSession.getCallerCompanyId()
		
		log.info "Generate Report ${params.generateReport}"
		
		try {
			webServicesSession.createUpdateBillingProcessConfiguration(configuration)
			flash.message = 'billing.configuration.save.success'
		} catch (SessionInternalError e){
			viewUtils.resolveException(flash, session.locale, e);
		} catch (Exception e) {
			log.info e.getMessage()
			flash.error = 'billing.configuration.save.fail'
		}
		
		chain action: 'index'
	}
	
	def runBilling () {
		def alreadyRunning= false
		def isFutureRunAttempt = false
		def configuration= webServicesSession.getBillingProcessConfiguration()
		
		//Check billing Run Date cannot be in future.
		if(configuration.nextRunDate.after(new Date())) {
			isFutureRunAttempt = true
		}
		try {
			if (!webServicesSession.isBillingRunning(webServicesSession.getCallerCompanyId())) {
				webServicesSession.triggerBillingAsync(new Date())
				//flash.message = 'prompt.billing.trigger'
			} else {
				flash.error = 'prompt.billing.already.running'
				alreadyRunning= true
			}
		} catch (Exception e) {
			log.error e.getMessage()
			viewUtils.resolveException(flash, session.locale, e);
		}

		chain action: 'index', params: ['trigger': true, 'alreadyRunning': alreadyRunning, 'isFutureRunAttempt': isFutureRunAttempt]
	}
}
