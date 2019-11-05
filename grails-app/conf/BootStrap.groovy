

import com.sapienter.jbilling.client.process.JobScheduler
import com.sapienter.jbilling.common.Util
import com.sapienter.jbilling.server.discount.db.DiscountDTO
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO
import com.sapienter.jbilling.server.item.db.AssetDTO
import com.sapienter.jbilling.server.item.db.ItemDTO
import com.sapienter.jbilling.server.item.db.ItemTypeDTO
import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.server.metafields.db.MetaField
import com.sapienter.jbilling.server.metafields.db.MetaFieldGroup
import com.sapienter.jbilling.server.notification.db.NotificationMessageTypeDTO
import com.sapienter.jbilling.server.order.db.OrderDTO
import com.sapienter.jbilling.server.order.db.OrderLineTypeDTO
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO
import com.sapienter.jbilling.server.order.db.OrderStatusDTO
import com.sapienter.jbilling.server.payment.db.PaymentDTO
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDTO
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeCategoryDTO
import com.sapienter.jbilling.server.process.db.BillingProcessDTO
import com.sapienter.jbilling.server.report.db.ReportDTO
import com.sapienter.jbilling.server.report.db.ReportTypeDTO
import com.sapienter.jbilling.server.user.UserBL
import com.sapienter.jbilling.server.user.contact.db.ContactDTO
import com.sapienter.jbilling.server.user.db.AccountTypeDTO
import com.sapienter.jbilling.server.user.db.UserDTO
import com.sapienter.jbilling.server.user.partner.db.PartnerDTO
import com.sapienter.jbilling.server.user.permisson.db.RoleDTO
import com.sapienter.jbilling.server.util.ServerConstants
import com.sapienter.jbilling.server.util.Context
import com.sapienter.jbilling.server.util.db.AbstractDescription
import com.sapienter.jbilling.server.util.db.EnumerationDTO
import com.sapienter.jbilling.server.util.db.NotificationCategoryDTO
import com.sapienter.jbilling.server.util.db.PreferenceTypeDTO
import grails.converters.JSON
import grails.util.Holders
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.webflow.execution.repository.impl.DefaultFlowExecutionRepository
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

class BootStrap {
    def init = { servletContext ->

        // Setting web-flow default snapshot size to 100. The default size of the web-flow snapshot is 15 which was the cause of the issue at the time of create order. Issue #8712
        DefaultFlowExecutionRepository defaultFlowExecutionRepository=(DefaultFlowExecutionRepository)Holders.applicationContext.getBean('flowExecutionRepository');
        defaultFlowExecutionRepository.setMaxSnapshots(100)

        // schedule jbilling background processes
        def schedulerBootstrapHelper = Context.getBean("schedulerBootstrapHelper");
		if (!Util.getSysPropBooleanTrue(ServerConstants.PROPERTY_RUN_API_ONLY_BUT_NO_BATCH)) {
	        schedulerBootstrapHelper.scheduleBatchJobs();
	        schedulerBootstrapHelper.schedulePluggableTasks();
		}

        // start up the job scheduler
        JobScheduler.getInstance().start();

        registerUserDTOMarshaller()
        registerOrderDTOMarshaller()
        registerPartnerDTOMarshaller()
        registerContactDTOMarshaller()
        registerInvoiceDTOMarshaller()
        registerPaymentDTOMarshaller()
        registerBillingProcessDTOMarshaller()
        registerDiscountDTOMarshaller()
        registerItemTypeDTOMarshaller()
        registerItemDTOMarshaller()
        registerPreferenceTypeDTOMarshaller()
        registerRoleDTOMarshaller()
        registerEnumerationDTOMarshaller()
        registerNotificationCategoryDTOMarshaller()
        registerNotificationMessageTypeDTOMarshaller()
        registerAccountTypeDTOMarshaller()
        registerOrderPeriodDTOMarshaller()
        registerPaymentMethodTypeDTOMarshaller()
        registerOrderStatusDTOMarshaller()
        registerPluggableTaskTypeCategoryDTOMarshaller()
        registerPluggableTaskDTOMarshaller()
        registerReportTypeDTOMarshaller()
        registerReportDTOMarshaller()

        registerEntityTypeMarshaller()
        registerMetaFieldMarshaller()
        registerMetaFieldGroupMarshaller()
        registerDescriptionMarshaller()
        registerAssetDTOMarshaller()

    }

    def destroy = {
        // shut down the job scheduler
        JobScheduler.getInstance().shutdown();
    }

    private registerUserDTOMarshaller() {
        JSON.registerObjectMarshaller(UserDTO) {
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            def status = ''
            if (it.userStatus.id > 1 && !it.userStatus.isSuspended()) {
                status = 'overdue'
            } else if (it.userStatus.id > 1 && it.userStatus.isSuspended()) {
                status = 'suspended'
            }

            def balance = UserBL.getBalance(it.id) as BigDecimal
            def currencySymbol = it.currency.symbol
            def customer = it.customer
            def contact = ContactDTO.findByUserId(it.id)
            def type = it.roles.asList().first()?.getTitle(session['language_id'])
            def hierarchy = [:]
            if (customer?.children?.size() > 0) {
                hierarchy.parent = true
                hierarchy.children = customer.children.findAll { it.baseUser.deleted == 0 }.size()
            }
            if (customer?.parent) {
                hierarchy.child = true
            }

            [cell: [userId        : it.id,
                    userName      : it.userName,
                    company       : it.company.description,
                    status        : status,
                    balance       : balance,
                    contact       : contact,
                    currencySymbol: currencySymbol,
                    hierarchy     : hierarchy,
                    type          :type
            ],
             id  : it.id]
        }
    }

    private registerOrderDTOMarshaller() {
        JSON.registerObjectMarshaller(OrderDTO){
            def total = it.total
            def currencySymbol = it.currency.symbol
            def date = it.createDate
            def customer = it.baseUserByUserId

            def hierarchy = [:]
            if (it.childOrders?.size() > 0) {
                hierarchy.parent = true
                hierarchy.children = it.childOrders.findAll { it.deleted == 0 }.size()
            }
            if (it.parentOrder) {
                hierarchy.child = true
            }

            [cell:  [   orderid: it.id,
                        customer: customer.userName,
                        company: customer.company.description,
                        date: date,
                        amount: total,
                        currencySymbol: currencySymbol,
                        hierarchy: hierarchy
            ] ,
             id:    it.id]
        }
    }

    private registerPartnerDTOMarshaller() {
        JSON.registerObjectMarshaller(PartnerDTO){
            def customer = it.baseUserByUserId
            def status = ''
            if (customer.userStatus?.id > 1 && !customer.userStatus?.isSuspended()) {
                status = 'overdue'
            } else if (customer.userStatus?.id > 1 && customer.userStatus?.isSuspended()) {
                status = 'suspended'
            }
            def contact = ContactDTO.findByUserId(customer.id)

            def hierarchy = [:]
            if (it.children?.size() > 0) {
                hierarchy.parent = true
                hierarchy.children = it.children.findAll { it.baseUser.deleted == 0 }.size()
            }
            if (it.parent) {
                hierarchy.child = true
            }

            [cell:  [   userid: it.id,
                        username: customer.userName,
                        company: customer.company.description,
                        status: status,
                        contact: contact,
                        hierarchy: hierarchy
            ] ,
             id:    it.id]
        }
    }

    private registerContactDTOMarshaller() {
        JSON.registerObjectMarshaller(ContactDTO){
            [firstName: it.firstName,
             lastName: it.lastName,
             organization: it.organizationName ]
        }
    }

    private registerInvoiceDTOMarshaller() {
        JSON.registerObjectMarshaller(InvoiceDTO){
            def currencySymbol = it.currencyDTO.symbol
            def dueDate = it.dueDate
            def customer = it.baseUser
            def status = it.invoiceStatus

            [cell:  [   invoiceId: it.id,
                        invoiceNumber: it.publicNumber,
                        userName: customer.userName,
                        company: customer.company.description,
                        dueDate: dueDate,
                        status: status,
                        amount: it.total,
                        balance: it.balance,
                        currencySymbol: currencySymbol
            ] ,
             id:    it.id]

        }
    }

    private registerPaymentDTOMarshaller() {
        JSON.registerObjectMarshaller(PaymentDTO){
            def currencySymbol = it.currencyDTO.symbol
            def date = it.paymentDate
            def customer = it.baseUser
            def paymentOrRefund = it.isRefund ? 'R': 'P'

            [cell:  [   paymentId: it.id,
                        userName: customer.userName,
                        company: customer.company.description,
                        date: date,
                        paymentOrRefund: paymentOrRefund,
                        amount: it.amount,
                        currencySymbol: currencySymbol,
                        method: it.paymentMethod,
                        result: it.paymentResult
            ] ,
             id:    it.id]

        }
    }

    private registerBillingProcessDTOMarshaller() {
        JSON.registerObjectMarshaller(BillingProcessDTO) {
            def orderCount = it.orderProcesses?.size()
            def invoiceCount = it.invoices?.size()
            def invoiced = [:]
            def invoiceCarried = [:]
            it.invoices?.each { invoice ->
                invoiced[invoice.currency] = invoiced.get(invoice.currency, BigDecimal.ZERO).add(invoice.total.subtract(invoice.carriedBalance))
                invoiceCarried[invoice.currency] = invoiceCarried.get(invoice.currency, BigDecimal.ZERO).add(invoice.carriedBalance)
            }
            def multiCurrency = invoiced.keySet().size() == 1 ? Boolean.FALSE : Boolean.TRUE
            def currencySymbol
            def totalInvoiced
            def totalCarried
            if (multiCurrency) {
                currencySymbol = ''
                totalInvoiced = ''
                totalCarried = ''
            } else {
                invoiced.entrySet().each { total ->
                    totalInvoiced = total.value
                    currencySymbol = total.key.symbol
                }
                invoiceCarried.entrySet().each { total ->
                    totalCarried = total.value
                }
            }
            [cell:  [   billingId: it.id,
                        date: it.billingDate,
                        orderCount: orderCount,
                        invoiceCount: invoiceCount,
                        multiCurrency: multiCurrency,
                        currencySymbol: currencySymbol,
                        totalInvoiced: totalInvoiced,
                        totalCarried: totalCarried,
                        isReview: it.isReview
            ] ,
             id:    it.id]
        }
    }

    private registerDiscountDTOMarshaller() {
        JSON.registerObjectMarshaller(DiscountDTO,2){
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            [cell:  [   discountId: it.id,
                        code: it.code,
                        description: it.getDescription(session['language_id']),
                        type: it.type
            ] ,
             id:    it.id]

        }
    }

    private registerItemTypeDTOMarshaller() {
        JSON.registerObjectMarshaller(ItemTypeDTO,3){
            def lineType = new OrderLineTypeDTO(it.orderLineTypeId,0)
            def entities = it.entities?.toArray()
            def company = ''
            if (it.entity == null && entities?.size() > 0){
                company = entities[0]?.description
            } else {
                company = it.entity.description
            }
            [cell:  [   categoryId: it.id,
                        company: company,
                        global: it.global,
                        multiple: it.entities?.size() > 1,
                        lineType: lineType,
                        name: it.description
            ] ,
             id:    it.id]

        }
    }

    private registerItemDTOMarshaller() {
        JSON.registerObjectMarshaller(ItemDTO,4){
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            def name = it?.getDescription(session['language_id']).encodeAsHTML()
            def entities = it.entities?.toArray()
            def company = ''
            if (entities?.size() > 0){
                company = entities[0]?.description
            }
            [cell:  [   productId: it.id,
                        company: company,
                        global: it.global,
                        multiple: it.entities?.size() > 1,
                        name: name,
                        number: it.internalNumber
            ] ,
             id:    it.id]

        }
    }

    private registerPreferenceTypeDTOMarshaller() {
        JSON.registerObjectMarshaller(PreferenceTypeDTO,5){
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            def preference = it.preferences.find{
                        it.jbillingTable.name == ServerConstants.TABLE_ENTITY &&
                        it.foreignId == session['company_id']}
            def value = preference ? preference.value : it.defaultValue
            [cell:  [   preferenceId: it.id,
                        description: it.getDescription(session['language_id']) ?: '',
                        value: value
            ] ,
             id:    it.id]

        }
    }

    private registerRoleDTOMarshaller() {
        JSON.registerObjectMarshaller(RoleDTO, 6){
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            [cell:  [   roleId: it.id,
                        title: it.getDescription(session['language_id'], 'title') ?: ''
            ] ,
             id:    it.id]
        }
    }

    private registerEnumerationDTOMarshaller() {
        JSON.registerObjectMarshaller(EnumerationDTO){
            [cell:  [   enumId: it.id,
                        name: it.getName() ?: ''
            ] ,
             id:    it.id]
        }
    }

    private registerNotificationCategoryDTOMarshaller() {
        JSON.registerObjectMarshaller(NotificationCategoryDTO, 7){
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            [cell:  [   categoryId: it.id,
                        description: it?.getDescription(session['language_id']) ?: ''
            ] ,
             id:    it.id]
        }
    }

    private registerNotificationMessageTypeDTOMarshaller() {
        JSON.registerObjectMarshaller(NotificationMessageTypeDTO, 8){
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            def languageId = session['language_id'] as Integer
            def entityId = session['company_id'] as Integer
            def active = true
            it.getNotificationMessages().each{
                if (languageId == it.language.id && entityId == it.entity.id && it.useFlag > 0){
                    active = false
                }
            }

            [cell:  [   notificationId: it.id,
                        description: it?.getDescription(languageId) ?: '',
                        active: !active
            ] ,
             id:    it.id]
        }
    }

    private registerAccountTypeDTOMarshaller() {
        JSON.registerObjectMarshaller(AccountTypeDTO, 9){
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            [cell:  [   typeId: it.id,
                        description: it.getDescription(session['language_id']) ?: ''
            ] ,
             id:    it.id]
        }
    }

    private registerOrderPeriodDTOMarshaller() {
        JSON.registerObjectMarshaller(OrderPeriodDTO, 11){
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            [cell:  [   periodId: it.id,
                        description: it.getDescription(session['language_id']) ?: '',
                        unit: it.getPeriodUnit()?.getDescription(session['language_id']) ?: '',
                        value: it.value
            ] ,
             id:    it.id]
        }
    }

    private registerPaymentMethodTypeDTOMarshaller() {
        JSON.registerObjectMarshaller(PaymentMethodTypeDTO){
            [cell:  [   paymentMethodId: it.id,
                        name: it.methodName ?: ''
            ] ,
             id:    it.id]
        }
    }

    private registerOrderStatusDTOMarshaller() {
        JSON.registerObjectMarshaller(OrderStatusDTO, 12){
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            [cell:  [   orderStatusId: it.id,
                        flag: it.orderStatusFlag as String,
                        description: it.getDescription(session['language_id']) ?: ''
            ] ,
             id:    it.id]
        }
    }

    private registerPluggableTaskTypeCategoryDTOMarshaller() {
        JSON.registerObjectMarshaller(PluggableTaskTypeCategoryDTO, 13){
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            [cell:  [   categoryId: it.id,
                        interfaceName: it.interfaceName,
                        description: it.getDescription(session['language_id']) ?: ''
            ] ,
             id:    it.id]
        }
    }

    private registerPluggableTaskDTOMarshaller() {
        JSON.registerObjectMarshaller(PluggableTaskDTO){
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            def type = it.type
            [cell:  [   pluginId: it.id,
                        typeClassName: type.className,
                        typeTitle: type.getDescription(session['language_id'], 'title') ?: '',
                        order: it.processingOrder
            ] ,
             id:    it.id]
        }
    }

    private registerReportTypeDTOMarshaller() {
        JSON.registerObjectMarshaller(ReportTypeDTO, 14){
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            [cell:  [   typeId: it.id,
                        reports: it.reports?.size(),
                        description: it.getDescription(session['language_id']) ?: ''
            ] ,
             id:    it.id]
        }
    }

    private registerReportDTOMarshaller() {
        JSON.registerObjectMarshaller(ReportDTO, 15){
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            [cell:  [   reportId: it.id,
                        fileName: it.fileName,
                        name: it.name
            ] ,
             id:    it.id]
        }
    }

    private registerEntityTypeMarshaller() {
        JSON.registerObjectMarshaller(EntityType){
            [cell:  [   name: it as String
            ] ,
             id:   it as String]
        }
    }

    private registerMetaFieldMarshaller() {
        JSON.registerObjectMarshaller(MetaField, 14){
            [cell:  [   metaFieldId: it.id,
                        name: it.name
            ] ,
             id:   it.id]
        }
    }

    private registerMetaFieldGroupMarshaller() {
        JSON.registerObjectMarshaller(MetaFieldGroup, 15){
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            [cell:  [   groupId: it.id,
                        description: it?.getDescription(session['language_id'])
            ] ,
             id:   it.id]
        }
    }

    private registerDescriptionMarshaller() {
        JSON.registerObjectMarshaller(AbstractDescription, 1){
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            [description: it?.getDescription(session['language_id'])]
        }
    }
    private registerAssetDTOMarshaller() {
        JSON.registerObjectMarshaller(AssetDTO, 17) {
            def entities = it.entities?.toArray()
            def company = ''
            if (entities?.size() > 0) {
                company = entities[0]?.description
            }
            [cell: [assetId: it.id,
                    identifier    : it.identifier,
                    company       : company,
                    global        : it.global,
                    multiple      : it.entities?.size() > 1,
                    createDatetime: it.createDatetime,
                    status: it.assetStatus.description

            ],
             id  : it.id]
        }
    }
}
