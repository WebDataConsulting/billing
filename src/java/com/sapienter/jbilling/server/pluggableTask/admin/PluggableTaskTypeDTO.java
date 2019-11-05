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
package com.sapienter.jbilling.server.pluggableTask.admin;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.db.AbstractDescription;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "pluggable_task_type")
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
public class PluggableTaskTypeDTO extends AbstractDescription implements Serializable {

    private static final FormatLogger LOG = new FormatLogger(PluggableTaskTypeDTO.class);

    @Id
    @Column(name = "id")
    private Integer pk;

    @Column(name = "class_name")
    private String className;

    @Column(name = "min_parameters")
    private Integer minParameters;

    @ManyToOne
    @JoinColumn(name = "category_id")
    @Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
    private PluggableTaskTypeCategoryDTO category;

    public PluggableTaskTypeDTO() {
        // default
    }

    public PluggableTaskTypeDTO(Integer id) {
        this.pk = id;
    }

    // method useful to get a detached copy
    public PluggableTaskTypeDTO(PluggableTaskTypeDTO otherDto) {
        pk = otherDto.getId();
        className = otherDto.getClassName();
        minParameters = otherDto.getMinParameters();
        category = otherDto.getCategory();
    }

    protected String getTable() {
        return ServerConstants.TABLE_PLUGGABLE_TASK_TYPE;
    }

    public int getId() {
        return pk;
    }

    public void setId(Integer id) {
        this.pk = id;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Integer getPk() {
        return pk;
    }

    public void setPk(Integer id) {
        pk = id;
    }

    public Integer getMinParameters() {
        return minParameters;
    }

    public void setMinParameters(Integer minParameters) {
        this.minParameters = minParameters;
    }

    public void setCategory(PluggableTaskTypeCategoryDTO category) {
        this.category = category;
    }

    public PluggableTaskTypeCategoryDTO getCategory() {
        return category;
    }

    public String toString() {
        StringBuffer str = new StringBuffer("{");
        str.append("-").append(this.getClass().getName()).append("-");
        str.append("id=").append(getPk()).append(" ").append("className=").append(getClassName()).append(" ").append("minParameters=").append(getMinParameters()).append(" ").append("category=").append(getCategory());
        str.append('}');

        return (str.toString());

    }

    public void touch(){
        if(null != getCategory()){
            getCategory().getId();
        }
    }

}
