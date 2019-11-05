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

import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.common.CommonConstants
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.notification.MessageDTO
import com.sapienter.jbilling.server.notification.MessageSection
import com.sapienter.jbilling.server.notification.db.NotificationMessageDAS
import com.sapienter.jbilling.server.notification.db.NotificationMessageDTO
import com.sapienter.jbilling.server.notification.db.NotificationMessageTypeDAS
import com.sapienter.jbilling.server.notification.db.NotificationMessageTypeDTO
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.util.*
import com.sapienter.jbilling.server.util.db.*
import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap

@Secured(["isAuthenticated()"])
class NotificationsController {

    static pagination = [ max: 10, offset: 0, sort: 'id', order: 'desc' ]

    static final viewColumnsToFields =
            ['categoryId': 'id',
             'notificationId': 'id']

	static scope = "prototype"

    IWebServicesSessionBean webServicesSession
    def breadcrumbService
    def viewUtils

	def index () {
        redirect(action: 'list')
    }

    def preferences () {
        render template: "preferences", model: [subList: getPreferenceMapByTypeId()]
    }

    def list () {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order

        log.debug "METHOD: list\nId=${params.id} selectedId= ${params.selectedId}"

        Integer categoryId = params.int('id')
        def category = categoryId ? NotificationCategoryDTO.get(categoryId) : null
        breadcrumbService.addBreadcrumb(controllerName, actionName, null, categoryId)

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], CommonConstants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid) {
            if (params.template || params.partial) {
                render template: 'notificationsTemplate', model: [categoryId: categoryId]
            } else {
                render(view: 'list', model: [selected: categoryId])
            }
            return
        }

        def categories = getCategories(params)
        def notifications = category ? getNotifications(categoryId, params) : null
		boolean isFromConfiguration = true

        if (!(categoryId) && params.partial) {
            render template: 'categoriesTemplate', model: [selected: categoryId, lst: categories, lstByCategory: notifications]
        } else if (params.template || params.partial) {
            render template: 'notificationsTemplate', model: [lstByCategory: notifications, selected: categoryId]
        } else {
            render(view: 'list', model: [selected: categoryId, lst: categories, lstByCategory: notifications, isFromConfiguration: isFromConfiguration])
        }
    }

    def findCategories () {
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def prefs = getCategories(params)

        try {
            def jsonData = getNotificationJsonData(prefs, params)

            render jsonData as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }
    }

    def findNotifications () {
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        Integer categoryId = params.int('id')
        def prefs = getNotifications(categoryId, params)

        try {
            def jsonData = getNotificationJsonData(prefs, params)

            render jsonData as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }
    }

    /**
     * Converts Notification Categories or Notification Message to JSon
     */
    private def Object getNotificationJsonData(categories, GrailsParameterMap params) {
        def jsonCells = categories
        def currentPage = params.page ? Integer.valueOf(params.page) : 1
        def rowsNumber = params.rows ? Integer.valueOf(params.rows): 1
        def totalRecords =  jsonCells ? jsonCells.totalCount : 0
        def numberOfPages = Math.ceil(totalRecords / rowsNumber)

        def jsonData = [rows: jsonCells, page: currentPage, records: totalRecords, total: numberOfPages]

        jsonData
    }

    private def getCategories(GrailsParameterMap params) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order
        def languageId = session['language_id']

        return NotificationCategoryDTO.createCriteria().list(
                max: params.max,
                offset: params.offset
        ){
            if (params.categoryId){
                def searchParam = params.categoryId
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
                                             (select b.id from jbilling_table b where b.name = ? )
                                            and a.language_id = ?
                                            and lower(a.content) like ?
                                        )
                                    """, [ServerConstants.TABLE_NOTIFICATION_CATEGORY, languageId, "%" + searchParam + "%"]
                    )
                }
            }
            SortableCriteria.sort(params, delegate)
        }
    }

    private def getNotifications(Integer categoryId, GrailsParameterMap params) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        def languageId = session['language_id']

        return NotificationMessageTypeDTO.createCriteria().list(
                max: params.max,
                offset: params.offset
        ){
            eq('category.id', categoryId)
            if (params.notificationId){
                def searchParam = params.notificationId
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
                                             (select b.id from jbilling_table b where b.name = ? )
                                            and a.language_id = ?
                                            and lower(a.content) like ?
                                        )
                                    """, [ServerConstants.TABLE_NOTIFICATION_MESSAGE_TYPE, languageId, "%" + searchParam + "%"]
                    )
                }
            }
            SortableCriteria.sort(params, delegate)
        }
    }

    def show () {
        log.debug "METHOD: show"
        log.debug "Id is= ${params.id}"
        Integer messageTypeId = params.id.toInteger()

        Integer _languageId = session['language_id']
        if (params.get('language.id')) {
            log.debug "params.language.id is not null= ${params.get('language.id')}"
            _languageId = params.get('language.id')?.toInteger()
            log.debug "setting language id from requrest= ${_languageId}"
        }

        Integer entityId = webServicesSession.getCallerCompanyId();

        NotificationMessageTypeDTO typeDto = NotificationMessageTypeDTO.findById(messageTypeId)
        NotificationMessageDTO dto = null
        for (NotificationMessageDTO messageDTO : typeDto.getNotificationMessages()) {
            if (messageDTO?.getEntity()?.getId()?.equals(entityId)
                    && messageDTO.getLanguage().getId().equals(_languageId)) {
                dto = messageDTO;
                break;
            }
        }

        if (params.template) {
            // render requested template, usually "_show.gsp"
            render template: "show", model: [typeDto: typeDto, dto: dto, messageTypeId: messageTypeId, languageDto: LanguageDTO.findById(_languageId), entityId: entityId]
        } else {
            Integer categoryId = typeDto?.getCategory()?.getId()
            def category = categoryId ? NotificationCategoryDTO.get(categoryId) : null
            def categories = getCategories(params)
            def notifications = category ? getNotifications(categoryId, params) : null

            render(view: 'list', model: [
                    selectedNotification:typeDto.id, selected: categoryId, lst: categories,
                    lstByCategory: notifications,typeDto: typeDto, dto: dto, messageTypeId: messageTypeId,
                    languageDto: LanguageDTO.findById(_languageId), entityId: entityId])
        }

    }

    private getPreferenceMapByTypeId() {
        Map<PreferenceDTO> subList = new HashMap<PreferenceDTO>();
        List<PreferenceDTO> masterList = PreferenceDTO.findAllByForeignId(webServicesSession.getCallerCompanyId())
        log.debug "masterList.size= ${masterList.size()}"
        for (PreferenceDTO dto : masterList) {
            Integer prefid = dto.getPreferenceType().getId()
            switch (prefid) {
                case ServerConstants.PREFERENCE_TYPE_SELF_DELIVER_PAPER_INVOICES:
                case ServerConstants.PREFERENCE_TYPE_INCLUDE_CUSTOMER_NOTES:
                case ServerConstants.PREFERENCE_TYPE_DAY_BEFORE_ORDER_NOTIF_EXP:
                case ServerConstants.PREFERENCE_TYPE_DAY_BEFORE_ORDER_NOTIF_EXP2:
                case ServerConstants.PREFERENCE_TYPE_DAY_BEFORE_ORDER_NOTIF_EXP3:
                case ServerConstants.PREFERENCE_TYPE_USE_INVOICE_REMINDERS:
                case ServerConstants.PREFERENCE_TYPE_NO_OF_DAYS_INVOICE_GEN_1_REMINDER:
                case ServerConstants.PREFERENCE_TYPE_NO_OF_DAYS_NEXT_REMINDER:
                    log.debug "Adding dto: ${dto.getPreferenceType().getId()}"
                    subList.put(dto.getPreferenceType().getId(), dto)
                    break;
            }
        }
        subList
    }

    def cancelEditPrefs () {
        render view: "viewPrefs", model: [lst: NotificationCategoryDTO.list(), subList: getPreferenceMapByTypeId()]
    }

    def editPreferences () {

        Map<PreferenceDTO> subList = new HashMap<PreferenceDTO>();
        List<PreferenceDTO> masterList = PreferenceDTO.findAllByForeignId(webServicesSession.getCallerCompanyId())
        log.debug "masterList.size= ${masterList.size()}"
        for (PreferenceDTO dto : masterList) {
            Integer prefid = dto.getPreferenceType().getId()
            switch (prefid) {
                case ServerConstants.PREFERENCE_TYPE_SELF_DELIVER_PAPER_INVOICES:
                case ServerConstants.PREFERENCE_TYPE_INCLUDE_CUSTOMER_NOTES:
                case ServerConstants.PREFERENCE_TYPE_DAY_BEFORE_ORDER_NOTIF_EXP:
                case ServerConstants.PREFERENCE_TYPE_DAY_BEFORE_ORDER_NOTIF_EXP2:
                case ServerConstants.PREFERENCE_TYPE_DAY_BEFORE_ORDER_NOTIF_EXP3:
                case ServerConstants.PREFERENCE_TYPE_USE_INVOICE_REMINDERS:
                case ServerConstants.PREFERENCE_TYPE_NO_OF_DAYS_INVOICE_GEN_1_REMINDER:
                case ServerConstants.PREFERENCE_TYPE_NO_OF_DAYS_NEXT_REMINDER:
                    log.debug "Adding dto: ${dto.getPreferenceType().getId()}"
                    subList.put(dto.getPreferenceType().getId(), dto)
                    break;
            }
        }
        breadcrumbService.addBreadcrumb(controllerName, actionName, null, null)
        [subList: subList, languageId: session['language_id']]
    }

    def savePrefs () {
        log.debug "pref[5].value= ${params.get('pref[5].value')}"
        List<PreferenceWS> prefDTOs

        try {
            prefDTOs = bindDTOs(params)
        } catch (SessionInternalError e) {
            viewUtils.resolveExceptionForValidation(flash, session.locale, e);
            redirect action: "editPreferences"
        }
        log.debug "Calling: webServicesSession.saveNotificationPreferences(prefDTOs); List Size: ${prefDTOs.size()}"
        PreferenceWS[] array = new PreferenceWS[prefDTOs.size()]
        array = prefDTOs.toArray(array)
        try {
            webServicesSession.updatePreferences(array)
        } catch (SessionInternalError e) {
            log.error "Error: ${e.getMessage()}"
            flash.errorMessages = e.getErrorMessages()
            //boolean retValue = viewUtils.resolveExceptionForValidation(flash, session.'org.springframework.web.servlet.i18n.SessionLocaleResolver.LOCALE', e);
        }
        log.debug "Finished: webServicesSession.saveNotificationPreferences(prefDTOs);"
        if (flash.errorMessages?.size() > 0) {
            redirect(action: 'editPreferences')
        } else {
            flash.message = 'preference.saved.success'
            redirect(action: 'cancelEditPrefs')
        }
    }


    def List<PreferenceWS> bindDTOs(params) {
        log.debug "bindDTOs"
        List<PreferenceWS> prefDTOs = new ArrayList<PreferenceWS>();

        def count = params.recCnt.toInteger()

        for (int i = 0; i < count; i++) {
            log.debug "loop= ${params.get("pref[" + i + "].id")}"
            PreferenceWS dto = new PreferenceWS()
            dto.setPreferenceType(new PreferenceTypeWS())

            dto.setForeignId(webServicesSession.getCallerCompanyId())

            bindData(dto, params["pref[" + i + "]"])

            switch (i) {
                case 0:
                case 1:
                case 5:
                    if (params["pref[" + i + "].value"]) {
                        dto.setValue("1")
                    } else {
                        dto.setValue("0")
                    }
                    break;
                default:
                    if (params["pref[" + i + "].value"]) {
                        def val = params["pref[" + i + "].value"]
                        try {
                            Integer value = val.toInteger()
                            dto.setValue(value?.toString())
                        } catch (NumberFormatException e) {
                            SessionInternalError exception = new SessionInternalError("Validation of Preference Value");
                            String[] errmsgs = new String[1]
                            errmsgs[0] = "PreferenceWS,intValue,validation.error.nonnumeric.days.order.notification," + val;
                            exception.setErrorMessages(errmsgs);
                            throw exception;
                        }
                    } else {
                        dto.setValue("0")
                    }
            }
            log.debug "dto.intValue= ${dto.value}"
            prefDTOs.add(dto);
        }
        return prefDTOs;
    }

    def edit () {
        log.debug "METHOD: edit"

        //set cookies here..
        log.debug("doNotAskAgain= ${params.doNotAskAgain}, askPreference= ${params.askPreference}")

        def askPreference = request.getCookie("doNotAskAgain")
        log.debug("Cooke set to was= ${askPreference}")
        if ("true".equals(params.doNotAskAgain)) {
            response.setCookie('doNotAskAgain', String.valueOf(params.askPreference), 604800)
            log.debug("Setting the cookie to value ${params.askPreference}")
            askPreference = params.askPreference
        }

        log.debug "Id is= ${params.id}"
        Integer messageTypeId = params.id?.toInteger()

        if (!messageTypeId) {
            redirect action: 'list'
        }

        Integer _languageId = session['language_id']
        if (params.get('language.id')) {
            log.debug "Param 'language.id' is Not Null [${params.language.id}]"
            _languageId = params.get('language.id')?.toInteger()
        }
        Integer entityId = webServicesSession.getCallerCompanyId()?.toInteger()

        log.debug "Language Id Set to ${_languageId}, Entity ${entityId}, askPreference= ${askPreference}"

        NotificationMessageTypeDTO typeDto = messageTypeId ? NotificationMessageTypeDTO.findById(messageTypeId) : null
        NotificationMessageDTO dto = null
        for (NotificationMessageDTO messageDTO : typeDto?.getNotificationMessages()) {
            if (messageDTO?.getEntity()?.getId().equals(entityId)
                    && messageDTO?.getLanguage()?.getId().equals(_languageId)) {
                dto = messageDTO;
                break;
            }
        }
        // If notification under category "Order" then show order specific tokens
        def isOrderTypeNotification = typeDto?.category.equals(NotificationMessageTypeDTO.findById(ServerConstants.NOTIFICATION_TYPE_ORDER_EXPIRE_1).category)
        def isPaymentTypeNotification = typeDto?.category.equals(NotificationMessageTypeDTO.findById(ServerConstants.NOTIFICATION_TYPE_PAYMENT_FAILED).category)
        breadcrumbService.addBreadcrumb(controllerName, actionName, null, messageTypeId)

        [dto: dto, messageTypeId: messageTypeId, isOrderTypeNotification: isOrderTypeNotification, isPaymentTypeNotification: isPaymentTypeNotification, languageId: _languageId, entityId: entityId, askPreference: askPreference, includeAttachment: params.includeAttachment]
    }

    def saveAndRedirect () {
        log.debug "METHOD: saveAndRedirect"
        try {
            saveAction(params)
        } catch (SessionInternalError e) {
            log.error "Error: ${e.getMessage()}"
            flash.error = "error.illegal.modification"
        }
        redirect(action: 'edit', params: params)
    }

    def saveNotification () {

        log.debug "METHOD: saveNotification"
        def _id = params._id
        try {
            saveAction(params)
        } catch (SessionInternalError e) {
            log.error "Error: ${e.getMessage()}"
            flash.error = "error.illegal.modification"
        }
        if (_id)
            redirect(action: 'cancelEdit', params: [id: _id])
        else
            redirect(action: 'list')
    }

    def saveAction(params) {
        log.debug "METHOD: saveAction\nAll params\n${params}"

        NotificationMessageDTO msgDTO = new NotificationMessageDTO()
        msgDTO.setLanguage(new LanguageDTO())
        msgDTO.setEntity(new CompanyDTO())
        params.includeAttachment = params.includeAttachment == "on" ? Integer.valueOf(1) : Integer.valueOf(0)
        log.debug("binding data with params ${params}")
        bindData(msgDTO, params)
        def _id = null;
        if (params._id) {
            _id = params._id?.toInteger()
            msgDTO.setId(_id)
        }

        log.debug "useFlag: '${params.useFlag}', NotificationMessageDTO.useFlag=${msgDTO.getUseFlag()}"
        if ('on' == params.useFlag) {
            msgDTO.setUseFlag((short) 1)
        } else {
            msgDTO.setUseFlag((short) 0)
        }

        log.debug "NotificationMessageType ID=${_id}, Entity=${params.get('entity.id')?.toInteger()}, Language = ${params._languageId}"
        MessageDTO messageDTO = new MessageDTO()

        if (params._id) {
            messageDTO.setTypeId(_id)
        } else {
            messageDTO.setTypeId(saveNotificationMessageType(params))
        }
        messageDTO.setLanguageId(params.get('_languageId')?.toInteger())
        messageDTO.setUseFlag(1 == msgDTO.getUseFlag())
        messageDTO.setContent(bindSections(params))
        //setting additional params
        messageDTO.setAttachmentDesign(params.attachmentDesign)
        messageDTO.setAttachmentType(params.attachmentType)
        messageDTO.setIncludeAttachment(params.includeAttachment)

        Integer entityId = params.get('entity.id')?.toInteger()
        Integer messageId = null;
        if (params.msgDTOId) {
            messageId = params.msgDTOId.toInteger()
        } else {
            //new record
            messageId = null;
        }

        log.debug "msgDTO.language.id= ${messageDTO?.getLanguageId()}"
        log.debug "msgDTO.type.id= ${messageDTO?.getTypeId()}"
        log.debug "msgDTO.use.flag= ${messageDTO.getUseFlag()}"

        if (params.notifyAdmin) {
            messageDTO.setNotifyAdmin(1)
        } else {
            messageDTO.setNotifyAdmin(0)
        }

        if (params.notifyPartner) {
            messageDTO.setNotifyPartner(1)
        } else {
            messageDTO.setNotifyPartner(0)
        }

        if (params.notifyParent) {
            messageDTO.setNotifyParent(1)
        } else {
            messageDTO.setNotifyParent(0)
        }

        if (params.notifyAllParents) {
            messageDTO.setNotifyAllParents(1)
        } else {
            messageDTO.setNotifyAllParents(0)
        }
        messageDTO.setMediumTypes(msgDTO.getMediumTypes());
        log.debug "EntityId = ${entityId?.intValue()}, callerCompanyId= ${webServicesSession.getCallerCompanyId()?.intValue()}"
        if (entityId?.intValue() == webServicesSession.getCallerCompanyId()?.intValue()) {
            log.debug "Calling createUpdateNotifications..."
            try {
                webServicesSession.createUpdateNotification(messageId, messageDTO)
                flash.message = 'notification.save.success'
            } catch (Exception e) {
                log.error("ERROR: ${e.getMessage()}")
                throw new SessionInternalError(e)
            }
        } else {
            log.error("ERROR: Entity Idis do not match.")
            throw new SessionInternalError("Cannot update another company data.")
        }
    }

    def MessageSection[] bindSections(params) {
        log.debug "METHOD: bindSections"
        MessageSection[] lines = new MessageSection[3];
        Integer section = null;
        String content = null;
        MessageSection obj = null;

        for (int i = 1; i <= 3; i++) {
            log.debug "messageSections[" + i + "].section= ${params.get("messageSections[" + i + "].section")}"
            log.debug "messageSections[" + i + "].id= ${params.get("messageSections[" + i + "].id")}"

            if (params.get("messageSections[" + i + "].notificationMessageLines.content")) {
                content = params.get("messageSections[" + i + "].notificationMessageLines.content")
                obj = new MessageSection(i, content)
            } else {
                obj = new MessageSection(i, "")
            }
            lines[(i - 1)] = obj;
        }
        log.debug "Line 1= ${lines[0]}"
        log.debug "Line 2= ${lines[1]}"
        log.debug "Line 3= ${lines[2]}"
        return lines;
    }

    def saveAndCancel () {
        log.debug "METHOD: saveAndCancel"
        try {
            saveAction(params)
        } catch (SessionInternalError e) {
            log.error "Error: ${e.getMessage()}"
            flash.error = "error.illegal.modification"
        }
        redirect(action: 'cancelEdit', params: params)
    }

    def editCategory () {
        if (!params.categoryId && !params.boolean('add')) {
            flash.error = 'notification.category.not.selected'
            flash.args = [params.categoryId as String]

            redirect(controller: 'notifications', action: 'list')
            return
        } else {
            def category
            try {
                category = params.categoryId ? new NotificationCategoryDAS().find(params.int('categoryId')) : null
            } catch (SessionInternalError e) {
                log.error("Could not fetch object", e)

                flash.error = 'notification.category.not.found'
                flash.args = [params.categoryId as String]

                redirect controller: 'notifications', action: 'list'
                return
            }

            [category: category]
        }
    }

    def cancelEdit () {
        log.debug "METHOD: cancelEdit\nid=${params.id}"
        if (!params.id) {
            redirect(action: 'list')
            return
        }

        NotificationMessageTypeDTO typeDto = NotificationMessageTypeDTO.findById(Integer.parseInt(params["id"]))
        Integer entityId = webServicesSession.getCallerCompanyId()
        NotificationMessageDTO dto = null
        Integer languageId = session['language_id']
        for (NotificationMessageDTO messageDTO : typeDto.getNotificationMessages()) {
            if (messageDTO?.getEntity()?.getId()?.equals(entityId)
                    && messageDTO.getLanguage().getId().equals(languageId)) {
                dto = messageDTO;
                break;
            }
        }

        chain action: 'show', params: [id: typeDto.getId(), selected:typeDto.getCategory().getId()]

    }

    /**
     * Validate and save a category.
     */
    def saveCategory () {
        try {
            NotificationCategoryDTO nc
            if (params?.id) {
                nc = new NotificationCategoryDAS().find(params.int('id'))
            } else {
                nc = new NotificationCategoryDTO()
            }

            if (params?.description) {
                createUpdateNotificationCategory(nc, params?.description, session['language_id'])
                flash.message = 'notification.category.save.success'
                redirect(action: 'list')
            } else {
                flash.error = 'Description is required'
                redirect(action: 'editCategory')
            }
        } catch (Exception e) {
            log.error("ERROR: ${e.getMessage()}")
            throw new SessionInternalError(e)
        }
    }

    /**
     * Validate and save a category.
     */
    def saveCategory1 () {
        try {
            def das = new NotificationCategoryDAS()
            def catDTO
            if (params?.id) {
                catDTO = das.find(params.int('id'))
            } else {
                catDTO = new NotificationCategoryDTO()
            }

            log.debug "CATDescription ${params.description}"
            log.debug "IS ${catDTO.id} - ${!catDTO.id}"
            if (params?.description) {
                if (!catDTO.id) {
                    catDTO = das.save(catDTO)
                }
                log.debug "Category id = ${catDTO.id}"
                log.debug "Table name is ${ServerConstants.TABLE_NOTIFICATION_CATEGORY}"
                new InternationalDescriptionDAS().create(ServerConstants.TABLE_NOTIFICATION_CATEGORY, catDTO.id, "description", session['language_id'], params.description)
                catDTO = das.get(catDTO.id)
                flash.message = 'notification.category.save.success'
                redirect(action: 'list')
            } else {
                flash.error = 'Description is required'
                redirect(action: 'editCategory')
            }
        } catch (Exception e) {
            e.printStackTrace()
            log.error("ERROR: ${e.getMessage()}")
            throw new SessionInternalError(e)
        }
    }

    def editNotification () {
        if (params?.categoryId != null) {
            int categoryId = Integer.parseInt(params?.categoryId)
            def category = new NotificationCategoryDAS().find(categoryId)
            render template: 'editNotification', model: [category: category, selectedCategoryId: params.categoryId]
        } else {
            flash.error = "Category is not selected"
            redirect(action: 'list')
        }
    }

    def saveNotificationMessage () {
        if (saveNotificationMessageType(params)) {
            flash.message = 'Notification Saved Successfully'
            redirect action: 'list', params: [id: params.categoryId]
        }
    }

    Integer saveNotificationMessageType(params) {
        NotificationCategoryDTO notificationCategory = null

        if (!params.categoryId) {
            log.error("Category not selected.")
            flash.error = "Category not selected."

            return
        } else {
            notificationCategory = new NotificationCategoryDAS().find(params.int('categoryId'))
        }

        NotificationMessageTypeDTO notificationMessageType = new NotificationMessageTypeDTO()

        return createUpdateNotificationMessageType(notificationMessageType, notificationCategory, params?.description, session['language_id'])
    }

    Integer createUpdateNotificationCategory(def notificationCategoryDTO, String description, Integer language_id) {
        notificationCategoryDTO = new NotificationCategoryDAS().save(notificationCategoryDTO);
        new NotificationCategoryDAS().flush();

        notificationCategoryDTO.setDescription(description, language_id);

        log.debug("Notification category saved successfully ${notificationCategoryDTO.getId()}");
        return notificationCategoryDTO.getId();
    }

    void deleteNotificationCategory(def notificationCategoryDTO) {
        new NotificationCategoryDAS().delete(notificationCategoryDTO);
        log.debug("Notification category deleted successfully");
    }

    Integer createUpdateNotificationMessageType(def notificationMessageType, def notificationCategory, String description, Integer language_id) {
        notificationMessageType.setCategory(notificationCategory);
        notificationMessageType = new NotificationMessageTypeDAS().save(notificationMessageType);
        new NotificationMessageTypeDAS().flush();

        notificationMessageType.setDescription(description, language_id);

        log.debug("Notification message type saved successfully ${notificationMessageType.getId()}");
        return notificationMessageType.getId();
    }

    void deleteNotificationMessageType(def notificationMessageTypeDTO) {
        new NotificationMessageTypeDAS().delete(notificationMessageTypeDTO);
        log.debug("Notification message type deleted successfully");
    }

    void deleteNotificationMessage(def notificationMessage) {
        new NotificationMessageDAS().delete(notificationMessage);
        log.debug("Notification message deleted successfully");
    }
}
