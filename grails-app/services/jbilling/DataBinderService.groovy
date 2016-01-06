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

import groovy.util.slurpersupport.GPathResult
import org.apache.commons.lang.WordUtils
import org.springframework.beans.BeanWrapper
import org.springframework.beans.BeanWrapperImpl
import com.sapienter.jbilling.client.util.CustomDateBinder
/**
 * DataBinderService.
 * @author othman El Moulat
 * @since  6/10/12
 *
 */
class DataBinderService {

    boolean transactional = false
    def grailsApplication
    private BeanWrapper wrapper = new BeanWrapperImpl()

    public List bindAllXmlData (Class targetClass, GPathResult source, List properties) {
        if (targetClass == null || source == null || properties == null) return null
        def resultList = []
        def  className = WordUtils.uncapitalize(targetClass.simpleName)
        source[className]?.each {
            def boundObj = bindXmlData(targetClass, it, properties)
            System.out.println(boundObj)
            resultList.add(boundObj)
        }
        return resultList
    }

    public Object bindXmlData (Class targetClass, GPathResult source, List properties) {
        if (targetClass == null || source == null || properties == null) return null
        def targetObject = grailsApplication.classLoader.loadClass(targetClass.name).newInstance()
        if (targetObject) {
            return bindXmlData(targetObject, source, properties)
        } else {
            return null
        }
    }

    public Object bindXmlData (Object target, GPathResult source, List properties) {
        if (target == null || source == null || properties == null) return null
        wrapper.registerCustomEditor (Date.class, new CustomDateBinder())
        wrapper.setWrappedInstance(target)
        properties.each {String property ->
            if (property.contains('.')) {//This indicates a domain class to bind e.g. experiment.id -> Experiment
                def propertyName = property.tokenize('.')
                def id = source[propertyName[0]]["@${propertyName[1]}"]?.toString()
                if (id != null) {
                    def subdomainInstance = null
                    try {subdomainInstance = grailsApplication.classLoader.loadClass("edu.kit.iism.experimentcenter.${WordUtils.capitalize(propertyName[0])}").get(id)} catch (Exception ex) {}
                    if (subdomainInstance != null) wrapper.setPropertyValue(propertyName[0], subdomainInstance)
                }
            } else if (property.equals('id')) { //The id property is set as an attribute rather than text
                def id = source['@id']?.toString()
                if (id != null) wrapper.setPropertyValue(property, id)
            } else { //regular attributes
                def prop = source[property]?.toString()
                if (prop != null) wrapper.setPropertyValue(property, prop)
            }
        }
        return target
    }
}
