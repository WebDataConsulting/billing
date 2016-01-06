/**
 * 
 */
package com.sapienter.jbilling.server.util.db;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.OrderBy;

import com.sapienter.jbilling.server.user.db.CompanyDTO;

/**
 * @author Vikas Bodani
 * @since 08-Aug-2011
 * 
 */

@Entity
@TableGenerator(
        name = "enumeration_GEN", 
        table = "jbilling_seqs", 
        pkColumnName = "name", 
        valueColumnName = "next_id", 
        pkColumnValue = "enumeration", 
        allocationSize = 10
        )
@Table(name = "enumeration")
@NamedQueries({
        @NamedQuery(name = "EnumerationDTO.findByEntityAndId",
                    query = "from EnumerationDTO as enum " +
                            "where enum.entity.id = :entityId " +
                            "and enum.id = :enumerationId"),
        @NamedQuery(name = "EnumerationDTO.findByEntityAndName",
                    query = "from EnumerationDTO as enum " +
                            "where enum.entity.id = :entityId " +
                            "and enum.name = :name"),
        @NamedQuery(name = "EnumerationDTO.findAll",
                    query = "from EnumerationDTO as enum " +
                            "where enum.entity.id = :entityId " +
                            "order by enum.id desc"),
        @NamedQuery(name = "EnumerationDTO.getCountAll",
                    query = "select count(enum.id) " +
                            "from EnumerationDTO as enum " +
                            "where enum.entity.id = :entityId")
})
//@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class EnumerationDTO implements Serializable {

    private int id;
    private String name;
    private List<EnumerationValueDTO> values= new ArrayList<EnumerationValueDTO>(0);
    private CompanyDTO entity;
    private int versionNum;
    
    public EnumerationDTO() {
    }

    public EnumerationDTO(int id) {
        this.id = id;
    }

    public EnumerationDTO(int id, String name, List<EnumerationValueDTO> values, CompanyDTO entity) {
        this.id = id;
        this.name = name;
        this.values = values;
        this.entity= entity;
    }

    @Id 
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "enumeration_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id")
    public CompanyDTO getEntity() {
        return this.entity;
    }

    public void setEntity(CompanyDTO entity) {
        this.entity = entity;
    }

    @Column(name = "name", nullable = false, length = 50)
    @NotNull(message="validation.error.notnull")
    @Size(min = 1, max = 50, message = "validation.error.size,1,50")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="enumeration")
    @Cascade(value= org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    @Fetch (FetchMode.SUBSELECT)
    @OrderBy(clause = "id")
    @Valid
    public List<EnumerationValueDTO> getValues() {
        return this.values;
    }

    public void setValues(List<EnumerationValueDTO> values) {
        this.values = values;
    }

    @Version
    @Column(name = "OPTLOCK")
    public Integer getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(Integer versionNum) {
        this.versionNum = versionNum;
    }
    
    @Override
    public String toString() {
        return "EnumerationDTO [id=" + id + ", name=" + name + ", values #=" + values.size() + "]";
    }

}
