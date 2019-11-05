package com.sapienter.jbilling.server.user;


import java.io.Serializable;
import java.util.Date;

/**
 * Created by vivekmaster146 on 8/8/14.
 */
public class AccountTypePriceWS implements Serializable {

    private Integer id;
    private  Integer precedence;
    private Integer model_map_id;
    private Integer itemId;
    private Integer account_type_Id;
    private Date startDate;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getPrecedence() {
        return precedence;
    }

    public void setPrecedence(Integer precedence) {
        this.precedence = precedence;
    }

    public Integer getAccount_type_Id() {
        return account_type_Id;
    }

    public void setAccount_type_Id(Integer account_type_Id) {
        this.account_type_Id = account_type_Id;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Integer getModel_map_id() {
        return model_map_id;
    }

    public void setModel_map_id(Integer model_map_id) {
        this.model_map_id = model_map_id;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }
}
