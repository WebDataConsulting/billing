package com.sapienter.jbilling.server.account;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.db.InvoiceDeliveryMethodDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDeliveryMethodDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDAS;
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDTO;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDTO;
import com.sapienter.jbilling.server.user.db.AccountTypeDAS;
import com.sapienter.jbilling.server.user.db.AccountTypeDTO;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;
import com.sapienter.jbilling.server.util.db.LanguageDAS;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.math.BigDecimal;
import java.util.*;

;

public class AccountTypeBL {

    private static final FormatLogger LOG = new FormatLogger(AccountTypeBL.class);

    private AccountTypeDTO accountTypeDTO = null;
    private AccountTypeDAS accountTypeDAS = null;

    private void init() {
        accountTypeDAS = new AccountTypeDAS();
        accountTypeDTO = new AccountTypeDTO();
    }

    public AccountTypeWS getWS(Integer languageId) {
        return getWS(accountTypeDTO);
    }


    public static final AccountTypeWS getWS(AccountTypeDTO dto) {

        return getWS(dto.getId(), dto.getCompany().getId(), dto.getInvoiceDesign(), dto.getDateCreated(),
                dto.getCreditNotificationLimit1(), dto.getCreditNotificationLimit2(),
                dto.getCreditLimit(), dto.getInvoiceDeliveryMethod(),
                dto.getCurrencyId(), dto.getLanguageId(), dto.getDescription(),
                UserBL.convertMainSubscriptionToWS(dto.getBillingCycle()), dto.getInformationTypes(), dto.getPaymentMethodTypes(), dto.getPreferredNotificationAitId());
    }

    public static final AccountTypeWS getWS(Integer id, Integer entityId, String invoiceDesign, Date dateCreated, BigDecimal creditNotificationLimit1,
                                            BigDecimal creditNotificationLimit2, BigDecimal creditLimit, InvoiceDeliveryMethodDTO invoiceDeliveryMethod,
                                            Integer currencyId, Integer languageId, String description, MainSubscriptionWS mainSubscription,
                                            Set<AccountInformationTypeDTO> informationTypes, Set<PaymentMethodTypeDTO> paymentMethodTypes, Integer preferredNotificationAitId) {

        AccountTypeWS ws = new AccountTypeWS();
        ws.setId(id);
        ws.setEntityId(entityId);
        ws.setInvoiceDesign(invoiceDesign);
        ws.setDateCreated(dateCreated);
        ws.setCreditNotificationLimit1(creditNotificationLimit1 != null ? creditNotificationLimit1
                .toString() : null);
        ws.setCreditNotificationLimit2(creditNotificationLimit2 != null ? creditNotificationLimit2
                .toString() : null);
        ws.setCreditLimit(creditLimit != null ? creditLimit.toString() : null);
        ws.setInvoiceDeliveryMethodId(invoiceDeliveryMethod != null ? invoiceDeliveryMethod
                .getId() : null);
        ws.setCurrencyId(currencyId);
        ws.setLanguageId(languageId);
        ws.setMainSubscription(mainSubscription);

        if (description != null) {
            ws.setName(description, ServerConstants.LANGUAGE_ENGLISH_ID);
        }

        if (null != informationTypes && informationTypes.size() > 0) {
            List<Integer> informationTypeIds = new ArrayList<Integer>();
            for (AccountInformationTypeDTO ait : informationTypes) {
                informationTypeIds.add(ait.getId());
            }
            if (!informationTypeIds.isEmpty()) {
                ws.setInformationTypeIds(informationTypeIds
                        .toArray(new Integer[informationTypeIds.size()]));
            }
        }

        if (null != paymentMethodTypes && paymentMethodTypes.size() > 0) {
            List<Integer> paymentMethodTypeIds = new ArrayList<Integer>(0);
            for (PaymentMethodTypeDTO paymentMethodType : paymentMethodTypes) {
                paymentMethodTypeIds.add(paymentMethodType.getId());
            }
            ws.setPaymentMethodTypeIds(paymentMethodTypeIds
                    .toArray(new Integer[paymentMethodTypeIds.size()]));
        }

        ws.setpreferredNotificationAitId(preferredNotificationAitId);
        return ws;
    }

    public static final AccountTypeDTO getDTO(AccountTypeWS ws, Integer entityId) {

        AccountTypeDTO accountTypeDTO = new AccountTypeDTO();
        if (ws.getId() != null && ws.getId() > 0) {
            accountTypeDTO.setId(ws.getId());
        }

		accountTypeDTO.setCompany(new CompanyDTO(entityId));

		accountTypeDTO.setCreditLimit(ws.getCreditLimitAsDecimal());
		accountTypeDTO.setCreditNotificationLimit1(ws
				.getCreditNotificationLimit1AsDecimal());
		accountTypeDTO.setCreditNotificationLimit2(ws
				.getCreditNotificationLimit2AsDecimal());
		accountTypeDTO.setInvoiceDesign(ws.getInvoiceDesign());
		accountTypeDTO.setBillingCycle(UserBL.convertMainSubscriptionFromWS(
				ws.getMainSubscription(), entityId));
		accountTypeDTO.setLanguage(new LanguageDAS().find(ws.getLanguageId()));
		accountTypeDTO.setCurrency(new CurrencyDAS().find(ws.getCurrencyId()));
		accountTypeDTO.setInvoiceDeliveryMethod(new InvoiceDeliveryMethodDTO(ws
				.getInvoiceDeliveryMethodId()));
		accountTypeDTO.setPreferredNotificationAitId(ws
				.getpreferredNotificationAitId());
		// set payment method types
		if (ws.getPaymentMethodTypeIds() != null) {
			Set<PaymentMethodTypeDTO> paymentMethodTypes = new HashSet<PaymentMethodTypeDTO>(
					0);
			PaymentMethodTypeDAS das = new PaymentMethodTypeDAS();

			for (Integer paymentMethodTypeId : ws.getPaymentMethodTypeIds()) {
				paymentMethodTypes.add(das.find(paymentMethodTypeId));
			}
			accountTypeDTO.setPaymentMethodTypes(paymentMethodTypes);
		}

		List<PaymentMethodTypeDTO> globalPaymentMethods = new PaymentMethodTypeDAS()
				.findByAllAccountType(entityId);
		for (PaymentMethodTypeDTO globalPaymentMethod : globalPaymentMethods) {
			accountTypeDTO.addPaymentMethodType(globalPaymentMethod);
		}

        if (CollectionUtils.isNotEmpty(ws.getDescriptions())) {
            for (InternationalDescriptionWS desc: ws.getDescriptions()) {
                accountTypeDTO.setDescription(desc.getContent(), desc.getLanguageId());
            }
        }

		return accountTypeDTO;
    }

    public AccountTypeBL() {
        init();
    }

    public AccountTypeBL(Integer accountTypeId) {
        init();
        setAccountType(accountTypeId);
    }

    public void setAccountType(Integer accountTypeId) {
        accountTypeDTO = accountTypeDAS.find(accountTypeId);
    }

    public AccountTypeDTO getAccountType() {
        return accountTypeDTO;
    }

    public boolean delete() {

        if (accountTypeDTO.getCustomers().size() > 0) {
            return false;
        }

        for (AccountInformationTypeDTO ait : accountTypeDTO.getInformationTypes()) {
            new AccountInformationTypeBL(ait.getId()).delete();
        }
        accountTypeDTO.getInformationTypes().clear();
        accountTypeDAS.delete(accountTypeDTO);
        return true;
    }

    public AccountTypeDTO create(AccountTypeDTO accountTypeDTO) {

        accountTypeDTO.setDateCreated(new Date());
        accountTypeDTO = accountTypeDAS.save(accountTypeDTO);

        accountTypeDAS.flush();
        accountTypeDAS.clear();
        return accountTypeDTO;
    }

    public void update(AccountTypeDTO accountType) {

        AccountTypeDTO accountTypeDTO = accountTypeDAS.find(accountType.getId());

        accountTypeDTO.setCreditLimit(accountType.getCreditLimit());
        accountTypeDTO.setCreditNotificationLimit1(accountType.getCreditNotificationLimit1());
        accountTypeDTO.setCreditNotificationLimit2(accountType.getCreditNotificationLimit2());
        accountTypeDTO.setInvoiceDesign(accountType.getInvoiceDesign());
        accountTypeDTO.setBillingCycle(accountType.getBillingCycle());
        accountTypeDTO.setLanguage(new LanguageDAS().find(accountType.getLanguageId()));
        accountTypeDTO.setCurrency(new CurrencyDAS().find(accountType.getCurrencyId()));
        accountTypeDTO.setInvoiceDeliveryMethod(new InvoiceDeliveryMethodDAS().find(accountType.getInvoiceDeliveryMethod().getId()));
        accountTypeDTO.setPaymentMethodTypes(accountType.getPaymentMethodTypes());
        accountTypeDTO.setPreferredNotificationAitId(accountType.getPreferredNotificationAitId());
        accountTypeDAS.save(accountTypeDTO);

        accountTypeDAS.flush();
        accountTypeDAS.clear();
    }

    public static boolean isAccountTypeUnique(Integer entityId, String name, boolean isNew) {
        List<AccountTypeDTO> accountTypeDTOList = new AccountTypeDAS().findAll(entityId);
        List<String> descriptionList = new ArrayList<String>();
        for (AccountTypeDTO accountType1 : accountTypeDTOList) {

            descriptionList.add(accountType1.getDescription());
        }

        if (isNew) {
            return !descriptionList.contains(name);
        } else {
            return Collections.frequency(descriptionList, name) < 2;
        }
    }

    public static void validateAccountType(AccountTypeWS accountType, Integer entityId, boolean isNew) {

        if (CollectionUtils.isNotEmpty(accountType.getDescriptions())) {
            accountType.getDescriptions().forEach( desc -> {
                // verify if description is non empty and unique
                if (StringUtils.isEmpty(StringUtils.trim(desc.getContent()))) {
                    String[] errmsgs = new String[1];
                    errmsgs[0] = "AccountTypeWS,descriptions,accountTypeWS.error.blank.name";
                    throw new SessionInternalError(
                            "There is an error in  data.", errmsgs);
                } else if (!AccountTypeBL.isAccountTypeUnique(entityId, desc.getContent(), isNew)) {
                    String[] errmsgs = new String[1];
                    errmsgs[0] = "AccountTypeWS,descriptions,accountTypeWS.error.unique.name";
                    throw new SessionInternalError(
                            "There is an error in  data.", errmsgs);
                }
            });

            BigDecimal creditLimit = accountType.getCreditLimitAsDecimal();
            BigDecimal notification1 = accountType
                    .getCreditNotificationLimit1AsDecimal();
            if (creditLimit != null && notification1 != null
                    && !(creditLimit.compareTo(notification1) >= 0)) {
                String[] errmsgs = new String[1];
                errmsgs[0] = "AccountTypeWS,creditNotificationLimit1,accountTypeWS.error.credit.limit";
                throw new SessionInternalError("There is an error in  data.",
                        errmsgs);
            }
            BigDecimal notification2 = accountType
                    .getCreditNotificationLimit2AsDecimal();
            if (creditLimit != null && notification2 != null
                    && !(creditLimit.compareTo(notification2) >= 0)) {
                String[] errmsgs = new String[1];
                errmsgs[0] = "AccountTypeWS,creditNotificationLimit2,accountTypeWS.error.credit.limit";
                throw new SessionInternalError("There is an error in  data.",
                        errmsgs);
            }
        }
    }

}
