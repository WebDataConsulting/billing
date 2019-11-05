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

package com.sapienter.jbilling.server.user;

import java.util.*;

import javax.naming.NamingException;

import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.util.audit.EventLogger;

import org.apache.commons.lang.StringUtils;


import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.user.contact.db.ContactDAS;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.contact.db.ContactMapDAS;
import com.sapienter.jbilling.server.user.contact.db.ContactMapDTO;
import com.sapienter.jbilling.server.user.event.NewContactEvent;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.db.JbillingTableDAS;

public class ContactBL {
    private static final FormatLogger LOG = new FormatLogger(ContactBL.class);

    // contact types in synch with the table contact_type
    static public final Integer ENTITY = new Integer(1);
    
    // private methods
    private ContactDAS contactDas = null;
    private ContactDTO contact = null;
    private Integer entityId = null;
    private JbillingTableDAS jbDAS = null;
    private EventLogger eLogger = null;
    
    public ContactBL(Integer contactId)
            throws NamingException {
        init();
        contact = contactDas.find(contactId);
    }
    
    public ContactBL() {
        init();
    }
    
    public static final ContactWS getContactWS(ContactDTOEx other) {
    	
    	ContactWS ws = new ContactWS();
        ws.setId(other.getId());
        ws.setOrganizationName(other.getOrganizationName());
        ws.setAddress1(other.getAddress1());
        ws.setAddress2(other.getAddress2());
        ws.setCity(other.getCity());
        ws.setStateProvince(other.getStateProvince());
        ws.setPostalCode(other.getPostalCode());
        ws.setCountryCode(other.getCountryCode());
        ws.setLastName(other.getLastName());
        ws.setFirstName(other.getFirstName());
        ws.setInitial(other.getInitial());
        ws.setTitle(other.getTitle());
        ws.setPhoneCountryCode(null != other.getPhoneCountryCode() ? String.valueOf(other.getPhoneCountryCode()) : null);
        ws.setPhoneAreaCode(null != other.getPhoneAreaCode() ? String.valueOf(other.getPhoneAreaCode()) : null );
        ws.setPhoneNumber(other.getPhoneNumber());
        ws.setFaxCountryCode(other.getFaxCountryCode());
        ws.setFaxAreaCode(other.getFaxAreaCode());
        ws.setFaxNumber(other.getFaxNumber());
        ws.setEmail(other.getEmail());
        ws.setCreateDate(other.getCreateDate());
        ws.setDeleted(other.getDeleted());
        ws.setInclude(other.getInclude() != null && other.getInclude().equals(1) );
        return ws;
    }
    
    
    private void setEntityFromUser(Integer userId) {
        // id the entity
        if (userId != null) {
            try {
                entityId = new UserBL().getEntityId(userId);
            } catch (Exception e) {
                LOG.error("Finding the entity", e);
            }
        }
    }
 
    public void set(Integer userId) {
        contact = contactDas.findContact(userId);
        setEntityFromUser(userId);
    }

    public void setEntity(Integer entityId) {
        this.entityId = entityId;
        contact = contactDas.findEntityContact(entityId);
    }

    public boolean setInvoice(Integer invoiceId) {
        boolean retValue = false;
        contact = contactDas.findInvoiceContact(invoiceId);
        InvoiceBL invoice = new InvoiceBL(invoiceId);
        if (contact == null) {
            set(invoice.getEntity().getBaseUser().getUserId());
        } else {
            entityId = invoice.getEntity().getBaseUser().getCompany().getId();
            retValue = true;
        }
        return retValue;
    }

    /**
     * Rather confusing considering the previous method, but necessary
     * to follow the convention
     * @return
     */
    public ContactDTO getEntity() {
        return contact;
    }
    
    
    public ContactDTOEx getVoidDTO(Integer myEntityId) {
        entityId = myEntityId;
        ContactDTOEx retValue = new ContactDTOEx();
        return retValue;
    }
    
    public ContactDTOEx getDTO() {

        ContactDTOEx retValue =  new ContactDTOEx(
            contact.getId(),
            contact.getOrganizationName(),
            contact.getAddress1(),
            contact.getAddress2(),
            contact.getCity(),
            contact.getStateProvince(),
            contact.getPostalCode(),
            contact.getCountryCode(),
            contact.getLastName(),
            contact.getFirstName(),
            contact.getInitial(),
            contact.getTitle(),
            contact.getPhoneCountryCode(),
            contact.getPhoneAreaCode(),
            contact.getPhoneNumber(),
            contact.getFaxCountryCode(),
            contact.getFaxAreaCode(),
            contact.getFaxNumber(),
            contact.getEmail(),
            contact.getCreateDate(),
            contact.getDeleted(),
            contact.getInclude());
        
        return retValue;
    }
    
    public List<ContactDTOEx> getAll(Integer userId)  {
        List<ContactDTOEx> retValue = new ArrayList<ContactDTOEx>();
        UserBL user = new UserBL(userId);
        entityId = user.getEntityId(userId);
        contact = contactDas.findContact(userId);
        if (contact != null) {
            ContactDTOEx dto = getDTO();
            retValue.add(dto);
        }
        return retValue;
    }

    private void init() {
        contactDas = new ContactDAS();
        jbDAS = (JbillingTableDAS) Context.getBean(Context.Name.JBILLING_TABLE_DAS);
        eLogger = EventLogger.getInstance();
    }
    

    /**
     * Finds what is the next contact type and creates a new
     * contact with it
     * @param dto
     */
    public boolean append(ContactDTOEx dto, Integer userId) 
                throws SessionInternalError {
        UserBL user = new UserBL(userId);
        set(userId);
        if (contact == null) {
            // this one is available
            createForUser(dto, userId, null);
            return true;
        }

        return false;
    }
    
    public Integer createForUser(ContactDTOEx dto, Integer userId, Integer executorUserId)
            throws SessionInternalError {
        try {
            //Null check for contact user_id field BugFix:5498
            if(dto.getUserId()==null){
                dto.setUserId(userId);
                return create(dto, ServerConstants.TABLE_BASE_USER, userId, executorUserId);
            }
            return create(dto, ServerConstants.TABLE_BASE_USER, userId, executorUserId);
        } catch (Exception e) {
            LOG.debug("Error creating contact for user %s", userId);
            throw new SessionInternalError(e);
        }
    }
    
    public Integer createForInvoice(ContactDTOEx dto, Integer invoiceId) {
        return create(dto, ServerConstants.TABLE_INVOICE, invoiceId, null);
    }
    
    /**
     * 
     * @param dto
     * @param table
     * @param foreignId
     * @return
     * @throws NamingException
     */
    public Integer create(ContactDTOEx dto, String table,  
            Integer foreignId, Integer executorUserId) {
        // first thing is to create the map to the user
        ContactMapDTO map = new ContactMapDTO();
        map.setJbillingTable(jbDAS.findByName(table));
        map.setForeignId(foreignId);
        map = new ContactMapDAS().save(map);
        
        // now the contact itself
        dto.setCreateDate(new Date());
        dto.setDeleted(0);
        dto.setVersionNum(0);
        dto.setId(0);

        contact = contactDas.save(new ContactDTO(dto)); // it won't take the Ex
        contact.setContactMap(map);
        map.setContact(contact);
        
        LOG.debug("created %s", contact);

        // do an event if this is a user contact (invoices, companies, have
        // contacts too)
        if (table.equals(ServerConstants.TABLE_BASE_USER)) {
            NewContactEvent event = new NewContactEvent(contact.getUserId(), contact, entityId);
            EventManager.process(event);

            if ( null != executorUserId) {
                eLogger.audit(executorUserId,
                        contact.getUserId(),
                        ServerConstants.TABLE_CONTACT,
                        contact.getId(),
                        EventLogger.MODULE_USER_MAINTENANCE,
                        EventLogger.ROW_CREATED, null, null, null);
            } else {
                eLogger.auditBySystem(entityId,
                                  contact.getUserId(),
                                  ServerConstants.TABLE_CONTACT,
                                  contact.getId(),
                                  EventLogger.MODULE_USER_MAINTENANCE,
                                  EventLogger.ROW_CREATED, null, null, null);
            }
        }

        return contact.getId();
    }
    

    public void updateForUser(ContactDTOEx dto, Integer userId, Integer executorUserId)
            throws SessionInternalError {
        contact = contactDas.findContact(userId);
        if (contact != null) {
            if (entityId == null) {
                setEntityFromUser(userId);
            }
            update(dto, executorUserId);
        } else {
            try {
                createForUser(dto, userId, executorUserId);
            } catch (Exception e1) {
                throw new SessionInternalError(e1);
            }
        } 
    }
    
    private void update(ContactDTOEx dto, Integer executorUserId) {
        contact.setAddress1(dto.getAddress1());
        contact.setAddress2(dto.getAddress2());
        contact.setCity(dto.getCity());
        contact.setCountryCode(dto.getCountryCode());
        contact.setEmail(dto.getEmail());
        contact.setFaxAreaCode(dto.getFaxAreaCode());
        contact.setFaxCountryCode(dto.getFaxCountryCode());
        contact.setFaxNumber(dto.getFaxNumber());
        contact.setFirstName(dto.getFirstName());
        contact.setInitial(dto.getInitial());
        contact.setLastName(dto.getLastName());
        contact.setOrganizationName(dto.getOrganizationName());
        contact.setPhoneAreaCode(dto.getPhoneAreaCode());
        contact.setPhoneCountryCode(dto.getPhoneCountryCode());
        contact.setPhoneNumber(dto.getPhoneNumber());
        contact.setPostalCode(dto.getPostalCode());
        contact.setStateProvince(dto.getStateProvince());
        contact.setTitle(dto.getTitle());
        contact.setInclude(dto.getInclude());

        if (entityId == null) {
            setEntityFromUser(contact.getUserId());
        }

        NewContactEvent event = new NewContactEvent(contact.getUserId(), contact, entityId);
        EventManager.process(event);

        eLogger.auditBySystem(entityId,
                              contact.getUserId(),
                              ServerConstants.TABLE_CONTACT,
                              contact.getId(),
                              EventLogger.MODULE_USER_MAINTENANCE,
                              EventLogger.ROW_UPDATED, null, null, null);
    }

    public void delete() {
        
        if (contact == null) return;
        
        LOG.debug("Deleting contact %s", contact.getId());
        // delete the map first
        new ContactMapDAS().delete(contact.getContactMap());

        // for the logger
        Integer entityId = this.entityId;
        Integer userId = contact.getUserId();
        Integer contactId = contact.getId();

        // the contact goes last
        contactDas.delete(contact);
        contact = null;

        // log event
        eLogger.auditBySystem(entityId,
                              userId,
                              ServerConstants.TABLE_CONTACT,
                              contactId,
                              EventLogger.MODULE_USER_MAINTENANCE,
                              EventLogger.ROW_DELETED, null, null, null);
    }
    
    /**
     * Sets this contact object to that on the parent, taking the children id
     * as a parameter. 
     * @param userId
     */
    public void setFromChild(Integer userId) {
        UserBL customer = new UserBL(userId);
        set(customer.getEntity().getCustomer().getParent().getBaseUser().getUserId());
    }

    public static ContactDTOEx buildFromMetaField(Integer userId, Date effectiveDate) {
        return buildFromMetaField(userId, null, effectiveDate);
    }

    /**
     * Builds a contact object for a user from meta fields. The meta fields used
     * to build a contact object will always belong to one AIT group. If the
     * <code>groupId</code> parameter is null than this method will return
     * contcat object built from first AIT with non null email meta field.
     * If the <code>groupId</code> than this method will return contact object
     * build from the specified AIT group.
     *
     * @param userId - user for which we build contact object from meta fields
     * @param groupId - the designated AIT group from which we want to build contact object, Could be AIT ID in future,
     *                currently system defaults to 'use for notifications ait id'
     * @param effectiveDate	-	Date instance for which ait meta fields will be get
     * @return
     */
    public static ContactDTOEx buildFromMetaField(Integer userId, Integer groupId, Date effectiveDate){
        if(null == userId) {
            throw new IllegalArgumentException("userId argument can not be null");
        }

        UserBL userBl = new UserBL(userId);
        CustomerDTO customer= userBl.getEntity().getCustomer();
        ContactDTOEx contact = new ContactDTOEx();
        
        Integer preferredAITID  = null;
        Integer customerId      = null;
        if ( null != customer ) {
            preferredAITID = customer.getAccountType().getPreferredNotificationAitId();
            customerId     = customer.getId();
        }

        if ( null != preferredAITID && null != customerId) {

            String email = MetaFieldBL.getStringMetaFieldValue(customerId, MetaFieldType.EMAIL, preferredAITID, effectiveDate);
            boolean emailPresent = StringUtils.isNotEmpty(email);

            if ( emailPresent ) {
                
                contact.setEmail(MetaFieldBL.getStringMetaFieldValue(customerId, MetaFieldType.EMAIL, preferredAITID, effectiveDate));

                contact.setOrganizationName(MetaFieldBL.getStringMetaFieldValue(customerId, MetaFieldType.ORGANIZATION, preferredAITID, effectiveDate));
                contact.setAddress1(MetaFieldBL.getStringMetaFieldValue(customerId, MetaFieldType.ADDRESS1, preferredAITID, effectiveDate));
                contact.setAddress2(MetaFieldBL.getStringMetaFieldValue(customerId, MetaFieldType.ADDRESS2, preferredAITID, effectiveDate));
                contact.setCity(MetaFieldBL.getStringMetaFieldValue(customerId, MetaFieldType.CITY, preferredAITID, effectiveDate));
                contact.setStateProvince(MetaFieldBL.getStringMetaFieldValue(customerId, MetaFieldType.STATE_PROVINCE, preferredAITID, effectiveDate));
                contact.setPostalCode(MetaFieldBL.getStringMetaFieldValue(customerId, MetaFieldType.POSTAL_CODE, preferredAITID, effectiveDate));
                contact.setCountryCode(MetaFieldBL.getStringMetaFieldValue(customerId, MetaFieldType.COUNTRY_CODE, preferredAITID, effectiveDate));

                contact.setFirstName(MetaFieldBL.getStringMetaFieldValue(customerId, MetaFieldType.FIRST_NAME, preferredAITID, effectiveDate));
                contact.setLastName(MetaFieldBL.getStringMetaFieldValue(customerId, MetaFieldType.LAST_NAME, preferredAITID, effectiveDate));
                contact.setInitial(MetaFieldBL.getStringMetaFieldValue(customerId, MetaFieldType.INITIAL, preferredAITID, effectiveDate));
                contact.setTitle(MetaFieldBL.getStringMetaFieldValue(customerId, MetaFieldType.TITLE, preferredAITID, effectiveDate));

                contact.setPhoneCountryCode(MetaFieldBL.getIntegerMetaFieldValue(customerId, MetaFieldType.PHONE_COUNTRY_CODE, preferredAITID, effectiveDate));
                contact.setPhoneAreaCode(MetaFieldBL.getIntegerMetaFieldValue(customerId, MetaFieldType.PHONE_AREA_CODE, preferredAITID, effectiveDate));
                contact.setPhoneNumber(MetaFieldBL.getStringMetaFieldValue(customerId, MetaFieldType.PHONE_NUMBER, preferredAITID, effectiveDate));

                contact.setFaxCountryCode(MetaFieldBL.getIntegerMetaFieldValue(customerId, MetaFieldType.FAX_COUNTRY_CODE, preferredAITID, effectiveDate));
                contact.setFaxAreaCode(MetaFieldBL.getIntegerMetaFieldValue(customerId, MetaFieldType.FAX_AREA_CODE, preferredAITID, effectiveDate));
                contact.setFaxNumber(MetaFieldBL.getStringMetaFieldValue(customerId, MetaFieldType.FAX_NUMBER, preferredAITID, effectiveDate));

            }
        }

        return contact;
    }

}
