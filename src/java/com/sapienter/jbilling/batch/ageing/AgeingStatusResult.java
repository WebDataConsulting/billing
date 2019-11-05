package com.sapienter.jbilling.batch.ageing;

import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects the status of the ageing processing
 *
 * @author Panche Isajeski
 * @since 30-Jan-2014
 */
public class AgeingStatusResult {

    private Integer userId;
    private List<InvoiceDTO> overdueInvoices = new ArrayList<InvoiceDTO>();

    public AgeingStatusResult() {
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public List<InvoiceDTO> getOverdueInvoices() {
        return overdueInvoices;
    }

    public void setOverdueInvoices(List<InvoiceDTO> overdueInvoices) {
        this.overdueInvoices = overdueInvoices;
    }

    @Override
    public String toString() {
        return "AgeingStatusResult{" +
                "userId=" + userId +
                ", overdueInvoices=" + overdueInvoices +
                '}';
    }
}
