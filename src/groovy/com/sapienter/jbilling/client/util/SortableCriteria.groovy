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

package com.sapienter.jbilling.client.util

import grails.orm.HibernateCriteriaBuilder
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.hibernate.criterion.Order

import java.util.regex.Pattern

/**
 * Sortable 
 *
 * @author Brian Cowdery
 * @since 08/06/11
 */
class SortableCriteria {

    static String NO_ALIAS = "NONE";

    static def sort(GrailsParameterMap params, builder) {
        def sort = params.sort?.tokenize(',')?.collect { it.trim() }

        if (params.alias) {
            if (params.alias != NO_ALIAS) {
                // explicit alias definitions
                params.alias.each { alias, aliasPath ->
                    builder.createAlias(aliasPath, alias)
                }
            }
        } else {
            // try and automatically add aliases for sorted associations
            def associations = sort.findAll{ it.contains('.') }
            associations.collect{ it.substring(0, it.indexOf('.')) }.unique().each {
                builder.createAlias(it, it)
            }
        }

        // add order by clauses
        sort.each {
            builder.order(it, params.order)
        }
    }
    
    /**
    * New function for sort added for cases where sort is specified for attributes of an association
    * and alias for that association is already created in the controller list action query.
    * Using regular sort method from above was leading duplicate association path error with alias getting created twice.
    */
    static def buildSortNoAlias(GrailsParameterMap params, builder) {
        def sort = params.sort?.tokenize(',')?.collect { it.trim() }

        // add order by clauses
        sort.each {
            builder.order(params.order=='asc' ?
                Order.asc(it).ignoreCase() :
                Order.desc(it).ignoreCase())
        }
    }

    /**
     * Extract a subset of entries from the map.
     * List can either contain keys or Patterns which will matched against all keys.
     *
     * @param params
     * @param names
     * @return
     */
    static def extractParameters(Map params, List names) {
        def aliases = [:]
        if (names) {
            names.each{
                if(it instanceof Pattern) {
                    params.each { k, v ->
                        if (it.matcher(k).matches()) {
                            aliases[k] = v
                        }
                    }
                } else if (params[it]) {
                    aliases[it] = params[it]
                }
            }
        }
        return aliases
    }
}

