/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */
package com.sapienter.jbilling.server.mediation.db;

import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Query;

import java.util.List;

public class MediationRecordLineDAS extends AbstractDAS<MediationRecordLineDTO> {

    private static final String findByOrder =
            " select a " +
            "   from MediationRecordLineDTO a " +
            "  where a.orderLine.purchaseOrder.id = :orderId " +
            "    and a.orderLine.deleted = 0 " +
            "  order by a.orderLine.id, a.id";

    public List<MediationRecordLineDTO> findByOrder(Integer orderId) {
        Query query = getSession().createQuery(findByOrder);
        query.setParameter("orderId", orderId);
        return query.list();
    }

    private static final String FIND_BY_INVOICE_HQL =
        "select recordLine " +
            "from MediationRecordLineDTO recordLine " +
            "    inner join recordLine.orderLine.purchaseOrder as purchaseOrder " +
            "    inner join purchaseOrder.orderProcesses orderProcess " +
            "where orderProcess.invoice.id = :invoiceId";

    /**
     * Find all MediationRecordLineDTO events incorporated into the given
     * invoice.
     *
     * @param invoiceId invoice id
     * @return list of mediation events, empty list if none found
     */
    @SuppressWarnings("unchecked")
    public List<MediationRecordLineDTO> findByInvoice(Integer invoiceId) {
        Query query = getSession().createQuery(FIND_BY_INVOICE_HQL);
        query.setParameter("invoiceId", invoiceId);
        return query.list();
    }

}
