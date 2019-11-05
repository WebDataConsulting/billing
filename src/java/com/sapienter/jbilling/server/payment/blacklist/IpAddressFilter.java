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
package com.sapienter.jbilling.server.payment.blacklist;

import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.blacklist.db.BlacklistDAS;
import com.sapienter.jbilling.server.payment.blacklist.db.BlacklistDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Util;

/**
 * Filters by custom contact field: IP Address.
 */
public class IpAddressFilter implements BlacklistFilter {

    private Integer ipAddressCcf; // custom contact field id for ip address

    public IpAddressFilter(Integer ipAddressCcf) {
        if (ipAddressCcf == null) {
            throw new IllegalArgumentException("IP Address CCF id is null");
        }
        this.ipAddressCcf = ipAddressCcf;
    }

    public Result checkPayment(PaymentDTOEx paymentInfo) {
        return checkUser(paymentInfo.getUserId());
    }

    public Result checkUser(Integer userId) {
        UserDTO user = new UserDAS().find(userId);

        if (user == null || user.getCustomer() == null) {
            return new Result(false, null);
        }

        String ipAddress = null;
        MetaField metaField = new MetaFieldDAS().find(ipAddressCcf);
        if (metaField != null) {
            MetaFieldValue metaFieldValue = user.getCustomer().getMetaField(metaField.getName());
            if (metaFieldValue != null) {
                ipAddress = (String) metaFieldValue.getValue();
            }
        }

        // user has no ip address custom contact field
        if (ipAddress == null) {
            return new Result(false, null);
        }

        Integer entityId = new UserDAS().find(userId).getCompany().getId();
        List<BlacklistDTO> blacklist = new BlacklistDAS().filterByIpAddress(
                entityId, ipAddress, ipAddressCcf);

        if (!blacklist.isEmpty()) {
            ResourceBundle bundle = Util.getEntityNotificationsBundle(userId);
            return new Result(true, 
                    bundle.getString("payment.blacklist.ip_address_filter"));
        }

        return new Result(false, null);
    }

    public String getName() {
        return "IP address blacklist filter";
    }
}
