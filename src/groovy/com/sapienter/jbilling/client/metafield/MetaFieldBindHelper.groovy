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

package com.sapienter.jbilling.client.metafield

import com.sapienter.jbilling.common.FormatLogger
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.metafields.MetaFieldWS
import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.server.metafields.db.MetaField
import com.sapienter.jbilling.server.metafields.MetaFieldType
import com.sapienter.jbilling.server.util.InternationalDescriptionWS

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap

import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS


import org.codehaus.groovy.grails.web.metaclass.BindDynamicMethod

import com.sapienter.jbilling.server.metafields.validation.ValidationRuleType;
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleWS

import org.apache.commons.lang.StringUtils

/**
 * @author Alexander Aksenov
 * @since 12.10.11
 */
class MetaFieldBindHelper {

    private static def log = new FormatLogger(this.class)
    private static int DATE_YEAR = 9999;
    static def List<MetaFieldValueWS> bindMetaFields(Collection<MetaField> metaFields, GrailsParameterMap params) {
        List<MetaFieldValueWS> fieldsArray = new LinkedList<MetaFieldValueWS>();
        metaFields.each{
            def fieldValue = it.createValue();
            bindData(fieldValue, params, "metaField_${it.id}")
            if(!fieldValue.getValue()){
                fieldValue.setValue(null)
            }

            def metaFieldWS = MetaFieldBL.getWS (fieldValue);
            if(!fieldValue.getValue()){
                fieldValue.setValue(null)
            }

            if (metaFieldWS == null && it.isMandatory() && it.getDataType().equals(DataType.BOOLEAN)) {
                // FALSE for unselected checkbox
                fieldValue = it.createValue();
                fieldValue.setValue(false)
                metaFieldWS = MetaFieldBL.getWS (fieldValue)
            }

            if (metaFieldWS != null) {
                fieldsArray << metaFieldWS;
            }
        };
        return fieldsArray;
    }

    static def List<MetaFieldValueWS> bindMetaFields(Map<Integer, Collection<MetaField>> metaFields, GrailsParameterMap params) {
        List<MetaFieldValueWS> fieldsArray = new LinkedList<MetaFieldValueWS>()
        metaFields.each{ key, value ->
            if(value){
                value.each {
                    def fieldValue = it.createValue();
                    bindData(fieldValue, params, "metaField_${it.id}")

                    //treat empty string as null values
                    if (it.getDataType().equals(DataType.STRING) &&
                            fieldValue.getValue().toString().isEmpty()){
                        fieldValue.setValue(null)
                    }

                    def metaFieldWS = MetaFieldBL.getWS(fieldValue)
                    metaFieldWS.setGroupId(key)

                    if (metaFieldWS == null && it.isMandatory() && it.getDataType().equals(DataType.BOOLEAN)) {
                        // FALSE for unselected checkbox
                        fieldValue = it.createValue();
                        fieldValue.setValue(false)
                        metaFieldWS = MetaFieldBL.getWS(fieldValue)
                    }

                    if (metaFieldWS != null && (metaFieldWS.getValue() != null)) {
                        fieldsArray << metaFieldWS;
                    }
                }
            }
        };
        return fieldsArray;
    }

    static def MetaFieldWS bindMetaFieldName(GrailsParameterMap params, String index = '') {
        bindMetaFieldName(params, false, index)
    }

    /**
     * Convenience method to bind a MetaFieldWS object which forms part of a list.
     *
     * @param params
     * @param swallowException  - swallow parse exception when trying to parse the default value
     * @param index             - will be appended to parameters starting with metaField (e.g. metaField1.name) after
     *      the string metaField. If parameters do not start with metaField it will appended at the end (e.g. entityType1). For error
     *      messages it will be appended after the word 'errorMessages' (e.g. errorMessages1[0].content
     * @return
     */
    static def MetaFieldWS bindMetaFieldName(GrailsParameterMap params, boolean swallowException, String index = '') {
        def metaField = new MetaFieldWS()
        bindMetaFieldName(metaField, params, swallowException, index)
    }

    static def MetaFieldWS bindMetaFieldName(MetaFieldWS metaField, GrailsParameterMap params, boolean swallowException, String index=''){
        bindData(metaField, params, 'metaField'+index)
        //#7731 - field name restriction is not working
        if(metaField.name.size() > 100) {
            throw new SessionInternalError("Meta field name too long ", [
                    "MetaFieldWS,name,metafield.validation.name.too.long,"
            ] as String[])
        }
        metaField.setEntityType(EntityType.valueOf(params['entityType'+index]))

        //bind the field types
		
        if(params['fieldType'+index]){
            def types= []
            if (params['fieldType'+index] instanceof String) {
                types<<MetaFieldType.valueOf(params['fieldType'+index])
            } else {
                for (item in params['fieldType'+index]) {
                    types<<MetaFieldType.valueOf(item)
                }
            }
            metaField.setFieldUsage(MetaFieldType.valueOf(params['fieldType'+index]))

        }

        //bind the default value
        if (params['defaultValue'+index]) {
            def defaultValue = new MetaFieldBL().createValue(metaField,params['defaultValue'+index])
            if (defaultValue == null || defaultValue.getValue() == null) {
                if (!swallowException) {
                    throw new SessionInternalError("Unable to parse default value for "+metaField.name, [
                            "MetaFieldWS,defaultValue,metafield.validation.defaultvalue.parse.failure,"+metaField.name
                    ] as String[])
                }
            } else {
                metaField.setDefaultValue(defaultValue)
                defaultValue.setDataType(metaField.dataType)
                defaultValue.setFieldName(metaField.name)
            }
        }

        //bind validationRule
        params.put('metaFieldIdx', index)
        ValidationRuleWS validationRuleWS = bindValidationRule(params)
        metaField.validationRule = validationRuleWS;

        return metaField
    }

    private static def bindData(Object model, modelParams, String prefix) {
        def args = [ model, modelParams, [exclude:[], include:[]]]
        if (prefix) args << prefix

        new BindDynamicMethod().invoke(model, 'bind', (Object[]) args)
    }

    static def bindValidationRule(params){
        def metaFieldIdx = params.metaFieldIdx
        def mf = params["metaField"+metaFieldIdx]
        def vr = mf?.validationRule

        //first check if validation rule is enabled
        String enabled = vr?.enabled
        if (enabled!=null && enabled.equalsIgnoreCase("false")){
            log.debug("Validation rule is not enabled")
            return null;
        }

        ValidationRuleWS validationRule = new ValidationRuleWS();
        validationRule.setId(vr?.id?Integer.valueOf(vr?.id):0)
        validationRule.setEnabled(Boolean.valueOf(vr?.enabled))
        validationRule.setRuleType(ValidationRuleType.valueOf(vr?.ruleType).name())

        // bind rule attributes
        def attrs = vr?.ruleAttributes

        attrs.each{ j, attrParams ->
            if (attrParams instanceof Map)
                if (attrParams.name)
                    validationRule.ruleAttributes.put(attrParams.name, attrParams.value)
        }

        //bind error messages
        def langNums = params['allDescriptionLanguages'+metaFieldIdx] ? params['allDescriptionLanguages'+metaFieldIdx].split(',').size():0
        if(langNums > 0) {
            def content
            def lang
            def deleted
            def errorMsgs=[]
            for (int i=0; i<langNums; i++) {
                content = params["errorMessages"+metaFieldIdx+"[${i}].content"]
                lang = params.int("errorMessages"+metaFieldIdx+"[${i}].languageId")
                deleted = params.boolean("errorMessages"+metaFieldIdx+"[${i}].deleted")
                if(content && lang&& !deleted){
                    InternationalDescriptionWS errorMsg = new InternationalDescriptionWS(lang, content)
                    errorMsgs<<errorMsg
                }

            }
            validationRule.setErrorMessages(errorMsgs)
        }

        log.debug "Validation Rule after bind: ${validationRule}"
        return validationRule;
    }

    static def List<MetaFieldValueWS> bindMetaFields(Collection<MetaField> metaFields, Integer modelIndex, GrailsParameterMap params) {
        List<MetaFieldValueWS> fieldsArray = new LinkedList<MetaFieldValueWS>();
        metaFields.each{
            def fieldValue = it.createValue();
            bindData(fieldValue, params, "${modelIndex}_metaField_${it.id}")
            if(fieldValue != null && fieldValue.field.dataType.equals(DataType.DATE) && fieldValue.value != null) {
                Date date = (Date)fieldValue.value
                GregorianCalendar cal = new GregorianCalendar();
                cal.setTime(date);
                if(cal.get(Calendar.YEAR) > DATE_YEAR){
                    fieldValue.setValue(null)
                }
            }

            def metaFieldWS = MetaFieldBL.getWS(fieldValue);

            if (metaFieldWS == null && it.isMandatory() && it.getDataType().equals(DataType.BOOLEAN)) {
                // FALSE for unselected checkbox
                fieldValue = it.createValue();
                fieldValue.setValue(false)
                metaFieldWS =MetaFieldBL.getWS(fieldValue)
            }

            if (metaFieldWS != null) {
                fieldsArray << metaFieldWS;
            }
        };
        return fieldsArray;
    }

    static char[] BLACK_LIST_PATTERN = ['<','>']
    static boolean validateErrorMessage(List<InternationalDescriptionWS> errorMessages){
        for(InternationalDescriptionWS msg : errorMessages){
            if(StringUtils.containsAny(msg.content, BLACK_LIST_PATTERN)){
                return false;
            }
        }

        return true;
    }
}
