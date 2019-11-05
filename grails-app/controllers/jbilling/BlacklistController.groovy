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

import java.beans.PropertyChangeEvent;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.value.StringMetaFieldValue;
import com.sapienter.jbilling.server.payment.blacklist.db.BlacklistDAS
import com.sapienter.jbilling.server.payment.blacklist.db.BlacklistDTO
import com.sapienter.jbilling.server.payment.IPaymentSessionBean
import com.sapienter.jbilling.server.util.Context
import com.sapienter.jbilling.server.user.UserBL

import grails.plugin.springsecurity.annotation.Secured

import com.sapienter.jbilling.server.payment.blacklist.CsvProcessor


import org.hibernate.Hibernate;
import org.hibernate.criterion.*;

@Secured(["isAuthenticated()"])
class BlacklistController {
	
	static scope = "singleton"
	
    def index () {
        redirect action: 'list', params: params
    }

    def getFilteredList(params) {
		
		def company_id = session['company_id']
        def blacklist= BlacklistDTO.createCriteria().listDistinct() {
	        createAlias("company", "company", CriteriaSpecification.INNER_JOIN)
			createAlias("user", "user", CriteriaSpecification.LEFT_JOIN)
			
			if (params.filterBy && params.filterBy != message(code: 'blacklist.filter.by.default')) {
				createAlias("creditCard.metaFields", "mf", CriteriaSpecification.LEFT_JOIN)
				createAlias("mf.field","fieldName", CriteriaSpecification.LEFT_JOIN)
				 
                 or {
                     eq('user.id', params.int('filterBy'))
					 ilike('user.userName', "${params.filterBy}%")
					 def subCriteria = DetachedCriteria.forClass(StringMetaFieldValue.class, "stringValue")
					 .setProjection(Projections.property('id'))
					 .add(Restrictions.like('stringValue.value', "%${params.filterBy}%").ignoreCase())
					and{
						 addToCriteria(Property.forName("mf.id").in(subCriteria))
						 eq('fieldName.name',CommonConstants.METAFIELD_NAME_CC_NUMBER)
					 }
                 }
             }
            eq('company.id', company_id)
            or {
                isNull('user')
                eq('user.deleted', 0)
            }
            order('id', 'asc')
        }
    }

    def list () {
        def blacklist = getFilteredList(params)
        def selected = params.id ? BlacklistDTO.get(params.int('id')) : null
        render view: 'list', model: [ blacklist: blacklist, selected: selected ]
    }

	def filter () {
		 def blacklist = getFilteredList(params)
		 render template: 'entryList', model: [blacklist: blacklist]
	}

    def show () {
        def entry = BlacklistDTO.get(params.int('id'))

        render template: 'show', model: [selected: entry]
    }

    def save () {
        def replace = params.csvUpload == 'modify'
        def file = request.getFile('csv');
        if (!params.csv.getContentType().toString().contains('text/csv')) {
            flash.error = "csv.error.found"
            redirect action: 'list'
            return
        } else if (!file.empty) {
            def csvFile = File.createTempFile("blacklist", ".csv")
            file.transferTo(csvFile)

            IPaymentSessionBean paymentSession = Context.getBean(Context.Name.PAYMENT_SESSION)
            def added
            try {
                added = paymentSession.processCsvBlacklist(csvFile.getAbsolutePath(), replace, (Integer) session['company_id'])
                flash.message = replace ? 'blacklist.updated' : 'blacklist.added'
                flash.args = [added]
                redirect view: 'list'
            } catch (CsvProcessor.ParseException e) {
                log.debug "Invalid format for the Blacklsit CSV file"
                flash.error = "Invalid format for the Blacklist CSV file"
                redirect action: 'list'
            }
        }

        redirect action: 'list'
    }

    def user () {
        if (params.id) {
            def bl = new UserBL(params.int('id'))
            bl.setUserBlacklisted((Integer) session['user_id'], true)

            flash.message = 'user.blacklisted'
            flash.args = [params.id as String]
        }

        redirect controller: 'customerInspector', action: 'inspect', id: params.id
    }

}
