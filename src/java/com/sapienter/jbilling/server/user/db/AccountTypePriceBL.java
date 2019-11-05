package com.sapienter.jbilling.server.user.db;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;

/**
 * Business logic for account type pricing.
 *
 * This class manages the logic of applying different prices for different account types
 *
 * @author Panche Isajeski
 * @since 05/14/2013
 */
public class AccountTypePriceBL {

    private static final FormatLogger LOG = new FormatLogger(AccountTypePriceBL.class);

    private AccountTypePriceDAS accountTypePriceDAS;

    private AccountTypeDAS accountTypeDAS;
    private Integer accountTypeId;

    private AccountTypeDTO accountType;
    private AccountTypePriceDTO price;

    public AccountTypePriceBL() {
        _init();
    }

    public AccountTypePriceBL(Integer accountTypeId) {
        try{
            _init();
            this.accountType = accountTypeDAS.find(accountTypeId);
            this.accountTypeId = accountTypeId;
        } catch (Exception e){
            throw new SessionInternalError("Setting account type price", AccountTypePriceBL.class, e);
        }
    }

    public AccountTypePriceBL(AccountTypeDTO accountType) {
        _init();
        this.accountType = accountType;
        this.accountTypeId = accountType.getId();
    }

    public AccountTypePriceBL(Integer accountTypeId, Integer planItemId) {
        this(accountTypeId);
        setAccountTypePrice(planItemId);
    }

    public AccountTypePriceBL(AccountTypeDTO accountType, Integer planItemId) {
        this(accountType);
        setAccountTypePrice(planItemId);
    }

    private void _init() {
        accountTypePriceDAS = new AccountTypePriceDAS();
        accountTypeDAS = new AccountTypeDAS();
    }

    public void setAccountTypeId(Integer accountTypeId) {
        this.accountType = accountTypeDAS.find(accountTypeId);
        this.accountTypeId = accountTypeId;
    }

    public void setAccountTypePrice(Integer planItemId) {
        this.price = accountTypePriceDAS.find(accountTypeId, planItemId);
    }

    public Integer getAccountTypeId() {
        return accountTypeId;
    }

    public AccountTypeDTO getAccountType() {
        return accountType;
    }

    public AccountTypePriceDTO getPrice() {
        return price;
    }

    /**
     *  Deletes the account type price
     *
     */
    public void delete() {
        if (price != null) {
            // TODO (pai) Check if the plan item should be deleted as well
            accountTypePriceDAS.delete(price);
        } else {
            LOG.error("Cannot delete, AccountTypePriceDTO not found or not set!");
        }
    }
}
