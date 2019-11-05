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

import com.sapienter.jbilling.client.util.ClientConstants
import com.sapienter.jbilling.server.util.PreferenceBL
import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import com.sapienter.jbilling.server.process.db.BillingProcessDTO
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS
import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.server.util.ServerConstants
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.Util
import com.sapienter.jbilling.server.process.db.BillingProcessConfigurationDTO
import com.sapienter.jbilling.server.payment.db.PaymentDAS
import com.sapienter.jbilling.server.process.BatchProcessInfoBL
import com.sapienter.jbilling.batch.billing.BatchContextHandler
import com.sapienter.jbilling.server.process.BillingProcessFailedUserBL

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap

/**
* BillingController
*
* @author Vikas Bodani
* @since 07/01/11
*/
@Secured(["isAuthenticated()"])
class BillingController {
	static scope = "prototype"
	static pagination = [ max: 10, offset: 0, sort: 'id', order: 'desc' ]

    // Matches the columns in the JQView grid with the corresponding field
    static final viewColumnsToFields =
            ['billingId': 'id',
             'date': 'billingDate',
             'orderCount': 'orderProcesses',
             'invoiceCount': 'invoices']

	IWebServicesSessionBean webServicesSession
	def recentItemService
	def breadcrumbService
	def filterService

	def index () {
		list()
	}
	
	/*
	 * Renders/display list of Billing Processes Ordered by Process Id descending
	 * so that the lastest process shows first.
	 */
	def list () {
		def filters = filterService.getFilters(FilterType.BILLINGPROCESS, params)
        breadcrumbService.addBreadcrumb(controllerName, actionName, null, null)

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], ClientConstants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            if (params.applyFilter || params.partial) {
                render template: 'billingTemplate', model: [ filters:filters ]
            }else {
                render view: "index", model: [ filters:filters ]
            }
            return
        }

        def processes = getProcesses(filters, params)

        if (params.applyFilter || params.partial) {
            render template: 'billingTemplate', model: [ processes: processes, filters:filters ]
        } else {
            render view: "index", model: [ processes: processes, filters:filters ]
        }
	}

    def findProcesses (){
        def filters = filterService.getFilters(FilterType.BILLINGPROCESS, params)

        params.sort = viewColumnsToFields[params.sidx]
        params.order  = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page')-1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def processes = getProcesses(filters, params)

        try {
            render getBillingProcessesJsonData(processes, params) as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }

    }

    /**
     * Converts Billing processes to JSon
     */
    private def Object getBillingProcessesJsonData(processes, GrailsParameterMap params) {
        def jsonCells = processes
        def currentPage = params.page ? Integer.valueOf(params.page) : 1
        def rowsNumber = params.rows ? Integer.valueOf(params.rows): 1
        def numberOfPages = Math.ceil(jsonCells.totalCount / rowsNumber)

        def jsonData = [rows: jsonCells, page: currentPage, records: jsonCells.totalCount, total: numberOfPages]

        jsonData
    }

	/*
	 * Filter the process results based on the parameter filter values
	 */
	private def getProcesses(filters, GrailsParameterMap params) {
		params.max = (params?.max?.toInteger()) ?: pagination.max
		params.offset = (params?.offset?.toInteger()) ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order
		def company_id = session['company_id']
		return BillingProcessDTO.createCriteria().list(
			max:    params.max,
			offset: params.offset
			) {
				and {
					filters.each { filter ->
						if (filter.value) {
							addToCriteria(filter.getRestrictions());
						}
					}
					eq('entity', new CompanyDTO(session['company_id']))
                    if (params.billingId){
                        eq('id', params.getInt('billingId'))
                    }
                }

                // apply sorting
                SortableCriteria.sort(params, delegate)
			}
	}

	/*
	 * To display the run details of a given Process Id
	 */
	def show () {
        Integer processId = params.int('id')

        if ( !BillingProcessDTO.exists( processId ) ) {
            flash.error = 'billing.process.review.doesnotexist'
            flash.args = [processId]
            redirect action:'list'
        }

        // get billing process record
        def process = BillingProcessDTO.get(processId)
        def configuration = BillingProcessConfigurationDTO.findByEntity(new CompanyDTO(session['company_id']))

        // main billing process run (not a retry!)
        def processRuns = process?.processRuns?.asList()?.sort{ it.started }
        def processRun =  processRuns?.size() > 0 ? processRuns.first() : null 

        // all payments made to generated invoices between process start & end
		def generatedPayments = []
		if (processRun) {
        	generatedPayments = new PaymentDAS().findBillingProcessGeneratedPayments(processId, processRun.started, processRun.finished)
		}
        // all payments made to generated invoice after the process end
        def invoicePayments = []
		if (processRun) {
			invoicePayments = new PaymentDAS().findBillingProcessPayments(processId, processRun.finished)
		}

        // all invoices for the billing process. Avoiding using the associations
        def invoices = process ? new InvoiceDAS().findByProcess(process) : [] 

		recentItemService.addRecentItem(processId, RecentItemType.BILLINGPROCESS)
		breadcrumbService.addBreadcrumb(controllerName, actionName, null, processId)
		
		def jobs = new BatchProcessInfoBL().findByBillingProcessId(processId)
		def canRestart = false
		if(jobs!=null) {
			def job = jobs.iterator().next()
			canRestart = job.getTotalFailedUsers()>0 ? true : false
		}
		
        [ process: process, processRun: processRun, generatedPayments: generatedPayments, invoicePayments: invoicePayments, configuration: configuration, invoices: invoices,
          formattedPeriod: getFormattedPeriod(process?.periodUnit.id, process?.periodValue, session['language_id']), jobs:jobs, canRestart:canRestart]
	}
	
	def failed (){
		def users = new BillingProcessFailedUserBL().getUsersByExecutionId(params.int('id'))
		
		[users : users]
	}

	
	private String getFormattedPeriod(Integer periodUnitId, Integer periodValue, Integer languageId) {
		String periodUnitStr = Util.getPeriodUnitStr(periodUnitId, languageId)
		return periodValue + ServerConstants.SINGLE_SPACE + periodUnitStr;
	}

	def showInvoices () {
		redirect controller: 'invoice', action: 'byProcess', id: params.id, params: [ isReview : params.isReview ]
	}
	
	def showOrders () {
        redirect controller: 'order', action: 'byProcess', params: [processId: params.id]
	}
	
	def restart () {
		new BatchContextHandler().restartFailedJobByBillingProcessId(params.int('id'),session['company_id'])
		redirect controller: 'billing', action: 'show', id: params.id
	}

	def approve () {
		try {
			webServicesSession.setReviewApproval(Boolean.TRUE)
		} catch (Exception e) {
			throw new SessionInternalError(e)
		}
		flash.message = 'billing.review.approve.success'
		redirect action: 'list'
	}

	def disapprove () {
		try {
			webServicesSession.setReviewApproval(Boolean.FALSE)
		} catch (Exception e) {
			throw new SessionInternalError(e)
		}
		flash.message = 'billing.review.disapprove.success'
		redirect action: 'list'
	}
}
