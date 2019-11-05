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


import com.sapienter.jbilling.client.util.ClientConstants
import com.sapienter.jbilling.common.Util
import com.sapienter.jbilling.server.util.PreferenceBL
import org.apache.commons.lang.StringUtils
import org.springframework.dao.EmptyResultDataAccessException
import org.hibernate.ObjectNotFoundException
/**
 * PreferencesTagLib 
 *
 * @author Brian Cowdery
 * @since 12/01/11
 */
class PreferencesTagLib {

    /**
     * Prints the preference value
     *
     * @param preferenceId ID of the preference to check
     */
    def preference = { attrs, body ->

        def preferenceId = assertAttribute('preferenceId', attrs, 'preference') as Integer

        try {
            def preferenceValue = PreferenceBL.getPreferenceValue(session['company_id'], preferenceId)
            if (!StringUtils.isEmpty(preferenceValue))
                out << preferenceValue

        } catch (EmptyResultDataAccessException e) {
            /* ignore */
        } catch (ObjectNotFoundException e) {
            /* ignore */
        }
    }

    /**
     * Prints the tag body if the preference exists and is not null.
     *
     * @param preferenceId ID of the preference to check
     */
    def hasPreference = { attrs, body ->

        def preferenceId = assertAttribute('preferenceId', attrs, 'hasPreference') as Integer

        try {
            def preferenceValue = PreferenceBL.getPreferenceValue(session['company_id'], preferenceId)
            if (!StringUtils.isEmpty(preferenceValue))
                out << body()

        } catch (EmptyResultDataAccessException e) {
            /* ignore */
        } catch (ObjectNotFoundException e) {
            /* ignore */
        }
    }

    /**
     * Prints the tag body if the preference value or preference type default equals the given value.
     *
     * @param preferenceId ID of the preference to check
     * @param value to compare
     */
    def preferenceEquals = { attrs, body ->

        def preferenceId = assertAttribute('preferenceId', attrs, 'preferenceEquals') as Integer
        def value = assertAttribute('value', attrs, 'preferenceEquals') as String

        try {
			def preferenceValue = PreferenceBL.getPreferenceValue(session['company_id'], preferenceId)
			if (StringUtils.isEmpty(preferenceValue)) {
				return;
			}
			
            if (value.equals(preferenceValue))
                out << body()

            // Expected value and saved value both are greater then 0 then its assumed to be jqgrid
            if (preferenceId.equals(ClientConstants.PREFERENCE_USE_JQGRID) && preferenceValue.isInteger() && value.isInteger() && preferenceValue.toInteger()>0 && value.toInteger()>0)
                out << body()

        } catch (EmptyResultDataAccessException e) {
            /* ignore */
            log.debug("empty result data access exception")

        } catch (ObjectNotFoundException e) {
            /* ignore */
            log.debug("object not found exception")
        }
    }

    /**
     * Prints the tag body if the preference value is equal, or if the preference is not set and has no
     * default value for the preference type. Useful for "default if not set" style preferences.
     *
     * @param preferenceId ID of the preference to check
     * @param value to compare
     */
    def preferenceIsNullOrEquals = { attrs, body ->

        def preferenceId = assertAttribute('preferenceId', attrs, 'preferenceIsNullOrEquals') as Integer
        def value = assertAttribute('value', attrs, 'preferenceIsNullOrEquals') as String

        try {
            def preferenceValue = PreferenceBL.getPreferenceValue(session['company_id'], preferenceId)

            if (StringUtils.isEmpty(preferenceValue) || preferenceValue.equals(value))
                out << body()

            // Expected value and saved value both are greater then 0 then its assumed to be jqgrid
            if (preferenceId.equals(ClientConstants.PREFERENCE_USE_JQGRID) && preferenceValue.isInteger() && value.isInteger() && preferenceValue.toInteger()>0 && value.toInteger()>0)
                out << body()

        } catch (EmptyResultDataAccessException e) {
            /* ignore */
        } catch (ObjectNotFoundException e) {
            /* ignore */
        }
    }

    /**
     * Prints the tag body if the jbilling.properties setting is equal to the given value.
     *
     * @param property property key of the value from jbilling.properties
     * @param value value to compare
     */
    def settingEquals = { attrs, body ->
        def propertyKey = assertAttribute('property', attrs, 'settingEquals') as String
        def value = assertAttribute('value', attrs, 'settingEquals') as String

        def prop = Util.getSysProp(propertyKey)
        if (StringUtils.isEmpty(prop)) {
            return
        }

        if (value.equals(prop))
            out << body()
    }

    /**
     * Prints the tag body if the jbilling.properties setting, when read as a boolean,
     * evaluates to true.
     *
     * @param property property key of the value from jbilling.properties
     * @param value value to compare
     */
    def settingEnabled = { attrs, body ->
        def propertyKey = assertAttribute('property', attrs, 'settingEnabled') as String

        if (Util.getSysPropBooleanTrue(propertyKey))
            out << body()
    }

    protected assertAttribute(String name, attrs, String tag) {
        if (!attrs.containsKey(name)) {
            throwTagError "Tag [$tag] is missing required attribute [$name]"
        }
        attrs.remove name
    }
}
