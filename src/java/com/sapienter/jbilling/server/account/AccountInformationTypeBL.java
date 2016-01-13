package com.sapienter.jbilling.server.account;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.metafields.*;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.user.AccountInformationTypeWS;
import com.sapienter.jbilling.server.user.db.*;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.Util;

import javax.persistence.Transient;
import java.util.*;



/**
 * Business logic class for managing the account information types
 *
 * @author Panche Isajeski
 * @since 05/23/2013
 */
public class AccountInformationTypeBL {

    private static final FormatLogger LOG = new FormatLogger(AccountInformationTypeBL.class);

    private AccountInformationTypeDTO accountInformationTypeDTO = null;
    private AccountInformationTypeDAS accountInformationTypeDAS = null;

    private void init() {
        accountInformationTypeDAS = new AccountInformationTypeDAS();
        accountInformationTypeDTO = new AccountInformationTypeDTO();
    }

    public AccountInformationTypeBL() {
        init();
    }

    public AccountInformationTypeBL(Integer accountInformationType) {
        init();
        setAccountInformationType(accountInformationType);
    }

    public void setAccountInformationType(Integer accountInformationType) {
        accountInformationTypeDTO = accountInformationTypeDAS.find(accountInformationType);
    }

    public AccountInformationTypeDTO getAccountInformationType() {
        return accountInformationTypeDTO;
    }

    public AccountInformationTypeDTO create(AccountInformationTypeDTO accountInformationType) {

        if (checkDuplicateAIT(accountInformationType)) {
            throw new SessionInternalError("Account Information Type should be unique", new String[]{
                    "AccountInformationTypeDTO,metaField,metafield.validation.ait.unique"
            });
        }

        AccountInformationTypeDTO ait = accountInformationTypeDAS.findByName(accountInformationType.getName(), accountInformationType.getEntity().getId(), accountInformationType.getAccountType().getId());
        if (ait != null) {
            throw new SessionInternalError("Account Information Type", new String[]{
                    "AccountInformationTypeDTO,name,accountInformationType.name.exists"
            });
        }
        accountInformationType.setDateCreated(new Date());
        saveMetaFields(accountInformationType);
        boolean useForNotifications = accountInformationType.isUseForNotifications();
        accountInformationType = accountInformationTypeDAS.save(accountInformationType);
        accountInformationType.setUseForNotifications(useForNotifications);

        AccountTypeDTO aitAccType = new AccountTypeDAS().findForUpdate(accountInformationType.getAccountType().getId());
        if (useForNotifications) {
            aitAccType.setPreferredNotificationAitId(accountInformationType.getId());
        }
        return accountInformationType;
    }

    public void update(AccountInformationTypeDTO accountInformationTypeDTO) {

        if (checkDuplicateAIT(accountInformationTypeDTO)) {
            throw new SessionInternalError("Account Information Type should be unique", new String[]{
                    "AccountInformationTypeDTO,metaField,metafield.validation.ait.unique"
            });
        }

        AccountInformationTypeDTO dto = accountInformationTypeDAS.findNow(accountInformationTypeDTO.getId());
        //workaround: touch on account type to load it
        dto.getAccountType().getPreferredNotificationAitId();

        dto.setDateUpdated(new Date());
        dto.setDisplayOrder(accountInformationTypeDTO.getDisplayOrder());
        dto.setName(accountInformationTypeDTO.getName());
        saveMetaFields(dto, accountInformationTypeDTO);
        accountInformationTypeDAS.save(dto);
        boolean useForNotifications = accountInformationTypeDTO.isUseForNotifications();
        dto.setUseForNotifications(useForNotifications);

        AccountTypeDTO aitAccType = new AccountTypeDAS().findForUpdate(dto.getAccountType().getId());
        if (useForNotifications) {
            aitAccType.setPreferredNotificationAitId(dto.getId());
        }

    }

    public boolean delete() {

        for (MetaField mf : accountInformationTypeDTO.getMetaFields()) {
            new MetaFieldBL().delete(mf.getId());
        }
        accountInformationTypeDTO.getMetaFields().clear();
        accountInformationTypeDAS.delete(accountInformationTypeDTO);
        return true;
    }

    private void saveMetaFields(AccountInformationTypeDTO accountInformationType) {

        Set<MetaField> metafields = new HashSet<MetaField>(accountInformationType.getMetaFields());
        accountInformationType.getMetaFields().clear();

        for (MetaField mf : metafields) {
            if (mf.getId() == 0) {
                accountInformationType.getMetaFields().add(new MetaFieldBL().create(mf));
            } else {
                new MetaFieldBL().update(mf);
                accountInformationType.getMetaFields().add(new MetaFieldDAS().find(mf.getId()));
            }
        }
    }

    private void saveMetaFields(AccountInformationTypeDTO persistedAccountInformationType,
                                AccountInformationTypeDTO accountInformationType) {

        Map<Integer, Collection<MetaField>> diffMap = Util.calculateCollectionDifference(
                persistedAccountInformationType.getMetaFields(),
                accountInformationType.getMetaFields(),
                new Util.IIdentifier<MetaField>() {

                    @Override
                    public boolean evaluate(MetaField input, MetaField output) {
                        if (input.getId() != 0 && output.getId() != 0) {
                            return input.getId() == output.getId();
                        } else {
                            return input.getName().equals(output.getName());
                        }
                    }

                    @Override
                    public void setIdentifier(MetaField input, MetaField output) {
                        output.setId(input.getId());
                    }
                });

        persistedAccountInformationType.getMetaFields().clear();

        for (MetaField mf : diffMap.get(-1)) {
            new MetaFieldBL().delete(mf.getId());
            persistedAccountInformationType.getMetaFields().remove(mf);
        }

        for (MetaField mf : diffMap.get(0)) {
            new MetaFieldBL().update(mf);
            persistedAccountInformationType.getMetaFields().add(new MetaFieldDAS().find(mf.getId()));
        }

        for (MetaField mf : diffMap.get(1)) {
            persistedAccountInformationType.getMetaFields().add(new MetaFieldBL().create(mf));
        }
    }

    public AccountInformationTypeWS getWS() {
        AccountInformationTypeWS accountInformationTypeWS = getWS(accountInformationTypeDTO);

        return accountInformationTypeWS;
    }

    public static final AccountInformationTypeWS getWS(AccountInformationTypeDTO dto) {
        AccountInformationTypeWS accountInformationTypeWS = MetaFieldGroupBL.getAccountInformationTypeWS(dto);

        //MetaFieldGroupBL.getWS(dto);
        accountInformationTypeWS.setName(dto.getName());
        accountInformationTypeWS.setAccountTypeId(dto.getAccountType().getId());
        accountInformationTypeWS.setUseForNotifications(AccountInformationTypeBL.checkUseForNotifications(dto));

        return accountInformationTypeWS;
    }

    @Transient
    public static final AccountInformationTypeDTO getDTO(AccountInformationTypeWS ws, Integer entityId) {

        AccountInformationTypeDTO ait = new AccountInformationTypeDTO();

        ait.setDisplayOrder(ws.getDisplayOrder());
        ait.setEntity(new CompanyDTO(entityId));
        ait.setEntityType(null == ws.getEntityType() ? ws.getEntityType() : EntityType.ACCOUNT_TYPE);
        ait.setId(ws.getId());
        try {

            MetaField metaField;
            Set<MetaField> metafieldsDTO = new HashSet<MetaField>();
            if (ws.getMetaFields() != null) {
                for (MetaFieldWS metafieldWS : ws.getMetaFields()) {
                    metaField = MetaFieldBL.getDTO(metafieldWS, entityId);
                    metafieldsDTO.add(metaField);
                }
            }
            ait.setMetaFields(metafieldsDTO);


            if (ws.getId() > 0) {
                List<InternationalDescriptionWS> descriptions = ws.getDescriptions();
                for (InternationalDescriptionWS description : descriptions) {
                    if (description.getLanguageId() != null
                            && description.getContent() != null) {
                        if (description.isDeleted()) {
                            ait.deleteDescription(description
                                    .getLanguageId());
                        } else {
                            ait.setDescription(description.getContent(),
                                    description.getLanguageId());
                        }
                    }
                }
            }

            ait.setName(ws.getName());
            ait.setUseForNotifications(ws.isUseForNotifications());

            if (ws.getAccountTypeId() != null) {
                AccountTypeDTO accountTypeDTO = new AccountTypeDAS().find(ws.getAccountTypeId());
                if (accountTypeDTO != null) {
                    ait.setAccountType(accountTypeDTO);
                }
            }
        } catch (Exception e) {

            throw new SessionInternalError("Exception converting MetaFieldGroupWS to DTO object", e,
                    new String[]{"MetaFieldGroupWS,metafieldGroups,cannot.convert.metafieldgroupws.error"});
        }

        return ait;
    }

    public List<AccountInformationTypeDTO> getAccountInformationTypes(Integer accountTypeId) {
        return accountInformationTypeDAS.getInformationTypesForAccountType(accountTypeId);
    }

    public static boolean checkDuplicateAIT(AccountInformationTypeDTO ait) {
        HashSet<MetaFieldType> unique = new HashSet<MetaFieldType>();
        boolean isDuplicate = false;
        if (ait.getMetaFields() != null) {
            outer:
            for (MetaField metaField : ait.getMetaFields()) {
                if (metaField.getFieldUsage() != null) {
                    if (!unique.add(metaField.getFieldUsage())) {
                        isDuplicate = true;
                        break outer;
                    }
                }
            }
        }
        return isDuplicate;
    }

    public void updateAccountTypeNotificationUseAIT(AccountInformationTypeDTO accountInformationTypeDTO) {
        AccountTypeDTO accountTypeDTO = null;
        if (accountInformationTypeDTO != null) {
            accountTypeDTO = new AccountTypeDAS().find(accountInformationTypeDTO.getAccountType().getId());
            if (accountInformationTypeDTO.isUseForNotifications()) {
                accountTypeDTO.setPreferredNotificationAitId(accountInformationTypeDTO.getId());
                new AccountTypeBL().update(accountTypeDTO);
            } else {
                if (checkUseForNotifications(accountInformationTypeDTO)) {
                    accountTypeDTO.setPreferredNotificationAitId(null);
                    new AccountTypeBL().update(accountTypeDTO);
                }
            }
        }
    }

    public static boolean checkUseForNotifications(AccountInformationTypeDTO accountInformationTypeDTO) {
        if (accountInformationTypeDTO.getAccountType() != null
                && accountInformationTypeDTO.getAccountType().getPreferredNotificationAitId() != null
                && accountInformationTypeDTO.getAccountType().getPreferredNotificationAitId() == accountInformationTypeDTO.getId()) {
            return true;
        }
        return false;
    }
}
