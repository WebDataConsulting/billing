package com.sapienter.jbilling.server.user.db;

import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.io.Serializable;

/**
 *  AccountTypePriceDTO composite primary key
 *
 *  @author Panche Isajeski
 *  @since 05/14/2013
 */
@Embeddable
public class AccountTypePricePK implements Serializable {

    private AccountTypeDTO accountType;

    public AccountTypePricePK() {
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_type_id", nullable = false)
    public AccountTypeDTO getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountTypeDTO accountType) {
        this.accountType = accountType;
    }
}
