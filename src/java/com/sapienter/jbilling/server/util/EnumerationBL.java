/**
 * 
 */
package com.sapienter.jbilling.server.util;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.db.EnumerationDAS;
import com.sapienter.jbilling.server.util.db.EnumerationDTO;
import com.sapienter.jbilling.server.util.db.EnumerationValueDAS;
import com.sapienter.jbilling.server.util.db.EnumerationValueDTO;

import java.util.*;


/**
 * @author Vikas Bodani
 * @since 10-Aug-2011
 *
 */
public class EnumerationBL {


    private static final FormatLogger LOG = new FormatLogger(EnumerationBL.class);

    private EnumerationDAS enumerationDas;
    private EnumerationDTO enumeration;
    private EnumerationValueDAS enumerationValueDas;
    
    public EnumerationBL() {
        _init();
    }

    public EnumerationBL(EnumerationDTO enumeration) {
        _init();
        this.enumeration = enumeration;
    }

    public EnumerationBL(Integer enumerationId) {
        _init();
        set(enumerationId);
    }

    private void _init() {
        this.enumerationDas = new EnumerationDAS();
        this.enumerationValueDas = new EnumerationValueDAS();
    }

    public void set(Integer enumerationId) {
        this.enumeration = enumerationDas.find(enumerationId);
    }

    public EnumerationDTO getEntity() {
        return enumeration;
    }

    /**
     * Saves a new Enumeration to and sets the BL entity to the newly created Enumeration. 
     *
     * @param enumeration Enumeration to save
     * @return id of the new Enumeration
     */
    public Integer create(EnumerationDTO enumeration) {
        if (enumeration != null) {
        	// Remove leading and trailing spaces from enumeration values.
        	for (EnumerationValueDTO value : enumeration.getValues()) {
                value.setValue(null != value.getValue() ? value.getValue().trim() : "");
                value.setEnumeration(enumeration);
        	}
            this.enumeration = enumerationDas.save(enumeration);
            enumerationDas.detach(this.enumeration);
            enumerationDas.clear();
            return this.enumeration.getId();
        }

        LOG.error("Cannot save a null EnumerationDTO!");
        return null;
    }

    /**
     * Updates this Enumeration's values with those of the given Enumeration. 
     * @param dto EnumerationDTO 
     */
    public Integer update(EnumerationDTO dto) {
        enumeration.setName(dto.getName());
        setEnumerationValues(dto.getValues());
	    return enumeration.getId();
    }

    /**
     * Updates the meta_field_name table
     * @param oldEnumName
     * @param newEnumName
     */
    public void updateMetaFields(String oldEnumName, String newEnumName){
        List<Integer> metaFieldIdList = new MetaFieldDAS().getAllIdsByDataTypeAndName(DataType.ENUMERATION, oldEnumName);
        for(Integer metaFieldId : metaFieldIdList ) {
            MetaField metaField = new MetaFieldDAS().find(metaFieldId);
            metaField.setName(newEnumName);
            new MetaFieldDAS().save(metaField);
            LOG.debug("Metafield %s updated.", metaField.getId());
        }
    }

    /**
     * Sets the granted values of this Enumeration to the given set.
     *
     */
    public void setEnumerationValues(List<EnumerationValueDTO> values) {
        if (enumeration != null) {
        	// Remove leading and trailing spaces from enumeration values.
        	for (EnumerationValueDTO value : values) {
        		value.setValue(null != value.getValue() ? value.getValue().trim() : "");
        	}
            // Store all persisted enums in a map
            SortedMap<Integer, EnumerationValueDTO> persistedValues = new TreeMap<Integer, EnumerationValueDTO>();
            for (EnumerationValueDTO value : enumeration.getValues()){
                persistedValues.put(value.getId(), value);
            }
            enumeration.getValues().clear();
            List<EnumerationValueDTO> futurePersistedValues = new ArrayList<EnumerationValueDTO>();
            for(EnumerationValueDTO value : values){
                EnumerationValueDTO val = persistedValues.get(value.getId());
                // Check if the new value has been persisted before
                if(null != val){
                    // Just update if the value is different
                    if(!val.getValue().equals(value.getValue())){
                        val.setValue(value.getValue());
                    }
                    futurePersistedValues.add(val);
                } else {
                    // Just add new value
                    futurePersistedValues.add(value);
                }
            }
            enumeration.getValues().addAll(futurePersistedValues);
            enumeration = enumerationDas.save(enumeration);
            enumerationDas.flush();

        } else {
            LOG.error("Cannot update, EnumerationDTO not found or not set!");
        }
    }

    /**
     * Deletes this Enumeration.
     */
    public void delete() {
        if (enumeration != null) {
            enumeration.getValues().clear();
            enumerationDas.delete(enumeration);
            enumerationDas.flush();

        } else {
            LOG.error("Cannot delete, EnumerationDTO not found or not set!");
        }
    }

    /**
     * This method is added specifically for duplicate name validation during update.
     * @param id
     * @param name
     * @return true/false depending on whether new name in update exists for another enumeration or not
     */
    public boolean exists(Integer id, String name, Integer entityId) {
    	return (null != id && null != name && null != entityId) && enumerationDas.exists(id, name, entityId);
    }

    /**
     * Queries the data source for an {@link com.sapienter.jbilling.server.util.db.EnumerationDTO}
     * entity filtered by <code>id</code> and <code>entityId</code>
     *
     * @param id representing the unique Enumeration entity.
     * @param entityId representing the callers company.
     * @return {@link com.sapienter.jbilling.server.util.db.EnumerationDTO} entity, representing the result set.
     */
    public EnumerationDTO getEnumeration(Integer id, Integer entityId){
        if((null == id || Integer.valueOf(0) >= id) || (null == entityId || Integer.valueOf(0) >= entityId)){
            throw new IllegalArgumentException("The ID values are required and must be positive numbers!!");
        }
        return enumerationDas.getEnumeration(id, entityId);
    }

    /**
     * Queries the data source for an {@link com.sapienter.jbilling.server.util.db.EnumerationDTO}
     * entity filtered by <code>name</code> and <code>entityId</code>
     *
     * @param name of the Enumeration entity.
     * @param entityId representing the callers company.
     * @return {@link com.sapienter.jbilling.server.util.db.EnumerationDTO} entity, representing the result set.
     */
    public EnumerationDTO getEnumerationByName(String name, Integer entityId){
        if(null == name || Integer.valueOf(0) >= name.length()){
            throw new IllegalArgumentException("The name value can not be empty or null!");
        }

        if(null == entityId || Integer.valueOf(0) >= entityId){
            throw new IllegalArgumentException("The Entity ID value is required and must be positive number!!");
        }

        return enumerationDas.getEnumerationByName(name, entityId);
    }

    /**
     *
     * Queries the data source for all {@link com.sapienter.jbilling.server.util.db.EnumerationDTO}
     * entities filtered by <code>entityId</code>, starting from <code>offset</code> row
     * and <code>max</code> number of rows.
     *
     * @param entityId representing the callers company.
     * @param max representing maximum number of rows (optional).
     * @param offset representing the offset (optional).
     *
     * @return list of {@link com.sapienter.jbilling.server.util.db.EnumerationDTO} entities, representing the result set.
     */
    public List<EnumerationDTO> getAllEnumerations(Integer entityId, Integer max, Integer offset){
        if(null == entityId || Integer.valueOf(0) >= entityId){
            throw new IllegalArgumentException("The Entity ID value is required and must be positive number!!");
        }

        return enumerationDas.getAllEnumerations(entityId, max, offset);
    }

    /**
     * Queries the data source for a number
     * representing the count of all {@link com.sapienter.jbilling.server.util.db.EnumerationDTO} persisted entities.
     *
     * @param entityId representing the callers company.
     * @return number of persisted entities.
     */
    public Long getAllEnumerationsCount(Integer entityId){
        if(null == entityId || Integer.valueOf(0) >= entityId){
            throw new IllegalArgumentException("The Entity ID value is required and must be positive number!!");
        }
        return enumerationDas.getAllEnumerationsCount(entityId);
    }

    /**
     * Converts {@link com.sapienter.jbilling.server.util.db.EnumerationDTO} object
     * to {@link com.sapienter.jbilling.server.util.EnumerationWS} object.
     *
     * @param enumeration object that is going to be converted.
     * @return {@link com.sapienter.jbilling.server.util.EnumerationWS} representation of the supplied <code>enumeration</code> object.
     */
    public static final EnumerationWS convertToWS(EnumerationDTO enumeration) {
        if(null == enumeration){
            return null;
        }
        EnumerationWS enumerationWS = new EnumerationWS();
        enumerationWS.setId(enumeration.getId());
        if(null != enumeration.getEntity()){
            enumerationWS.setEntityId(enumeration.getEntity().getId());
        }
        enumerationWS.setName(enumeration.getName());
        if(null != enumeration.getValues() && Integer.valueOf(0) < enumeration.getValues().size())
        for (EnumerationValueDTO valueDTO : enumeration.getValues()){
            if (null != valueDTO){
                EnumerationValueWS valueWS = new EnumerationValueWS();
                valueWS.setId(valueDTO.getId());
                valueWS.setValue(valueDTO.getValue());
                enumerationWS.addValue(valueWS);
            }
        }
        return enumerationWS;
    }

    /**
     * Converts {@link com.sapienter.jbilling.server.util.EnumerationWS} object
     * to {@link com.sapienter.jbilling.server.util.db.EnumerationDTO} object.
     *
     * @param enumeration object that is going to be converted.
     * @return {@link com.sapienter.jbilling.server.util.db.EnumerationDTO} representation of the supplied <code>enumeration</code> object.
     */
    public static final EnumerationDTO convertToDTO(EnumerationWS enumeration){
        if(null == enumeration){
            return null;
        }
        EnumerationDTO enumerationDTO = new EnumerationDTO();
        if(null != enumeration.getId() && Integer.valueOf(0) < enumeration.getId()){
            enumerationDTO.setId(enumeration.getId());
        }
        enumerationDTO.setName(enumeration.getName());
        enumerationDTO.setEntity(new CompanyDTO(enumeration.getEntityId()));
        List<EnumerationValueWS> wsValues = enumeration.getValues();
        if(null != wsValues && Integer.valueOf(0) < wsValues.size()){
            List<EnumerationValueDTO> values = new ArrayList<EnumerationValueDTO>();
            for (EnumerationValueWS valueWS : wsValues){
                if(null != valueWS){
                    EnumerationValueDTO valueDTO = new EnumerationValueDTO();
                    if(null != valueWS.getId()){
                        valueDTO.setId(valueWS.getId());
                    }
                    valueDTO.setValue(valueWS.getValue());
                    valueDTO.setEnumeration(enumerationDTO);
                    values.add(valueDTO);
                }
            }
            enumerationDTO.setValues(values);
        }
        return enumerationDTO;
    }
}
