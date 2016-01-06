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

import com.sapienter.jbilling.server.mediation.db.MediationRecordDTO;
import com.sapienter.jbilling.server.mediation.db.MediationRecordLineDTO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * MediationRecordBL
 *
 * @author Brian Cowdery
 * @since 21-10-2010
 */
public class MediationRecordBL {

    /**
     * Convert a given MediationRecordDTO into a MediationRecordWS web-service object.
     *
     * @param dto dto to convert
     * @return converted web-service object
     */
    public static MediationRecordWS getWS(MediationRecordDTO dto) {
        return dto != null ? new MediationRecordWS(dto, getWS(dto.getLines())) : null;
    }

    /**
     * Converts a list of MediationRecordDTO objects into MediationRecordWS web-service objects.
     *
     * @see #getWS(MediationRecordDTO)
     *
     * @param objects objects to convert
     * @return a list of converted DTO objects, or an empty list if ws objects list was empty.
     */
    public static List<MediationRecordWS> getWS(List<MediationRecordDTO> objects) {
        List<MediationRecordWS> ws = new ArrayList<MediationRecordWS>(objects.size());
        for (MediationRecordDTO dto : objects)
            ws.add(getWS(dto));
        return ws;
    }

    /**
     * Convert a given MediationRecordLineDTO into a MediationRecordLineWS web-service object.
     *
     * @param dto dto to convert
     * @return converted web-service object
     */
    public static MediationRecordLineWS getWS(MediationRecordLineDTO dto) {
        return dto != null ? new MediationRecordLineWS(dto) : null;
    }

    /**
     * Converts a list of MediationRecordLineDTO objects into MediationRecordLineWS web-service objects.
     *
     * @see #getWS(MediationRecordLineDTO)
     *
     * @param objects objects to convert
     * @return a list of converted DTO objects, or an empty list if ws objects list was empty.
     */
    public static List<MediationRecordLineWS> getWS(Collection<MediationRecordLineDTO> objects) {
        List<MediationRecordLineWS> ws = new ArrayList<MediationRecordLineWS>(objects.size());
        for (MediationRecordLineDTO dto : objects)
            ws.add(getWS(dto));
        return ws;
    }


}
