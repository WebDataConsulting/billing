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

import com.sapienter.jbilling.client.ViewUtils
import com.sapienter.jbilling.client.discount.DiscountHelper
import com.sapienter.jbilling.client.util.Constants
import com.sapienter.jbilling.client.util.DownloadHelper
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.discount.DiscountWS
import com.sapienter.jbilling.server.discount.db.DiscountDTO
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.PreferenceBL
import com.sapienter.jbilling.server.util.Util;
import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.server.util.csv.CsvExporter
import com.sapienter.jbilling.server.util.csv.Exporter
import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang.WordUtils
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import com.sapienter.jbilling.server.user.db.CompanyDTO
import org.hibernate.criterion.MatchMode
import org.hibernate.criterion.Restrictions

@Secured(["isAuthenticated()"])
class DiscountController {

    static pagination = [ max: 10, offset: 0, sort: 'id', order: 'desc' ]

    // Matches the columns in the JQView grid with the corresponding field
    static final viewColumnsToFields = ['code': 'code',
                                        'description': 'description',
                                        'type': 'type']

	static scope = "prototype"

    IWebServicesSessionBean webServicesSession
    ViewUtils viewUtils

    def breadcrumbService
    def recentItemService

    def index () {
        list()
    }

    def getList(def params) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order

        return DiscountDTO.createCriteria().list(
                max:    params.max,
                offset: params.offset
        ) {
			and{
				eq('entity', retrieveCompany())
                if (params.code){
                    addToCriteria(Restrictions.ilike('code', params.code, MatchMode.ANYWHERE))
                }
            }
        	SortableCriteria.sort(params, delegate)
        }
    }

    def list () {
        
        
        def selected = params.id ? DiscountDTO.get(params.int("id")) : null
        def crumbDescription = selected ? selected.code : null
        breadcrumbService.addBreadcrumb(controllerName, 'list', null, selected?.id, crumbDescription)
        
        def formattedRate = ""
        if (selected?.rate) {
			if (selected?.type?.name() == 'ONE_TIME_AMOUNT') {
				formattedRate = selected.rate
			} else if (selected?.type?.name() == 'ONE_TIME_PERCENTAGE') {
				formattedRate = Util.formatPercentageByEntity(selected.rate, session['company_id'])
			} else if (selected?.type?.name() == 'RECURRING_PERIODBASED') {
				if (Boolean.TRUE.equals(selected?.isPercentageRate())) {
					formattedRate = Util.formatPercentageByEntity(selected.rate, session['company_id'])
				} else {
					formattedRate = selected.rate
				}
			}
		}
        
        def selectedOrderPeriodDescription = ""
        def isPercentageValue = ""
        
		selected?.attributes?.each {
			if (it.key.equals("periodUnit")) {
				OrderPeriodDTO orderPeriod = OrderPeriodDTO.findById(it.value)
				selectedOrderPeriodDescription = orderPeriod.description
			} else if (it.key.equals("isPercentage")) {
				if (it.value && (it.value.equals("on") || it.value.equals("1"))) {
					isPercentageValue = "Yes"
				}
				else {
					isPercentageValue = "No"
				}
			}
		}

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], Constants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            if (params.applyFilter || params.partial) {
                render template: 'discountsTemplate', model: [selected: selected, selectedDiscountRate: formattedRate ]
            }else {
                render view: 'list', model: [selected: selected, selectedDiscountRate: formattedRate,selectedOrderPeriodDescription: selectedOrderPeriodDescription,isPercentageValue: isPercentageValue ]
            }
            return
        }

        def discounts = getList(params)

        params.totalCount = discounts.totalCount

        if (params.applyFilter || params.partial) {
            render template: 'discountsTemplate', model: [ discounts: discounts, selected: selected, selectedDiscountRate: formattedRate ]
        } else {
            render view: 'list', model: [ discounts: discounts, selected: selected,
            	selectedDiscountRate: formattedRate,
            	selectedOrderPeriodDescription: selectedOrderPeriodDescription,
            	isPercentageValue: isPercentageValue ]
        }
    }

    def findDiscounts () {
        params.sort = viewColumnsToFields[params.sidx]
        params.order  = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page')-1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def discounts = getList(params)

        try {
            render getDiscountsJsonData(discounts, params) as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }

    }

    /**
     * Converts Discounts to JSon
     */
    private def Object getDiscountsJsonData(discounts, GrailsParameterMap params) {
        def jsonCells = discounts
        def currentPage = params.page ? Integer.valueOf(params.page) : 1
        def rowsNumber = params.rows ? Integer.valueOf(params.rows): 1
        def numberOfPages = Math.ceil(jsonCells.totalCount / rowsNumber)

        def jsonData = [rows: jsonCells, page: currentPage, records: jsonCells.totalCount, total: numberOfPages]

        jsonData
    }

    def show () {
        def discount = DiscountDTO.get(params.int('id'))
		
		//prevent to display discount created for other company
		if( discount && discount.getEntity()?.id != session['company_id'] ) {
			redirect(action:'list')
			return
		}

        breadcrumbService.addBreadcrumb(controllerName, 'list', null, discount.id, discount.code)
		
		def formattedRate = ""
		if (discount?.rate) {
			formattedRate = discount.rate
		}
		
		def selectedOrderPeriodDescription = ""
		def isPercentageValue = ""
		
		discount.attributes?.each {
			
			log.debug "Discount Key/Values: ${it.key} ${it.value}"
			
			if (it.key.equals("periodUnit")) {
				OrderPeriodDTO orderPeriod = OrderPeriodDTO.findById(it.value)
				selectedOrderPeriodDescription = orderPeriod?.description
			} else if (it.key.equals("isPercentage")) {
				if (it.value && (it.value.equals("on") || it.value.equals("1"))) {
					isPercentageValue = "Yes"
				}
				else {
					isPercentageValue = "No"
				}
			}
		}
		
        render template: 'show', 
        		model: [ selected: discount, 
        				selectedDiscountRate: formattedRate, 
        				selectedOrderPeriodDescription: selectedOrderPeriodDescription,
        				isPercentageValue: isPercentageValue ]
    }
    
    def edit () {
        def discount

        try {
            discount = params.id ? webServicesSession.getDiscountWS(params.int('id')) : new DiscountWS() 
        } catch (SessionInternalError e) {
            log.error("Could not fetch WS object", e)

            flash.error = 'discount.not.found'
            flash.args = [ params.id as String ]

            redirect controller: 'discount', action: 'list'
            return
        }

        breadcrumbService.addBreadcrumb(controllerName, actionName, params.id ? 'update' : 'create', params.int('id'), discount?.code)

        [ discount: discount ]
    }
    
    def deleteDiscount () {
        int discountId = params.int('id')

        if (discountId) {
            try {
                webServicesSession.deleteDiscount(discountId)
                flash.message = 'discount.delete.success'
                flash.args = [discountId]

            } catch (SessionInternalError e) {
                log.debug(e.message)
                flash.error = e.cause.message
                redirect action: 'list', params: [id: discountId]
                return
            } catch (Exception e) {
                log.error("Exception deleting discount.", e)
                flash.error = 'error.discount.delete'
                flash.args = [params.id]
                redirect action: 'list', params: [id: discountId]
                return
            }
        }

        redirect action: 'list', params: [id: params.id]
    }
    
    def saveDiscount () {

        def discount = new DiscountWS()

        try {
        	discount = bindDiscount(discount, params)
			
			if (StringUtils.isEmpty(discount.code?.trim())) {
				//if blank code
				discount.code = ''
				String[] errmsgs= new String[1];
				errmsgs[0]= "DiscountWS,code,discount.error.code.blank"
				throw new SessionInternalError("There is an error in discount data.", errmsgs );
			}
			
			if (!discount?.descriptions?.isEmpty()) {
				def hasDescription = false
				discount.descriptions.each {
					if (!it.content?.trim().isEmpty()) {
						hasDescription = true
					}
				}
				if (!hasDescription) {
					String[] errmsgs= new String[1];
					errmsgs[0]= "DiscountWS,descriptions,discount.error.descriptions.blank"
					throw new SessionInternalError("There is an error in discount data.", errmsgs );
				}
			}
						
			discount.attributes?.each {
				if (it.key.equals("isPercentage")) {
					if (it.value==null) {
						it.value = ""
					} else if (it.value.equalsIgnoreCase("on")) {
						it.value = "1"
					}
				}
			}

            // save or update
            if (!discount.id || discount.id == 0) {
                log.debug("creating discount ${discount}")
				discount.id = webServicesSession.createOrUpdateDiscount(discount)
                flash.message = 'discount.created'
                flash.args = [discount.id]
                
            } else {
                log.debug("saving changes to discount ${discount.id}")
                discount.id = webServicesSession.createOrUpdateDiscount(discount)
                flash.message = 'discount.updated'
                flash.args = [discount.id]
            }

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
            render view: 'edit', model: [ discount: discount ]
            return
        }

        chain action: 'list', params: [id: discount.id]
    }
    
    def addAttribute () {
        def discount = params."discount.id" ? webServicesSession.getDiscountWS(params.int('discount.id')) : new DiscountWS()
        discount = bindDiscount(discount, params)

        int newIndex = params.int('attributeIndex')
        def attribute = message(code: 'plan.new.attribute.key', args: [ newIndex ])
        while (discount.attributes.containsKey(attribute)) {
            attribute = message(code: 'plan.new.attribute.key', args: [ newIndex ])
        }                
        discount.attributes.put(attribute, '')

        def templateName = WordUtils.uncapitalize(WordUtils.capitalizeFully(discount.type, ['_'] as char[]).replaceAll('_',''))
        render template: '/discount/strategy/' + templateName, model: [ discount: discount ]
    }

    def removeAttribute () {
        def discount = params."discount.id" ? webServicesSession.getDiscountWS(params.int('discount.id')) : new DiscountWS()
        discount = bindDiscount(discount, params)
        
        def attributeIndex = params.int('attributeIndex')

        def name = params["discount.attribute.${attributeIndex}.name"]
        discount.attributes.remove(name)

		def templateName = WordUtils.uncapitalize(WordUtils.capitalizeFully(discount.type, ['_'] as char[]).replaceAll('_',''))
        render template: '/discount/strategy/' + templateName, model: [ discount: discount ]
    }
    
    def updateStrategy () {
        def discount = params."discount.id" && params.int('discount.id') > 0 ? webServicesSession.getDiscountWS(params.int('discount.id')) : new DiscountWS()
        discount = bindDiscount(discount, params)
        discount?.attributes = null;
        def templateName = WordUtils.uncapitalize(WordUtils.capitalizeFully(discount.type, ['_'] as char[]).replaceAll('_',''))
        render template: '/discount/strategy/' + templateName, model: [ discount: discount ]
    }

	private def bindDiscount(DiscountWS discount, GrailsParameterMap params) {
        bindData(discount, params, 'discount')
        return DiscountHelper.bindDiscount(discount, params)
    }	
	
	private def retrieveCompany() {
		CompanyDTO.get(session['company_id'])
	}

    def csv (){
        params.sort = viewColumnsToFields[params.sidx] != null ? viewColumnsToFields[params.sidx] : params.sort
        params.order = params.sord
        params.max = CsvExporter.MAX_RESULTS

        def discounts = getList(params)

        if (discounts.totalCount > CsvExporter.MAX_RESULTS) {
            flash.error = message(code: 'error.export.exceeds.maximum')
            flash.args = [CsvExporter.MAX_RESULTS]
            redirect action: 'list', id: params.id

        } else {
            DownloadHelper.setResponseHeader(response, "discounts.csv")
            Exporter<DiscountDTO> exporter = CsvExporter.createExporter(DiscountDTO.class);
            render text: exporter.export(discounts), contentType: "text/csv"
        }
    }

}
