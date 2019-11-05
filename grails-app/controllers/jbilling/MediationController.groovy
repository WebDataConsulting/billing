/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package jbilling
import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO
import com.sapienter.jbilling.server.mediation.db.MediationProcess
import com.sapienter.jbilling.server.mediation.db.MediationRecordDTO
import com.sapienter.jbilling.server.order.db.OrderDTO
import grails.plugin.springsecurity.annotation.Secured
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
/**
 * MediationController
 *
 * @author Vikas Bodani
 * @since 17/02/2011
 */
@Secured(["isAuthenticated()"])
class MediationController {

    static pagination = [max: 10, offset: 0, sort: 'id', order: 'desc']

    def webServicesSession
    def recentItemService
    def breadcrumbService
    def filterService
    def mediationSession

    def index() {
        list()
    }

    def list () {
        def filters = filterService.getFilters(FilterType.MEDIATIONPROCESS, params)
        List<Integer> processValues = []
        List<MediationProcess> processes = []
        (processes, processValues) = getFilteredProcesses(filters, params)

        breadcrumbService.addBreadcrumb(controllerName, actionName, null, null)

        if (params.applyFilter || params.partial) {
            render template: 'processes', model: [processes: processes, filters: filters, processValues: processValues]
        } else {
            render view: "list", model: [processes: processes, filters: filters, processValues: processValues]
        }
    }

	private def getFilteredProcesses (filters, GrailsParameterMap params) {
		params.max = (params?.max?.toInteger()) ?: pagination.max
		params.offset = (params?.offset?.toInteger()) ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order

        List<MediationProcess> processes = []
        List<Integer> processValues = []

        processes = MediationProcess.createCriteria().list(
                max: params.max,
                offset: params.offset
        ) {
            and {
                filters.each { filter ->
                    if (filter.value != null) {
                        addToCriteria(filter.getRestrictions());
                    }
                }
            }

            configuration {
                eq("entityId", session['company_id'])
            }

            // apply sorting
            SortableCriteria.sort(params, delegate)

        }

        processes.eachWithIndex { process, idx ->
            processValues[idx] = getRecordCount(process)
        }

        return [processes, processValues]
    }

    def Integer getRecordCount(MediationProcess process) {
        return MediationRecordDTO.createCriteria().get() {
            eq('process.id', process.id)

            projections {
                rowCount()
            }
        }
    }

    def show () {
        def process = MediationProcess.get(params.int('id'))
        def recordCount = getRecordCount(process)

        recentItemService.addRecentItem(process.id, RecentItemType.MEDIATIONPROCESS)
        breadcrumbService.addBreadcrumb(controllerName, actionName, null, process.id)

        if (params.template) {
            render template: params.template, model: [selected: process, recordCount: recordCount]

        } else {
            def filters = filterService.getFilters(FilterType.MEDIATIONPROCESS, params)
            def processes
            def processValues
            (processes, processValues) = getFilteredProcesses(filters, params)

            render view: 'list', model: [selected: process, recordCount: recordCount, processes: processes,
                    processValues: processValues, filters: filters]
        }
    }

    def invoice () {
        def invoiceId = params.int('id')
        def invoice = InvoiceDTO.get(invoiceId)
        def records = mediationSession.getMediationRecordLinesForInvoice(invoiceId)

        render view: 'events', model: [invoice: invoice, records: records]
    }

    def order() {

        def orderId = params.int('id')
        def order, records

        try {
            order = OrderDTO.get(orderId)
            records = mediationSession.getMediationRecordLinesForOrder(orderId)
        } catch (Exception e) {
            flash.info = message(code: 'error.mediation.events.none')
            flash.args = [params.id]
        }
        render view: 'events', model: [ order: order, records: records ]
    }

}
