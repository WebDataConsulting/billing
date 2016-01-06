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

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.mediation.db.MediationConfiguration;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDAS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO;
import com.sapienter.jbilling.server.util.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * MediationBL
 *
 * @author Brian Cowdery
 * @since 21-10-2010
 */
public class MediationConfigurationBL {

    private static PluggableTaskDAS getPluggableTaskDAS() {
        return Context.getBean(Context.Name.PLUGGABLE_TASK_DAS);
    }

    /**
     * Convert a given MediationConfiguration into a MediationConfigurationWS web-service object.
     *
     * @param dto dto to convert
     * @return converted web-service object
     */
    public static MediationConfigurationWS getWS(MediationConfiguration dto) {
        return dto != null ? new MediationConfigurationWS(dto) : null;
    }

    /**
     * Converts a list of MediationConfiguration objects into MediationConfigurationWS web-service objects.
     *
     * @see #getWS(MediationConfiguration)
     *
     * @param objects objects to convert
     * @return a list of converted DTO objects, or an empty list if ws objects list was empty.
     */
    public static List<MediationConfigurationWS> getWS(List<MediationConfiguration> objects) {
        List<MediationConfigurationWS> ws = new ArrayList<MediationConfigurationWS>(objects.size());
        for (MediationConfiguration dto : objects)
            ws.add(getWS(dto));
        return ws;
    }

    /**
     * Convert a given MediationConfigurationWS web-service object into a MediationConfiguration entity.
     *
     * The MediationConfigurationWS must have a pluggable task ID or an exception will be thrown.
     *
     * @param ws ws object to convert
     * @return converted DTO object
     * @throws SessionInternalError if required field is missing
     */
    public static MediationConfiguration getDTO(MediationConfigurationWS ws) {
        if (ws != null) {
            if (ws.getPluggableTaskId() == null)
                throw new SessionInternalError("MediationConfiguration must have a pluggable task id.");

            PluggableTaskDTO pluggableTask = getPluggableTaskDAS().find(ws.getPluggableTaskId());

            return new MediationConfiguration(ws, pluggableTask);
        }
        return null;
    }

    /**
     * Converts a list of MediationConfigurationWS web-service objects into MediationConfiguration objects.
     *
     * @see #getDTO(MediationConfigurationWS)
     *
     * @param objects web-service objects to convert
     * @return a list of converted WS objects, or an empty list if DTO objects list was empty.
     */
    public static List<MediationConfiguration> getDTO(List<MediationConfigurationWS> objects) {
        List<MediationConfiguration> dto = new ArrayList<MediationConfiguration>(objects.size());
        for (MediationConfigurationWS ws : objects)
            dto.add(getDTO(ws));
        return dto;
    }
}
