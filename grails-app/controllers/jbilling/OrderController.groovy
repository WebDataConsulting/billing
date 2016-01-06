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
import com.sapienter.jbilling.client.util.Constants
import com.sapienter.jbilling.client.util.DownloadHelper
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.customer.CustomerBL
import com.sapienter.jbilling.server.invoice.InvoiceBL
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS
import com.sapienter.jbilling.server.item.CurrencyBL
import com.sapienter.jbilling.server.metafields.DataType
import com.sapienter.jbilling.server.metafields.db.MetaField
import com.sapienter.jbilling.server.metafields.db.value.IntegerMetaFieldValue
import com.sapienter.jbilling.server.metafields.db.value.StringMetaFieldValue
import com.sapienter.jbilling.server.order.OrderBL
import com.sapienter.jbilling.server.order.OrderWS
import com.sapienter.jbilling.server.order.db.OrderDAS
import com.sapienter.jbilling.server.order.db.OrderDTO
import com.sapienter.jbilling.server.order.db.OrderPeriodDAS
import com.sapienter.jbilling.server.order.db.OrderProcessDAS
import com.sapienter.jbilling.server.order.db.OrderStatusDAS
import com.sapienter.jbilling.server.user.UserWS
import com.sapienter.jbilling.server.user.db.CustomerDTO
import com.sapienter.jbilling.server.user.db.UserDAS
import com.sapienter.jbilling.server.user.db.UserDTO
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.PreferenceBL
import com.sapienter.jbilling.server.util.csv.CsvExporter
import com.sapienter.jbilling.server.util.csv.Exporter
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.partner.db.PartnerDTO;
import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap

import org.hibernate.FetchMode
import org.hibernate.criterion.MatchMode
import org.hibernate.criterion.Restrictions
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.Property
import org.hibernate.criterion.Projections
import org.hibernate.Criteria
import com.sapienter.jbilling.client.util.SortableCriteria
import grails.plugin.springsecurity.SpringSecurityUtils
import com.sapienter.jbilling.server.invoice.InvoiceWS
import com.sapienter.jbilling.server.metafields.MetaFieldBL
import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO


/**
 *
 * @author vikas bodani
 * @since 20-Jan-2011
 *
 */

@Secured(["isAuthenticated()"])
class OrderController {
	static scope = "prototype"
	static pagination = [ max: 10, offset: 0, sort: 'id', order: 'desc' ]
	IWebServicesSessionBean webServicesSession
	def viewUtils
	def filterService
	def recentItemService
	def breadcrumbService
	def subAccountService
	def auditBL
	def index () {
		list()
	}
	def getFilteredOrders(filters, params, ids) {
		params.max = params?.max?.toInteger() ?: pagination.max
		params.offset = params?.offset?.toInteger() ?: pagination.offset
		params.sort = params?.sort ?: pagination.sort
		params.order = params?.order ?: pagination.order

		def user_id = session['user_id']
		def partnerDtos = PartnerDTO.createCriteria().list(){
			eq('baseUser.id', session['user_id'])
		}
		log.debug "### partner:"+partnerDtos
		def customersForUser = new ArrayList()
		if(partnerDtos.size>0){
			customersForUser = CustomerDTO.createCriteria().list(){ 'in'('partner', partnerDtos) }
		}
		log.debug "### customersForUser:"+customersForUser
		def company_id = session['company_id']
		return OrderDTO.createCriteria().list(
		max: params.max,
		offset: params.offset
		) {
			createAlias('baseUserByUserId', 'u', Criteria.LEFT_JOIN)
			and {
				filters.each { filter ->
					if (filter.value) {
						//handle orderStatus & orderPeriod separately
						if (filter.constraintType == FilterConstraint.STATUS) {
							if (filter.field == 'orderStatus') {
								def statuses = new OrderStatusDAS().findAll()
								eq("orderStatus", statuses.find{ it.id == filter.integerValue })
							} else if (filter.field == 'orderPeriod') {
								def periods = new OrderPeriodDAS().findAll()
								eq("orderPeriod", periods.find{ it.id == filter.integerValue })
							}
						} else if (filter.field == 'contact.fields') {
							String typeId = params['contactFieldTypes']
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
										// def fieldValue = type.createValue();
										// bindData(fieldValue, ['value': ccfValue])
										// addToCriteria(Restrictions.eq("fieldValue.value", fieldValue.getValue()))
											addToCriteria(Restrictions.eq("fieldValue.value", ccfValue))
											break;
									}
								}
							}
						} else if (filter.field == 'orderProcesses.billingProcess.id') {
							List<Integer> orderProcessIds = new OrderProcessDAS().findByBillingProcess(filter.integerValue)
							createAlias("orderProcesses", "op")
							'in'('op.id',orderProcessIds)
						} else{
							addToCriteria(filter.getRestrictions());
						}
					}
				}
				eq('u.company', new CompanyDTO(company_id))
				eq('deleted', 0)
				if (ids) {
					'in'('id', ids.toArray(new Integer[ids.size()]))
				}
			}
			// apply sorting
			SortableCriteria.sort(params, delegate)
		}
    }

    def list () {
        def filters = filterService.getFilters(FilterType.ORDER, params)

        def orderIds = parameterIds

        log.debug(" ### filters: "+filters)        

        def selected = params.id ? webServicesSession.getOrder(params.int("id")) : null
        def orders = getFilteredOrders(filters, params, orderIds)
        def user = selected ? webServicesSession.getUserWS(selected.userId) : null

        breadcrumbService.addBreadcrumb(controllerName, 'list', null, selected?.id)

        // if the id exists and is valid and there is no record persisted for that id, write an error message
        if(params.id?.isInteger() && selected == null){
            flash.error = message(code: 'flash.order.not.found')
        }

        if (params.applyFilter || params.partial) {
            render template: 'orders', model: [ orders: orders, order: selected, user: user, currencies: retrieveCurrencies(), filters: filters, ids: params.ids, children: childrenMap(orders) ]
        } else {
			PeriodUnitDTO periodUnit = selected?.dueDateUnitId ? PeriodUnitDTO.get(selected.dueDateUnitId) : null
            render view: 'list', model: [ orders: orders, order: selected, user: user, currencies: retrieveCurrencies(), filters: filters, ids: params.ids, periodUnit: periodUnit, children: childrenMap(orders) ]
        }
    }

    def getSelectedOrder(selected, orderIds) {
        def idFilter = new Filter(type: FilterType.ALL, constraintType: FilterConstraint.EQ,
                field: 'id', template: 'id', visible: true, integerValue: selected.id)
        getFilteredOrders([idFilter], params, orderIds)
    }

    def show () {
        OrderWS order = webServicesSession.getOrder(params.int('id'))
        UserWS user = webServicesSession.getUserWS(order.getUserId())

        breadcrumbService.addBreadcrumb(controllerName, 'list', null, order.id)
        recentItemService.addRecentItem(order.id, RecentItemType.ORDER)

        PeriodUnitDTO periodUnit = order.dueDateUnitId ? PeriodUnitDTO.get(order.dueDateUnitId) : null

        render template:'show', model: [order: order, user: user, currencies: retrieveCurrencies(), periodUnit: periodUnit]
    }

    /**
     * Applies the set filters to the order list, and exports it as a CSV for download.
     */
    def csv () {
        def filters = filterService.getFilters(FilterType.ORDER, params)

        params.max = CsvExporter.MAX_RESULTS

        def orderIds = parameterIds
        def orders = getFilteredOrders(filters, params, orderIds)

        if (orders.totalCount > CsvExporter.MAX_RESULTS) {
            flash.error = message(code: 'error.export.exceeds.maximum')
            flash.args = [ CsvExporter.MAX_RESULTS ]
            redirect action: 'list', id: params.id

        } else {
            DownloadHelper.setResponseHeader(response, "orders.csv")
            Exporter<OrderDTO> exporter = CsvExporter.createExporter(OrderDTO.class);
            render text: exporter.export(orders), contentType: "text/csv"
        }
    }

    /**
     * Convenience shortcut, this action shows all invoices for the given user id.
     */
    def user () {
        def filter = new Filter(type: FilterType.ORDER, constraintType: FilterConstraint.EQ, field: 'baseUserByUserId.id', template: 'id', visible: true, integerValue: params.int('id'))
        filterService.setFilter(FilterType.ORDER, filter)
        redirect action: 'list'
    }

    def generateInvoice () {
        log.debug "generateInvoice for order ${params.id}"

        def orderId = params.id?.toInteger()

        Integer invoiceID= null;
        try {
            invoiceID = webServicesSession.createInvoiceFromOrder(orderId, null)

        } catch (SessionInternalError e) {
            flash.error= 'order.error.generating.invoice'
            redirect action: 'list', params: [ id: params.id ]
            return
        }

        if ( null != invoiceID) {
            flash.message ='order.geninvoice.success'
            flash.args = [orderId]
            redirect controller: 'invoice', action: 'list', params: [id: invoiceID]

        } else {
            flash.error ='order.error.geninvoice.inactive'
            redirect action: 'list', params: [ id: params.id ]
        }
    }

    def applyToInvoice () {
        def invoices = getApplicableInvoices(params.int('userId'))

        if (!invoices || invoices.size() == 0) {
            flash.error = 'order.error.invoices.not.found'
            flash.args = [params.userId]
            redirect (action: 'list', params: [ id: params.id ])
        }

        session.applyToInvoiceOrderId = params.int('id')
        [ invoices:invoices, currencies: retrieveCurrencies(), orderId: params.id ]
    }

    def apply () {
        def order =  new OrderDAS().find(params.int('id'))
        if (!order.getStatusId().equals(Constants.ORDER_STATUS_ACTIVE)) {
            flash.error = 'order.error.status.not.active'
        }

        // invoice with meta fields
        def invoiceTemplate = new InvoiceWS()
        bindData(invoiceTemplate, params, 'invoice')

        def invoiceMetaFields = retrieveInvoiceMetaFields();
        def fieldsArray = MetaFieldBindHelper.bindMetaFields(invoiceMetaFields, params);
        invoiceTemplate.metaFields = fieldsArray.toArray(new MetaFieldValueWS[fieldsArray.size()])

        // apply invoice to order.
        try {
            def invoice = webServicesSession.applyOrderToInvoice(order.getId(), invoiceTemplate)
            if (!invoice) {
                flash.error = 'order.error.apply.invoice'
                render view: 'applyToInvoice', model: [ invoice: invoice, invoices: getApplicableInvoices(params.int('userId')), currencies:retrieveCurrencies(), availableMetaFields: invoiceMetaFields, fieldsArray: fieldsArray ]
                return
            }

            flash.message = 'order.succcessfully.applied.to.invoice'
            flash.args = [params.id, invoice]

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);

            def invoice = webServicesSession.getInvoiceWS(params.int('invoice.id'))
            def invoices = getApplicableInvoices(params.int('userId'))
            render view: 'applyToInvoice', model: [ invoice: invoice, invoices: invoices, currencies:retrieveCurrencies(), availableMetaFields: invoiceMetaFields, fieldsArray: fieldsArray ]
            return
        }

        redirect action: 'list', params: [ id: params.id ]
    }

    def getApplicableInvoices(Integer userId) {

        CustomerDTO payingUser
        Integer _userId
        UserDTO user= new UserDAS().find(userId)
        if (user.getCustomer()?.getParent()) {
            payingUser= new CustomerBL(user.getCustomer().getId()).getInvoicableParent()
            _userId=payingUser.getBaseUser().getId()
        } else {
            _userId= user.getId()
        }
        InvoiceDAS das= new InvoiceDAS()
        List invoices =  new ArrayList()
        for (Iterator it= das.findAllApplicableInvoicesByUser(_userId ).iterator(); it.hasNext();) {
            invoices.add InvoiceBL.getWS(das.find (it.next()))
        }

        log.debug "Found ${invoices.size()} for user ${_userId}"

        invoices as List
    }


    def retrieveInvoiceMetaFields() {
        return MetaFieldBL.getAvailableFieldsList(session["company_id"], EntityType.INVOICE);
    }

    def retrieveCurrencies() {
		//in this controller we need only currencies objects with inUse=true without checking rates on date
        return new CurrencyBL().getCurrenciesWithoutRates(session['language_id'].toInteger(), session['company_id'].toInteger(),true)
    }

    def byProcess () {
        // limit by billing process
        def processFilter = new Filter(type: FilterType.ORDER, constraintType: FilterConstraint.EQ, field: 'orderProcesses.billingProcess.id', template: 'id', visible: true, integerValue: params.int('processId'))
        filterService.setFilter(FilterType.ORDER, processFilter)

        def filters = filterService.getFilters(FilterType.ORDER, params)

        def orders = getFilteredOrders(filters, params, null)

        render view: 'list', model: [orders: orders, filters: filters]
    }
	
    def deleteOrder () {
        try {
            webServicesSession.deleteOrder(params.int('id'))
            flash.message = 'order.delete.success'
            flash.args = [params.id, params.id]
        } catch (SessionInternalError e){
            flash.error ='order.error.delete'
            viewUtils.resolveException(flash, session.locale, e);
        } catch (Exception e) {
            log.error e
            flash.error= e.getMessage()
        }
        redirect action: 'list'
    }
    
    def retrieveAvailableMetaFields() {
        return MetaFieldBL.getAvailableFieldsList(session["company_id"], EntityType.ORDER);
    }

    def findMetaFieldType(Integer metaFieldId) {
        for (MetaField field : retrieveAvailableMetaFields()) {
            if (field.id == metaFieldId) {
                return field;
            }
        }
        return null;
    }

	
	def childrenMap(orders) {
		if (!orders) return [:]
		def queryResults = OrderDTO.executeQuery(
			   "select ord.parentOrder.id as id, count(*) as childCount from ${OrderDTO.class.getSimpleName()} ord " +
			   " where ord.parentOrder.id in (:orderIds) and ord.deleted = 0 group by ord.parentOrder.id ",
				[orderIds: orders.collect { it.id }]
		)
		def results = [:];
		queryResults.each({ record -> results.put(record[0], record[1]) })
		return results;
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

    def retrieveCompanyStatuses (){
        return webServicesSession.getOrderChangeStatusesForCompany() as List
    }
}
