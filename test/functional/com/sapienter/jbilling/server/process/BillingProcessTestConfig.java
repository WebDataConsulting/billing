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

package com.sapienter.jbilling.server.process;

import static com.sapienter.jbilling.test.TestUtils.buildDescriptions;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sapienter.jbilling.server.order.OrderChangeStatusWS;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.ApiTestCase;

@Configuration
public class BillingProcessTestConfig {

    @Autowired
    private JbillingAPI api;

    @Bean
    public OrderPeriodWS daily () {
        return getOrCreateOrderPeriod(ServerConstants.PERIOD_UNIT_DAY, ApiTestCase.TEST_LANGUAGE_ID, "Daily");
    }

    @Bean
    public OrderPeriodWS weekly () {
        return getOrCreateOrderPeriod(ServerConstants.PERIOD_UNIT_WEEK, ApiTestCase.TEST_LANGUAGE_ID, "Weekly");
    }

    @Bean
    public OrderPeriodWS semiMonthly () {
        return getOrCreateOrderPeriod(ServerConstants.PERIOD_UNIT_SEMI_MONTHLY, ApiTestCase.TEST_LANGUAGE_ID, "Semi-Monthly");
    }

    @Bean
    public OrderPeriodWS monthly () {
        return getOrCreateOrderPeriod(ServerConstants.PERIOD_UNIT_MONTH, ApiTestCase.TEST_LANGUAGE_ID, "Monthly");
    }

    @Bean
    public OrderChangeStatusWS applyToOrderYes () {
        for (OrderChangeStatusWS status : api.getOrderChangeStatusesForCompany()) {
            if (ApplyToOrder.YES.equals(status.getApplyToOrder()))
                return status;
        }
        throw new BeanCreationException("applyToOrderYes", "YES status not found in list of statuses for company");
    }

    private OrderPeriodWS getOrCreateOrderPeriod (Integer periodUnit, Integer value, String description) {

        for (OrderPeriodWS period : api.getOrderPeriods()) {
            if (period.getPeriodUnitId().intValue() == periodUnit.intValue()
                    && period.getValue().intValue() == value.intValue()) {
                return period;
            }
        }
        return createOrderPeriod(periodUnit, value, description);
    }

    private OrderPeriodWS createOrderPeriod (Integer periodUnit, Integer value, String description) {

        OrderPeriodWS orderPeriod = new OrderPeriodWS(999, ApiTestCase.TEST_ENTITY_ID, periodUnit, value);
        orderPeriod.setDescriptions(buildDescriptions(new InternationalDescriptionWS(ApiTestCase.TEST_LANGUAGE_ID,
                description)));
        orderPeriod.setId(api.createOrderPeriod(orderPeriod));

        return orderPeriod;
    }

}
