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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * MediationRecordWS
 *
 * @author Brian Cowdery
 * @since 21-10-2010
 */
public class MediationRecordWS implements Serializable {

    private Integer id;
    private String key;
    private Date started;
    private Integer processId;
    private Integer recordStatusId;
    private List<MediationRecordLineWS> lines = new ArrayList<MediationRecordLineWS>();

    public MediationRecordWS() {
    }

    public MediationRecordWS(MediationRecordDTO dto, List<MediationRecordLineWS> lines) {
        this.id = dto.getId();
        this.key = dto.getKey();
        this.started = dto.getStarted();
        this.processId = dto.getProcess() != null ? dto.getProcess().getId() : null;
        this.recordStatusId = dto.getRecordStatus() != null ? dto.getRecordStatus().getId() : null;
        this.lines = lines;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Date getStarted() {
        return started;
    }

    public void setStarted(Date started) {
        this.started = started;
    }

    public Integer getProcessId() {
        return processId;
    }

    public void setProcessId(Integer processId) {
        this.processId = processId;
    }

    public Integer getRecordStatusId() {
        return recordStatusId;
    }

    public void setRecordStatusId(Integer recordStatusId) {
        this.recordStatusId = recordStatusId;
    }

    public List<MediationRecordLineWS> getLines() {
        return lines;
    }

    public void setLines(List<MediationRecordLineWS> lines) {
        this.lines = lines;
    }

    @Override
    public String toString() {
        return "MediationRecordWS{"
               + "id=" + id
               + ", key='" + key + '\''
               + ", started=" + started
               + ", processId=" + processId
               + ", recordStatusId=" + recordStatusId
               + ", lines=" + (lines != null ? lines.size() : null)
               + '}';
    }
}
