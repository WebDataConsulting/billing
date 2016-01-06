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

package com.sapienter.jbilling.server.util.db;

import java.io.Serializable;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.util.Constants;
;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.util.Context;

public abstract class AbstractDescription implements Serializable {

    private String description = null;

    abstract public int getId();
    abstract protected String getTable();

    /**
     * Returns the InternationalDescriptionDTO for the given language and label for this entity.
     *
     * @param languageId language id
     * @param label psudo column label
     * @return description DTO
     */
    public InternationalDescriptionDTO getDescriptionDTO(Integer languageId, String label) {
        if (label == null || languageId == null)
            throw new SessionInternalError("Cannot find translation without label or language " + label + ":" + languageId);

        if (getId() == 0)
            return null;

        JbillingTableDAS tableDas = Context.getBean(Context.Name.JBILLING_TABLE_DAS);
        JbillingTable table = tableDas.findByName(getTable());

        InternationalDescriptionId id = new InternationalDescriptionId(table.getId(), getId(), label, languageId);
        return new DescriptionDAS().findNow(id);
    }

    /**
     * Returns the InternationalDescriptionDTO for the given language and default "description"
     * label for this entity.
     *
     * @param languageId language id
     * @return description DTO
     */
    public InternationalDescriptionDTO getDescriptionDTO(Integer languageId) {
        return getDescriptionDTO(languageId, "description");
    }

    /**
     * Returns the default english description for this entity. This method caches the description
     * in a local variable so that it can quickly be retrieved on subsequent calls.
     *
     * @return english description string
     */
    public String getDescription() {
        if (description == null) {
        	description = getDescription(Constants.LANGUAGE_ENGLISH_ID);
        }
        return description;
        
    }

    /**
     * Returns the description string for the given language for this entity.
     *
     * @param languageId language id
     * @return description string
     */
    public String getDescription(Integer languageId) {
        InternationalDescriptionDTO description = getDescriptionDTO(languageId);
        return description != null ? description.getContent() : ( !Constants.LANGUAGE_ENGLISH_ID.equals(languageId) ? getDescription(Constants.LANGUAGE_ENGLISH_ID) : null) ;
    }

    /**
     * Returns the string for the given language and the given label for this entity.
     *
     * @param languageId language id
     * @param label psudo column label
     * @return description string
     */
    public String getDescription(Integer languageId, String label) {
        InternationalDescriptionDTO description = getDescriptionDTO(languageId, label);
		if (description == null
				&& !Constants.LANGUAGE_ENGLISH_ID.equals(languageId)) {
        	//get string for the given label and default language
        	description = getDescriptionDTO(Constants.LANGUAGE_ENGLISH_ID, label);
        }
        return description != null ? description.getContent() : null;
        
    }

    /**
     * Sets the cached description to the given text. This does not update the database.
     *
     * @param text description
     */
    public void setDescription(String text) {
        description = text;
    }

    /**
     * Updates and saves the description for the given language id.
     *
     * @param content text description
     * @param languageId language id
     */
    public void setDescription(String content, Integer languageId) {
        setDescription("description", languageId, content);
    }

    /**
     * Updates ands aves the description for the given label (psudo column), and language id.
     *
     * @param label psudo column label
     * @param languageId language id
     * @param content text description
     */
    public void setDescription(String label, Integer languageId, String content) {
        JbillingTableDAS tableDas = Context.getBean(Context.Name.JBILLING_TABLE_DAS);
        JbillingTable table = tableDas.findByName(getTable());

        InternationalDescriptionId id = new InternationalDescriptionId(table.getId(), getId(), label, languageId);
        InternationalDescriptionDTO desc = new InternationalDescriptionDTO(id, content);

        new DescriptionDAS().save(desc);
    }

    public void deleteDescription(int languageId) {
        JbillingTableDAS tableDas = Context.getBean(Context.Name.JBILLING_TABLE_DAS);
        JbillingTable table = tableDas.findByName(getTable());

        InternationalDescriptionDAS descriptionDas = (InternationalDescriptionDAS) Context
                .getBean(Context.Name.DESCRIPTION_DAS);
        
        descriptionDas.delete(table.getId(), getId(), "description", languageId);
    }
    
    public void deleteDescription(String label, int languageId) {
        JbillingTableDAS tableDas = Context.getBean(Context.Name.JBILLING_TABLE_DAS);
        JbillingTable table = tableDas.findByName(getTable());

        InternationalDescriptionDAS descriptionDas = (InternationalDescriptionDAS) Context
                .getBean(Context.Name.DESCRIPTION_DAS);
        
        descriptionDas.delete(table.getId(), getId(), label, languageId);
    }
}
