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

package com.sapienter.jbilling.client

import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO
import com.sapienter.jbilling.server.order.ApplyToOrder
import com.sapienter.jbilling.server.util.ServerConstants
import com.sapienter.jbilling.server.util.db.EnumerationDTO
import com.sapienter.jbilling.server.util.db.EnumerationValueDTO;
import com.sapienter.jbilling.server.util.db.InternationalDescription
import com.sapienter.jbilling.server.util.db.InternationalDescriptionId
import com.sapienter.jbilling.server.user.db.UserDTO
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDTO
import com.sapienter.jbilling.server.user.db.AccountTypeDTO
import com.sapienter.jbilling.server.user.db.MainSubscriptionDTO
import com.sapienter.jbilling.server.util.db.LanguageDTO
import com.sapienter.jbilling.server.util.db.PreferenceDTO
import com.sapienter.jbilling.server.util.db.JbillingTable
import com.sapienter.jbilling.server.util.db.PreferenceTypeDTO
import com.sapienter.jbilling.server.invoice.db.InvoiceDeliveryMethodDTO
import com.sapienter.jbilling.server.metafields.DataType
import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.server.metafields.db.MetaField
import com.sapienter.jbilling.server.metafields.MetaFieldType
import com.sapienter.jbilling.server.metafields.db.ValidationRule
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleType;
import com.sapienter.jbilling.server.notification.NotificationMediumType
import com.sapienter.jbilling.server.notification.db.NotificationMessageDTO
import com.sapienter.jbilling.server.notification.db.NotificationMessageLineDTO
import com.sapienter.jbilling.server.notification.db.NotificationMessageSectionDTO
import com.sapienter.jbilling.server.notification.db.NotificationMessageTypeDTO
import com.sapienter.jbilling.server.order.OrderStatusFlag
import com.sapienter.jbilling.server.order.db.OrderStatusDTO
import com.sapienter.jbilling.server.payment.db.PaymentMethodDTO
import com.sapienter.jbilling.server.payment.db.PaymentMethodTemplateDTO
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeDAS
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeDTO
import com.sapienter.jbilling.server.process.db.BillingProcessConfigurationDTO
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO
import com.sapienter.jbilling.server.process.db.ProratingType
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskParameterDTO
import com.sapienter.jbilling.server.report.db.ReportDTO
import com.sapienter.jbilling.server.user.UserBL

import org.codehaus.groovy.grails.context.support.ReloadableResourceBundleMessageSource
import org.joda.time.DateMidnight

/**
 * EntityDefaults 
 *
 * @author Brian Cowdery
 * @since 10/03/11
 */
class EntityDefaults {

    def UserDTO rootUser
    def CompanyDTO company
    def LanguageDTO language
    def JbillingTable entityTable
    def Locale locale

    ReloadableResourceBundleMessageSource messageSource

	EntityDefaults() {
		
	}
	
    EntityDefaults(CompanyDTO company, UserDTO rootUser, LanguageDTO language, ReloadableResourceBundleMessageSource messageSource) {
        this.company = company
        this.rootUser = rootUser
        this.language = language
        this.entityTable = JbillingTable.findByName(ServerConstants.TABLE_ENTITY)
        this.locale = UserBL.getLocale(rootUser)
        this.messageSource = messageSource
    }

    /**
     * Initialize the entity, creating the necessary preferences, plugins and other defaults.
     */
    def init() {
         /*
            Order Status
         */
        def invoiceOS = new OrderStatusDTO(orderStatusFlag: OrderStatusFlag.INVOICE, entity:company).save()
        invoiceOS.setDescription("Active", language.id)
        def finishedOS = new OrderStatusDTO(orderStatusFlag: OrderStatusFlag.FINISHED, entity:company).save()
        finishedOS.setDescription("Finished", language.id)
        def notInvoiceOS = new OrderStatusDTO(orderStatusFlag: OrderStatusFlag.NOT_INVOICE, entity:company).save()
        notInvoiceOS.setDescription("Suspended", language.id)
        def suspAgeingOS = new OrderStatusDTO(orderStatusFlag: OrderStatusFlag.SUSPENDED_AGEING, entity:company).save()
        suspAgeingOS.setDescription("Suspended ageing(auto)", language.id)
        log.debug("company.id ********: ${company?.id}")

        // add company currency to the entity currency map
        // it's annoying that we need to build this association manually, it would be better mapped through CompanyDTO
        company.currency.entities_1 << company

        /*
            Order periods
         */
		def monthly = new OrderPeriodDTO(company, new PeriodUnitDTO(ServerConstants.PERIOD_UNIT_MONTH), 1).save()
        monthly.setDescription("Monthly", language.id)

		def maxStatusId= OrderChangeStatusDTO.createCriteria().get {
		    projections {
		        max "id"
		    }
		} as Integer
        //default order change status 'Default' apply
        def orderChangeStatusApply = new OrderChangeStatusDTO(company: company, applyToOrder: ApplyToOrder.YES, 
											deleted: 0, order: 1, id: ++maxStatusId).save()
        orderChangeStatusApply.setDescription(getMessage("order.change.status.default.apply"), ServerConstants.LANGUAGE_ENGLISH_ID)
        orderChangeStatusApply.save()
		//default account type
		def oneMonthly= new MainSubscriptionDTO(monthly, Integer.valueOf(1))
		def defaultAccountType = new AccountTypeDTO(company: company, billingCycle: oneMonthly ).save()
		defaultAccountType.setDescription('description', ServerConstants.LANGUAGE_ENGLISH_ID, getMessage("default.account.type.name"))

        /*
            Payment methods
         */
        PaymentMethodDTO.get(ServerConstants.PAYMENT_METHOD_CHEQUE).entities << company
        PaymentMethodDTO.get(ServerConstants.PAYMENT_METHOD_VISA).entities << company
        PaymentMethodDTO.get(ServerConstants.PAYMENT_METHOD_MASTERCARD).entities << company


        /*
            Invoice delivery methods
         */
        InvoiceDeliveryMethodDTO.get(ServerConstants.D_METHOD_EMAIL).entities << company
        InvoiceDeliveryMethodDTO.get(ServerConstants.D_METHOD_PAPER).entities << company
        InvoiceDeliveryMethodDTO.get(ServerConstants.D_METHOD_EMAIL_AND_PAPER).entities << company


        /*
            Reports
         */
        ReportDTO.list().each { report ->
            company.reports << report
        }


        /*
            Billing process configuration
         */
        new BillingProcessConfigurationDTO(
                entity: company,
				periodUnit: new PeriodUnitDTO(ServerConstants.PERIOD_UNIT_MONTH),
                nextRunDate: new DateMidnight().plusMonths(1).toDate(),
                generateReport: 1,
                retries: 0,
                daysForRetry: 1,
                daysForReport: 3,
                reviewStatus: 1,
				dueDateUnitId: 1,
                dueDateValue: 1,
                onlyRecurring: 1,
                invoiceDateProcess: 0,
                maximumPeriods: 99,
                autoPaymentApplication: 1,
				proratingType: ProratingType.PRORATING_AUTO_OFF
        ).save()


        /*
            Pluggable tasks
         */

        // PaymentFakeTask
        def paymentTask = new PluggableTaskDTO(entityId: company.id, type: new PluggableTaskTypeDTO(21), processingOrder: 1).save()
        new PluggableTaskParameterDTO(task: paymentTask, name: 'all', strValue: 'yes').save()

        // BasicEmailNotificationTask
        def emailTask = new PluggableTaskDTO(entityId: company.id, type: new PluggableTaskTypeDTO(9), processingOrder: 1, parameters: [
            new PluggableTaskParameterDTO(name: 'smtp_server', strValue: ''),
            new PluggableTaskParameterDTO(name: 'port', strValue: ''),
            new PluggableTaskParameterDTO(name: 'ssl_auth', strValue: 'false'),
            new PluggableTaskParameterDTO(name: 'tls', strValue: 'false'),
            new PluggableTaskParameterDTO(name: 'username', strValue: ''),
            new PluggableTaskParameterDTO(name: 'password', strValue: '')
        ]).save()

        updateParametersForTask(emailTask)

        // PaperInvoiceNotificationTask
        def notificationTask = new PluggableTaskDTO(entityId: company.id, type: new PluggableTaskTypeDTO(12), processingOrder: 2, parameters: [
            new PluggableTaskParameterDTO(name: 'design', strValue: 'simple_invoice_b2b')
        ]).save()

        updateParametersForTask(notificationTask)

		//The IDs may not be hard coded. They may be different under different circumstances.
		PluggableTaskTypeDAS pluggableTaskTypeDAS= new PluggableTaskTypeDAS();
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.pluggableTask.BasicLineTotalTask'), processingOrder: 1).save()    // BasicLineTotalTask
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.pluggableTask.CalculateDueDate'), processingOrder: 1).save()    // CalculateDueDate
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.pluggableTask.BasicCompositionTask'), processingOrder: 2).save()    // BasicCompositionTask
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.pluggableTask.BasicOrderFilterTask'), processingOrder: 1).save()    // BasicOrderFilterTask
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.pluggableTask.BasicInvoiceFilterTask'), processingOrder: 1).save()    // BasicInvoiceFilterTask
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.pluggableTask.BasicOrderPeriodTask'), processingOrder: 1).save()    // BasicOrderPeriodTask
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.pluggableTask.BasicPaymentInfoTask'), processingOrder: 1).save()   // BasicPaymentInfoTask
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.payment.tasks.NoAsyncParameters'), processingOrder: 1).save()   // NoAsyncParameters
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.item.tasks.BasicItemManager'), processingOrder: 1).save()   // BasicItemManager
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.user.balance.DynamicBalanceManagerTask'), processingOrder: 1).save()   // DynamicBalanceManagerTask
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.billing.task.BillingProcessTask'), processingOrder: 1).save()   // BillingProcessTask
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.process.task.AgeingProcessTask'), processingOrder: 2).save()   // AgeingProcessTask
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.process.task.BasicAgeingTask'), processingOrder: 1).save()   // BasicAgeingTask
		new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.process.task.BasicBillingProcessFilterTask'), processingOrder: 1).save()   // BasicBillingProcessFilterTask
		new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.order.task.CreateOrderForResellerTask'), processingOrder: 2).save()  // CreateOrderForResellerTask
		new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.invoice.task.DeleteResellerOrderTask'), processingOrder: 3).save()  // DeleteResellerOrderTask
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.order.task.OrderChangeApplyOrderStatusTask'), processingOrder: 7).save()  // CustomerPlanUnsubscriptionProcessingTask

        //This is for testing the initial email sent when the company is created
        //Otherwise, admin users WILL NOT have passwords configured
        //And the SMTP server will need to be configured
        def testNotificationTask = new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.notification.task.TestNotificationTask'), processingOrder: 3, parameters: [
                new PluggableTaskParameterDTO(name: 'from', strValue: 'admin@jbilling.com')
        ]).save()

        updateParametersForTask(testNotificationTask)
        /*
            Preferences
         */
		
        new PreferenceDTO(jbillingTable: entityTable, foreignId: company.id, preferenceType: new PreferenceTypeDTO(ServerConstants.PREFERENCE_SHOW_NOTE_IN_INVOICE), value: 1).save()
        new PreferenceDTO(jbillingTable: entityTable, foreignId: company.id, preferenceType: new PreferenceTypeDTO(ServerConstants.PREFERENCE_INVOICE_PREFIX), value: "").save()
        new PreferenceDTO(jbillingTable: entityTable, foreignId: company.id, preferenceType: new PreferenceTypeDTO(ServerConstants.PREFERENCE_INVOICE_NUMBER), value: 1).save()

        /*
            Notification messages
         */
        createNotificationMessage(ServerConstants.NOTIFICATION_TYPE_INVOICE_EMAIL, 'signup.notification.email.title', 'signup.notification.email')
        createNotificationMessage(ServerConstants.NOTIFICATION_TYPE_USER_REACTIVATED, 'signup.notification.user.reactivated.title', 'signup.notification.user.reactivated')
        createNotificationMessage(ServerConstants.NOTIFICATION_TYPE_USER_OVERDUE, 'signup.notification.overdue.title', 'signup.notification.overdue')
        createNotificationMessage(ServerConstants.NOTIFICATION_TYPE_ORDER_EXPIRE_1, 'signup.notification.order.expire.1.title', 'signup.notification.order.expire.1')
        createNotificationMessage(ServerConstants.NOTIFICATION_TYPE_PAYMENT_SUCCESS, 'signup.notification.payment.success.title', 'signup.notification.payment.success')
        createNotificationMessage(ServerConstants.NOTIFICATION_TYPE_PAYMENT_FAILED, 'signup.notification.payment.failed.title', 'signup.notification.payment.failed')
        createNotificationMessage(ServerConstants.NOTIFICATION_TYPE_INVOICE_REMINDER, 'signup.notification.invoice.reminder.title', 'signup.notification.invoice.reminder')
        createNotificationMessage(ServerConstants.NOTIFICATION_TYPE_CREDIT_CARD_UPDATE, 'signup.notification.credit.card.update.title', 'signup.notification.credit.card.update')
		createNotificationMessage(ServerConstants.NOTIFICATION_TYPE_LOST_PASSWORD, 'signup.notification.lost.password.update.title', 'signup.notification.lost.password.update')
        createNotificationMessage(ServerConstants.NOTIFICATION_TYPE_INITIAL_CREDENTIALS, 'signup.notification.initial.credentials.update.title', 'signup.notification.initial.credentials.update')


        /*
         * Create payment method template meta fields for entity
         */
		// Credit card meta file
		MetaFieldType usage;
		ValidationRule rule;
		InternationalDescriptionId desId
		
		int valRulId = JbillingTable.findByName(ServerConstants.TABLE_VALIDATION_RULE).getId();
	
		PaymentMethodTemplateDTO template = PaymentMethodTemplateDTO.findByTemplateName(ServerConstants.PAYMENT_CARD)
		usage = MetaFieldType.TITLE
		template.paymentTemplateMetaFields << new MetaField(entity : company, name : "cc.cardholder.name", entityType : EntityType.PAYMENT_METHOD_TEMPLATE, dataType : DataType.STRING, disabled : false, mandatory : true, primary : true, displayOrder : 1, fieldUsage : usage).save()
		
		usage =MetaFieldType.PAYMENT_CARD_NUMBER
		rule = new ValidationRule(ruleType : ValidationRuleType.PAYMENT_CARD, enabled : true).save()
		template.paymentTemplateMetaFields << new MetaField(entity : company, name : "cc.number", entityType : EntityType.PAYMENT_METHOD_TEMPLATE, dataType : DataType.STRING, disabled : false, mandatory : true, primary : true, displayOrder : 2, validationRule : rule, fieldUsage : usage).save()
		desId = new InternationalDescriptionId(valRulId, rule.getId(), "errorMessage", language.id)
		new InternationalDescription(id : desId, content : getMessage("validation.payment.card.number.invalid")).save()
		
		usage = MetaFieldType.DATE
		SortedMap<String, String> attributes = new TreeMap<String, String>()
		attributes.put('regularExpression', '(?:0[1-9]|1[0-2])/[0-9]{4}')
		rule = new ValidationRule(ruleType : ValidationRuleType.REGEX, enabled : true, ruleAttributes : attributes).save()
		rule.ruleAttributes = attributes
		template.paymentTemplateMetaFields << new MetaField(entity : company, name : "cc.expiry.date", entityType : EntityType.PAYMENT_METHOD_TEMPLATE, dataType : DataType.STRING, disabled : false, mandatory : true, primary : true, displayOrder : 3, validationRule : rule, fieldUsage : usage).save()
		desId = new InternationalDescriptionId(valRulId, rule.getId(), "errorMessage", language.id)
		new InternationalDescription(id : desId, content : getMessage("validation.payment.card.expiry.date.invalid")).save()
		
		usage = MetaFieldType.GATEWAY_KEY
		template.paymentTemplateMetaFields << new MetaField(entity : company, name : "cc.gateway.key", entityType : EntityType.PAYMENT_METHOD_TEMPLATE, dataType : DataType.STRING, disabled : true, mandatory : false, primary : true, displayOrder : 4, fieldUsage : usage).save()
		
		usage = MetaFieldType.CC_TYPE
		template.paymentTemplateMetaFields << new MetaField(entity : company, name : "cc.type", entityType : EntityType.PAYMENT_METHOD_TEMPLATE, dataType : DataType.INTEGER, disabled : true, mandatory : false, primary : true, displayOrder : 5, fieldUsage : usage).save()
		
		// Ach template meta fields 
		template = PaymentMethodTemplateDTO.findByTemplateName(ServerConstants.ACH);
		usage = MetaFieldType.BANK_ROUTING_NUMBER
		attributes = new TreeMap<String, String>()
		attributes.put('regularExpression', '(?<=\\s|^)\\d+(?=\\s|$)')
		rule = new ValidationRule(ruleType : ValidationRuleType.REGEX, enabled : true, ruleAttributes : attributes).save()
		rule.ruleAttributes = attributes
		template.paymentTemplateMetaFields << new MetaField(entity : company, name : "ach.routing.number", entityType : EntityType.PAYMENT_METHOD_TEMPLATE, dataType : DataType.STRING, disabled : false, mandatory : true, primary : true, displayOrder : 1, validationRule : rule, fieldUsage : usage).save()
		desId = new InternationalDescriptionId(valRulId, rule.getId(), "errorMessage", language.id)
		new InternationalDescription(id : desId, content : getMessage("validation.ach.aba.routing.number.invalid")).save()
		
		usage = MetaFieldType.TITLE
		template.paymentTemplateMetaFields << new MetaField(entity : company, name : "ach.customer.name", entityType : EntityType.PAYMENT_METHOD_TEMPLATE, dataType : DataType.STRING, disabled : false, mandatory : true, primary : true, displayOrder : 2, fieldUsage : usage).save()
		
		usage = MetaFieldType.BANK_ACCOUNT_NUMBER
		template.paymentTemplateMetaFields << new MetaField(entity : company, name : "ach.account.number", entityType : EntityType.PAYMENT_METHOD_TEMPLATE, dataType : DataType.STRING, disabled : false, mandatory : true, primary : true, displayOrder : 3, fieldUsage : usage).save()
		
		usage = MetaFieldType.BANK_NAME
		template.paymentTemplateMetaFields << new MetaField(entity : company, name : "ach.bank.name", entityType : EntityType.PAYMENT_METHOD_TEMPLATE, dataType : DataType.STRING, disabled : false, mandatory : true, primary : true, displayOrder : 4, fieldUsage : usage).save()
		
		usage = MetaFieldType.BANK_ACCOUNT_TYPE
		template.paymentTemplateMetaFields << new MetaField(entity : company, name : "ach.account.type", entityType : EntityType.PAYMENT_METHOD_TEMPLATE, dataType : DataType.ENUMERATION, disabled : false, mandatory : true, primary : true, displayOrder : 5, fieldUsage : usage).save()
		
		EnumerationDTO enumeration = new EnumerationDTO(name : "ach.account.type", entity : company).save()
		new EnumerationValueDTO(value : 'CHECKING', enumeration : enumeration).save()
		new EnumerationValueDTO(value : 'SAVINGS', enumeration : enumeration).save()
		
		usage = MetaFieldType.GATEWAY_KEY
		template.paymentTemplateMetaFields << new MetaField(entity : company, name : "ach.gateway.key", entityType : EntityType.PAYMENT_METHOD_TEMPLATE, dataType : DataType.STRING, disabled : true, mandatory : false, primary : true, displayOrder : 6, fieldUsage : usage).save()
		
		// cheque template meta fields
		template = PaymentMethodTemplateDTO.findByTemplateName(ServerConstants.CHEQUE)
		
		usage = MetaFieldType.BANK_NAME
		template.paymentTemplateMetaFields << new MetaField(entity : company, name : "cheque.bank.name", entityType : EntityType.PAYMENT_METHOD_TEMPLATE, dataType : DataType.STRING, disabled : false, mandatory : true, primary : true, displayOrder : 1, fieldUsage : usage).save()
		
		usage = MetaFieldType.CHEQUE_NUMBER
		template.paymentTemplateMetaFields << new MetaField(entity : company, name : "cheque.number", entityType : EntityType.PAYMENT_METHOD_TEMPLATE, dataType : DataType.STRING, disabled : false, mandatory : true, primary : true, displayOrder : 2, fieldUsage : usage).save()
		
		usage = MetaFieldType.DATE
		template.paymentTemplateMetaFields << new MetaField(entity : company, name : "cheque.date", entityType : EntityType.PAYMENT_METHOD_TEMPLATE, dataType : DataType.DATE, disabled : false, mandatory : true, primary : true, displayOrder : 3, fieldUsage : usage).save()
    }

    /**
     * When we construct the pluggable task the way we do it now, we enable the pluggable task
     * to have parameters immediately but we need to still reference their parameters
     * to the task they are in
     * @param taskDTO
     */
    def updateParametersForTask(PluggableTaskDTO taskDTO) {
        for (PluggableTaskParameterDTO parameter : taskDTO.getParameters()) {
            parameter.task = taskDTO
            parameter.save()
        }
    }
/**
     * Create a new, 2 section notification message for the given type id and messages. Messages are
     * resolved from the grails 'messages.properties' bundle.
     *
     * @param typeId notification type id
     * @param titleCode message code for the notification message title
     * @param bodyCode message code for the notification message body
     */
    def createNotificationMessage(Integer typeId, String titleCode, String bodyCode) {
        def message = new NotificationMessageDTO(
                entity: company,
                language: language,
                useFlag: 1,
                notificationMessageType: new NotificationMessageTypeDTO(id: typeId)
        )
		
		def mediumTypes = new ArrayList<NotificationMediumType>(Arrays.asList(NotificationMediumType.values()));
		message.mediumTypes = mediumTypes

        message.save()

        def titleSection = new NotificationMessageSectionDTO(notificationMessage: message, section: 1)
        titleSection.notificationMessageLines << new NotificationMessageLineDTO(notificationMessageSection: titleSection, content: getMessage(titleCode))
        message.notificationMessageSections << titleSection
        titleSection.save()

        def bodySection = new NotificationMessageSectionDTO(notificationMessage: message, section: 2)
        bodySection.notificationMessageLines << new NotificationMessageLineDTO(notificationMessageSection: bodySection, content: getMessage(bodyCode))
        message.notificationMessageSections << bodySection
        bodySection.save()
    }

    def String getMessage(String code) {
        return messageSource.getMessage(code, new Object[0], code, locale)
    }

    def String getMessage(String code, Object[] args) {
        return messageSource.getMessage(code, args, code, locale)
    }
}
