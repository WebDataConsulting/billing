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

import com.sapienter.jbilling.server.mediation.db.MediationOrderMap;
import com.sapienter.jbilling.server.mediation.db.MediationProcess;
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
        return dto != null ? MediationRecordBL.getDTO(dto, getWS(dto.getLines())) : null;
    }

    /**
     * Converts a list of MediationRecordDTO objects into MediationRecordWS web-service objects.
     *
     * @param objects objects to convert
     * @return a list of converted DTO objects, or an empty list if ws objects list was empty.
     * @see #getWS(MediationRecordDTO)
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
        return dto != null ? MediationRecordBL.getDTO(dto) : null;
    }

    /**
     * Converts a list of MediationRecordLineDTO objects into MediationRecordLineWS web-service objects.
     *
     * @param objects objects to convert
     * @return a list of converted DTO objects, or an empty list if ws objects list was empty.
     * @see #getWS(MediationRecordLineDTO)
     */

    public static MediationProcessWS getWS(MediationProcess dto) {
        return dto != null ? MediationRecordBL.getDTO(dto) : null;
    }

    public static List<MediationRecordLineWS> getWS(Collection<MediationRecordLineDTO> objects) {
        List<MediationRecordLineWS> ws = new ArrayList<MediationRecordLineWS>(objects.size());
        for (MediationRecordLineDTO dto : objects)
            ws.add(getWS(dto));
        return ws;
    }

    public static MediationRecordWS getDTO(MediationRecordDTO dto, List<MediationRecordLineWS> lines) {
        MediationRecordWS retvalue = new MediationRecordWS();
        retvalue.setId(dto.getId());
        retvalue.setKey(dto.getKey());
        retvalue.setStarted(dto.getStarted());
        retvalue.setProcessId(dto.getProcess() != null ? dto.getProcess().getId() : null);
        retvalue.setRecordStatusId(dto.getRecordStatus() != null ? dto.getRecordStatus().getId() : null);
        retvalue.setLines(lines);
        return retvalue;
    }

    public static MediationRecordLineWS getDTO(MediationRecordLineDTO dto) {
        MediationRecordLineWS retrecord = new MediationRecordLineWS();
        retrecord.setId(dto.getId());
        retrecord.setOrderLineId(dto.getOrderLine() != null ? dto.getOrderLine().getId() : null);
        retrecord.setEventDate(dto.getEventDate());
        retrecord.setAmount(dto.getAmount());
        retrecord.setQuantity(dto.getQuantity());
        retrecord.setDescription(dto.getDescription());
        return retrecord;
    }

    public static MediationProcessWS getDTO(MediationProcess dto) {
        MediationProcessWS retval = new MediationProcessWS();
        retval.setId(dto.getId());
        retval.setConfigurationId(dto.getConfiguration() != null ? dto.getConfiguration().getId() : null);
        retval.setStartDatetime(dto.getStartDatetime());
        retval.setEndDatetime(dto.getEndDatetime());
        retval.setOrdersAffected(dto.getOrdersAffected());

        // order ID's
        retval.setOrderIds(new ArrayList<Integer>(dto.getOrderMap().size()));
        for (MediationOrderMap map : dto.getOrderMap())
            retval.getOrderIds().add(map.getOrderId());

        // mediation record ID's
        retval.setRecordIds(new ArrayList<Integer>(dto.getRecords().size()));
        for (MediationRecordDTO record : dto.getRecords())
            retval.getOrderIds().add(record.getId());
        return retval;
    }


}
