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

import java.util.List;

import javax.persistence.Transient;

import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;

/**
 * Interface for marking classes that can contain meta-fields. This interface enforces a set
 * of convenience methods for accessing the meta data.
 *
 * @author Brian Cowdery
 * @since 03-Oct-2011
 */
public interface MetaContent {

    // for hibernate mappings
    public List<MetaFieldValue> getMetaFields();
    public void setMetaFields(List<MetaFieldValue> fields);

    /**
     * Returns the meta field by name if it's been defined for this object.
     *
     * @param name meta field name
     * @return field if found, null if not set.
     */
    public MetaFieldValue getMetaField(String name);

    /**
     * Returns the meta field by name if it's been defined for this object.
     *
     * @param name meta field name
     * @param groupId group id
     * @return field if found, null if not set.
     */
    public MetaFieldValue getMetaField(String name, Integer groupId);

    /**
     * Returns the meta field by name if it's been defined for this object.
     *
     * @param metaFieldNameId ID of meta field name
     * @return field if found, null if not set.
     */
    public MetaFieldValue getMetaField(Integer metaFieldNameId);

    /**
     * Adds a meta field to this object. If there is already a field associated with
     * this object then the existing value should be updated.
     *
     * @param field field to update.
     */
    public void setMetaField(MetaFieldValue field, Integer groupId);

    /**
     * Sets the value of a meta field that is already associated with this object. If
     * the field does not already exist, or if the value class is of an incorrect type
     * then an IllegalArgumentException will be thrown.
     *
     * @param name field name
     * @param value field value
     * @throws IllegalArgumentException thrown if field name does not exist, or if value is of an incorrect type.
     */
    public void setMetaField(Integer entityId, Integer groupId, String name, Object value) throws IllegalArgumentException;
    
    /**
     * Usefull method for updating meta fields with validation before entity saving
     *
     * @param dto dto with new data
     */
    @Transient
    public void updateMetaFieldsWithValidation(Integer entityId, Integer accountTypeId, MetaContent dto);

    /**
     * Returns entity type, that defines available custom fields for entity
     *
     * @return EntityType
     */
    public abstract EntityType[] getCustomizedEntityType();
}