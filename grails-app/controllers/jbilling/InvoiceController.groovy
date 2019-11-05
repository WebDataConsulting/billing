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

import com.sapienter.jbilling.client.util.DownloadHelper
import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.common.Util
import com.sapienter.jbilling.server.invoice.InvoiceBL
import com.sapienter.jbilling.server.invoice.InvoiceWS
import com.sapienter.jbilling.server.invoice.PaperInvoiceBatchBL
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO
import com.sapienter.jbilling.server.invoice.db.InvoiceStatusDAS
import com.sapienter.jbilling.server.item.CurrencyBL
import com.sapienter.jbilling.server.metafields.DataType
import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.server.metafields.MetaFieldBL
import com.sapienter.jbilling.server.metafields.db.MetaField
import com.sapienter.jbilling.server.metafields.db.value.IntegerMetaFieldValue
import com.sapienter.jbilling.server.metafields.db.value.StringMetaFieldValue
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.csv.CsvExporter
import com.sapienter.jbilling.server.util.csv.Exporter
import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import org.apache.commons.io.IOUtils
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.hibernate.FetchMode
import org.hibernate.criterion.*

/**
 * InvoiceController
 *
 * @author Vikas Bodani
 * @since
 */
@Secured(["isAuthenticated()"])
class InvoiceController {
	static scope = "prototype"
    static pagination = [max: 10, offset: 0, sort: 'id', order: 'desc']
    static versions = [ max: 25 ]

    // Matches the columns in the JQView grid with the corresponding field
    static final viewColumnsToFields =
            ['userName': 'baseUser.userName',
             'company': 'company.description',
             'invoiceId': 'id',
             'dueDate': 'dueDate',
             'status': 'invoiceStatus',
             'amount':'total',
             'balance':'balance']

    IWebServicesSessionBean webServicesSession
    def viewUtils
    def filterService
    def breadcrumbService
    def subAccountService
    def recentItemService

    def auditBL

    def index () {
        list()
    }

    def list () {
        def filters = filterService.getFilters(FilterType.INVOICE, params)
        def invoiceIds = parameterIds

        def selected = params.id ? InvoiceDTO.get(params.int('id')) : null

		if (selected) {
			def hierachyEntityIds = retrieveCompanies()*.id
			
			if ( hierachyEntityIds && !hierachyEntityIds.contains(selected?.baseUser?.company?.id) ) {
				selected= null
			}
		}

        breadcrumbService.addBreadcrumb(controllerName, 'list', null, selected?.id)

        // if id is present and invoice not found, give an error message along with the list
        if (params.id?.isInteger() && selected == null) {
            flash.error = 'validation.error.company.hierarchy.invalid.invoiceid'
            flash.args = [params.id]
        }

        def contactFieldTypes = params['contactFieldTypes']

        def invoices;
        if (selected) {
            invoices = getInvoicesWithSelected(filters, invoiceIds, selected)
        } else {
            invoices = getInvoices(filters, params, invoiceIds)
        }

        if (params.applyFilter || params.partial) {
            render template: 'invoices', model: [invoices: invoices, filters: filters, selected: selected, currencies: retrieveCurrencies(), contactFieldTypes: contactFieldTypes]
        } else {
            def lines = null
            if(selected){
                InvoiceBL invoiceBl = new InvoiceBL(selected);
                InvoiceDTO invoiceDto = invoiceBl.getInvoiceDTOWithHeaderLines();
                lines = invoiceDto.getInvoiceLines();
            }

            render view: 'list', model: [invoices: invoices, filters: filters, selected: selected, currencies: retrieveCurrencies(), lines: lines]
        }
    }

    def findInvoices (){
        def filters = filterService.getFilters(FilterType.INVOICE, params)
        def invoiceIds = parameterIds

        params.sort = viewColumnsToFields[params.sidx]
        params.order  = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page')-1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def selected = params.id ? InvoiceDTO.get(params.int('id')) : null
        def invoices;
        if (selected) {
            invoices = getInvoicesWithSelected(filters, invoiceIds, selected)
        } else {
            invoices = getInvoices(filters, params, invoiceIds)
        }

        try {
            render getInvoicesJsonData(invoices, params) as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }

    }

    /**
     * Converts Invoices to JSon
     */
    private def Object getInvoicesJsonData(invoices, GrailsParameterMap params) {
        def jsonCells = invoices
        def currentPage = params.page ? Integer.valueOf(params.page) : 1
        def rowsNumber = params.rows ? Integer.valueOf(params.rows): 1
        def numberOfPages = Math.ceil(jsonCells.totalCount / rowsNumber)

        def jsonData = [rows: jsonCells, page: currentPage, records: jsonCells.totalCount, total: numberOfPages]

        jsonData
    }

    def getInvoicesWithSelected(filters, invoiceIds, selected) {
        def idFilter = new Filter(type: FilterType.ALL, constraintType: FilterConstraint.EQ,
                field: 'id', template: 'id', visible: true, integerValue: selected.id)
        getInvoices([idFilter], params, invoiceIds)
    }



    def getInvoices(filters, params, ids) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order

        // hide review invoices by default
        def reviewFilter = filters.find { it.field == 'isReview' }
        if (reviewFilter && reviewFilter.value == null) {
            reviewFilter.integerValue = Integer.valueOf(0)
        }

        // get list
		def company_id = session['company_id']
        return InvoiceDTO.createCriteria().list(
                max: params.max,
                offset: params.offset
        ) {
            and {
                filters.each { filter ->
                    if (filter.value != null) {
                        //handle invoiceStatus
                        if (filter.field == 'invoiceStatus') {
                            def statuses = new InvoiceStatusDAS().findAll()
                            eq("invoiceStatus", statuses.find { it.primaryKey?.equals(filter.integerValue) })
                        } else if (filter.field == 'contact.fields') {
                            String typeId = params['contact.fields.fieldKeyData']?params['contact.fields.fieldKeyData']:filter.fieldKeyData
                            String ccfValue = filter.stringValue;
                            log.debug "Contact Field Type ID: ${typeId}, CCF Value: ${ccfValue}"
                            
                            if (typeId && ccfValue) {
                                MetaField type = findMetaFieldType(typeId.toInteger());
                                if (type != null) {
                                    createAlias("metaFields", "fieldValue")
                                    createAlias("fieldValue.field", "type")
                                    setFetchMode("type", FetchMode.JOIN)
                                    eq("type.id", typeId.toInteger())

                                    switch (type.getDataType()) {
                                        case DataType.STRING:
                                        	def subCriteria = DetachedCriteria.forClass(StringMetaFieldValue.class, "stringValue")
                                        					.setProjection(Projections.property('id'))
										    				.add(Restrictions.like('stringValue.value', ccfValue + '%').ignoreCase())
     
                                        	addToCriteria(Property.forName("fieldValue.id").in(subCriteria))
                                            break;
                                        case DataType.INTEGER:
                                        	def subCriteria = DetachedCriteria.forClass(IntegerMetaFieldValue.class, "integerValue")
                                        					.setProjection(Projections.property('id'))
										    				.add(Restrictions.eq('integerValue.value', ccfValue.toInteger()))
     
                                        	addToCriteria(Property.forName("fieldValue.id").in(subCriteria))
                                            break;
                                        case DataType.ENUMERATION:
                                        case DataType.JSON_OBJECT:
                                            addToCriteria(Restrictions.ilike("fieldValue.value", ccfValue, MatchMode.ANYWHERE))
                                            break;
                                        default:
                                        // todo: now searching as string only, search for other types is impossible
//                                            def fieldValue = type.createValue();
//                                            bindData(fieldValue, ['value': ccfValue])
//                                            addToCriteria(Restrictions.eq("fieldValue.value", fieldValue.getValue()))

                                            addToCriteria(Restrictions.eq("fieldValue.value", ccfValue))
                                            break;
                                    }

                                }
                            }
                        } else if(filter.field == 'u.company.description') {
							eq('baseUser.company', CompanyDTO.findByDescriptionIlike('%' + filter.stringValue + '%'))
                        } else {
                            addToCriteria(filter.getRestrictions());
                        }
                    }
                }

                createAlias('baseUser', 'baseUser')
				//invoices of parent + child companies
				'in'('baseUser.company', retrieveCompanies())
                eq('deleted', 0)

                if (ids) {
                    'in'('id', ids.toArray(new Integer[ids.size()]))
                }

                if(params.company) {
                    eq('baseUser.company', CompanyDTO.findByDescriptionIlike('%' + params.company + '%'))
                }
                if (params.userName) {
                    addToCriteria(Restrictions.ilike('baseUser.userName', params.userName, MatchMode.ANYWHERE))
                }
                if(params.invoiceId) {
                    or {
                        eq('publicNumber', params.get('invoiceId'))
                        if (params.invoiceId.isInteger()) {
                            eq('id', params.int('invoiceId'))
                        }
                    }
                }
            }

            // apply sorting
            SortableCriteria.sort(params, delegate)
        }
    }

    /**
     * Applies the set filters to the order list, and exports it as a CSV for download.
     */
    def csv () {
        def filters = filterService.getFilters(FilterType.INVOICE, params)

        params.sort = viewColumnsToFields[params.sidx] != null ? viewColumnsToFields[params.sidx] : params.sort
        params.order = params.sord
        params.max = CsvExporter.MAX_RESULTS

        def invoices = getInvoices(filters, params, null)
        renderCsvFor(invoices)
    }

    /**
     * Applies the set filters to the order list, and exports it as a CSV for download.
     */
    @Secured(["INVOICE_73"])
    def csvByProcess (){
        params.sort = viewColumnsToFields[params.sidx] != null ? viewColumnsToFields[params.sidx] : params.sort
        params.order = params.sord
        params.max = CsvExporter.MAX_RESULTS

        // limit by billing process
        def processFilter = new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.EQ, field: 'billingProcess.id', template: 'id', visible: true, integerValue: params.int('id'))
        filterService.setFilter(FilterType.INVOICE, processFilter)

        // show review invoices if process generated a review
        def reviewFilter = new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.EQ, field: 'isReview', template: 'invoice/review', visible: true, integerValue: params.int('isReview'))
        filterService.setFilter(FilterType.INVOICE, reviewFilter, false)

        def filters = filterService.getFilters(FilterType.INVOICE, params)

        def invoices = getInvoices(filters, params, null)
        renderCsvFor(invoices)
    }

    def renderCsvFor(invoices) {
        if (invoices.totalCount > CsvExporter.MAX_RESULTS) {
            flash.error = message(code: 'error.export.exceeds.maximum')
            flash.args = [CsvExporter.MAX_RESULTS]
            redirect action: 'list', id: params.id

        } else {
            DownloadHelper.setResponseHeader(response, "invoices.csv")
            Exporter<InvoiceDTO> exporter = CsvExporter.createExporter(InvoiceDTO.class);
            render text: exporter.export(invoices), contentType: "text/csv"
        }
    }

    def batchPdf (){
        def filters = filterService.getFilters(FilterType.INVOICE, params)

        params.sort = viewColumnsToFields[params.sidx] != null ? viewColumnsToFields[params.sidx] : params.sort
        params.order = params.sord
        params.max = PaperInvoiceBatchBL.MAX_RESULTS

        def invoices = getInvoices(filters, params, null)

        if (invoices.totalCount > PaperInvoiceBatchBL.MAX_RESULTS) {
            flash.error = message(code: 'error.export.exceeds.maximum')
            flash.args = [PaperInvoiceBatchBL.MAX_RESULTS]
            redirect action: 'list', id: params.id
        } else {
            try {
                PaperInvoiceBatchBL paperInvoiceBatchBL = new PaperInvoiceBatchBL();
                String fileName = paperInvoiceBatchBL.generateBatchPdf((List<InvoiceDTO>) invoices, (Integer) session['company_id'])
                String realPath = Util.getSysProp("base_dir") + "invoices" + File.separator;
                File file = new File(realPath+fileName)
                FileInputStream fileInputStream = new FileInputStream(file)
                byte[] pdfBytes = IOUtils.toByteArray(fileInputStream)
                DownloadHelper.sendFile(response, fileName, "application/pdf", pdfBytes)
            } catch (SessionInternalError e) {
                viewUtils.resolveException(flash, session.locale, e)
                redirect action: 'list', id: params.id
            } catch (Exception e) {
                log.error("Exception fetching PDF invoice data.", e)
                flash.error = 'invoice.prompt.failure.downloadPdf'
                redirect action: 'list', id: params.id
            }
        }
    }

    /**
     * Convenience shortcut, this action shows all invoices for the given user id.
     */
    def user () {
        def filter = new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.EQ, field: 'baseUser.id', template: 'id', visible: true, integerValue: params.int('id'))
        filterService.setFilter(FilterType.INVOICE, filter)

        redirect action: 'list'
    }

    def show () {
        def invoice = InvoiceDTO.get(params.int('id'))
        if (!invoice) {
            log.debug("Redirecting to list")
            redirect(action: 'list')
            return
        }

        breadcrumbService.addBreadcrumb(controllerName, 'list', null, invoice.id, invoice.number)

        InvoiceBL invoiceBl = new InvoiceBL((InvoiceDTO)invoice);
        InvoiceDTO invoiceDto = invoiceBl.getInvoiceDTOWithHeaderLines();
        List<InvoiceLineDTO> lines = invoiceDto.getInvoiceLines();

        render template: params.template ?: 'show', model: [selected: invoice, currencies: retrieveCurrencies(), lines : lines]
    }

    def snapshot () {
        def invoiceId = params.int('id')
        if (invoiceId) {
            InvoiceWS invoice = webServicesSession.getInvoiceWS(invoiceId)
            render template: 'snapshot', model: [ invoice: invoice, currencies: retrieveCurrencies(), availableMetaFields: retrieveAvailableMetaFields() ]
        }
    }

    def delete () {
        int invoiceId = params.int('id')

        if (invoiceId) {
            try {
                webServicesSession.deleteInvoice(invoiceId)
                flash.message = 'invoice.delete.success'
                flash.args = [invoiceId]

            } catch (SessionInternalError e) {
                viewUtils.resolveException(flash, session.locale, e);
            } catch (Exception e) {
                log.error("Exception deleting invoice.", e)
                flash.error = 'error.invoice.delete'
                flash.args = [params.id]
                redirect action: 'list', params: [id: invoiceId]
                return
            }
        }

        redirect action: 'list'
    }

    def email () {
        if (params.id) {
            try {
                def sent = webServicesSession.notifyInvoiceByEmail(params.int('id'))

                if (sent) {
                    flash.message = 'invoice.prompt.success.email.invoice'
                    flash.args = [params.id]
                } else {
                    flash.error = 'invoice.prompt.failure.email.invoice'
                    flash.args = [params.id]
                }

            } catch (SessionInternalError sie) {
                log.error("Exception occurred sending invoice email", sie)
                viewUtils.resolveException(flash, session.locale, sie)
            }
        }

        redirect action: 'list', params: [id: params.id]
    }

    def downloadPdf () {
        Integer invoiceId = params.int('id')

        try {
            byte[] pdfBytes = webServicesSession.getPaperInvoicePDF(invoiceId)
            def invoice = webServicesSession.getInvoiceWS(invoiceId)
            DownloadHelper.sendFile(response, "invoice-${invoice?.number}.pdf", "application/pdf", pdfBytes)

        }catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            redirect action: 'list', params: [id: invoiceId]
        } catch (Exception e) {
            log.error("Exception fetching PDF invoice data.", e)
            flash.error = 'invoice.prompt.failure.downloadPdf'
            redirect action: 'list', params: [id: params.id]
        }
    }

    def unlink () {
        try {
            webServicesSession.removePaymentLink(params.int('id'), params.int('paymentId'))
            flash.message = "payment.unlink.success"

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);

        } catch (Exception e) {
            log.error("Exception unlinking invoice.", e)
            flash.error = "error.invoice.unlink.payment"
        }

        redirect action: 'list', params: [id: params.id]
    }

    def findByProcess () {
        if (!params.id) {
            flash.error = 'error.invoice.byprocess.missing.id'
            chain action: 'list'
            return
        }

        params.sort = viewColumnsToFields[params.sidx]
        params.order  = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page')-1) * params.int('rows') : 0

        // limit by billing process
        def processFilter = new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.EQ, field: 'billingProcess.id', template: 'id', visible: true, integerValue: params.int('id'))
        filterService.setFilter(FilterType.INVOICE, processFilter)

        // show review invoices if process generated a review
        def reviewFilter = new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.EQ, field: 'isReview', template: 'invoice/review', visible: true, integerValue: params.int('isReview'))
        filterService.setFilter(FilterType.INVOICE, reviewFilter, false)

        def filters = filterService.getFilters(FilterType.INVOICE, params)
        def invoices = getInvoices(filters, params, null)

        try {
            render getInvoicesJsonData(invoices, params) as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }
    }

    def byProcess () {
        if (!params.id) {
            flash.error = 'error.invoice.byprocess.missing.id'
            chain action: 'list'
            return
        }

        // limit by billing process
        def processFilter = new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.EQ, field: 'billingProcess.id', template: 'id', visible: true, integerValue: params.int('id'))
        filterService.setFilter(FilterType.INVOICE, processFilter)

        // show review invoices if process generated a review
        def reviewFilter = new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.EQ, field: 'isReview', template: 'invoice/review', visible: true, integerValue: params.int('isReview'))
        filterService.setFilter(FilterType.INVOICE, reviewFilter, false)

        def filters = filterService.getFilters(FilterType.INVOICE, params)
        def invoices = getInvoices(filters, params, null)

        render view: 'list', model: [invoices: invoices, filters: filters, currencies: retrieveCurrencies()]

    }

    def retrieveCurrencies() {
		//in this controller we need only currencies objects with inUse=true without checking rates on date
        return new CurrencyBL().getCurrenciesWithoutRates(session['language_id'].toInteger(), session['company_id'].toInteger(),true)
    }

	def retrieveCompanies() {
		def parentCompany = CompanyDTO.get(session['company_id'])
		def childs = CompanyDTO.findAllByParent(parentCompany)
		childs.add(parentCompany)
		return childs;
	}
	
    def getAvailableMetaFields() {
        return MetaFieldBL.getAvailableFieldsList(session["company_id"], EntityType.INVOICE);
    }

    def getParameterIds() {

        // Grails bug when using lists with <g:remoteLink>
        // http://jira.grails.org/browse/GRAILS-8330
        // TODO (pai) remove workaround

        def parameterIds = new ArrayList<Integer>()
        def idParamList = params.list('ids')
        idParamList.each { idParam ->
            if (idParam?.isInteger()) {
                parameterIds.add(idParam.toInteger())
            }
        }
        if (parameterIds.isEmpty()) {
            String ids = params.ids
            if (ids) {
                ids = ids.replace('[', "").replace(']', "")
                String [] numbers = ids.split(", ")
                numbers.each { paramId ->
                    if (paramId?.isInteger()) {
                        parameterIds.add(paramId.toInteger());
                    }
                }
            }
        }

        return parameterIds;
    }
	
    def retrieveAvailableMetaFields() {
        return MetaFieldBL.getAvailableFieldsList(session["company_id"], EntityType.INVOICE);
    }
    
    def findMetaFieldType(Integer metaFieldId) {
        for (MetaField field : retrieveAvailableMetaFields()) {
            if (field.id == metaFieldId) {
                return field;
            }
        }
        return null;
    }

    @Secured(["INVOICE_72"])
    def history (){
        def invoice = InvoiceDTO.get(params.int('id'))

        def currentInvoice = auditBL.getColumnValues(invoice)
        def invoiceVersions = auditBL.get(InvoiceDTO.class, invoice.getAuditKey(invoice.id), versions.max)
        def lines = auditBL.find(InvoiceLineDTO.class, getInvoiceLineSearchPrefix(invoice))

        def records = [
                [ name: 'invoice', id: invoice.id, current: currentInvoice, versions:  invoiceVersions ]
        ]

        render view: '/audit/history', model: [ records: records, historyid: invoice.id, lines: lines, linecontroller: 'invoice', lineaction: 'linehistory' ]
    }

    def getInvoiceLineSearchPrefix(invoice) {
        return "${invoice.baseUser.company.id}-usr-${invoice.baseUser.id}-inv-${invoice.id}-"
    }

    @Secured(["INVOICE_72"])
    def linehistory (){
        def line = InvoiceLineDTO.get(params.int('id'))

        def currentLine = auditBL.getColumnValues(line)
        def lineVersions = auditBL.get(InvoiceLineDTO.class, line.getAuditKey(line.id), versions.max)

        def records = [
                [ name: 'line', id: line.id, current: currentLine, versions: lineVersions ]
        ]

        render view: '/audit/history', model: [ records: records, historyid: line.invoice.id ]
    }

}
