package com.sapienter.jbilling.server.item;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Version of ItemDependencyDTO safe for WS.
 *
 * @author Gerhard
 * @since 12/09/13
 */
public class ItemDependencyDTOEx implements Serializable {

    private Integer id;
    @NotNull(message = "validation.error.notnull")
    private ItemDependencyType type;

    private Integer itemId;
    @NotNull(message = "validation.error.notnull")
    @Min(value = 0, message = "validation.error.min,0")
    private Integer minimum;
    private Integer maximum;
    @NotNull(message = "validation.error.notnull")
    private Integer dependentId;  //maps to ItemDTO or ItemTypeDTO id

    private String dependentDescription;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public ItemDependencyType getType() {
        return type;
    }

    public void setType(ItemDependencyType type) {
        this.type = type;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public Integer getMinimum() {
        return minimum;
    }

    public void setMinimum(Integer minimum) {
        this.minimum = minimum;
    }

    public Integer getMaximum() {
        return maximum;
    }

    public void setMaximum(Integer maximum) {
        this.maximum = maximum;
    }

    public Integer getDependentId() {
        return dependentId;
    }

    public void setDependentId(Integer dependentId) {
        this.dependentId = dependentId;
    }

    public String getDependentDescription() {
        return dependentDescription;
    }

    public void setDependentDescription(String dependentDescription) {
        this.dependentDescription = dependentDescription;
    }

    @Override
    public String toString() {
        return "ItemDependencyDTOEx{" +
                "id=" + id +
                ", type=" + type +
                ", itemId=" + itemId +
                ", minimum=" + minimum +
                ", maximum=" + maximum +
                ", dependentId=" + dependentId +
                ", dependentDescription='" + dependentDescription + '\'' +
                '}';
    }
}
