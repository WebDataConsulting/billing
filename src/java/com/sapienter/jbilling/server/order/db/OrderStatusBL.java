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
package com.sapienter.jbilling.server.order.db;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.list.ResultList;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.OrderStatusWS;
import com.sapienter.jbilling.server.user.EntityBL;

import java.util.ArrayList;
import java.util.List;


/**
 * @author
 */
public class OrderStatusBL extends ResultList {

    private OrderStatusDTO orderStatus;
    private OrderStatusDAS orderStatusDas;
    public OrderStatusBL(Integer orderStatus) {
        init();
        set(orderStatus);
    }

    public OrderStatusBL() {
        init();
    }

    public void set(Integer id) {
        orderStatus = orderStatusDas.find(id);
    }

    public void init() {
        //orderStatus=new OrderStatusDTO();
        orderStatusDas = new OrderStatusDAS();
    }


    public OrderStatusDTO getEntity() {
        return orderStatus;
    }

    public void delete(Integer entityId) {
        OrderStatusDAS orderStatusDas = new OrderStatusDAS();
        try {
            Integer count = orderStatusDas.findByOrderStatusFlag(orderStatus.getOrderStatusFlag(), entityId);
            if (count <= 1) {
                throw new SessionInternalError("There needs to be atleast one status of this type ",
                        new String[]{"OrderStatusWS,statusExist,validation.error.status.should.exists"});
            } else if (count > 1) {
                orderStatusDas.delete(orderStatus);
            }
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }
    
    public static final OrderStatusWS getOrderStatusWS(OrderStatusDTO orderStatus) {
    	OrderStatusWS statusWS = new OrderStatusWS();
    	statusWS.setId(orderStatus.getId());
    	statusWS.setEntity(EntityBL.getCompanyWS(orderStatus.getEntity()));
    	statusWS.setOrderStatusFlag(orderStatus.getOrderStatusFlag());
    	statusWS.setDescription(orderStatus.getDescription());
    	return statusWS;
    }


    public static OrderStatusDTO getDTO(OrderStatusWS orderStatusWS) {
        OrderStatusDTO orderStatusDTO = new OrderStatusDTO();
        if (orderStatusWS.getId() != null)
            orderStatusDTO.setId(orderStatusWS.getId());
        orderStatusDTO.setOrderStatusFlag(orderStatusWS.getOrderStatusFlag());
        orderStatusDTO.setDescription(orderStatusWS.getDescription());
        if (orderStatusWS.getEntity() != null) {
        	 orderStatusDTO.setEntity(EntityBL.getDTO(orderStatusWS.getEntity()));
       }
        return orderStatusDTO;
    }

    public Integer create(OrderStatusWS orderStatusWS, Integer entityId, Integer languageId) throws SessionInternalError

    {
        OrderStatusDTO newOrderStatus = getDTO(orderStatusWS);
        newOrderStatus = new OrderStatusDAS().createOrderStatus(newOrderStatus);
        newOrderStatus.setDescription(orderStatusWS.getDescription(), languageId);
        return newOrderStatus.getId();
    }


    public boolean isOrderStatusValid(OrderStatusWS orderStatusWS, Integer entityId, String name) {

        List<OrderStatusDTO> orderStatusDTOList = new OrderStatusDAS().findAll(entityId);
        List<String> descriptionList = new ArrayList<String>();
        for (OrderStatusDTO orderStatusDTO : orderStatusDTOList) {
            if (orderStatusWS.getOrderStatusFlag() == OrderStatusFlag.FINISHED || orderStatusWS.getOrderStatusFlag() == OrderStatusFlag.SUSPENDED_AGEING) {
                //save
                if (orderStatusWS.getId() == null && orderStatusDTO.getOrderStatusFlag() == orderStatusWS.getOrderStatusFlag()) {
                    return false;
                }
                //update
                else if (orderStatusWS.getId() != null && orderStatusDTO.getOrderStatusFlag() == orderStatusWS.getOrderStatusFlag() && orderStatusDTO.getId() != orderStatusWS.getId()) {
                    return false;
                }
            }
            if (orderStatusWS.getId() != null) {
                if (orderStatusWS.getId() != orderStatusDTO.getId()) {
                    descriptionList.add(orderStatusDTO.getDescription());
                }

            } else {
                descriptionList.add(orderStatusDTO.getDescription());
            }
        }
        if (descriptionList.contains(name)) {
            String[] errmsgs = new String[1];
            errmsgs[0] = "OrderStatusWS,description,OrderStatusWS.error.unique.name";
            throw new SessionInternalError("There is an error in  data.", errmsgs);
        } else {
            return true;
        }

    }
}