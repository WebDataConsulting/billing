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

package com.sapienter.jbilling.server.metafields;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.metafields.db.*;
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDTO;
import com.sapienter.jbilling.server.user.db.CustomerAccountInfoTypeMetaField;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.user.db.CustomerDTO;

import org.apache.commons.lang.ArrayUtils;

import java.util.*;

/**
 * Helper class for working with custom fields. It is needed because some classes
 * cann't extends CustomizedEntity directly. Instead they can implement MetaContent interface
 * and use this helper to do work.
 *
 * @author Alexander Aksenov
 * @since 11.10.11
 */
public class MetaFieldHelper {

    /**
     * Returns the meta field by name if it's been defined for this object.
     *
     * @param customizedEntity entity for searching fields
     * @param name             meta field name
     * @return field if found, null if not set.
     */
    public static MetaFieldValue getMetaField(MetaContent customizedEntity, String name) {
        return MetaFieldHelper.getMetaField(customizedEntity, name, null);
    }

    /**
     * Returns the meta field by name if it's been defined for this object.
     *
     * @param customizedEntity entity for searching fields
     * @param name             meta field name
     * @param groupId          group id
     * @return field if found, null if not set.
     */
    public static MetaFieldValue getMetaField(MetaContent customizedEntity, String name, Integer groupId) {
        for (MetaFieldValue value : customizedEntity.getMetaFields()) {
            if (value.getField() != null && value.getField().getName().equals(name)) {
                if (null != groupId) {
                    if(null != value.getField().getMetaFieldGroups()){
                        for (MetaFieldGroup group : value.getField().getMetaFieldGroups()) {
                            if (group.getId() == groupId) {
                                return value;
                            }
                        }
                    }
                } else {
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * Adds a meta field to this object. If there is already a field associated with
     * this object then the existing value should be updated.
     *
     * @param customizedEntity entity for searching fields
     * @param field            field to update.
     */
    public static void setMetaField(MetaContent customizedEntity, MetaFieldValue field, Integer groupId) {
        MetaFieldValue oldValue = customizedEntity.getMetaField(field.getField().getName(), groupId);
        if (oldValue != null) {
            customizedEntity.getMetaFields().remove(oldValue);
        }
        customizedEntity.getMetaFields().add(field);
    }

    public static void setMetaField(Integer entityId, MetaContent customizedEntity, String name, Object value){
        MetaFieldHelper.setMetaField(entityId, null, customizedEntity, name, value);
    }

    public static void setMetaField(Integer entityId, MetaContent customizedEntity, Integer groupId, String name, Object value){
        MetaFieldHelper.setMetaField(entityId, groupId, customizedEntity, name, value);
    }
    
    public static void setAitMetaField(Integer entityId, CustomerDTO entity, Integer groupId, String name, Object value){
        MetaFieldHelper.setAitMetaField(entityId, groupId, entity, name, value);
    }

    /**
     * Sets the value of an ait meta field that is already associated with this object for a given date. If
     * the field does not already exist, or if the value class is of an incorrect type
     * then an IllegalArgumentException will be thrown.
     *
     * @param customizedEntity entity for search/set fields
     * @param name field name
     * @param value	field value
     * @throws IllegalArgumentException thrown if field name does not exist, or if value is of an incorrect type.
     */
    public static void setMetaField(Integer entityId, Integer groupId, MetaContent customizedEntity, String name, Object value) throws IllegalArgumentException {
        MetaFieldValue fieldValue = customizedEntity.getMetaField(name, groupId);
        if (fieldValue != null) { // common case during editing
            try {
                fieldValue.setValue(value);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Incorrect type for meta field with name " + name, ex);
            }
        } else {
            EntityType[] types = customizedEntity.getCustomizedEntityType();
            if (types == null) {
                throw new IllegalArgumentException("Meta Fields could not be specified for current entity");
            }
            MetaField fieldName = null;
            if(null != groupId){
                fieldName = new MetaFieldDAS().getFieldByNameTypeAndGroup(entityId, types, name, groupId);
            } else if (ArrayUtils.contains(types, EntityType.PAYMENT_METHOD_TYPE)) {
                PaymentMethodTypeDTO type = ((PaymentInformationDTO) customizedEntity).getPaymentMethodType();
                fieldName = findPaymentMethodMetaField(name, type.getId());
            } else {
                fieldName = new MetaFieldDAS().getFieldByName(entityId, types, name);
            }
            if (fieldName == null) {
                throw new IllegalArgumentException("Meta Field with name " + name + " was not defined for current entity");
            }
            MetaFieldValue field = fieldName.createValue();
            try {
                field.setValue(value);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Incorrect type for meta field with name " + name, ex);
            }
            customizedEntity.setMetaField(field, groupId);
        }
    }

    /**
     * Sets the value of an ait meta field in a map.
     *
     * @param entity	:	 customer entity for search/set fields
     * @param name	:	field name
     * @param value	:	field value
     * @throws IllegalArgumentException thrown if field name does not exist, or if value is of an incorrect type.
     */
    public static void setAitMetaField(Integer entityId, Integer groupId, CustomerDTO entity, String name, Object value) throws IllegalArgumentException {
    	EntityType[] types = entity.getCustomizedEntityType();
        if (types == null) {
        	throw new IllegalArgumentException("Meta Fields could not be specified for current entity");
        }
        MetaField fieldName = null;
        if(null != groupId){
        	fieldName = new MetaFieldDAS().getFieldByNameTypeAndGroup(entityId, types, name, groupId);
        }
        if (fieldName == null) {
        	throw new IllegalArgumentException("Meta Field with name " + name + " was not defined for current entity");
        }
        MetaFieldValue field = fieldName.createValue();
        try {
        	field.setValue(value);
        } catch (Exception ex) {
        	throw new IllegalArgumentException("Incorrect type for meta field with name " + name, ex);
        }
        entity.setAitMetaField(field, groupId);
    }

    /**
     * Usefull method for updating meta fields with validation before entity saving
     * @param entity    target entity
     * @param dto       dto with new data
     */
    public static void updateMetaFieldsWithValidation(Integer entityId, Integer accountTypeId, MetaContent entity, MetaContent dto, Boolean global) {
        List<EntityType> entityTypes = new LinkedList(Arrays.asList(entity.getCustomizedEntityType()));
        if(entityTypes.contains(EntityType.ACCOUNT_TYPE)){
            entityTypes.remove(EntityType.ACCOUNT_TYPE);
        }

        Map<String, MetaField> availableMetaFields =
                MetaFieldBL.getAvailableFields(entityId, entityTypes.toArray(new EntityType[entityTypes.size()]));

        for (String fieldName : availableMetaFields.keySet()) {
            MetaFieldValue newValue = dto.getMetaField(fieldName, null);
            MetaFieldValue prevValue = entity.getMetaField(fieldName, null);
            if (newValue == null) { // try to search by id, may be temp fix
                MetaField metaFieldName = availableMetaFields.get(fieldName);
                newValue = dto.getMetaField(metaFieldName.getId());
            }
            // TODO: (VCA) - we want the null values for the validation
            // if ( null != newValue && null != newValue.getValue() ) {

            if(newValue != null){
                entity.setMetaField(entityId, null, fieldName, newValue.getValue());
            }else if((global != null) && global.equals(Boolean.TRUE) && (prevValue != null)){
                /*
                 * if user edits a global category and retains its global scope
                 * then don't filter out null meta-fields and retain previous values
                 * */
                entity.setMetaField(entityId, null, fieldName, prevValue.getValue());
            }else{
                /*
                 * if user edits a global category and marks it as non-global only then filter out null meta-fields
                 * */
                entity.setMetaField(entityId, null, fieldName, null);
            }
             // } //else {
              //no point creating null/empty-value records in db
              //}
        }

        // Updating and validating of ait meta fields is done in a separate method
        for (MetaFieldValue value : entity.getMetaFields()) {
            MetaFieldBL.validateMetaField(value.getField(), value, entity);
        }

        removeEmptyMetaFields(entity);
    }

    public static void updateMetaFieldsWithValidation(Integer entityId, Integer accountTypeId, MetaContent entity, MetaContent dto) {
        updateMetaFieldsWithValidation(entityId, accountTypeId, entity, dto, null);
    }

	/**
	 * Usefull method for updating ait meta fields with validation before entity
	 * saving
	 * 
	 * @param entity
	 *            target entity
	 * @param dto
	 *            dto with new data
	 */
	public static void updateAitMetaFieldsWithValidation(Integer entityId,
			Integer accountTypeId, CustomerDTO entity, MetaContent dto) {
		if (null != accountTypeId) {
			Map<Integer, List<MetaField>> groupMetaFields = MetaFieldBL
					.getAvailableAccountTypeFieldsMap(accountTypeId);

			for (Map.Entry<Integer, List<MetaField>> entry : groupMetaFields
					.entrySet()) {
				Integer groupId = entry.getKey();
				List<MetaField> fields = entry.getValue();

				for (MetaField field : fields) {
					String fieldName = field.getName();
					MetaFieldValue newValue = dto.getMetaField(fieldName,
							groupId);
					if (newValue == null) {
						newValue = dto.getMetaField(field.getId());
					}
					entity.setAitMetaField(entityId, groupId, fieldName,
							newValue != null ? newValue.getValue() : null);
				}
			}
		}

		for (Map.Entry<Integer, List<MetaFieldValue>> entry : entity
				.getAitMetaFieldMap().entrySet()) {
			for (MetaFieldValue value : entry.getValue()) {
				MetaFieldBL.validateMetaField(value.getField(), value, entity);
			}
		}

		removeEmptyAitMetaFields(entity);
	}

    /**
     * Usefull method for updating meta fields with validation before entity saving
     * @param entity    target entity
     * @param dto       dto with new data
     */
    public static void updatePaymentMethodMetaFieldsWithValidation(Integer entityId, Integer paymentMethodTypeId, PaymentInformationDTO entity, MetaContent dto) {

        for (MetaField field : MetaFieldBL.getPaymentMethodMetaFields(paymentMethodTypeId)) {
            String fieldName = field.getName();
            MetaFieldValue newValue = dto.getMetaField(fieldName, null);
            if (newValue == null) {
                newValue = dto.getMetaField(field.getId());
            }
            
            entity.setMetaField(entityId, null, fieldName,
                    newValue != null ? newValue.getValue() : null);
        }

        // Updating and validating of ait meta fields is done in a separate method

        for (MetaFieldValue value : entity.getMetaFields()) {
            MetaFieldBL.validateMetaField(value.getField(), value, entity);
        }
    }

    public static MetaField findPaymentMethodMetaField(String fieldName, Integer paymentMethodTypeId) {

        for (MetaField field : MetaFieldBL.getPaymentMethodMetaFields(paymentMethodTypeId)) {
            if(field.getName().equals(fieldName)){
                return field;
            }
        }
        return null;
    }

    /**
     * Update MetaFieldValues in entity with the values in dto. Only values of MetaFields in {@code metaFieldCollection}
     * will be updated
     *
     * @param metaFieldCollection   meta fields that will be updated
     * @param entity                destination object
     * @param dto                   source object
     */
    public static void updateMetaFieldsWithValidation(Collection<MetaField> metaFieldCollection, MetaContent entity, MetaContent dto) {
        Map<String, MetaField> metaFields = new LinkedHashMap<String, MetaField>();
        for (MetaField field : metaFieldCollection) {
            metaFields.put(field.getName(), field);
        }

        //loop through all the meta fields
        for (String fieldName : metaFields.keySet()) {
            //get the new value
            MetaFieldValue newValue = dto.getMetaField(fieldName);
            if (newValue == null) { // try to search by id, may be temp fix
                MetaField metaFieldName = metaFields.get(fieldName);
                newValue = dto.getMetaField(metaFieldName.getId());
            }

            //create a new value and set it to the default if it exists
            if(newValue == null) {
                MetaField metaField = metaFields.get(fieldName);
                newValue = metaField.createValue();
                if(metaField.getDefaultValue() != null) {
                    newValue.setValue(metaField.getDefaultValue().getValue());
                }
            }

            if(newValue != null) {
                entity.setMetaField(newValue, null);
            }
        }

        //do validation
        for (MetaFieldValue value : entity.getMetaFields()) {
            MetaFieldBL.validateMetaField(value.getField(), value, entity);
        }

        removeEmptyMetaFields(entity);
    }

    /**
     * Remove metafields from the entity with a value of null or ''
     *
     * @param entity
     */
    public static void removeEmptyMetaFields(MetaContent entity) {
        List<MetaFieldValue> metaFields = entity.getMetaFields();
        List<MetaFieldValue> valuesToRemove = new ArrayList<MetaFieldValue>(metaFields.size());

        for(MetaFieldValue mfValue : metaFields) {
            Object value = mfValue.getValue();
            if(value == null ||
                    value.toString().trim().isEmpty()) {
                valuesToRemove.add(mfValue);
            }
        }

        metaFields.removeAll(valuesToRemove);
    }

    /**
     * Remove metafields from the entity with a value of null or ''
     *
     * @param customer
     */
	public static void removeEmptyAitMetaFields(CustomerDTO customer) {
		List<CustomerAccountInfoTypeMetaField> valuesToRemove = new ArrayList<CustomerAccountInfoTypeMetaField>();

		Set<CustomerAccountInfoTypeMetaField> metaFieldsSet = customer
				.getCustomerAccountInfoTypeMetaFields();

		for (CustomerAccountInfoTypeMetaField metaField : metaFieldsSet) {
			MetaFieldValue value = metaField.getMetaFieldValue();

			if (value.getValue() == null
					|| value.getValue().toString().trim().isEmpty()) {
				valuesToRemove.add(metaField);
			}
		}
		metaFieldsSet.removeAll(valuesToRemove);
	}
    
    /**
     * Create missing MetaFieldValues in entity from the values in {@code metaFieldCollection}.
     * Then do validation.
     *
     * @param metaFieldCollection   meta fields that will be updated
     * @param entity                destination object
     */
    public static void updateMetaFieldDefaultValuesWithValidation(Collection<MetaField> metaFieldCollection, MetaContent entity) {
        Map<String, MetaField> metaFields = new LinkedHashMap<String, MetaField>();
        for (MetaField field : metaFieldCollection) {
            metaFields.put(field.getName(), field);
        }

        //loop through all the meta fields
        for (String fieldName : metaFields.keySet()) {
            //get the value
            MetaFieldValue value = entity.getMetaField(fieldName);
            if (value == null) { // try to search by id, may be temp fix
                MetaField metaFieldName = metaFields.get(fieldName);
                value = entity.getMetaField(metaFieldName.getId());
            }

            //create a new value and set it to the default if it exists
            if(value == null) {
                MetaField metaField = metaFields.get(fieldName);
                value = metaField.createValue();
                if(metaField.getDefaultValue() != null) {
                    value.setValue(metaField.getDefaultValue().getValue());
                }
                entity.setMetaField(value, null);
            }
        }

        //do validation
        for (MetaFieldValue value : entity.getMetaFields()) {
            MetaFieldBL.validateMetaField(value.getField(), value, entity);
        }
        removeEmptyMetaFields(entity);
    }

    public static MetaFieldValue getMetaField(MetaContent customizedEntity, Integer metaFieldNameId) {
        for (MetaFieldValue value : customizedEntity.getMetaFields()) {
            if (value.getField() != null && value.getField().getId()==metaFieldNameId){
                return value;
            }
        }
        return null;
    }

    /**
     * Creates a copy of {@code source}. If {@code clearId} is true the MetaFieldValueWS.id field will
     * be set to 0.
     *
     * @param source
     * @param clearId
     * @return
     */
    public static MetaFieldValueWS[] copy(MetaFieldValueWS[] source, boolean clearId) {
        if(source == null) {
            return new MetaFieldValueWS[0];
        }

        MetaFieldValueWS[] copy = Arrays.copyOf(source, source.length);
        if(clearId) {
            for(MetaFieldValueWS ws: copy) {
                ws.setId(0);
            }
        }
        return copy;
    }

    /**
     * Convert a collection of MetaFieldValues to MetaFieldValueWS[]
     *
     * @param metaFieldValues
     * @return
     */
    public static MetaFieldValueWS[] toWSArray(Collection<MetaFieldValue> metaFieldValues) {
        if(metaFieldValues == null) {
            return new MetaFieldValueWS[0];
        }

        MetaFieldValueWS[] result = new MetaFieldValueWS[metaFieldValues.size()];
        int idx = 0;
        for(MetaFieldValue mf : metaFieldValues) {
            result[idx++] = MetaFieldBL.getWS(mf);
        }
        return result;
    }

    /**
     * Comparator for sorting meta field values after retrieving from DB
     */
    public final static class MetaFieldValuesOrderComparator implements Comparator<MetaFieldValue> {
        public int compare(MetaFieldValue o1, MetaFieldValue o2) {
            if (o1.getField().getDisplayOrder() == null && o2.getField().getDisplayOrder() == null) {
                return 0;
            }
            if (o1.getField().getDisplayOrder() != null) {
                return o1.getField().getDisplayOrder().compareTo(o2.getField().getDisplayOrder());
            } else {
                return -1 * o2.getField().getDisplayOrder().compareTo(o1.getField().getDisplayOrder());
            }
        }
    }

    /**
     * Set the values of meta fields (as specified by {@code metaFieldNames}) on {@code entity} with values
     * found in {@code metaFields}.
     *
     * @param metaFieldNames    These MetaFields will get their values set
     * @param entity
     * @param metaFields        New values for MetaFields
     */
    public static void fillMetaFieldsFromWS(Set<MetaField> metaFieldNames, CustomizedEntity entity, MetaFieldValueWS[] metaFields) {
        Map<String, MetaField> metaFieldMap = new HashMap<String, MetaField>(metaFieldNames.size() * 2);
        for(MetaField metaField : metaFieldNames) {
            metaFieldMap.put(metaField.getName(), metaField);
        }

        if (metaFields != null) {
            for (MetaFieldValueWS fieldValue : metaFields) {
                MetaField metaField = metaFieldMap.get(fieldValue.getFieldName());
                if(metaField == null) {
                    throw new SessionInternalError("MetaField ["+fieldValue.getFieldName()+"] does not exist for entity "+entity);
                }
                entity.setMetaField(metaField, fieldValue.getValue());
            }
        }
    }
}
