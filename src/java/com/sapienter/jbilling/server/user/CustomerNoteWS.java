package com.sapienter.jbilling.server.user;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

public class CustomerNoteWS implements Serializable {
    private int noteId;
    @NotNull(message="validation.error.notnull")
    @Size(min= 1, max = 50, message = "validation.error.size,5,50")
    private String noteTitle;
    @NotNull(message="validation.error.notnull")
    @Size(min = 1, max = 1000, message = "validation.error.size,5,1000")
    private String noteContent;
    private Date creationTime;
    private Integer entityId;
    private Integer customerId;
    private Integer userId;

    public CustomerNoteWS() {
    }

    public CustomerNoteWS(int noteId, String noteTitle, String noteContent, Date creationTime, Integer entityId, Integer customerId, Integer userId) {
        this.noteId = noteId;
        this.noteTitle = noteTitle;
        this.noteContent = noteContent;
        this.creationTime = creationTime;
        this.entityId = entityId;
        this.customerId = customerId;
        this.userId = userId;
    }

    public int getNoteId() {
        return noteId;
    }

    public void setNoteId(int noteId) {
        this.noteId = noteId;
    }

    public String getNoteTitle() {
        return noteTitle;
    }

    public void setNoteTitle(String noteTitle) {
        this.noteTitle = noteTitle;
    }

    public String getNoteContent() {
        return noteContent;
    }

    public void setNoteContent(String noteContent) {
        this.noteContent = noteContent;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }


    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    @Override
    public String toString() {
        return "CustomerNoteWS{" +
                "noteId=" + noteId +
                ", noteTitle='" + noteTitle + '\'' +
                ", noteContent='" + noteContent + '\'' +
                ", creationTime=" + creationTime +
                ", entityId=" + entityId +
                ", customerId=" + customerId +
                ", userId=" + userId +
                '}';
    }
}
