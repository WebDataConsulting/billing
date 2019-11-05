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

import com.sapienter.jbilling.server.util.db.AbstractGenericStatusDAS;

import java.util.List;

public class MediationRecordStatusDAS extends AbstractGenericStatusDAS<MediationRecordStatusDTO> {

    public void touch(List<MediationRecordStatusDTO> list) {
        for(MediationRecordStatusDTO proc: list) {
            initialize(proc);
        }
    }

    /** This method can be used for initializing any proxy (entities, collections)
     *
     * @param proxy Hibernate proxy object for initialization
     */
    protected void initialize(MediationRecordStatusDTO proxy) {
        getHibernateTemplate().initialize(proxy);
    }
}

