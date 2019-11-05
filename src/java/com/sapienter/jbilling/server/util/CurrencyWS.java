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

import com.sapienter.jbilling.server.util.api.validation.CreateValidationGroup;
import org.apache.commons.lang.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * CurrencyWS
 *
 * @author Brian Cowdery
 * @since 07/04/11
 */
public class CurrencyWS implements Serializable {

    private Integer id;

    @NotEmpty(message = "validation.error.notnull", groups = CreateValidationGroup.class)
    private String description;
    @NotNull(message = "validation.error.notnull")
    @Size(min = 1, max = 10, message = "validation.error.size,1,10")
    private String symbol;
    @NotNull(message = "validation.error.notnull")
    @Size(min = 1, max = 3, message = "validation.error.size,1,3")
    private String code;
    @NotNull(message = "validation.error.notnull")
    @Size(min = 2, max = 2, message = "validation.error.size.exact,2")
    private String countryCode;
    private Boolean inUse;

    @Digits(integer = 10, fraction = 4, message = "validation.error.invalid.number.or.fraction")
    private String rate;
    @NotNull(message = "validation.error.notnull")
    @Digits(integer = 10, fraction = 4, message = "validation.error.invalid.number.or.fraction")
    private String sysRate;
    private Date fromDate;

    private boolean defaultCurrency;

    public CurrencyWS() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public Boolean getInUse() {
        return inUse;
    }

    public void setInUse(Boolean inUse) {
        this.inUse = inUse;
    }

    public String getRate() {
        return rate;
    }

    public BigDecimal getRateAsDecimal() {
        return com.sapienter.jbilling.common.Util.string2decimal(rate);
    }

    public void setRate(String rate) {
        if(!StringUtils.isEmpty(rate)) {
            this.rate = rate;
        } else {
            this.rate = null;
        }
    }

    public void setRate(BigDecimal rate) {
        this.rate = (rate != null ? rate.toString() : null);
    }

    public void setRateAsDecimal(BigDecimal rate) {
        setRate(rate);
    }

    public String getSysRate() {
        return sysRate;
    }

    public BigDecimal getSysRateAsDecimal() {
        return com.sapienter.jbilling.common.Util.string2decimal(sysRate);
    }

    public void setSysRate(String sysRate) {
        this.sysRate = sysRate;
    }

    public void setSysRate(BigDecimal systemRate) {
        this.sysRate = (systemRate != null ? systemRate.toString() : null);
    }

    public void setSysRateAsDecimal(BigDecimal systemRate) {
        setSysRate(systemRate);
    }

    public boolean isDefaultCurrency() {
        return defaultCurrency;
    }

    public boolean getDefaultCurrency() {
        return defaultCurrency;
    }

    public void setDefaultCurrency(boolean defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    @Override
    public String toString() {
        return "CurrencyWS{"
               + "id=" + id
               + ", symbol='" + symbol + '\''
               + ", code='" + code + '\''
               + ", countryCode='" + countryCode + '\''
               + ", inUse=" + inUse
               + ", rate='" + rate + '\''
               + ", systemRate='" + sysRate + '\''
               + ", isDefaultCurrency=" + defaultCurrency
               + ", fromDate=" + fromDate
               + '}';
    }
}
