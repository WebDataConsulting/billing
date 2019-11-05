package jbilling

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


import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.mediation.MediationConfigurationWS
import com.sapienter.jbilling.server.mediation.db.MediationConfiguration
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDAS
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO
import com.sapienter.jbilling.server.util.ServerConstants
import grails.plugin.springsecurity.annotation.Secured

/**
 * MediationConfigController
 *
 * @author Vikas Bodani
 * @since 15-Feb-2011
 */

@Secured(["isAuthenticated()"])
class MediationConfigController {

    static pagination = [max: 10, offset: 0]

    def webServicesSession
    def viewUtils
    def breadcrumbService
    PluggableTaskDAS pluggableTaskDAS

    def index() {
        list()
    }

    def list() {

        def configurations = webServicesSession.getAllMediationConfigurations()

        def selected = null;
        if (params['id']) {
            selected = MediationConfiguration.get(params.int('id'))
            if (selected) {
                breadcrumbService.addBreadcrumb(controllerName, actionName, null, params.int('id'))
            }
        }

        if (params.applyFilter) {
            //flash.message=flash.message
            render template: 'configs', model: [types: configurations, readers: readers, selected: selected]
        } else {
            render view: 'list', model: [types: configurations, readers: readers, selected: selected]
        }
    }

    def show() {

        def configId = params.int('id')

        log.debug "Show config id $params.id"

        def config = MediationConfiguration.get(configId)

        if (config.entityId != session['company_id']) {
            flash.error = 'configuration.does.not.exists.for.entity'
            list()
        }

        if (config) {
            breadcrumbService.addBreadcrumb(controllerName, 'list', config?.name, configId)
        }
        render template: 'show', model: [selected: config]

    }

    def edit() {

        def configId = params.int('id')

        def config = configId ? MediationConfiguration.get(configId) : null

        def crumbName = configId ? 'update' : 'create'
        def crumbDescription = params.id ? config?.name : null
        breadcrumbService.addBreadcrumb(controllerName, actionName, crumbName, configId, crumbDescription)

        render template: 'edit', model: [config: config, readers: readers]
    }

    def triggerMediation() {

        def configId = params.int('id')

        def config = configId ? MediationConfiguration.get(configId) : null

        if (config && config.getEntityId() == session['company_id'] as Integer) {
            Integer processId = webServicesSession.triggerMediationByConfiguration(configId)
            if (processId) {
                log.debug "Trigered mediation wirh process id ${processId}"
                flash.message = 'mediation.trigger.success'
                flash.args = [processId]
            } else {
                flash.error = 'mediation.trigger.failure'
                flash.info = 'Mediation may already be running'
            }
        } else {
            flash.error = 'mediation.trigger.failure'
        }

        redirect action: 'list', model: [id: configId]
    }

    def save() {

        def ws = new MediationConfigurationWS()

        bindData(ws, params)

        if (params.int('id') > 0) {
            log.debug "config exists.."
            webServicesSession.updateAllMediationConfigurations([ws])
            flash.message = 'mediation.config.update.success'
        } else {
            log.debug "New config.."
            ws.setCreateDatetime new Date()
            ws.setEntityId webServicesSession.getCallerCompanyId()
            webServicesSession.createMediationConfiguration(ws)
            flash.message = 'mediation.config.create.success'
        }

        redirect action: 'list'
    }

    def delete() {

        try {
            webServicesSession.deleteMediationConfiguration(params.int('id'))
            flash.message = 'mediation.config.delete.success'
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
        } catch (Exception e) {
            log.error e.getMessage()
            flash.error = 'mediation.config.delete.failure'
        }

        // render list
        params.applyFilter = true
        list()

    }

    private List<PluggableTaskDTO> getReaders() {
        def readers = new ArrayList<PluggableTaskDTO>()
        if (session.company_id) {
            readers = pluggableTaskDAS.findByEntityCategory(session.company_id as Integer, ServerConstants.PLUGGABLE_TASK_MEDIATION_READER);
        }
        return readers
    }

}
