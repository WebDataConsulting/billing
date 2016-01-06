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

package com.sapienter.jbilling.server.mediation;

import java.io.Serializable;

/**
 * Web-service compatible representation of the value map returned by
 * {@link IMediationSessionBean#getMediationRecordsByMediationProcess(Integer)}.
 *
 * This class is necessary as Apache CXF (JAXB) does not handle Maps and a custom JAXB binding
 * might not be supported by SOAP clients.
 *
 * @author Brian Cowdery
 * @since 25-10-2010
 */
public class RecordCountWS implements Serializable {

    private Integer statusId;
    private Long count;

    public RecordCountWS() {
    }

    public RecordCountWS(Integer statusId, Long count) {
        this.statusId = statusId;
        this.count = count;
    }

    public Integer getStatusId() {
        return statusId;
    }

    public void setStatusId(Integer statusId) {
        this.statusId = statusId;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "RecordCountWS{"
               + "statusId=" + statusId
               + ", count=" + count
               + '}';
    }
}
