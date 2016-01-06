package com.sapienter.jbilling.server.order.db;

import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;


@Entity
@TableGenerator(
        name="order_change_type_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="order_change_type",
        allocationSize = 100
)
@Table(name="order_change_type")
@Cache(usage = CacheConcurrencyStrategy.NONE)
public class OrderChangeTypeDTO implements Serializable {

    private Integer id;

    private String name;
    private CompanyDTO entity;

    private boolean defaultType = true;
    private Set<ItemTypeDTO> itemTypes = new HashSet<ItemTypeDTO>(0);

    private boolean allowOrderStatusChange = false;

    private Set<MetaField> orderChangeTypeMetaFields = new HashSet<MetaField>();
    private int optLock;

    @Id
    @GeneratedValue(strategy= GenerationType.TABLE, generator="order_change_type_GEN")
    @Column(name="id", unique=true, nullable=false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id", nullable = true)
    public CompanyDTO getEntity() {
        return entity;
    }

    public void setEntity(CompanyDTO entity) {
        this.entity = entity;
    }

    @Column(name = "default_type", nullable = false, updatable = true)
    public boolean isDefaultType() {
        return defaultType;
    }

    public void setDefaultType(boolean defaultType) {
        this.defaultType = defaultType;
    }

    @Column(name = "allow_order_status_change", nullable = false, updatable = true)
    public boolean isAllowOrderStatusChange() {
        return allowOrderStatusChange;
    }

    public void setAllowOrderStatusChange(boolean allowOrderStatusChange) {
        this.allowOrderStatusChange = allowOrderStatusChange;
    }

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name="order_change_type_item_type_map",
            joinColumns={@JoinColumn(name="order_change_type_id", referencedColumnName="id")},
            inverseJoinColumns={@JoinColumn(name="item_type_id", referencedColumnName="id", unique = true)})
    public Set<ItemTypeDTO> getItemTypes() {
        return itemTypes;
    }

    public void setItemTypes(Set<ItemTypeDTO> itemTypes) {
        this.itemTypes = itemTypes;
    }

    @OneToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name="order_change_type_meta_field_map",
            joinColumns={@JoinColumn(name="order_change_type_id", referencedColumnName="id")},
            inverseJoinColumns={@JoinColumn(name="meta_field_id", referencedColumnName="id", unique = true)})
    @OrderBy("displayOrder")
    public Set<MetaField> getOrderChangeTypeMetaFields() {
        return orderChangeTypeMetaFields;
    }

    public void setOrderChangeTypeMetaFields(Set<MetaField> orderChangeTypeMetaFields) {
        this.orderChangeTypeMetaFields = orderChangeTypeMetaFields;
    }

    @Version
    @Column(name = "optlock")
    public int getOptLock() {
        return optLock;
    }

    public void setOptLock(int optLock) {
        this.optLock = optLock;
    }
}
