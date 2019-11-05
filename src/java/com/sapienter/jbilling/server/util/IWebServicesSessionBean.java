/*
 jBilling - The Enterprise Open Source Billing System
 Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

 This file is part of jbilling.

 jbilling is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 jbilling is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with jbilling.  If not, see <http://www.gnu.org/licenses/>.

 This source was modified by Web Data Technologies LLP (www.webdatatechnologies.in) since 15 Nov 2015.
 You may download the latest source from webdataconsulting.github.io.

 */

package com.sapienter.jbilling.server.util;

import java.io.File;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.jws.WebService;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.discount.DiscountWS;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.*;
import com.sapienter.jbilling.server.metafields.MetaFieldGroupWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.order.*;
import com.sapienter.jbilling.server.payment.PaymentAuthorizationDTOEx;
import com.sapienter.jbilling.server.payment.PaymentMethodTemplateWS;
import com.sapienter.jbilling.server.payment.PaymentMethodTypeWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeCategoryWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.process.AgeingWS;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.BillingProcessWS;
import com.sapienter.jbilling.server.process.ProcessStatusWS;
import com.sapienter.jbilling.server.user.AccountInformationTypeWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.user.ContactWS;
import com.sapienter.jbilling.server.user.CreateResponseWS;
import com.sapienter.jbilling.server.user.CustomerNoteWS;
import com.sapienter.jbilling.server.user.UserCodeWS;
import com.sapienter.jbilling.server.user.UserTransitionResponseWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.ValidatePurchaseWS;
import com.sapienter.jbilling.server.user.partner.CommissionProcessConfigurationWS;
import com.sapienter.jbilling.server.user.partner.CommissionProcessRunWS;
import com.sapienter.jbilling.server.user.partner.CommissionWS;
import com.sapienter.jbilling.server.user.partner.PartnerWS;
import com.sapienter.jbilling.server.util.search.SearchCriteria;
import com.sapienter.jbilling.server.item.AssetSearchResult;
import com.sapienter.jbilling.server.mediation.MediationConfigurationWS;
import com.sapienter.jbilling.server.mediation.MediationProcessWS;
import com.sapienter.jbilling.server.mediation.MediationRecordLineWS;
import com.sapienter.jbilling.server.mediation.MediationRecordWS;
import com.sapienter.jbilling.server.mediation.RecordCountWS;
import javax.jws.WebService;
import javax.persistence.criteria.CriteriaBuilder.In;

import java.io.File;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.sapienter.jbilling.server.util.search.SearchResultString;


/**
 * Web service bean interface. 
 * {@see com.sapienter.jbilling.server.util.WebServicesSessionSpringBean} for documentation.
 */
public interface IWebServicesSessionBean {

    public Integer getCallerId();
    public Integer getCallerCompanyId();
    public Integer getCallerLanguageId();
    public Integer getCallerCurrencyId();

    /*
        Users
     */
    public UserWS getUserWS(Integer userId) throws SessionInternalError;
    public Integer createUser(UserWS newUser) throws SessionInternalError;
    public Integer createUserWithCompanyId(UserWS newUser, Integer entityId) throws SessionInternalError;
    public void updateUser(UserWS user) throws SessionInternalError;
    public void updateUserWithCompanyId(UserWS user, Integer entityId) throws SessionInternalError;
    public void deleteUser(Integer userId) throws SessionInternalError;

    public boolean userExistsWithName(String userName);
    public boolean userExistsWithId(Integer userId);

    public ContactWS[] getUserContactsWS(Integer userId) throws SessionInternalError;
    public void updateUserContact(Integer userId, ContactWS contact) throws SessionInternalError;

    public void setAuthPaymentType(Integer userId, Integer autoPaymentType, boolean use) throws SessionInternalError;
    public Integer getAuthPaymentType(Integer userId) throws SessionInternalError;

    public Integer[] getUsersByStatus(Integer statusId, boolean in) throws SessionInternalError;
    public Integer[] getUsersInStatus(Integer statusId) throws SessionInternalError;
    public Integer[] getUsersNotInStatus(Integer statusId) throws SessionInternalError;

    public Integer getUserId(String username) throws SessionInternalError;
    public Integer getUserIdByEmail(String email) throws SessionInternalError;

    public UserTransitionResponseWS[] getUserTransitions(Date from, Date to) throws SessionInternalError;
    public UserTransitionResponseWS[] getUserTransitionsAfterId(Integer id) throws SessionInternalError;

    public CreateResponseWS create(UserWS user, OrderWS order, OrderChangeWS[] orderChanges) throws SessionInternalError;

    public Integer createUserCode(UserCodeWS userCode) throws SessionInternalError;
    public UserCodeWS[] getUserCodesForUser(Integer userId) throws SessionInternalError;
    public void updateUserCode(UserCodeWS userCode) throws SessionInternalError;
    public Integer[] getCustomersByUserCode(String userCode) throws SessionInternalError;
    public Integer[] getOrdersByUserCode(String userCode) throws SessionInternalError;
    public Integer[] getOrdersLinkedToUser(Integer userId) throws SessionInternalError;
    public Integer[] getCustomersLinkedToUser(Integer userId) throws SessionInternalError;
    public void resetPassword(int userId) throws SessionInternalError;


    /*
        Partners
     */

    public PartnerWS getPartner(Integer partnerId) throws SessionInternalError;
    public Integer createPartner(UserWS newUser, PartnerWS partner) throws SessionInternalError;
    public void updatePartner(UserWS newUser, PartnerWS partner) throws SessionInternalError;
    public void deletePartner (Integer partnerId) throws SessionInternalError;


    /*
        Items
     */

    // categories parentness
    public ItemTypeWS[] getItemCategoriesByPartner(String partner, boolean parentCategoriesOnly);
    public ItemTypeWS[] getChildItemCategories(Integer itemTypeId);

    public ItemDTOEx getItem(Integer itemId, Integer userId, String pricing);
    public ItemDTOEx[] getAllItems() throws SessionInternalError;
    public Integer createItem(ItemDTOEx item) throws SessionInternalError;
    public void updateItem(ItemDTOEx item);
    public void deleteItem(Integer itemId);

    public ItemDTOEx[] getAddonItems(Integer itemId);

    public ItemDTOEx[] getItemByCategory(Integer itemTypeId);
    public Integer[] getUserItemsByCategory(Integer userId, Integer categoryId);

	public ItemTypeWS getItemCategoryById(Integer id);
    public ItemTypeWS[] getAllItemCategories();
    public Integer createItemCategory(ItemTypeWS itemType) throws SessionInternalError;
    public void updateItemCategory(ItemTypeWS itemType) throws SessionInternalError;
    public void deleteItemCategory(Integer itemCategoryId);
    
    public ItemTypeWS[] getAllItemCategoriesByEntityId(Integer entityId);
    public ItemDTOEx[] getAllItemsByEntityId(Integer entityId);
    
    public String isUserSubscribedTo(Integer userId, Integer itemId);

    public InvoiceWS getLatestInvoiceByItemType(Integer userId, Integer itemTypeId) throws SessionInternalError;
    public Integer[] getLastInvoicesByItemType(Integer userId, Integer itemTypeId, Integer number) throws SessionInternalError;

    public OrderWS getLatestOrderByItemType(Integer userId, Integer itemTypeId) throws SessionInternalError;
    public Integer[] getLastOrdersByItemType(Integer userId, Integer itemTypeId, Integer number) throws SessionInternalError;

    public ValidatePurchaseWS validatePurchase(Integer userId, Integer itemId, String fields);
    public ValidatePurchaseWS validateMultiPurchase(Integer userId, Integer[] itemId, String[] fields);
    public Integer getItemID(String productCode) throws SessionInternalError;


    /*
        Orders
     */

    public OrderWS getOrder(Integer orderId) throws SessionInternalError;
    public Integer createOrder(OrderWS order, OrderChangeWS[] orderChanges) throws SessionInternalError;
    public void updateOrder(OrderWS order, OrderChangeWS[] orderChanges) throws SessionInternalError;
    public Integer createUpdateOrder(OrderWS order, OrderChangeWS[] orderChanges) throws SessionInternalError;
    public String deleteOrder(Integer id) throws SessionInternalError;

    public Integer createOrderAndInvoice(OrderWS order, OrderChangeWS[] orderChanges) throws SessionInternalError;

    public OrderWS getCurrentOrder(Integer userId, Date date) throws SessionInternalError;
    public OrderWS updateCurrentOrder(Integer userId, OrderLineWS[] lines, String pricing, Date date, String eventDescription) throws SessionInternalError;

    public OrderWS[] getUserSubscriptions(Integer userId) throws SessionInternalError;
    
    public OrderLineWS getOrderLine(Integer orderLineId) throws SessionInternalError;
    public void updateOrderLine(OrderLineWS line) throws SessionInternalError;

    public Integer[] getOrderByPeriod(Integer userId, Integer periodId) throws SessionInternalError;
    public OrderWS getLatestOrder(Integer userId) throws SessionInternalError;
    public Integer[] getLastOrders(Integer userId, Integer number) throws SessionInternalError;
    public Integer[] getOrdersByDate (Integer userId, Date since, Date until);
    public OrderWS[] getUserOrdersPage(Integer user, Integer limit, Integer offset) throws SessionInternalError;

    public Integer[] getLastOrdersPage(Integer userId, Integer limit, Integer offset) throws SessionInternalError;

    public OrderWS rateOrder(OrderWS order, OrderChangeWS[] orderChanges) throws SessionInternalError;
    public OrderWS[] rateOrders(OrderWS orders[], OrderChangeWS[] orderChanges) throws SessionInternalError;

    public boolean updateOrderPeriods(OrderPeriodWS[] orderPeriods) throws SessionInternalError;
    public boolean updateOrCreateOrderPeriod(OrderPeriodWS orderPeriod) throws SessionInternalError;
    public boolean deleteOrderPeriod(Integer periodId) throws SessionInternalError;

    public PaymentAuthorizationDTOEx createOrderPreAuthorize(OrderWS order, OrderChangeWS[] orderChanges) throws SessionInternalError;
    
    public OrderPeriodWS[] getOrderPeriods() throws SessionInternalError;
    public OrderPeriodWS getOrderPeriodWS(Integer orderPeriodId) throws SessionInternalError;

    public void updateOrders(OrderWS[] orders, OrderChangeWS[] orderChanges) throws SessionInternalError;
    
    /*
    Account Type
    */
    public Integer createAccountType(AccountTypeWS accountType) throws SessionInternalError;
    public boolean updateAccountType(AccountTypeWS accountType);
    public boolean deleteAccountType(Integer accountTypeId) throws SessionInternalError;
    public AccountTypeWS getAccountType(Integer accountTypeId) throws SessionInternalError;
    public AccountTypeWS[] getAllAccountTypes() throws SessionInternalError;

    /*
        Account Information Types
    */
    public AccountInformationTypeWS[] getInformationTypesForAccountType(Integer accountTypeId);
    public Integer createAccountInformationType(AccountInformationTypeWS accountInformationType);
    public void updateAccountInformationType(AccountInformationTypeWS accountInformationType);
    public boolean deleteAccountInformationType(Integer accountInformationTypeId);
    public AccountInformationTypeWS getAccountInformationType(Integer accountInformationType);
   
    public OrderWS[] getLinkedOrders(Integer primaryOrderId) throws SessionInternalError;
    public Integer createOrderPeriod(OrderPeriodWS orderPeriod) throws SessionInternalError;

    /*
        Invoices
     */

    public InvoiceWS getInvoiceWS(Integer invoiceId) throws SessionInternalError;
    public Integer[] createInvoice(Integer userId, boolean onlyRecurring);
    public Integer[] createInvoiceWithDate(Integer userId, Date billingDate, Integer dueDatePeriodId, Integer dueDatePeriodValue, boolean onlyRecurring);
    public Integer createInvoiceFromOrder(Integer orderId, Integer invoiceId) throws SessionInternalError;
    public Integer applyOrderToInvoice(Integer orderId, InvoiceWS invoiceWs);
    public void deleteInvoice(Integer invoiceId);
    public Integer saveLegacyInvoice(InvoiceWS invoiceWS);
    public Integer saveLegacyPayment(PaymentWS paymentWS);
    public Integer saveLegacyOrder(OrderWS orderWS);

    public InvoiceWS[] getAllInvoicesForUser(Integer userId);
    public Integer[] getAllInvoices(Integer userId);
    public InvoiceWS getLatestInvoice(Integer userId) throws SessionInternalError;
    public Integer[] getLastInvoices(Integer userId, Integer number) throws SessionInternalError;

    public Integer[] getInvoicesByDate(String since, String until) throws SessionInternalError;
    public Integer[] getUserInvoicesByDate(Integer userId, String since, String until) throws SessionInternalError;
    public Integer[] getUnpaidInvoices(Integer userId) throws SessionInternalError;
    public InvoiceWS[] getUserInvoicesPage(Integer userId, Integer limit, Integer offset) throws SessionInternalError;

    public byte[] getPaperInvoicePDF(Integer invoiceId) throws SessionInternalError;
    public boolean notifyInvoiceByEmail(Integer invoiceId);
    public boolean notifyPaymentByEmail(Integer paymentId);

    /*
        Payments
     */

    public PaymentWS getPayment(Integer paymentId) throws SessionInternalError;
    public PaymentWS getLatestPayment(Integer userId) throws SessionInternalError;
    public Integer[] getLastPayments(Integer userId, Integer number) throws SessionInternalError;

    public Integer[] getLastPaymentsPage(Integer userId, Integer limit, Integer offset) throws SessionInternalError;
    public Integer[] getPaymentsByDate(Integer userId, Date since, Date until) throws SessionInternalError;

    public BigDecimal getTotalRevenueByUser (Integer userId) throws SessionInternalError;

    public PaymentWS getUserPaymentInstrument(Integer userId) throws SessionInternalError;
    public PaymentWS[] getUserPaymentsPage(Integer userId, Integer limit, Integer offset) throws SessionInternalError;

    public Integer createPayment(PaymentWS payment);
    public void updatePayment(PaymentWS payment);
    public void deletePayment(Integer paymentId);

    public void removePaymentLink(Integer invoiceId, Integer paymentId) throws SessionInternalError;
    public void createPaymentLink(Integer invoiceId, Integer paymentId);
    public void removeAllPaymentLinks(Integer paymentId) throws SessionInternalError;

    public PaymentAuthorizationDTOEx payInvoice(Integer invoiceId) throws SessionInternalError;
    public Integer applyPayment(PaymentWS payment, Integer invoiceId) throws SessionInternalError;
    public PaymentAuthorizationDTOEx processPayment(PaymentWS payment, Integer invoiceId);

//    public CardValidationWS validateCreditCard(com.sapienter.jbilling.server.entity.CreditCardDTO creditCard, ContactWS contact, int level);

    public PaymentAuthorizationDTOEx[] processPayments(PaymentWS[] payments, Integer invoiceId);

    public Integer[] createPayments(PaymentWS[] payment);
    
    /*
        Billing process
     */

    public boolean isBillingRunning(Integer entityId);
    public ProcessStatusWS getBillingProcessStatus();
    public void triggerBillingAsync(final Date runDate);
    public boolean triggerBilling(Date runDate);

    public void triggerAgeing(Date runDate);
    public void triggerCollectionsAsync (final Date runDate);
    public boolean isAgeingProcessRunning();
    public ProcessStatusWS getAgeingProcessStatus();

    public BillingProcessConfigurationWS getBillingProcessConfiguration() throws SessionInternalError;
    public Integer createUpdateBillingProcessConfiguration(BillingProcessConfigurationWS ws) throws SessionInternalError;

    public Integer createUpdateCommissionProcessConfiguration(CommissionProcessConfigurationWS ws) throws SessionInternalError;
    public void calculatePartnerCommissions() throws SessionInternalError;
    public void calculatePartnerCommissionsAsync() throws SessionInternalError;
    public boolean isPartnerCommissionRunning();
    public CommissionProcessRunWS[] getAllCommissionRuns() throws SessionInternalError;
    public CommissionWS[] getCommissionsByProcessRunId(Integer processRunId) throws SessionInternalError;

    public BillingProcessWS getBillingProcess(Integer processId);
    public Integer getLastBillingProcess() throws SessionInternalError;
    
    public OrderProcessWS[] getOrderProcesses(Integer orderId);
    public OrderProcessWS[] getOrderProcessesByInvoice(Integer invoiceId);

    public BillingProcessWS getReviewBillingProcess();
    public BillingProcessConfigurationWS setReviewApproval(Boolean flag) throws SessionInternalError;

    public Integer[] getBillingProcessGeneratedInvoices(Integer processId);

    public AgeingWS[] getAgeingConfiguration(Integer languageId) throws SessionInternalError;
    public void saveAgeingConfiguration(AgeingWS[] steps, Integer languageId) throws SessionInternalError;

    /*
    Mediation process
	 */
	
	public void triggerMediation();
	public Integer triggerMediationByConfiguration(Integer cfgId);
	public boolean isMediationProcessRunning();
	public ProcessStatusWS getMediationProcessStatus();
	
	public MediationProcessWS getMediationProcess(Integer mediationProcessId);
	public List<MediationProcessWS> getAllMediationProcesses();
	public List<MediationRecordLineWS> getMediationEventsForOrder(Integer orderId);
	public List<MediationRecordLineWS> getMediationEventsForInvoice(Integer invoiceId);
	public List<MediationRecordWS> getMediationRecordsByMediationProcess(Integer mediationProcessId);
	public List<RecordCountWS> getNumberOfMediationRecordsByStatuses();
	
	public List<MediationConfigurationWS> getAllMediationConfigurations();
	public void createMediationConfiguration(MediationConfigurationWS cfg);
	public List<Integer> updateAllMediationConfigurations(List<MediationConfigurationWS> configurations) throws SessionInternalError;
	public void deleteMediationConfiguration(Integer cfgId);    
    
    /*
        Preferences
     */

    public void updatePreferences(PreferenceWS[] prefList);
    public void updatePreference(PreferenceWS preference);
    public PreferenceWS getPreference(Integer preferenceTypeId);

    /*
        Currencies
     */

    public CurrencyWS[] getCurrencies();
    public void updateCurrencies(CurrencyWS[] currencies);
    public void updateCurrency(CurrencyWS currency);
    public Integer createCurrency(CurrencyWS currency);
    public boolean deleteCurrency(Integer currencyId);

    public CompanyWS getCompany();
    public void updateCompany(CompanyWS companyWS);
    
    /*
        Notifications
    */

    public void createUpdateNotification(Integer messageId, MessageDTO dto);


    /*
        Plug-ins
     */

    public PluggableTaskWS getPluginWS(Integer pluginId);
    public Integer createPlugin(PluggableTaskWS plugin);
    public void updatePlugin(PluggableTaskWS plugin);
    public void deletePlugin(Integer plugin);

	/*
	 * Quartz jobs
	 */
	public void rescheduleScheduledPlugin(Integer pluginId);
    public void unscheduleScheduledPlugin(Integer pluginId);

    public Usage getItemUsage(Integer excludedOrderId, Integer itemId, Integer owner, List<Integer> userIds , Date startDate, Date endDate);

    public void createCustomerNote(CustomerNoteWS note);
    /*
     * Assets
     */

    public Integer createAsset(AssetWS asset) throws SessionInternalError ;
    public void updateAsset(AssetWS asset) throws SessionInternalError ;
    public AssetWS getAsset(Integer assetId);
    public AssetWS getAssetByIdentifier(String assetIdentifier);
    public void deleteAsset(Integer assetId) throws SessionInternalError ;
    public Integer[] getAssetsForCategory(Integer categoryId);
    public Integer[] getAssetsForItem(Integer itemId) ;
    public AssetTransitionDTOEx[] getAssetTransitions(Integer assetId);
    public Long startImportAssetJob(int itemId, String identifierColumnName, String notesColumnName,String globalColumnName,String entitiesColumnName, String sourceFilePath, String errorFilePath) throws SessionInternalError;
    public AssetSearchResult findAssets(int productId, SearchCriteria criteria) throws SessionInternalError ;
    public AssetWS[] findAssetsByProductCode(String productCode) throws SessionInternalError ;
    public AssetStatusDTOEx[] findAssetStatuses(String identifier) throws SessionInternalError ;
    public AssetWS findAssetByProductCodeAndIdentifier(String productCode, String identifier) throws SessionInternalError ;
    public AssetWS[] findAssetsByProductCodeAndStatus(String productCode, Integer assetStatusId) throws SessionInternalError ;

    public Integer reserveAsset(Integer assetId, Integer userId);
    public void releaseAsset(Integer assetId, Integer userId);

	public AssetAssignmentWS[] getAssetAssignmentsForAsset(Integer assetId);
	public AssetAssignmentWS[] getAssetAssignmentsForOrder(Integer orderId);
	public Integer findOrderForAsset(Integer assetId, Date date);
	public Integer[] findOrdersForAssetAndDateRange(Integer assetId, Date startDate, Date endDate);
    public List<AssetWS> getAssetsByUserId(Integer userId);

    /*
     *  MetaField Group
     */
    
    public Integer createMetaFieldGroup(MetaFieldGroupWS metafieldGroup);
	public void updateMetaFieldGroup(MetaFieldGroupWS metafieldGroupWs);
	public void deleteMetaFieldGroup(Integer metafieldGroupId);
	public MetaFieldGroupWS getMetaFieldGroup(Integer metafieldGroupId);
    public MetaFieldGroupWS[] getMetaFieldGroupsForEntity(String entityType);

	public Integer createMetaField(MetaFieldWS metafield);
	public void updateMetaField(MetaFieldWS metafieldWs);
	public void deleteMetaField(Integer metafieldId);
	public MetaFieldWS getMetaField(Integer metafieldId);
    public MetaFieldWS[] getMetaFieldsForEntity(String entityType);

    /*
        Discounts
     */
    public Integer createOrUpdateDiscount(DiscountWS discount);
    public DiscountWS getDiscountWS(Integer discountId);
    public void deleteDiscount(Integer discountId);

    /*
     * OrderChangeStatus
     */
    public OrderChangeStatusWS[] getOrderChangeStatusesForCompany();
    public Integer createOrderChangeStatus(OrderChangeStatusWS orderChangeStatusWS) throws SessionInternalError;
    public void updateOrderChangeStatus(OrderChangeStatusWS orderChangeStatusWS) throws SessionInternalError;
    public void deleteOrderChangeStatus(Integer id) throws SessionInternalError;
    public void saveOrderChangeStatuses(OrderChangeStatusWS[] orderChangeStatuses) throws SessionInternalError;

    /*
     * OrderChangeType
     */
    public OrderChangeTypeWS[] getOrderChangeTypesForCompany();
    public OrderChangeTypeWS getOrderChangeTypeByName(String name);
    public OrderChangeTypeWS getOrderChangeTypeById(Integer orderChangeTypeId);
    public Integer createUpdateOrderChangeType(OrderChangeTypeWS orderChangeTypeWS);
    public void deleteOrderChangeType(Integer orderChangeTypeId);

    /*
     * OrderChange
     */
    public OrderChangeWS[] getOrderChanges(Integer orderId);
    
    /*
     *Payment Method 
     */
    public PaymentMethodTemplateWS getPaymentMethodTemplate(Integer templateId);
    
    public Integer createPaymentMethodType(PaymentMethodTypeWS paymentMethod);
    public void updatePaymentMethodType(PaymentMethodTypeWS paymentMethod);
    public boolean deletePaymentMethodType(Integer paymentMethodTypeId);
    public PaymentMethodTypeWS getPaymentMethodType(Integer paymentMethodTypeId);
    
    public boolean removePaymentInstrument(Integer instrumentId);
	
    /*
     *  Order status
     */
    
    public Integer createUpdateOrderStatus(OrderStatusWS newOrderStatus) throws SessionInternalError;
    public void deleteOrderStatus(OrderStatusWS orderStatus);
    public OrderStatusWS findOrderStatusById(Integer orderStatusId);
    public int getDefaultOrderStatusId(OrderStatusFlag flag, Integer entityId);
    
    /*
     * Plugin
     */
    
    public PluggableTaskTypeWS getPluginTypeWS(Integer id);
    public PluggableTaskTypeWS getPluginTypeWSByClassName(String className);
    public PluggableTaskTypeCategoryWS getPluginTypeCategory(Integer id);
    public PluggableTaskTypeCategoryWS getPluginTypeCategoryByInterfaceName(String interfaceName);
    
    /*
     * Subscription category
     */
    
    public Integer[] createSubscriptionAccountAndOrder(Integer parentAccountId, OrderWS order, boolean createInvoice, List<OrderChangeWS> orderChanges);

    /* Language */
    public Integer createOrEditLanguage(LanguageWS languageWS);

    /*
     * Enumerations
     */
    public EnumerationWS getEnumeration(Integer enumerationId);
    public EnumerationWS getEnumerationByName(String name);
    public List<EnumerationWS> getAllEnumerations(Integer max, Integer offset);
    public Long getAllEnumerationsCount();
    public Integer createUpdateEnumeration(EnumerationWS enumerationWS) throws SessionInternalError;
    public boolean deleteEnumeration(Integer enumerationId) throws SessionInternalError;

}
