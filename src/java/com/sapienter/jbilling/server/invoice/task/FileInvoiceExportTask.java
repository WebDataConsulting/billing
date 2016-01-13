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


package com.sapienter.jbilling.server.invoice.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.invoice.InvoiceLineComparator;
import com.sapienter.jbilling.server.invoice.NewInvoiceEvent;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.ContactBL;
import com.sapienter.jbilling.server.user.UserBL;
import org.joda.time.format.DateTimeFormat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

;

/**
 *
 * @author emilc
 */
public class FileInvoiceExportTask extends PluggableTask implements IInternalEventsTask {
    
    private static final Class<Event> events[] = new Class[] { NewInvoiceEvent.class };

    private static final FormatLogger LOG = new FormatLogger(FileInvoiceExportTask.class);

    // Required parameters
    private static final ParameterDescription PARAMETER_FILE = new ParameterDescription("file", true, ParameterDescription.Type.STR);

    { 
        descriptions.add(PARAMETER_FILE);
    }
    
    public void process(Event event) throws PluggableTaskException {
        NewInvoiceEvent myEvent = (NewInvoiceEvent) event;
        if (myEvent.getInvoice().getIsReview() != null && myEvent.getInvoice().getIsReview() == 1) {
            return;
        }

        LOG.debug("Exporting invoice %s", myEvent.getInvoice().getId());

        // get filename
        String filename = (String) parameters.get(PARAMETER_FILE.getName());
        if (!(new File(filename)).isAbsolute()) {
            // prepend the default directory if file path is relative
            String defaultDir = Util.getSysProp("base_dir");
            filename = defaultDir + filename;
        }

        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(filename, true));
            List<InvoiceLineDTO> ordInvoiceLines = new ArrayList<InvoiceLineDTO>(myEvent.getInvoice().getInvoiceLines());
            Collections.sort(ordInvoiceLines, new InvoiceLineComparator());
            for (InvoiceLineDTO line : ordInvoiceLines) {
                out.write(composeLine(myEvent.getInvoice(), line, myEvent.getUserId()));
                out.newLine();
            }
            out.close();
        } catch (IOException e) {
            LOG.error("Can not write invoice to export file", e);
            throw new PluggableTaskException("Can not write invoice to export file" + e.getMessage());
        }

    }

    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    private String composeLine(InvoiceDTO invoice, InvoiceLineDTO invoiceLine, Integer userId) {
        StringBuffer line = new StringBuffer();
        ContactBL contact = new ContactBL();
        contact.set(userId);

        // cono                                                         
        line.append("\"").append(emptyIfNull(contact.getEntity().getPostalCode())).append("\"");
        line.append(',');
        // custno
        line.append("\"").append(userId).append("\"");
        line.append(',');
        // naddrcode
        line.append("\"" + "000" + "\"");
        line.append(',');
        // lookupnm
        line.append("\"").append(emptyIfNull(contact.getEntity().getOrganizationName())).append("\"");
        line.append(',');
        // totallineamt
        line.append("\"").append(invoiceLine.getAmount()).append("\"");
        line.append(',');
        // period
        line.append("\"").append(DateTimeFormat.forPattern("yyyyMM").print(invoice.getCreateDatetime().getTime())).append("\"");
        line.append(',');
        // name
        line.append("\"").append(emptyIfNull(contact.getEntity().getOrganizationName())).append("\"");
        line.append(',');
        // deliveryaddr
        line.append("\"").append(emptyIfNull(contact.getEntity().getAddress1())).append("\"");
        line.append(',');
        // city
        line.append("\"").append(emptyIfNull(contact.getEntity().getCity())).append("\"");
        line.append(',');
        // state
        line.append("\"").append(emptyIfNull(contact.getEntity().getStateProvince())).append("\"");
        line.append(',');
        // zip5
        line.append("\"").append(emptyIfNull(contact.getEntity().getPostalCode())).append("\"");
        line.append(',');
        // totdue - round to two decimals
        line.append("\"").append(UserBL.getBalance(userId).round(new MathContext(2))).append("\"");
        line.append(',');
        // qty
        line.append("\"").append(invoiceLine.getQuantity()).append("\"");
        line.append(',');
        // description
        line.append("\"").append(invoiceLine.getDescription()).append("\"");
        line.append(',');
        // invoiceno
        line.append("\"").append(invoice.getNumber()).append("\"");
        line.append(',');
        // custstatus
        line.append("\"" + "TRUE" + "\"");

        LOG.debug("Line to export: %s", line);
        return line.toString();
    }

    private String emptyIfNull(String str) {
        if (str == null) {
            return "";
        } else {
            return str;
        }
    }

}
