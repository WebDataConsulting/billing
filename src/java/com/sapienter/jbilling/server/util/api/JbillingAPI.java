package com.sapienter.jbilling.server.util.api;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.jws.WebService;

import com.sapienter.jbilling.server.discount.DiscountWS;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.order.OrderProcessWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentAuthorizationDTOEx;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.BillingProcessWS;
import com.sapienter.jbilling.server.process.ProcessStatusWS;
import com.sapienter.jbilling.server.user.ContactWS;
import com.sapienter.jbilling.server.user.CreateResponseWS;
import com.sapienter.jbilling.server.user.UserTransitionResponseWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.ValidatePurchaseWS;
import com.sapienter.jbilling.server.user.partner.PartnerWS;
import com.sapienter.jbilling.server.util.CurrencyWS;
import com.sapienter.jbilling.server.util.PreferenceWS;
import com.sapienter.jbilling.server.util.search.SearchCriteria;
import com.sapienter.jbilling.server.util.search.SearchResultString;

@WebService(targetNamespace = "http://jbilling/", name = "jBillingSalesforceService")
public interface JbillingAPI {

    Integer createUser (UserWS customer);

    Integer createOrder (OrderWS mainOrder);

    List<OrderWS> getLinkedOrders (Integer orderId);

    void deleteUser (Integer customerId);

    void deleteItem (Integer itemId);

    void deleteOrder (Integer orderId);

    UserWS getUserWS (Integer customerId);

    void updateUser (UserWS customer);

    OrderWS getOrder (Integer orderId);

    void triggerBillingAsync (Date date);

    Object getCallerCompanyId ();

    boolean isBillingRunning (Object callerCompanyId);

    ProcessStatusWS getBillingProcessStatus ();

    InvoiceWS[] getAllInvoicesForUser (Integer userId);

    Integer createItem (ItemDTOEx newItem);

    Integer createOrUpdateDiscount (DiscountWS discountWs);

    DiscountWS getDiscountWS (Integer discountId);

    BillingProcessConfigurationWS getBillingProcessConfiguration ();

    void createUpdateBillingProcessConfiguration (BillingProcessConfigurationWS config);

    void updatePreference (PreferenceWS continuousDate);

    InvoiceWS getInvoiceWS (Integer delegatedInvoiceId);

    OrderWS rateOrder (OrderWS testOrder);

    void deleteDiscount (int id);

    InvoiceWS getLatestInvoice (int i);

    Integer[] getLastInvoices (int i, int j);

    Integer[] getInvoicesByDate (String string, String string2);

    void deleteInvoice (Integer invoiceId);

    Integer[] createInvoice (Integer uSER_ID, boolean b);

    Integer createInvoiceFromOrder (Integer orderId1, Object object);

    Integer[] getUserInvoicesByDate (Integer uSER_ID, String string, String string2);

    byte[] getPaperInvoicePDF (Integer integer);

    void triggerAgeing (Date date);

    PaymentWS getLatestPayment (Integer userId);

    CurrencyWS[] getCurrencies ();

    void updateCurrency (CurrencyWS audCurrency);

    ItemDTOEx getItem (Integer id, int i, Object object);

    void updateItem (ItemDTOEx newItem);

    ItemDTOEx[] getAllItems ();

    ItemTypeWS[] getAllItemCategories ();

    Integer createItemCategory (ItemTypeWS itemType);

    void updateItemCategory (ItemTypeWS itemTypeWS);

    ItemDTOEx[] getItemByCategory (Integer dRINK_ITEM_CATEGORY_ID);

    Integer[] getLastOrders (Integer userId, int i);

    OrderPeriodWS[] getOrderPeriods ();

    Integer createOrderAndInvoice (OrderWS newOrder);

    OrderLineWS getOrderLine (Integer lineId);

    void updateOrderLine (OrderLineWS retOrderLine);

    void updateOrder (OrderWS retOrder);

    OrderWS getLatestOrder (Integer integer);

    Integer[] getOrderByPeriod (Integer integer, Integer integer2);

    PaymentAuthorizationDTOEx createOrderPreAuthorize (OrderWS newOrder);

    PaymentAuthorizationDTOEx payInvoice (Integer id);

    OrderWS getCurrentOrder (Integer uSER_ID, Date date);

    OrderWS updateCurrentOrder (Integer uSER_ID, OrderLineWS[] orderLineWSs, Object object, Date date, String string);

    String isUserSubscribedTo (int i, int j);

    Integer[] getUserItemsByCategory (Integer valueOf, Integer valueOf2);

    PaymentWS getPayment (Integer paymentId);

    void removeAllPaymentLinks (Integer paymentId);

    void deletePayment (Integer paymentId);

    void updatePayment (PaymentWS payment);

    Integer createPayment (PaymentWS payment);

    PaymentAuthorizationDTOEx processPayment (PaymentWS payment, Object object);

    void removePaymentLink (Integer invIds, Integer paymentId);

    Integer applyPayment (PaymentWS payment, Integer integer);

    Integer[] getLastPayments (Integer integer, Integer integer2);

    Boolean triggerBilling (Date date);

    BillingProcessWS getReviewBillingProcess ();

    List<Integer> getBillingProcessGeneratedInvoices (Integer id);

    void setReviewApproval (Boolean false1);

    void deleteCreditCard (int userId);

    boolean isAgeingProcessRunning ();

    ProcessStatusWS getAgeingProcessStatus ();

    Integer createUpdateOrder (OrderWS order);

    Integer[] getAllInvoices (int userId);

    BillingProcessWS getBillingProcess (int i);

    Integer getLastBillingProcess ();

    Integer getUserId (String string);

    List<OrderProcessWS> getOrderProcessesByInvoice (Integer id);

    List<OrderProcessWS> getOrderProcesses (Integer id);

    PluggableTaskWS getPluginWS (Integer orderPeriodPluginId);

    void updatePlugin (PluggableTaskWS plugin);

    Integer[] createInvoiceWithDate (int userId, Date billingDate, int day, int i, boolean b);

    Integer createPlugin (PluggableTaskWS plugin);

    void deletePlugin (Integer taxPluginId);

    OrderWS[] getUserOrdersPage (Integer gandalfUserId, int i, int j);

    InvoiceWS[] getUserInvoicesPage (Integer gandalfUserId, int i, int j);

    PaymentWS[] getUserPaymentsPage (Integer gandalfUserId, int i, int j);

    void triggerPartnerPayoutProcess (Date time);

    PartnerWS getPartner (int i);

    Integer createPartner (UserWS user, PartnerWS partner);

    void deletePartner (Integer partnerId);

    void updatePartner (Object object, PartnerWS partner);

    void processPartnerPayout (Integer id);

    CreateResponseWS create (UserWS retUser, OrderWS newOrder);

    void updateUserContact (int userId, Integer integer, ContactWS contact);

    UserTransitionResponseWS[] getUserTransitions (Object object, Object object2);

    UserTransitionResponseWS[] getUserTransitionsAfterId (Integer myId);

    ValidatePurchaseWS validatePurchase (Integer userId, int lEMONADE_ITEM_ID, Object object);

    ValidatePurchaseWS validateMultiPurchase (Integer myId, Integer[] integers, Object object);

    Object getAutoPaymentType (int userId);

    void setAutoPaymentType (int userId, Integer autoPaymentTypeAch, boolean b);

    boolean userExistsWithName (String string);

    boolean userExistsWithId (int maxValue);

    Integer getUserIdByEmail (String string);

    PreferenceWS getPreference (Integer preferenceForceUniqueEmails);

    Integer createOrderPeriod (OrderPeriodWS orderPeriodWS);
}
