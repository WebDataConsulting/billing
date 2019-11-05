package com.sapienter.jbilling.server.user.db;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.util.Date;

@Entity
@TableGenerator(
        name="customer_notes_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="customer_notes",
        allocationSize = 100
)
@Table(name = "customer_notes")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class CustomerNoteDTO implements java.io.Serializable
{
    private int noteId;
    private String noteTitle;
    private String noteContent;
    private Date creationTime;
    private CompanyDTO company;
    private CustomerDTO customer;
    private UserDTO user;

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "customer_notes_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getNoteId() {
        return this.noteId;
    }

    public void setNoteId(int noteId) {
        this.noteId = noteId;
    }
    @Column(name = "note_title", length = 50)
    public String getNoteTitle() {
        return noteTitle;
    }

    public void setNoteTitle(String noteTitle) {
        this.noteTitle = noteTitle;
    }
    @Column(name = "note_content", length = 1000)
    public String getNoteContent() {
        return noteContent;
    }

    public void setNoteContent(String noteContent) {
        this.noteContent = noteContent;
    }
    @Column(name = "creation_time", nullable = false, length = 29)
    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }
    @PrePersist
    protected void onCreate() {
        creationTime = new Date();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id")
    public CompanyDTO getCompany() {
        return this.company;
    }

    public void setCompany(CompanyDTO entity) {
        this.company = entity;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    public CustomerDTO getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerDTO customer) {
        this.customer = customer;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "CustomerNoteDTO{" +
                "noteId=" + noteId +
                ", noteTitle='" + noteTitle + '\'' +
                ", noteContent='" + noteContent + '\'' +
                ", creationTime=" + creationTime +
                ", company=" + company +
                ", customer=" + customer +
                ", user=" + user +
                '}';
    }
}
