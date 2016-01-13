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
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@TableGenerator(
        name = "pluggable_task_parameter_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "pluggable_task_parameter",
        allocationSize = 100
)
@Table(name = "pluggable_task_parameter")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class PluggableTaskParameterDTO implements Serializable {

    private static final FormatLogger LOG = new FormatLogger(PluggableTaskParameterDTO.class);

    public static final int INT = 1;
    public static final int STR = 2;
    public static final int FLO = 3;

    // MAPPED COLUMS

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "pluggable_task_parameter_GEN")
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "int_value")
    private Integer intValue;

    @Column(name = "str_value")
    private String strValue;

    @Column(name = "float_value")
    private BigDecimal floatValue;

    @ManyToOne
    @JoinColumn(name = "task_id")
    private PluggableTaskDTO task;

    @Version
    @Column(name = "OPTLOCK")
    private Integer versionNum;


    // INTERNAL FIELDS
    @Transient
    private Integer type = null; // this indicates the data type of the value
    @Transient
    private String value = null;

    public BigDecimal getFloatValue() {
        return floatValue;
    }

    public void setFloatValue(BigDecimal floatValue) {
        this.floatValue = floatValue;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getIntValue() {
        return intValue;
    }

    public void setIntValue(Integer intValue) {
        this.intValue = intValue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStrValue() {
        return strValue;
    }

    public void setStrValue(String strValue) {
        this.strValue = strValue;
    }

    public PluggableTaskDTO getTask() {
        return task;
    }

    public void setTask(PluggableTaskDTO task) {
        this.task = task;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PluggableTaskParameterDTO{");
        sb.append("id=").append(id);
        sb.append(", name='").append(name).append('\'');
        sb.append(", intValue=").append(intValue);
        sb.append(", strValue='").append(strValue).append('\'');
        sb.append(", floatValue=").append(floatValue);
        sb.append(", value='").append(value).append('\'');
        sb.append(", type=").append(type);
        sb.append('}');
        return sb.toString();
    }

    public void populateValue() {
        if (getIntValue() != null) {
            type = new Integer(INT);
            value = String.valueOf(getIntValue());
        } else if (getStrValue() != null) {
            type = new Integer(STR);
            value = getStrValue();
        } else if (getFloatValue() != null) {
            type = new Integer(FLO);
            value = String.valueOf(getFloatValue());
        } else {
            // the value of this parameter is null
            // we default the type to String
            type = new Integer(STR);
        }
    }

    public void expandValue() throws NumberFormatException {
        if (type == null) return;

        switch (type.intValue()) {
            case INT:
                setIntValue(Integer.valueOf(value));
                setStrValue(null);
                setFloatValue(null);
                break;
            case STR:
                setIntValue(null);
                setStrValue(value);
                setFloatValue(null);
                break;
            case FLO:
                setIntValue(null);
                setStrValue(null);
                setFloatValue(new BigDecimal(value));
                break;
        }
    }

    public Integer getType() {
        return type;
    }

    public String getValue() {
        if (value == null) {
            populateValue();
        }
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setVersionNum(Integer versionNum) {
        this.versionNum = versionNum;
    }

    public Integer getVersionNum() {
        return versionNum;
    }

}
