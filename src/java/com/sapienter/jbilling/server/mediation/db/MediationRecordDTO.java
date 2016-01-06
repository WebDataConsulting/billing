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

import com.sapienter.jbilling.server.mediation.MediationRecordWS;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Version;

@TableGenerator(
        name = "mediation_record_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "mediation_record", 
        allocationSize = 100)
@Entity
@Table(name = "mediation_record")
// no cache : it is hardly ever re-read 
public class MediationRecordDTO implements Serializable {

    private Integer id;
    private String key;
    private Date started;
    private MediationProcess process;
    private MediationRecordStatusDTO recordStatus;
    private Collection<MediationRecordLineDTO> lines = new ArrayList<MediationRecordLineDTO>();
    private int optlock;

    protected MediationRecordDTO() {
    }

    public MediationRecordDTO(String key, Date started, MediationProcess process, MediationRecordStatusDTO recordStatus) {
        this.key = key;
        this.started = started;
        this.process = process;
        this.recordStatus = recordStatus;
    }

    public MediationRecordDTO(MediationRecordWS ws, MediationProcess process, MediationRecordStatusDTO recordStatus,
                              Collection<MediationRecordLineDTO> lines) {

        this.id = ws.getId();
        this.key = ws.getKey();
        this.started = ws.getStarted();
        this.process = process;
        this.recordStatus = recordStatus;
        this.lines = lines;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "mediation_record_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "id_key", nullable = false)
    public String getKey() {
        return key;
    }

    protected void setKey(String key) {
        this.key = key;
    }

    @Column(name = "start_datetime")
    public Date getStarted() {
        return started;
    }
    
    protected void setStarted(Date started) {
        this.started = started;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mediation_process_id")
    public MediationProcess getProcess() {
        return process;
    }

    public void setProcess(MediationProcess process) {
        this.process = process;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    public MediationRecordStatusDTO getRecordStatus() {
        return recordStatus;
    }

    public void setRecordStatus(MediationRecordStatusDTO recordStatus) {
        this.recordStatus = recordStatus;
    }

    @OneToMany(cascade=CascadeType.ALL, fetch = FetchType.LAZY, mappedBy="record")
    public Collection<MediationRecordLineDTO> getLines() {
        return lines;
    }

    public void setLines(Collection<MediationRecordLineDTO> lines) {
        this.lines = lines;
    }

    @Version
    @Column(name = "OPTLOCK")
    public int getOptlock() {
        return optlock;
    }

    protected void setOptlock(int optlock) {
        this.optlock = optlock;
    }
}
