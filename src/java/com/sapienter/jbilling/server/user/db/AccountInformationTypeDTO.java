package com.sapienter.jbilling.server.user.db;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.sapienter.jbilling.server.metafields.db.MetaFieldGroup;

/**
 * Account Information Type entity.
 * 
 * @author Oleg Baskakov
 * @since 07-May-2013
 */
@Entity
@DiscriminatorValue("ACCOUNT_TYPE")
public class AccountInformationTypeDTO extends MetaFieldGroup implements Serializable {

    @Column(name = "name", nullable = false, length = 100)
    @NotNull(message = "validation.error.notnull")
    @Size(min = 1, max = 100, message = "validation.error.size,1,100")
    private String name;

    @ManyToOne(targetEntity = AccountTypeDTO.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "account_type_id", nullable = false)
    private AccountTypeDTO accountType;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "accountInfoType", cascade = CascadeType.ALL)
    private Set<CustomerAccountInfoTypeMetaField> customerAccountInfoTypeMetaFields = new HashSet<CustomerAccountInfoTypeMetaField>(0);

	@Transient
    private boolean useForNotifications;

    public AccountInformationTypeDTO() {
        super();
    }

    public AccountInformationTypeDTO(String name,
                                     AccountTypeDTO accountType) {
        super();

        this.name = name;
        this.accountType = accountType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AccountTypeDTO getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountTypeDTO accountType) {
        this.accountType = accountType;
    }
    
    public Set<CustomerAccountInfoTypeMetaField> getCustomerAccountInfoTypeMetaFields() {
    	return this.customerAccountInfoTypeMetaFields;
    }
     
    public void setCustomerAccountInfoTypeMetaFields(Set<CustomerAccountInfoTypeMetaField> customerAccountInfoTypeMetaFields) {
    	this.customerAccountInfoTypeMetaFields = customerAccountInfoTypeMetaFields;
    }

    public boolean isUseForNotifications() {
        return useForNotifications;
    }

    public void setUseForNotifications(boolean useForNotifications) {
        this.useForNotifications = useForNotifications;
    }
}
