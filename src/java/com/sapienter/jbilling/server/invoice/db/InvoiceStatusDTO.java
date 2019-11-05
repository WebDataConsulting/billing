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
package com.sapienter.jbilling.server.invoice.db;

import com.sapienter.jbilling.server.util.db.AbstractGenericStatus;
import com.sapienter.jbilling.server.util.ServerConstants;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Entity;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Set;
import java.util.HashSet;

@Entity
@DiscriminatorValue("invoice_status")
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
public class InvoiceStatusDTO extends AbstractGenericStatus implements Serializable {

    private Set<InvoiceDTO> invoiceDTOs = new HashSet<InvoiceDTO>(0);

    public InvoiceStatusDTO() { }

    public InvoiceStatusDTO(int statusValue) {
        this.statusValue = statusValue;
    }

    public InvoiceStatusDTO(int statusValue, Set<InvoiceDTO> invoiceDTOs) {
        this.statusValue = statusValue;
        this.invoiceDTOs = invoiceDTOs;
    }

    @Transient
    protected String getTable() {
        return ServerConstants.TABLE_INVOICE_STATUS;
    }
}
