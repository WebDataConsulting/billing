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

package com.sapienter.jbilling.server.notification;

import com.sapienter.jbilling.common.CommonConstants;

import java.io.Serializable;
import java.util.*;

public class MessageDTO implements Serializable {
	
    // message type definitions (synch with DB)
    public static final Integer TYPE_INVOICE_EMAIL = new Integer(1);
    public static final Integer TYPE_CLERK_PAYOUT = new Integer(10);
    public static final Integer TYPE_PAYOUT = new Integer(11);
    public static final Integer TYPE_INVOICE_PAPER = new Integer(12);
    public static final Integer TYPE_ORDER_NOTIF = new Integer(13); // take from 13 to 15
    public static final Integer TYPE_PAYMENT = new Integer(16); // 16 & 17
    public static final Integer TYPE_INVOICE_REMINDER = new Integer(18);
    public static final Integer TYPE_CREDIT_CARD = new Integer(19);
    public static final Integer TYPE_FORGETPASSWORD_EMAIL = new Integer(20);
    public static final Integer TYPE_CREDENTIALS_EMAIL = new Integer(21);
    public static final Integer TYPE_PAYMENT_ENTERED = new Integer(22);    
    public static final Integer TYPE_PAYMENT_REFUND = new Integer(23);
    public static final Integer TYPE_BAL_BELOW_THRESHOLD_EMAIL = new Integer(24); //below threshold message

    // below credit limitation 1
    public static final Integer TYPE_BAL_BELOW_CREDIT_LIMIT_1 = new Integer(26);
    // below credit limitation 2
    public static final Integer TYPE_BAL_BELOW_CREDIT_LIMIT_2 = new Integer(27);

    // max length of a line (as defined in DB schema
    public static final Integer LINE_MAX = new Integer(1000);
    // most messages are emails. If they have an attachment the file name is here
    private String attachmentFile = null;
    
    private Integer typeId;
    private Integer languageId;
    private Boolean useFlag;
    private Integer deliveryMethodId;
    /*
     * The parameters to be used to get the replacements in the text
     */
    private HashMap parameters = null;
    // this is the message itself, after being loaded from the DB
    private List content = null;

    private Integer includeAttachment;
    private String attachmentDesign;
    private String attachmentType;

    private Integer notifyAdmin = 0;
    private Integer notifyPartner = 0;
    private Integer notifyParent = 0;
    private Integer notifyAllParents = 0;
    private List<NotificationMediumType> mediumTypes;

    public Integer getIncludeAttachment() {
        return includeAttachment;
    }

    public void setIncludeAttachment(Integer includeAttachment) {
        this.includeAttachment = includeAttachment;
    }

    public String getAttachmentType() {
        return attachmentType;
    }

    public void setAttachmentType(String attachmentType) {
        this.attachmentType = attachmentType;
    }

    public String getAttachmentDesign() {
        return attachmentDesign;
    }

    public void setAttachmentDesign(String attachmentDesign) {
        this.attachmentDesign = attachmentDesign;
    }

    public MessageDTO() {
        parameters = new HashMap();
        content = new Vector();
        deliveryMethodId = CommonConstants.D_METHOD_EMAIL;
    }
    /**
     * @return
     */
    public MessageSection[] getContent() {
        return (MessageSection[]) content.toArray(new MessageSection[0]);
    }
    
    public void setContent(MessageSection[] lines) {
        for (int f = 0; f < lines.length; f++) {
            addSection(lines[f]);
        }
    }

    /**
     * @return
     */
    public HashMap getParameters() {
        return parameters;
    }

    /**
     * @return
     */
    public Integer getTypeId() {
        return typeId;
    }

    /**
     * @param line
     */
    public void addSection(MessageSection line) {
        content.add(line);
    }

    /**
     * @param value
     */
    public void addParameter(String name, Object value) {
        parameters.put(name, value);
    }

    /**
     * @param integer
     */
    public void setTypeId(Integer integer) {
        typeId = integer;
    }

    public boolean validate() {
        if (typeId == null || parameters == null || content == null ||
                content.isEmpty()) {
            return false;
        } else {
            return true;
        }
    }
    /**
     * @return
     */
    public Integer getLanguageId() {
        return languageId;
    }

    /**
     * @param languageId
     */
    public void setLanguageId(Integer languageId) {
        this.languageId = languageId;
    }

    /**
     * @return
     */
    public Integer getDeliveryMethodId() {
        return deliveryMethodId;
    }

    /**
     * @param deliveryMethodId
     */
    public void setDeliveryMethodId(Integer deliveryMethodId) {
        this.deliveryMethodId = deliveryMethodId;
    }

    public Boolean getUseFlag() {
        return useFlag;
    }
    public void setUseFlag(Boolean useFlag) {
        this.useFlag = useFlag;
    }

    public String getAttachmentFile() {
        return attachmentFile;
    }
    public void setAttachmentFile(String attachmentFile) {
        this.attachmentFile = attachmentFile;
    }
    
    public void setContentSize(int i) {
        ((Vector) content).setSize(i);
    }

    public Integer getNotifyAdmin() {
        return this.notifyAdmin;
    }

    public void setNotifyAdmin(Integer notifyAdmin) {
        this.notifyAdmin = notifyAdmin;
    }

    public Integer getNotifyPartner() {
        return this.notifyPartner;
    }

    public void setNotifyPartner(Integer notifyPartner) {
        this.notifyPartner = notifyPartner;
    }

    public Integer getNotifyParent() {
        return this.notifyParent;
    }

    public void setNotifyParent(Integer notifyParent) {
        this.notifyParent = notifyParent;
    }

    public Integer getNotifyAllParents() {
        return this.notifyAllParents;
    }

    public void setNotifyAllParents(Integer notifyAllParents) {
        this.notifyAllParents = notifyAllParents;
    }

    public List<NotificationMediumType> getMediumTypes() {
    	if (mediumTypes == null || mediumTypes.isEmpty()) {
            mediumTypes = new ArrayList<NotificationMediumType>(Arrays.asList(NotificationMediumType.values()));
        }
        return mediumTypes;
    }

    public void setMediumTypes(List<NotificationMediumType> mediumTypes) {
        this.mediumTypes = mediumTypes;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MessageDTO{");
        sb.append("attachmentFile='").append(attachmentFile).append('\'');
        sb.append(", typeId=").append(typeId);
        sb.append(", languageId=").append(languageId);
        sb.append(", useFlag=").append(useFlag);
        sb.append(", deliveryMethodId=").append(deliveryMethodId);
        sb.append(", includeAttachment=").append(includeAttachment);
        sb.append(", attachmentDesign='").append(attachmentDesign).append('\'');
        sb.append(", attachmentType='").append(attachmentType).append('\'');
        sb.append(", notifyAdmin=").append(notifyAdmin);
        sb.append(", notifyPartner=").append(notifyPartner);
        sb.append(", notifyParent=").append(notifyParent);
        sb.append(", notifyAllParents=").append(notifyAllParents);
        sb.append('}');
        return sb.toString();
    }
}
