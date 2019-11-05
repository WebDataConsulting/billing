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
package com.sapienter.jbilling.server.payment.event;



import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.pluggableTask.ProcessorAlarm;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.EventProcessor;
import com.sapienter.jbilling.server.util.ServerConstants;

public class GatewayAlarmEventProcessor extends EventProcessor<ProcessorAlarm> {
    private static final FormatLogger LOG = new FormatLogger(GatewayAlarmEventProcessor.class);
    
    @Override
    public void process(Event event) {
        if (false == event instanceof AbstractPaymentEvent){
            return;
        }
        
        // the alarm does not care about entered payments. Filter them out
        if (event instanceof PaymentSuccessfulEvent) {
            PaymentSuccessfulEvent success = (PaymentSuccessfulEvent) event;
            if (new Integer(success.getPayment().getPaymentResult().getId()).equals(ServerConstants.RESULT_ENTERED)) {
                return;
            }
        }
        
        AbstractPaymentEvent paymentEvent = (AbstractPaymentEvent)event;
        ProcessorAlarm alarm = getPluggableTask(event.getEntityId(), 
                ServerConstants.PLUGGABLE_TASK_PROCESSOR_ALARM);
        
        if (alarm == null){
            // it is OK not to have an alarm configured
            LOG.info("Alarm not present for entity %s", event.getEntityId());
            return;
        }
        
        String paymentProcessor = paymentEvent.getPaymentProcessor();
        if (paymentProcessor == null){
            LOG.warn("Payment event without payment processor id : %s", event);
            return;
        }
        alarm.init(paymentProcessor, event.getEntityId());
        if (event instanceof PaymentFailedEvent){
            alarm.fail();
        } else if (event instanceof PaymentProcessorUnavailableEvent){
            alarm.unavailable();
        } else if (event instanceof PaymentSuccessfulEvent){
            alarm.successful();
        }
    }
    
    
}
