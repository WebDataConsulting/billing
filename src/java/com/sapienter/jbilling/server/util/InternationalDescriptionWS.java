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


import java.io.Serializable;

import com.sapienter.jbilling.server.util.db.InternationalDescriptionDTO;

/**
 * InternationalDescriptionWS
 *
 * @author Brian Cowdery
 * @since 27/01/11
 */
public class InternationalDescriptionWS implements Serializable{

    private static final long serialVersionUID = 20130704L;

    private String psudoColumn;
    private Integer languageId;
    private String content;
    private boolean deleted;

    public InternationalDescriptionWS() {
    }

    public InternationalDescriptionWS(Integer languageId, String content) {
        this.psudoColumn = "description";
        this.languageId = languageId;
        this.content = content;
    }

    public InternationalDescriptionWS(String psudoColumn, Integer languageId, String content) {
        this.psudoColumn = psudoColumn;
        this.languageId = languageId;
        this.content = content;
    }

	public InternationalDescriptionWS(InternationalDescriptionDTO description) {
		if (description.getId() != null) {
			this.psudoColumn = description.getId().getPsudoColumn();
			this.languageId = description.getId().getLanguageId();
		}
		this.content = description.getContent();
	}
    
    /**
     * Alias for {@link #getPsudoColumn()}
     * @return psudo-column label
     */
    public String getLabel() {
        return getPsudoColumn();
    }

    /**
     * Alias for {@link #setPsudoColumn(String)}
     * @param label psudo-column label string
     */
    public void setLabel(String label) {
        setPsudoColumn(label);
    }

    public String getPsudoColumn() {
        return psudoColumn;
    }

    public void setPsudoColumn(String psudoColumn) {
        this.psudoColumn = psudoColumn;
    }

    public Integer getLanguageId() {
        return languageId;
    }

    public void setLanguageId(Integer languageId) {
        this.languageId = languageId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public String toString() {
        return "InternationalDescriptionWS{"
               + ", psudoColumn='" + psudoColumn + '\''
               + ", languageId=" + languageId
               + ", content='" + content + '\''
               + '}';
    }
}
