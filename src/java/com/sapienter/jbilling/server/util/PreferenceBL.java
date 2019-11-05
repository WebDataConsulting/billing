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

package com.sapienter.jbilling.server.util;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.validation.ValidationReport;
import com.sapienter.jbilling.server.util.db.*;
import org.apache.commons.lang.StringUtils;
import org.hibernate.ObjectNotFoundException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springmodules.cache.CachingModel;
import org.springmodules.cache.FlushingModel;
import org.springmodules.cache.provider.CacheProviderFacade;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sapienter.jbilling.server.util.ServerConstants.LANGUAGE_ENGLISH_ID;

public class PreferenceBL {
    private static FormatLogger LOG = new FormatLogger(PreferenceBL.class);

    private PreferenceDAS preferenceDas = null;
    private PreferenceTypeDAS typeDas = null;
    private PreferenceDTO preference = null;
    private PreferenceTypeDTO type = null;
    private JbillingTableDAS jbDAS = null;

    // cache management
    private static CacheProviderFacade cache;
    private static CachingModel cacheModelPreference;
    private static FlushingModel flushModelPreference;
    
    static {
    	cache = (CacheProviderFacade) Context.getBean(Context.Name.CACHE);
        cacheModelPreference = (CachingModel) Context.getBean(Context.Name.PREFERENCE_CACHE_MODEL);
        flushModelPreference = (FlushingModel) Context.getBean(Context.Name.PREFERENCE_FLUSH_MODEL);
    }
    
    public PreferenceBL() {
        init();
    }

    public PreferenceBL(Integer entityId, Integer preferenceTypeId) {
        init();
        set(entityId, preferenceTypeId);
    }
    
    public static final PreferenceWS getWS(PreferenceTypeDTO preferenceType) {
    	
    	PreferenceWS ws = new PreferenceWS();
        ws.setPreferenceType(PreferenceBL.getPreferenceTypeWS(preferenceType));
        return ws;
    }
    
    public static final PreferenceWS getWS(PreferenceDTO dto) {
    	
		PreferenceWS ws = new PreferenceWS();
		ws.setId(dto.getId());
		ws.setPreferenceType(dto.getPreferenceType() != null ? PreferenceBL
				.getPreferenceTypeWS(dto.getPreferenceType()) : null);
		ws.setTableId(dto.getJbillingTable() != null ? dto.getJbillingTable()
				.getId() : null);
		ws.setForeignId(dto.getForeignId());
		ws.setValue(dto.getValue());
		return ws;
    }

    
    public static final PreferenceTypeWS getPreferenceTypeWS(PreferenceTypeDTO dto) {
    	
		PreferenceTypeWS ws = new PreferenceTypeWS();
		ws.setId(dto.getId());
		ws.setDescription(dto.getDescription());
		ws.setDefaultValue(dto.getDefaultValue());
		ws.setValidationRule(null != dto.getValidationRule() ? MetaFieldBL
				.getValidationRuleWS(dto.getValidationRule()) : null);
		return ws;
    }

    
    private void init() {
        preferenceDas = new PreferenceDAS();
        typeDas = new PreferenceTypeDAS();
        jbDAS = (JbillingTableDAS) Context.getBean(Context.Name.JBILLING_TABLE_DAS);
    }

    /**
     * This function returns a preference by matching entity and preference type id.
     * Use this function in a static context to fetch the required preference value from cache.
     * No need to use new PreferenceBL().set(...) method as this is to be used only if you need PreferenceDTO.
     * As such set(..) method is used only from WS API.
     * @param entityId
     * @param preferenceTypeId
     * @return preference value, or default value from preference type if preference value is null/blank
     */
    public static synchronized String getPreferenceValue(Integer entityId, Integer preferenceTypeId) throws EmptyResultDataAccessException {
        
        LOG.debug("Looking for preference %d, for entity %d and table %s", preferenceTypeId, entityId, ServerConstants.TABLE_ENTITY);

        String preferenceValue = getPreferences(entityId).get(preferenceTypeId);
        
        if (preferenceValue == null) {
        	LOG.debug("throwing EmptyResultDataAccessException preferenceValue : |%s|", preferenceValue);
        	throw new EmptyResultDataAccessException("Could not find preference " + preferenceTypeId, 1);
        }
        
        LOG.debug("preferenceValue : |%s|", preferenceValue);
        return preferenceValue;
    }
    
    /**
     * This function is to be used when preference value is Integer and needs to be fetched as an Integer.
     * If the preference value or default value are both not set, this method returns null.
     * Instead of prefBL.set and prefBL.getInt, simply call PreferenceBL.getPreferenceValueAsInteger in a static context.
     * @param entityId
     * @param preferenceTypeId
     * @return Integer or null
     */
    public static synchronized Integer getPreferenceValueAsInteger(Integer entityId, Integer preferenceTypeId) {
        String value = getPreferenceValue(entityId, preferenceTypeId);
        return !StringUtils.isEmpty(value) ? Integer.valueOf(value) : null;
    }
    
    /**
     * This method is same as getPreferenceValueAsInteger, except that it returns zero in case
     * the preference value or default value, both are not set. This is to be used when value
     * is not to be checked against null, but needs to be compared against zero or any other number.
     * Instead of prefBL.set and prefBL.getInt, simply call PreferenceBL.getPreferenceValueAsIntegerOrZero in a static context.
     * @param entityId
     * @param preferenceTypeId
     * @return Integer
     */
    public static synchronized Integer getPreferenceValueAsIntegerOrZero(Integer entityId, Integer preferenceTypeId) {
        Integer value = getPreferenceValueAsInteger(entityId, preferenceTypeId);
        return value != null ? value : new Integer(0);
    }

    /**
     * Use this method if the preference value is a floating point value and needs to be returned as one.
     * Instead of prefBL.set and prefBL.getFloat, simply call PreferenceBL.getPreferenceValueAsFloat in a static context.
     * This method returns null if the value and default value both are not set.
     * @param entityId
     * @param preferenceTypeId
     * @return Float or null
     */
    public static synchronized Float getPreferenceValueAsFloat(Integer entityId, Integer preferenceTypeId) {
    	String value = getPreferenceValue(entityId, preferenceTypeId);
        return !StringUtils.isEmpty(value) ? Float.valueOf(value) : null;
    }

    /**
     * Use this method if the preference value is a BigDecimal value and needs to be returned as one.
     * Instead of prefBL.set and prefBL.getDecimal, simply call PreferenceBL.getPreferenceValueAsDecimal in a static context.
     * This method returns null if the value and default value both are not set.
     * @param entityId
     * @param preferenceTypeId
     * @return BigDecimal or null
     */
    public static synchronized BigDecimal getPreferenceValueAsDecimal(Integer entityId, Integer preferenceTypeId) {
    	String value = getPreferenceValue(entityId, preferenceTypeId);
        return !StringUtils.isEmpty(value) ? new BigDecimal(value) : null;
    }
    
    /**
     * This method is similar to getPreferenceValueAsDecimal, except that this one returns zero if the value,
     * and default value, both are not set. This is to be used when checking against null is to be avoided,
     * and functionality requires to compare the value against zero or any other number.
     * @param entityId
     * @param preferenceTypeId
     * @return BigDecimal
     */
    public static synchronized BigDecimal getPreferenceValueAsDecimalOrZero(Integer entityId, Integer preferenceTypeId) {
    	BigDecimal value = getPreferenceValueAsDecimal(entityId, preferenceTypeId);
        return value != null ? value : BigDecimal.ZERO;
    }

    /**
     * This method uses the getPreferenceValueAsIntegerOrZero and returns
     * Boolean.TRUE when the preference is 1
     * Boolean.FALSE otherwise
     * @param entityId
     * @param preferenceTypeId
     * @return Boolean
     */
    public static synchronized Boolean getPreferenceValueAsBoolean(Integer entityId, Integer preferenceTypeId) {
        Integer value = getPreferenceValueAsIntegerOrZero(entityId, preferenceTypeId);
        return value == 1 ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * This function is only used from WS API and as such should not be used from the application,
     * as it hits the db for fetching preference value or its default value.
     * The getPreferenceValue function should be used in static context to fetch preference value from cache.
     * @param entityId
     * @param preferenceTypeId
     * @throws EmptyResultDataAccessException
     */
    public void set(Integer entityId, Integer preferenceTypeId) throws EmptyResultDataAccessException {
        
        LOG.debug("Looking for preference %d, for entity %d and table %s", preferenceTypeId, entityId, ServerConstants.TABLE_ENTITY);
        
        try {
	        preference = preferenceDas.findByType_Row( preferenceTypeId, entityId, ServerConstants.TABLE_ENTITY);
	        type = typeDas.findNow(preferenceTypeId);
	    	
	        // throw exception if there is no preference, or if the type does not have a
	        // default value that can be returned.
	        if (preference == null) {
	        	if (type == null || type.getDefaultValue() == null) {
	        		throw new EmptyResultDataAccessException("Could not find preference " + preferenceTypeId, 1);
	        	}
	        }
        } catch (ObjectNotFoundException e) {
        	throw new EmptyResultDataAccessException("Could not find preference " + preferenceTypeId, 1);
        }
    }

    public void createUpdateForEntity(Integer entityId, Integer preferenceTypeId, Integer value) {
        createUpdateForEntity(entityId, preferenceTypeId, (value != null ? value.toString() : ""));
    }

    public void createUpdateForEntity(Integer entityId, Integer preferenceTypeId, BigDecimal value) {
        createUpdateForEntity(entityId, preferenceTypeId, (value != null ? value.toString() : ""));
    }

    public void createUpdateForEntity(Integer entityId, Integer preferenceTypeId, String value) {

    	preference = preferenceDas.findByType_Row(preferenceTypeId, entityId, ServerConstants.TABLE_ENTITY);
    	type = typeDas.find(preferenceTypeId);

        if(null!=type.getValidationRule()) {
            validate(value);
        }
    	
        if (preference != null) {
            // update preference
            preference.setValue(value);
            preferenceDas.save(preference);
        } else {
            // create a new preference
            preference = new PreferenceDTO();
            preference.setValue(value);
            preference.setForeignId(entityId);
            preference.setJbillingTable(jbDAS.findByName(ServerConstants.TABLE_ENTITY));
            preference.setPreferenceType(type);
            preference = preferenceDas.save(preference);
        }
        
        invalidatePreferencesCache();
    }

    /**
     * This function returns preferences from cache if available in the cache.
     * If not available in the cache, then selects from db and puts in the cache as a map.
     * The map key is a combination of preference type id, entity id and entity table name.
     * @param entityId
     * @return
     */
    private static synchronized Map<Integer, String> getPreferences(Integer entityId)
    {
    	String cacheKey = getPreferenceCacheKey(entityId);
    	Map<Integer, String> cachedPreferences = (Map<Integer, String>)cache.getFromCache(cacheKey, cacheModelPreference);
    	
    	if (cachedPreferences != null && !cachedPreferences.isEmpty()) {
            LOG.debug("Preferences Cache hit for %s", cacheKey);
            return cachedPreferences;
        }
    	
    	// not found in cache, fetch from db
    	List<Object[]> preferences = new PreferenceDAS().getPreferencesByEntity(entityId);
    	
    	cachedPreferences = new HashMap<Integer, String>();
    	
    	for (Object[] preferenceTypeAndValue : preferences) {
    		if (preferenceTypeAndValue != null && preferenceTypeAndValue[0] != null) {
	    		Integer preferenceTypeId = new Integer(preferenceTypeAndValue[0].toString());
	    		String preferenceValue = "";
	    		if (preferenceTypeAndValue[1] != null && !preferenceTypeAndValue[1].toString().isEmpty()) { 
	    			preferenceValue = preferenceTypeAndValue[1].toString(); 
	    		}
	    		else if (preferenceTypeAndValue[2] != null) { 
	    			preferenceValue = preferenceTypeAndValue[2].toString();
	    		}
	    		// populate the map
	    		cachedPreferences.put(preferenceTypeId, preferenceValue);
    		}
    	}
    	
    	// put the populated map in cache
    	cache.putInCache(cacheKey, cacheModelPreference, cachedPreferences);
        return cachedPreferences;
    }
    
    /**
     * Returns the preference value if set. If the preference is null or has no
     * set value (is blank), the preference type default value will be returned.
     *
     * @return preference value as a string
     */
    public String getString() {
    	if (preference != null && StringUtils.isNotBlank(preference.getValue())) {
            return preference.getValue();
        } else {
        	return type != null ? type.getDefaultValue() : null;
        }
    }

    public Integer getInt() {
        String value = getString();
        return value != null ? Integer.valueOf(value) : null;
    }

    public Float getFloat() {
        String value = getString();
        return value != null ? Float.valueOf(value) : null;
    }

    public BigDecimal getDecimal() {
        String value = getString();
        return value != null ? new BigDecimal(value) : null;
    }

    /**
     * Returns the preference value as a string.
     *
     * @see #getString()
     * @return string value of preference
     */
    public String getValueAsString() {
        return getString();
    }

    /**
     * Returns the default value for the given preference type.
     *
     * @param preferenceTypeId preference type id
     * @return default preference value
     */
    public String getDefaultValue(Integer preferenceTypeId) {
    	type = typeDas.find(preferenceTypeId);
        return type != null ? type.getDefaultValue() : null;
    }

    /**
     * Returns true if the preference value is null, false if value is set.
     *
     * This method ignores the preference type default value, unlike {@link #getString()}
     * and {@link #getValueAsString()} which will return the type default value if the
     * preference itself is unset.
     *
     * @return true if preference value is null, false if value is set.
     */
    public boolean isNull() {
        return preference == null || preference.getValue() == null;
    }
    
    public PreferenceDTO getEntity() {
        return preference;
    }
    
    private static synchronized String getPreferenceCacheKey(Integer entityId)  {
        return "preferenceCache entity:" + entityId;
    }
    
    public void invalidatePreferencesCache() {
        LOG.debug("Invalidating preferences cache");
        cache.flushCache(flushModelPreference);
    }

    private void validate(String value) {
        ValidationReport validationReport = this.type.getValidationRule().getRuleType().getValidationRuleModel().
                doValidation(null, value, type.getValidationRule(), LANGUAGE_ENGLISH_ID);
        if (validationReport != null && !validationReport.getErrors().isEmpty()) {
            throw new SessionInternalError("Field value failed validation.",
                    validationReport.getErrors().toArray(new String[validationReport.getErrors().size()]));
        }
    }

}

