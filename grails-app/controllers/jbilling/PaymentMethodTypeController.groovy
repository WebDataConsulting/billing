package jbilling

import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.server.item.CurrencyBL
import com.sapienter.jbilling.server.metafields.MetaFieldWS
import com.sapienter.jbilling.server.payment.PaymentMethodTypeWS
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDTO
import com.sapienter.jbilling.server.payment.db.PaymentMethodTemplateDTO
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.db.AccountTypeDTO
import com.sapienter.jbilling.client.metafield.MetaFieldBindHelper
import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.PreferenceBL

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.hibernate.criterion.MatchMode
import org.hibernate.criterion.Restrictions

/**
 * 
 * @author khobab
 *
 */
@Secured(["isAuthenticated()", "MENU_99"])
class PaymentMethodTypeController {
	
	static pagination = [max: 10, offset: 0, sort: 'id', order: 'desc']

    static final viewColumnsToFields =
            ['paymentMethodId': 'id']

	def breadcrumbService
	IWebServicesSessionBean webServicesSession
	def viewUtils
	
	/**
	 * Default landing action
	 */
	def index (){
		list()
	}

    def getList(params) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
		params.sort = params?.sort ?: pagination.sort
		params.order = params?.order ?: pagination.order

        return PaymentMethodTypeDTO.createCriteria().list(
                max:    params.max,
                offset: params.offset
        ) {
            eq('entity', new CompanyDTO(session['company_id']))
            if (params.paymentMethodId){
                def searchParam = params.paymentMethodId
                if (searchParam.isInteger()){
                    eq('id', Integer.valueOf(searchParam));
                } else {
                    addToCriteria(Restrictions.ilike("methodName", searchParam, MatchMode.ANYWHERE) );
                }
            }
            SortableCriteria.sort(params, delegate)
        }
    }

	def list (){
		breadcrumbService.addBreadcrumb(controllerName, actionName, null, null)
        def paymentMethod = params.id ? PaymentMethodTypeDTO.get(params.int('id')) : null

        if (params.id?.isInteger() && !paymentMethod) {
            flash.error = 'paymentMethod.not.found'
            flash.args = [params.id as String]
        }

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], Constants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            if (params.applyFilter || params.partial) {
                render template: 'paymentMethodsTemplate', model: [selected: paymentMethod]
            }else {
                render view: 'list', model: [selected: paymentMethod]
            }
            return
        }

        def paymentMethods = getList(params)
        if (params.applyFilter || params.partial) {
            render template: 'paymentMethodsTemplate', model: [paymentMethods: paymentMethods, selected: paymentMethod]
            return
        } else {
            render view: 'list', model: [paymentMethods: paymentMethods, selected: paymentMethod]
            return
        }
	}

    def findPaymentMethods (){
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def methods = getList(params)

        try {
            def jsonData = getAsJsonData(methods, params)

            render jsonData as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }
    }

    /**
     * Converts * to JSon
     */
    private def Object getAsJsonData(elements, GrailsParameterMap params) {
        def jsonCells = elements
        def currentPage = params.page ? Integer.valueOf(params.page) : 1
        def rowsNumber = params.rows ? Integer.valueOf(params.rows): 1
        def totalRecords =  jsonCells ? jsonCells.totalCount : 0
        def numberOfPages = Math.ceil(totalRecords / rowsNumber)

        def jsonData = [rows: jsonCells, page: currentPage, records: totalRecords, total: numberOfPages]

        jsonData
    }

	def show (){
		def paymentMethod = PaymentMethodTypeDTO.get(params.int('id'))

		breadcrumbService.addBreadcrumb(controllerName, 'show', null, paymentMethod.id, paymentMethod.methodName)

		render template: 'paymentMethodType', model: [selected: paymentMethod]
	}
	
	def edit (){
		def templateId
		def paymentMethodId
		def templates

		// Find out if template id is selected 
		templateId = params.templateId && params.templateId?.isInteger() ? params.int('templateId') : null
		paymentMethodId = params.id ? params.int('id') : null
		
		// if no template id or payment method id is given then select template page will be shown
		if(!templateId && !paymentMethodId){
			templates = PaymentMethodTemplateDTO.list()

			// Request is to create new payment method but there are no templates given
			if (!templates || templates.size == 0) {
				flash.error = message(code: 'paymentMethod.templates.not.available')
				redirect controller: 'paymentMethod', action: 'list'
			} else {
				render view: 'list', model: [ templates : templates ]
				return
			}
		}

		if(templateId) {
			redirect action : 'editPM', params : [ templateId : templateId]
		} else if(paymentMethodId) {
			redirect action : 'editPM', params : [ paymentMethodId : paymentMethodId]
		}
	}
	
	def delete (){
		if(params.id) {
			def paymentMethodType = PaymentMethodTypeDTO.get(params.int('id'))
			if(paymentMethodType) {	
				try {
					boolean retVal = webServicesSession.deletePaymentMethodType(params.id?.toInteger());
					if (retVal) {
						flash.message = 'config.paymentMethod.type.delete.success'
						flash.args = [params.id]
					} else {
						flash.info = 'config.paymentMethod.type.delete.failure'
					}
				} catch (SessionInternalError e) {
					System.out.println "I am here"
					viewUtils.resolveException(flash, session.locale, e);
				} catch (Exception e) {
					log.error e.getMessage()
					flash.error = 'config.paymentMethod.type.delete.error'
				}
			}
		}
		
		params.applyFilter = false
		params.id = null
		redirect (action: 'list')
	}

	def editPMFlow ={
		initialize {
			action {
				def templateId
				def paymentMethod
				def template
				def paymentMethodId = params.int('paymentMethodId')
				
				// if param id is given then its existing payment method. 
				if(paymentMethodId) {
					paymentMethod = webServicesSession.getPaymentMethodType(paymentMethodId)
					template = webServicesSession.getPaymentMethodTemplate(paymentMethod.templateId)
				} else {
					paymentMethod = new PaymentMethodTypeWS()
					template = webServicesSession.getPaymentMethodTemplate(params.int("templateId"))
					
					Set<MetaFieldWS> templateMetaFields = template?.metaFields
					MetaFieldWS[] metaFields
					if(templateMetaFields != null && templateMetaFields.size() > 0) {
						metaFields = new MetaFieldWS[templateMetaFields.size()]
						Integer i = 0
						for(MetaFieldWS metaField : templateMetaFields) {
							MetaFieldWS mf = copyMetaField(metaField)
							mf.entityId = session["company_id"] as Integer
							mf.entityType = EntityType.PAYMENT_METHOD_TYPE
							metaFields[i]= mf
							
							i++
						}
					} else {
						metaFields = new MetaFieldWS[0]
					}
					
					
					paymentMethod.metaFields = metaFields
					paymentMethod.templateId = template?.id
				}
				
				def accountTypes = AccountTypeDTO.createCriteria().list() {
					eq('company.id', session['company_id'] as Integer)
					order('id', 'asc')
				}
				def company = CompanyDTO.get(session['company_id'])
				def currencies =  new CurrencyBL().getCurrenciesWithoutRates(session['language_id'].toInteger(),
						session['company_id'].toInteger(),true)
				
				flow.template = template
				flow.availableAccountTypes = accountTypes
				flow.company = company
				flow.currencies = currencies
				
				
				conversation.paymentMethod = paymentMethod
			}
			on("success").to("build")
		}
		
		/**
		 * Renders the ait details tab panel.
		 */
		showDetails {
			action {
				params.template = 'detailsPM'
			}
			on("success").to("build")
		}
		
		addPMMetaField {
				action {
					def metaFieldId = params.int('id')
					
					def metaField = metaFieldId ? webServicesSession.getMetaField(metaFieldId) : new MetaFieldWS()
	
					metaField.primary = false
	
					if (metaField?.id || metaField.id != 0) {
						// set metafield defaults
						metaField.id = 0
					} else {
						metaField.entityType = EntityType.PAYMENT_METHOD_TYPE
						metaField.entityId = session['company_id'] as Integer
					}
					
					// add metafield to ait
					def paymentMethod = conversation.paymentMethod
					def metaFields = paymentMethod.metaFields as List
					metaFields.add(metaField)
					paymentMethod.metaFields = metaFields.toArray()
	
					conversation.paymentMethod = paymentMethod
	
					params.newLineIndex = metaFields.size() - 1
					params.template = 'reviewPM'
			}
			on("success").to("build")
		}
		
		updatePMMetaField {
				action {
					flash.errorMessages = null
					flash.error = null
					
					def paymentMethod = conversation.paymentMethod
	
					// get existing metafield
					def index = params.int('index')
					def metaField = paymentMethod.metaFields[index]
	
					if (!bindMetaFieldData(metaField, params, index)) {
						error()
					}
	
					// add metafield to the ait
					paymentMethod.metaFields[index] = metaField
	
					// sort metafields by displayOrder
					paymentMethod.metaFields = paymentMethod.metaFields.sort { it.displayOrder }
					conversation.paymentMethod = paymentMethod
	
					params.template = 'reviewPM'
			}
			on("success").to("build")
		}
		
		removePMMetaField {
				action {
					def paymentMethod = conversation.paymentMethod
					
					def index = params.int('index')
					def metaFields = paymentMethod.metaFields as List
	
					def metaField = metaFields.get(index)
					metaFields.remove(index)
	
					paymentMethod.metaFields = metaFields.toArray()
	
					conversation.paymentMethod = paymentMethod
	
					params.template = 'reviewPM'
			}
			on("success").to("build")
		}
		
		updatePM {
			action {
				def paymentMethod = conversation.paymentMethod
				bindData(paymentMethod, params)
				if(!params.allAccountType && !params.accountTypes){
					paymentMethod.setAccountTypes(new ArrayList<Integer>(0))
				}

				paymentMethod.metaFields = paymentMethod.metaFields.sort { it.displayOrder }
				conversation.paymentMethod = paymentMethod

				params.template = 'reviewPM'
			}
			on("success").to("build")
		}
		
		build {
			on("details").to("showDetails")
			on("addMetaField").to("addPMMetaField")
			on("updateMetaField").to("updatePMMetaField")
			on("removeMetaField").to("removePMMetaField")
			on("update").to("updatePM")

			on("save").to("savePM")
			on("cancel").to("finish")
		}
		
		savePM {
			action {
				try {
					def paymentMethod = conversation.paymentMethod

					Set<MetaFieldWS> metaFields = paymentMethod.metaFields
					Set<String> mfNames = metaFields*.name
					if (metaFields.size() != mfNames.size()) {
						throw new SessionInternalError("MetaField", ["PaymentMethodTypeDTO,metafield,metaField.name.exists"] as String[])
					}
					
					if(!paymentMethod.id || paymentMethod.id == 0) {
						paymentMethod.id = webServicesSession.createPaymentMethodType(paymentMethod)

						session.message = 'payment.method.type.created'
						session.args = [ paymentMethod.id ]
					} else {
						webServicesSession.updatePaymentMethodType(paymentMethod)

						session.message = 'payment.method.type.updated'
						session.args = [ paymentMethod.id ]
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
			redirect controller: 'paymentMethodType', action: 'list', id : conversation.paymentMethod?.id
		}
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
	
	private def copyMetaField(metaField) {
		MetaFieldWS mf = new MetaFieldWS()
		
		mf.dataType = metaField.dataType
		mf.defaultValue = metaField.defaultValue
		mf.disabled = metaField.disabled
		mf.displayOrder = metaField.displayOrder
		mf.fieldUsage = metaField.fieldUsage
		mf.filename = metaField.filename
		mf.mandatory = metaField.mandatory
		mf.name = metaField.name
		mf.validationRule = metaField.validationRule
		mf.primary = metaField.primary
		
		// set rule id to 0 so a new rule will be created
		mf.validationRule?.id = 0
		
		return mf
	}
}
