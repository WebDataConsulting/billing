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

package com.sapienter.jbilling.server.security;

import java.util.ArrayList;
import java.util.List;

import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDAS;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;

/**
 * The WSSecurityEntityMapper converts WS classes to a simple implementation of WSSecured so that
 * access can be validated.
 *
 * This mapper is intended to be used in situations where the WS class cannot be made to implement the
 * WSSecured interface itself. Generally these are situations where the addition of a userId or entityId
 * field would be impractical or confusing.
 *
 * @see com.sapienter.jbilling.server.security.WSSecurityAdvice
 *
 * @author Brian Cowdery
 * @since 02-11-2010
 */
public class WSSecurityEntityMapper {

    /**
     * Return a WSSecured object mapped from the given entity for validation. This method
     * converts legacy WS classes that cannot be made to implement the WSSecured interface.
     *
     * @param o object to convert
     * @return instance of WSSecured mapped from the given entity, null if entity could not be mapped.
     */
    public static WSSecured getMappedSecuredWS(Object o) {
        if (o instanceof OrderLineWS)
            return fromOrderLineWS((OrderLineWS) o);

        if (o instanceof ItemTypeWS)
            return fromItemTypeWS((ItemTypeWS) o);

        return null;
    }

    private static WSSecured fromOrderLineWS(OrderLineWS orderLine) {
        OrderDTO order = new OrderDAS().find(orderLine.getOrderId());
        return order != null ? new MappedSecuredWS(null, order.getUserId()) : null; // user id
    }

    private static WSSecured fromItemTypeWS(ItemTypeWS type) {
        ItemTypeDTO dto = new ItemTypeDAS().find(type.getId());
        return null; // An Item Type may not be owned by the caller company only. IT is now a shared/shareable entity
    }

}
