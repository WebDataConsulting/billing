package jbilling

import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.common.CommonConstants
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.client.metafield.MetaFieldBindHelper
import com.sapienter.jbilling.server.item.CurrencyBL
import com.sapienter.jbilling.server.item.db.ItemDTO
import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.MetaFieldWS
import com.sapienter.jbilling.server.metafields.db.MetaField
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS
import com.sapienter.jbilling.server.metafields.db.MetaFieldGroupDAS
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDTO
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO
import com.sapienter.jbilling.server.user.AccountInformationTypeWS
import com.sapienter.jbilling.server.user.AccountTypeWS
import com.sapienter.jbilling.server.user.MainSubscriptionWS
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDTO
import com.sapienter.jbilling.server.user.db.AccountTypeDTO
import com.sapienter.jbilling.server.user.db.AccountTypePriceDTO
import com.sapienter.jbilling.server.user.db.AccountTypePricePK
import com.sapienter.jbilling.server.user.db.AccountTypePriceBL
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS
import com.sapienter.jbilling.server.util.Util

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap

import org.joda.time.DateTime

@Secured(["isAuthenticated()", "MENU_99"])
class AccountTypeController {

    static pagination = [max: 10, offset: 0, sort: 'id', order: 'desc']

    static final viewColumnsToFields =
            ['typeId': 'id']

    def breadcrumbService
    IWebServicesSessionBean webServicesSession
    def viewUtils

    def productService


    def index () {
        list()
    }

    def list () {
        breadcrumbService.addBreadcrumb(controllerName, actionName, null, null)
        getList(params)
    }

    def findAccountTypes (){
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def accountTypes = getAccountTypesForEntity(params)

        try {
            def jsonData = getAccountTypesJsonData(accountTypes, params)

            render jsonData as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }
    }

    /**
     * Converts AccountTypes to JSon
     */
    private def Object getAccountTypesJsonData(accounts, GrailsParameterMap params) {
        def jsonCells = accounts
        def currentPage = params.page ? Integer.valueOf(params.page) : 1
        def rowsNumber = params.rows ? Integer.valueOf(params.rows): 1
        def totalRecords =  jsonCells ? jsonCells.totalCount : 0
        def numberOfPages = Math.ceil(totalRecords / rowsNumber)

        def jsonData = [rows: jsonCells, page: currentPage, records: totalRecords, total: numberOfPages]

        jsonData
    }

    def getList(params) {

        def accountType = AccountTypeDTO.get(params.int('id'))
        if (params.id?.isInteger() && !accountType) {
            flash.error = 'orderPeriod.not.found'
            flash.args = [params.id as String]
        }
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        def accountTypes = getAccountTypesForEntity(params)
        if (params.applyFilter || params.partial) {
            render template: 'accountTypes', model: [accountTypes: accountTypes, selected: accountType]
        } else {
            if (chainModel) {
                def cp = chainModel
                render view: 'list', model: [selected: accountType, accountTypes: accountTypes] + chainModel
            } else
                render view: 'list', model: [accountTypes: accountTypes, selected: accountType]
        }
    }

    private def getAccountTypesForEntity(GrailsParameterMap params) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        def languageId = session['language_id']

        return AccountTypeDTO.createCriteria().list(
                max:    params.max,
                offset: params.offset
        ) {
            eq('company', new CompanyDTO(session['company_id']))
            if (params.typeId){
                def searchParam = params.typeId
                if (searchParam.isInteger()){
                    eq('id', Integer.valueOf(searchParam));
                } else {
                    searchParam = searchParam.toLowerCase()
                    sqlRestriction(
                            """ exists (
                                            select a.foreign_id
                                            from international_description a
                                            where a.foreign_id = {alias}.id
                                            and a.table_id =
                                             (select b.id from jbilling_table b where b.name =
                                              ?)
                                            and a.language_id = ?
                                            and a.psudo_column = 'description'
                                            and lower(a.content) like ?
                                        )
                                    """, [Constants.TABLE_ACCOUNT_TYPE, languageId, "%" + searchParam + "%"]
                    )
                }
            }
            SortableCriteria.sort(params, delegate)
        }
    }

    def invalid (){
        render view: 'edit', model: [
                accountType: params.accountTypeWS,
                company: params.company
        ]
    }

    def edit (){
        def accountType
        if(params.id){
            accountType = webServicesSession.getAccountType(params.int('id'))
            if(!accountType){
                return response.sendError(Constants.ERROR_CODE_404)
            }
        }
        else {
            accountType = new AccountTypeWS()
        }
        def periodUnits = PeriodUnitDTO.list()
		
        def orderPeriods = OrderPeriodDTO.createCriteria().list() {
            eq('company', CompanyDTO.get(session['company_id'])) }
		log.debug "Order Period is: " + orderPeriods
        def crumbName = params.id ? 'update' : 'create'
        def crumbDescription = params.id ? accountType?.getDescription(session['language_id']) : null
        breadcrumbService.addBreadcrumb(controllerName, actionName, crumbName, params.int('id'), crumbDescription?.content)
		
		def selectedPaymentMethodTypeIds
        def globalPaymentMethodIds
        globalPaymentMethodIds=  PaymentMethodTypeDTO.findAllByAllAccountTypeAndEntity(true,CompanyDTO.get(session['company_id']))*.id
        selectedPaymentMethodTypeIds = accountType.paymentMethodTypeIds?.toList()
        render view: 'edit', model: [
                clone: params.clone,
                accountType: accountType,
                company: retrieveCompany(),
                periodUnits: periodUnits,
                orderPeriods: orderPeriods,
                currencies: retrieveCurrencies(),
				selectedPaymentMethodTypeIds :selectedPaymentMethodTypeIds,
                globalPaymentMethodIds :globalPaymentMethodIds,
				paymentMethodTypes : PaymentMethodTypeDTO.findAllByEntity(CompanyDTO.get(session['company_id'])),
        ]
    }

    def save (){
        AccountTypeWS ws = new AccountTypeWS();
        bindData(ws, params)
        log.debug ws
        if (params.description) {
            InternationalDescriptionWS descr =
                new InternationalDescriptionWS(session['language_id'] as Integer, params.description)
            log.debug descr
            ws.descriptions.add descr
        }
        ws.setEntityId(session['company_id'].toInteger())
        if (params.mainSubscription) {
            def mainSubscription = new MainSubscriptionWS()
            bindData(mainSubscription, params, 'mainSubscription')
            ws.setMainSubscription(mainSubscription)
            log.debug("Main Subscrption ${mainSubscription}")
        }
		if(params?.infoTypeName) {
			ws.preferredNotificationAitId = params?.infoTypeName as Integer
		}
		
        def periodUnits = PeriodUnitDTO.list()
        def orderPeriods = OrderPeriodDTO.createCriteria().list() { eq('company', CompanyDTO.get(session['company_id'])) }
        def globalPaymentMethodIds
        globalPaymentMethodIds=  PaymentMethodTypeDTO.findAllByAllAccountTypeAndEntity(true,CompanyDTO.get(session['company_id']))*.id

        try {
            if (params.clone == "true") {
                ws.setId(null);
                webServicesSession.createAccountType(ws);
                flash.message = 'config.account.type.created'
            } else if (!params.clone && ws.id) {
                webServicesSession.updateAccountType(ws);
                flash.message = 'config.account.type.updated'
            }
            else{
                webServicesSession.createAccountType(ws);
                flash.message = 'config.account.type.created'
            }
            if(!ws.paymentMethodTypeIds && globalPaymentMethodIds.size==0) {
                flash.info= g.message(code: 'accountTypeWS.info.for.customer',default: 'Any customers created with this Account Type will not have a Payment Method.')
            }

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
            def selectedPaymentMethodTypeIds
            selectedPaymentMethodTypeIds = ws.paymentMethodTypeIds?.toList()
            render view:'edit', model: [
                    accountType: ws,
                    company: retrieveCompany(),
                    periodUnits: periodUnits,
                    orderPeriods: orderPeriods,
                    selectedPaymentMethodTypeIds:selectedPaymentMethodTypeIds,
                    globalPaymentMethodIds:globalPaymentMethodIds,
                    currencies: retrieveCurrencies(),
					paymentMethodTypes : PaymentMethodTypeDTO.findAllByEntity(CompanyDTO.get(session['company_id'])),
                    selectedPaymentMethodTypeIds :ws?.paymentMethodTypeIds?.toList()
            ]
            return
        } catch (Exception e) {
            log.error e.getMessage()
            flash.error = 'config.account.type.saving.error'
        }
        redirect(action: 'list')
    }

    def show (){
        def accountType = AccountTypeDTO.get(params.int('id'))
        breadcrumbService.addBreadcrumb(controllerName, 'list', null, accountType.id, accountType.getDescription(session['language_id']))

        render template: 'accountType', model: [selected: accountType, infoTypeName:AccountInformationTypeDTO.get(accountType?.preferredNotificationAitId)?.name]
    }

    def delete (){
        log.debug 'delete called on ' + params.id
        if (params.id) {
            def accountType = AccountTypeDTO.get(params.int('id'))
            if (accountType) {
                try {
                    boolean retVal = webServicesSession.deleteAccountType(params.id?.toInteger());
                    if (retVal) {
                        flash.message = 'config.account.type.delete.success'
                        flash.args = [params.id]
                    } else {
                        flash.info = 'config.account.type.delete.failure'
                    }
                } catch (SessionInternalError e) {
                    viewUtils.resolveException(flash, session.locale, e);
                } catch (Exception e) {
                    log.error e.getMessage()
                    flash.error = 'config.account.type.delete.error'
                }
            }
        }
        params.applyFilter = false
        params.id = null
		getList(params)
    }

    def showAIT () {

        AccountInformationTypeDTO ait = AccountInformationTypeDTO.get(params.int('id'))
        AccountTypeDTO accountType = AccountTypeDTO.get(params.int('accountTypeId').toInteger())
        if (!ait) {
            log.debug "redirecting to list"
            redirect(action: 'listAIT')
            return
        }

        if (params.template) {
            // render requested template
            render template: params.template, model: [ selected: ait, accountType: accountType]

        } else {

            //search for AITs for the selected account type
            def aits = AccountInformationTypeDTO.createCriteria().list(
                    max:    params.max,
                    offset: params.offset,
                    sort:   params.sort,
                    order:  params.order
            ) {
                eq("accountType.id", accountType?.id.toInteger())
            }

            render view: 'listAIT', model: [ selected: ait, aits: aits, accountType: accountType ]
        }
    }

    def deleteAIT (){

        def accountInformationTypeId = params.int('id')
        log.debug 'AIT delete called on ' + accountInformationTypeId

        try {
            webServicesSession.deleteAccountInformationType(params.id?.toInteger());
            flash.message = 'config.account.information.type.delete.success'
            flash.args = [params.id]
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
        } catch (Exception e) {
            log.error e.getMessage()
            flash.error = 'config.account.information.type.delete.error'
        }

        params.id = null
        listAIT()

    }

    def listAIT (){

        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order

        def accountType = AccountTypeDTO.get(params.int('accountTypeId'))
        def ait = AccountInformationTypeDTO.get(params.int('id'))

        def aits = webServicesSession.getInformationTypesForAccountType(params.accountTypeId.toInteger())

        if (params?.applyFilter || params?.partial) {
            render template: 'accountInformationTypes', model: [ aits: aits, accountType: accountType, selected: ait]
        } else {
            render view: 'listAIT', model: [ aits: aits, accountType: accountType, selected: ait ]
        }
    }

    def editAITFlow ={

        initialize {
            action {

                AccountInformationTypeWS ait = params.id ? webServicesSession.getAccountInformationType(params.int('id')) :
                    new AccountInformationTypeWS();
                  
                if (!ait) {
                    log.error("Could not fetch WS object")
                    aitNotFoundErrorRedirect(params.id, params.accountTypeId)
                    return
                }

                def accountType = AccountTypeDTO.get(params.int('accountTypeId'))
                def company = CompanyDTO.get(session['company_id'])
                def currencies =  new CurrencyBL().getCurrenciesWithoutRates(session['language_id'].toInteger(),
                        session['company_id'].toInteger(),true)


                // set sensible defaults for new ait
                if (!ait.id || ait.id == 0) {
                    ait.accountTypeId = accountType?.id
                    ait.entityType = EntityType.ACCOUNT_TYPE
                    ait.entityId = session['company_id'].toInteger()
                    ait.metaFields  = []
                }


                if (params.clone == "true") {
                    ait.setId(0);
                    if (ait.getMetaFields() != null) {
                        for (MetaFieldWS mf : ait.getMetaFields()) {
                            mf.setId(0);
                            mf.setPrimary(false);
                        }
                    }
                }

                // available metafields and metafield groups
                def metaFields=retrieveMetaFieldsForAccountType()
                def metaFieldGroups = retrieveMetaFieldGroupsForAccountType()

				conversation.acctInfoType = removeUsedAit(ait)
				
                // model scope for this flow
                flow.accountType = accountType
                flow.company = company
                flow.currencies = currencies
				
                // conversation scope
                conversation.ait = ait
                conversation.metaFieldGroups = metaFieldGroups
                conversation.metaFields = metaFields
            }
            on("success").to("build")
        }

        /**
         * Renders the ait details tab panel.
         */
        showDetails {
            action {
                params.template = 'detailsAIT'
            }
            on("success").to("build")
        }

        /**
         * Renders the metafields tab panel, containing all the metafields that can be imported
         */
        showMetaFields {
            action {
                params.template = 'metafieldsAIT'
            }
            on("success").to("build")
        }

        /**
         * Renders the metafield groups tab panel, containing all the metafield groups that can be used as a template
         * for creation of the account information type
         */
        showMetaFieldGroups {
            action {
                params.template = 'metafieldGroupsAIT'
            }
            on("success").to("build")
        }

        /**
         *  Imports the selected metafield groups for using as a template for account information type creation
         *  Is available only for creating new information types
         */
        importFromMetaFieldGroup {
            action {

                def metaFieldGroupId = params.int('id')
                def metaFieldGroup = webServicesSession.getMetaFieldGroup(metaFieldGroupId)

                if (!metaFieldGroup) {
                    params.template = 'reviewAIT'
                    error()

                } else {

                    def ait = conversation.ait

                    ait.name = metaFieldGroup.getDescription()
                    ait.displayOrder = metaFieldGroup.displayOrder
                    ait.metaFields = []

                    def metaFields = ait.metaFields as List
                    metaFields.addAll(metaFieldGroup.metaFields as List)
                    metaFields.each {
                        it.setId(0)
                        it.setPrimary(false);
                    }

                    ait.metaFields = metaFields.toArray()

                    conversation.ait = ait

                    params.newLineIndex = metaFields.size() - 1
                    params.template = 'reviewAIT'
                }
            }
            on("success").to("build")
            on("error").to("build")
        }


        /**
         * Adds a metafield to the account information type
         */
        addAITMetaField {
            action {

                def metaFieldId = params.int('id')

                def metaField = metaFieldId ? webServicesSession.getMetaField(metaFieldId) :
                    new MetaFieldWS();

                metaField.primary = false

                if (metaField?.id || metaField.id != 0) {
                    // set metafield defaults
                    metaField.id = 0
                } else {
                    metaField.entityType = EntityType.ACCOUNT_TYPE
                    metaField.entityId = session['company_id'].toInteger()
                }

                // add metafield to ait
                def ait = conversation.ait
                def metaFields = ait.metaFields as List
                metaFields.add(metaField)
                ait.metaFields = metaFields.toArray()

                conversation.ait = ait
				
				conversation.acctInfoType = removeUsedAit(ait)
				
                params.newLineIndex = metaFields.size() - 1
                params.template = 'reviewAIT'
            }
            on("success").to("build")
        }

        /**
         * Updates an metafield  and renders the AIT metafields panel
         */
        updateAITMetaField {
            action {

                flash.errorMessages = null
                flash.error = null
                def ait = conversation.ait

                // get existing metafield
                def index = params.int('index')
                def metaField = ait.metaFields[index]

                if (!bindMetaFieldData(metaField, params, index)) {
                    error()
                }

                // add metafield to the ait
                ait.metaFields[index] = metaField

                // sort metafields by displayOrder
                ait.metaFields = ait.metaFields.sort { it.displayOrder }
                conversation.ait = ait
				
				conversation.acctInfoType = removeUsedAit(ait)

                params.template = 'reviewAIT'
            }
            on("success").to("build")
        }

        /**
         * Remove a metafield from the information type  and renders the AIT metafields panel
         */
        removeAITMetaField {
            action {

                def ait = conversation.ait

                def index = params.int('index')
                def metaFields = ait.metaFields as List

                def metaField = metaFields.get(index)
                metaFields.remove(index)

                ait.metaFields = metaFields.toArray()

                conversation.ait = ait

                params.template = 'reviewAIT'
            }
            on("success").to("build")
        }

        /**
         * Updates account information type attributes
         */
        updateAIT {
            action {

                def ait = conversation.ait
                bindData(ait, params)

                ait.metaFields = ait.metaFields.sort { it.displayOrder }
                conversation.ait = ait

                params.template = 'reviewAIT'
            }
            on("success").to("build")
        }

        /**
         * Shows the account information type metafield builder.
         *
         * If the parameter 'template' is set, then a partial view template will be rendered instead
         * of the complete 'build.gsp' page view (workaround for the lack of AJAX support in web-flow).
         */
        build {
            on("details").to("showDetails")
            on("metaFields").to("showMetaFields")
            on("metaFieldGroups").to("showMetaFieldGroups")
            on("addMetaField").to("addAITMetaField")
            on("importMetaFieldGroup").to("importFromMetaFieldGroup")
            on("updateMetaField").to("updateAITMetaField")
            on("removeMetaField").to("removeAITMetaField")
            on("update").to("updateAIT")

            on("save").to("saveAIT")

            on("cancel").to("finish")
        }

        /**
         * Saves the account information type and exits the builder flow.
         */
        saveAIT {
            action {
                try {

                    def ait = conversation.ait
                    Set<MetaField> metaFields = ait.metaFields
                    Set<String> mfNames = metaFields*.name
                    if (metaFields.size() != mfNames.size()) {
                        throw new SessionInternalError("MetaField", ["AccountInformationTypeDTO,metafield,metaField.name.exists"] as String[])
                    }

                    if (!ait.id || ait.id == 0) {

                        ait.id = webServicesSession.createAccountInformationType(ait)
                        session.message = 'account.information.type.created'
                        session.args = [ ait.id ]

                    } else {
                        webServicesSession.updateAccountInformationType(ait)

                        session.message = 'account.information.type.updated'
                        session.args = [ ait.id ]
                    }

                } catch (SessionInternalError e) {
                    viewUtils.resolveException(flow, session.locale, e)
                    error()
                }
            }
            on("error").to("build")
            on("success").to("finish")
        }

        finish {
            redirect controller: 'accountType', action: 'listAIT',
                    id: conversation.ait?.id, params: [accountTypeId: conversation.ait?.accountTypeId]
        }
    }

    private void aitNotFoundErrorRedirect(aitId, accountTypeId) {
        session.error = 'ait.not.found'
        session.args = [ aitId as String ]
        redirect controller: 'accountType', action: 'listAIT',
                params: [accountTypeId: accountTypeId]
    }

	private boolean bindMetaFieldData(MetaFieldWS metaField, params, index){
        try{
            MetaFieldBindHelper.bindMetaFieldName(metaField, params, false, index.toString())
        } catch (Exception e){
            log.debug("Error at binding meta field  : "+e)
            return false;
        }

        return true

	}

    def retrieveCompany() {
        CompanyDTO.get(session['company_id'])
    }

    def retrieveCurrencies() {
        def currencies = new CurrencyBL().getCurrencies(session['language_id'].toInteger(), session['company_id'].toInteger())
        return currencies.findAll { it.inUse }
    }
	
	private def retrieveMetaFieldsForAccountType(){
        def types = new EntityType[1];
        types[0] = EntityType.ACCOUNT_TYPE
        new MetaFieldDAS().getAvailableFields(session['company_id'], types, true)
    }

	private def retrieveMetaFieldGroupsForAccountType(){
        return new MetaFieldGroupDAS().getAvailableFieldGroups(session['company_id'], EntityType.ACCOUNT_TYPE)
   }
	
	private def removeUsedAit(ait) {
		def acctInfoType = MetaFieldType.values()
		ait?.metaFields.each {
		   acctInfoType -= it.fieldUsage
	   }
		return acctInfoType
	}	
	
}
