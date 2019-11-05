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

package com.sapienter.jbilling.server.payment.tasks;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.payment.IExternalCreditCardStorage;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskBL;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.ContactBL;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.event.NewContactEvent;
import com.sapienter.jbilling.server.user.event.NewCreditCardEvent;

import java.util.Date;
import java.util.List;

import static com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription.Type.*;

/**
 * Internal Event Plugin designed to save a unique "gateway key" returned by the configured IExternalCreditCardStorage
 * task instead of the complete credit card number.
 *
 * This plugin subscribes to both NewCreditCardEvent and NewContactEvent. For NewContactEvent, this plugin
 * will only invoke the external save logic for new contacts matching the configured "contactType" id.
 */
public class SaveCreditCardExternallyTask extends PluggableTask implements IInternalEventsTask {
    private static final FormatLogger LOG = new FormatLogger(SaveCreditCardExternallyTask.class);

    private static final ParameterDescription PARAM_CONTACT_TYPE = new ParameterDescription("contactType", true, INT);
    private static final ParameterDescription PARAM_EXTERNAL_SAVING_PLUGIN_ID = new ParameterDescription("externalSavingPluginId", true, INT);
    private static final ParameterDescription PARAM_REMOVE_ON_FAIL = new ParameterDescription("removeOnFail", false, BOOLEAN);
    private static final ParameterDescription PARAM_OBSCURE_ON_FAIL = new ParameterDescription("obscureOnFail", false, BOOLEAN);

    //initializer for pluggable params
    {
    	descriptions.add(PARAM_CONTACT_TYPE);
        descriptions.add(PARAM_EXTERNAL_SAVING_PLUGIN_ID);
        descriptions.add(PARAM_OBSCURE_ON_FAIL);
        descriptions.add(PARAM_REMOVE_ON_FAIL);
    }

    private static final boolean DEFAULT_REMOVE_ON_FAIL = false;
    private static final boolean DEFAULT_OBSCURE_ON_FAIL = false;

    private Integer contactType;
    private Integer externalSavingPluginId;

    @SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[] {
            NewCreditCardEvent.class,
            NewContactEvent.class
    };

    public Class<Event>[] getSubscribedEvents() { return events; }

    // fixme: user parameters always come out as strings, int/float only available through db configured plugins

    /**
     * WARN: this parameter was used to compare against the
     * user's contact type. But, since now we do now have contact
     * types we use this parameter to compare against AIT id.
     *
     * Returns the configured contact type as an integer.
     *
     * @return contact type
     * @throws PluggableTaskException if type cannot be converted to an integer
     */
    public Integer getContactType() throws PluggableTaskException {
        if (contactType == null) {
            try {
                if (parameters.get(PARAM_CONTACT_TYPE.getName()) == null) {
                    contactType = -1; // default if not configured
                } else {
                    contactType = Integer.parseInt(parameters.get(PARAM_CONTACT_TYPE.getName()));
                }
            } catch (NumberFormatException e) {
                throw new PluggableTaskException("Configured contactType must be an integer!", e);
            }
        }
        return contactType;
    }

    /**
     * Returns the configured external saving event plugin id ({@link IExternalCreditCardStorage})
     * as an integer.
     *
     * @return plugin id of the configured external saving event plugin
     * @throws PluggableTaskException if id cannot be converted to an integer
     */
    public Integer getExternalSavingPluginId() throws PluggableTaskException {
        if (externalSavingPluginId == null) {
            try {
                externalSavingPluginId = Integer.parseInt(parameters.get(PARAM_EXTERNAL_SAVING_PLUGIN_ID.getName()));
            } catch (NumberFormatException e) {
                throw new PluggableTaskException("Configured externalSavingPluginId must be an integer!", e);
            }
        }
        return externalSavingPluginId;
    }

    /**
     * @see IInternalEventsTask#process(com.sapienter.jbilling.server.system.event.Event)
     *
     * @param event event to process
     * @throws PluggableTaskException
     */
    public void process(Event event) throws PluggableTaskException {
        PluggableTaskBL<IExternalCreditCardStorage> ptbl = new PluggableTaskBL<IExternalCreditCardStorage>(getExternalSavingPluginId());
        IExternalCreditCardStorage externalCCStorage = ptbl.instantiateTask();

        if (event instanceof NewCreditCardEvent) {
            LOG.debug("Processing NewCreditCardEvent ...");
            NewCreditCardEvent ev = (NewCreditCardEvent) event;

            // only save credit cards associated with users
            if (ev.getCreditCard().getUser() != null) {
                String gateWayKey = externalCCStorage.storeCreditCard(null, ev.getCreditCard());
                updateCreditCard(ev.getCreditCard(), gateWayKey);
            } else {
                LOG.debug("Credit card is not associated with a user (card for payment) - can't save through gateway.");
            }

        } else if (event instanceof NewContactEvent) {
            LOG.debug("Processing NewContactEvent ...");
            NewContactEvent ev = (NewContactEvent) event;

            Integer userId = ev.getUserId();
            UserDTO user = new UserDAS().find(userId);

            if(null != user.getCustomer()){
                Integer groupId = ev.getGroupId();
                LOG.debug("Group Id: %s, plug-in expects: %s", groupId, getContactType());

                if ((null == groupId && null != ev.getContactDto()) || (getContactType() == groupId)) {
                    ContactDTO contact = null;

                    if(null != groupId) {
                        contact = ContactBL.buildFromMetaField(userId, groupId, new Date());
                    } else {
                        contact = ev.getContactDto();
                    }

                    UserBL userBl = new UserBL(contact.getUserId());
                    List<PaymentInformationDTO> creditCards = userBl.getAllCreditCards();

                    if (creditCards != null) {
                        // credit card has changed or was not previously obscured
                    	for(PaymentInformationDTO creditCard : creditCards) {
                    		String gateWayKey = externalCCStorage.storeCreditCard(contact, creditCard);
                    		updateCreditCard(creditCard, gateWayKey);
                    	}
                    } else {
                        /*  call the external store without a credit card. It's possible the payment gateway
                            may have some vendor specific recovery facilities, or perhaps they operate on different
                            data? We'll leave it open ended so we don't restrict possible client implementations.
                         */
                        LOG.warn("Cannot determine credit card for storage, invoking external store with contact only");
                        String gateWayKey = externalCCStorage.storeCreditCard(contact, null);
                        updateCreditCard(null, gateWayKey);
                    }
                }
            } else {
                LOG.debug("The user is not customer. We do not store CC for non customer users");
            }
        } else {
            throw new PluggableTaskException("Cant not process event " + event);
        }
    }

    /**
     * Update the credit card object with the given gateway key. If the gateway key is null,
     * handle the external storage as a failure.
     *
     * If PARAM_OBSCURE_ON_FAIL is true, obscure the card number even if gateway key is null.
     * If PARAM_REMOVE_ON_FAIL is true, delete the credit card and remove from the user map if the gateway key is null.
     *
     * @param creditCard credit card to update
     * @param gatewayKey gateway key from external storage, null if storage failed.
     */
    private void updateCreditCard(PaymentInformationDTO creditCard, String gatewayKey) {
        PaymentInformationBL piBl= new PaymentInformationBL();
    	if (gatewayKey != null) {
            LOG.debug("Storing gateway key: %s", gatewayKey);
            piBl.updateStringMetaField(creditCard, gatewayKey, MetaFieldType.GATEWAY_KEY);
            piBl.obscureCreditCardNumber(creditCard);
        } else {

            // obscure credit cards on failure, useful for clients who under no circumstances want a plan-text
            // card to be stored in the jBilling database
            if (getParameter(PARAM_OBSCURE_ON_FAIL.getName(), DEFAULT_OBSCURE_ON_FAIL)) {
                piBl.obscureCreditCardNumber(creditCard);
                LOG.warn("gateway key returned from external store is null, obscuring credit card with no key");
            } else {
                LOG.warn("gateway key returned from external store is null, credit card will not be obscured!");
            }

            // delete the credit card on failure so that it cannot be used for future payments. useful when
            // paired with PARAM_OBSCURE_ON_FAIL as it prevents accidental payments with invalid cards.
            if (getParameter(PARAM_REMOVE_ON_FAIL.getName(), DEFAULT_REMOVE_ON_FAIL)) {
                piBl.delete(creditCard.getId());
                LOG.warn("gateway key returned from external store is null, deleting card and removing from user map");
            }
        }
    }
}
