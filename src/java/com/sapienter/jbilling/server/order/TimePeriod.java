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

/*
 * Created on Oct 7, 2004
 *
 */
package com.sapienter.jbilling.server.order;

import java.util.Calendar;

/**
 * @author Emil
 */
public class TimePeriod {

    private Integer unitId = null;
    private Integer value = null;
    private Boolean df_fm = null;
    private Long own_invoice = null;

    public Integer getUnitId() {
        return unitId;
    }

    public void setUnitId(Integer unitId) {
        this.unitId = unitId;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public Boolean getDf_fm() {
        return df_fm;
    }

    public void setDf_fm(Boolean df_fm) {
        this.df_fm = df_fm;
    }

    public void setDf_fm(Integer df_fm) {
        if (df_fm == null) {
            this.df_fm = null;
        } else {
            this.df_fm = Boolean.valueOf(df_fm.intValue() == 1);
        }
    }

    public Long getOwn_invoice() {
        return own_invoice;
    }

    public void setOwn_invoice(Integer own_invoice) {
        if (own_invoice != null && own_invoice.intValue() == 1) {
            // give a unique number to it
            Calendar cal = Calendar.getInstance();
            this.own_invoice = new Long(cal.getTimeInMillis());
        } else {
            this.own_invoice = new Long(0);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TimePeriod that = (TimePeriod) o;

        if (df_fm != null ? !df_fm.equals(that.df_fm) : that.df_fm != null) return false;
        if (own_invoice != null ? !own_invoice.equals(that.own_invoice) : that.own_invoice != null) return false;
        if (!unitId.equals(that.unitId)) return false;
        if (!value.equals(that.value)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = unitId.hashCode();
        result = 31 * result + value.hashCode();
        result = 31 * result + (df_fm != null ? df_fm.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TimePeriod{" +
                "unitId=" + unitId +
                ", value=" + value +
                '}';
    }
}
