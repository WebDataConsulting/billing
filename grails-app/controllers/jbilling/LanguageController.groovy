package jbilling

import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.server.util.LanguageWS
import com.sapienter.jbilling.server.util.db.LanguageDTO
import grails.plugin.springsecurity.annotation.Secured

@Secured(["isAuthenticated()"])
class LanguageController {

    def breadcrumbService
    def viewUtils
    def webServicesSession
    static pagination = [ max: 10, offset: 0, sort: 'id', order: 'desc' ]
    static versions = [ max: 25 ]
	
    def index (){
        redirect action: list, params: params
    }

    def getList(params) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order

        return LanguageDTO.createCriteria().list(
                max:    params.max,
                offset: params.offset
        ) {
            SortableCriteria.sort(params, delegate)
        }
    }

    def list (){
        List<LanguageDTO> languages = getList(params)
        LanguageDTO selected = params.id ? LanguageDTO.get(params.int('id')) : null
        breadcrumbService.addBreadcrumb(controllerName, 'list',
                null, selected?.id, selected?.getDescription())

        // if id is present and object not found, give an error message to the user along with the list
        if (params.id?.isInteger() && params.int("id") > 0 && selected == null) {
            flash.error = 'language.not.found'
            flash.args = [params.id]
        }
        if (params.partial){
            render template: 'languages', model: [ languages: languages, selected: selected, languageWS:chainModel?.languageWS]
        } else{
            render view: 'list', model: [ languages: languages, selected: selected, languageWS:chainModel?.languageWS]
        }
    }

    def show (){
        LanguageDTO language = LanguageDTO.get(params.int('id'))
        breadcrumbService.addBreadcrumb(controllerName, 'list', null, language.id, language.getDescription())
        render template: 'show', model: [ selected: language ]
    }

    def save(){
        Integer languageId=null
        LanguageWS languageWS=new LanguageWS();
        bindData(languageWS, params)
        try{
            languageId=webServicesSession.createOrEditLanguage(languageWS)
        }catch (Exception e){
            viewUtils.resolveException(flash, session.locale, e)
            chain(action: 'list', model: [languageWS:languageWS])
            return
        }
        if (!params.id || params.id=='0'){
            flash.message=message(code:"language.creation.success.message")
        }else{
            flash.message=message(code:"language.updation.success.message")
        }
        flash.args = [languageId]
        redirect(action: 'list', params: params+[id:languageId])
    }

    def edit (){
        if(request.xhr){
            LanguageDTO language = chainModel?.language ?: params.id ? LanguageDTO.get(params.int('id')) : new LanguageDTO()
            if (language == null) {
                redirect action: 'list', params:params
                return
            }
            breadcrumbService.addBreadcrumb(controllerName, 'edit', null, language.id, language.getDescription())
            render template: 'edit', model: [ language: language]
        }else{
            redirect(controller: 'language', action: 'list', params:params)
        }
    }
}
