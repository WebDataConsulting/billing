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

package com.sapienter.jbilling.server.util.csv;

import au.com.bytecode.opencsv.CSVWriter;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.payment.db.PaymentDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.partner.db.PartnerDTO;
import com.sapienter.jbilling.server.util.converter.BigDecimalConverter;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.List;

/**
 * CsvExporter
 *
 * @author Brian Cowdery
 * @since 03/03/11
 */
public class CsvExporter<T extends Exportable> implements Exporter<T> {

    private static final FormatLogger LOG = new FormatLogger(CsvExporter.class);

    /** The maximum safe number of exportable elements to processes.  */
    public static final Integer MAX_RESULTS = 1000000;

    static {
        ConvertUtils.register(new BigDecimalConverter(), BigDecimal.class);
    }

    private Class<T> type;

    private CsvExporter(Class<T> type) {
        this.type = type;
    }

    /**
     * Factory method to produce a new instance of CsvExporter for the given type.
     *
     * @param type type of exporter
     * @param <T> type T
     * @return new exporter of type T
     */
    public static <T extends Exportable> CsvExporter<T> createExporter(Class<T> type) {
        return new CsvExporter<T>(type);
    }

    public Class<T> getType() {
        return type;
    }

    public String export(List<? extends Exportable> list) {
        String[] header;
        
        T instance = null;
		try {
			instance = type.newInstance();
		} catch (InstantiationException | IllegalAccessException e1) {
			e1.printStackTrace();
		}
        if(instance.getClass() == UserDTO.class) {
        	instance.metaFieldsNames.addAll(MetaFieldBL.getMetaFieldsByType(1,EntityType.ACCOUNT_TYPE));
        	instance.customerMetaFieldsNames.addAll(MetaFieldBL.getMetaFieldsByCustomerType(1,EntityType.CUSTOMER));
        }
        else if(instance.getClass() == ItemDTO.class)      	ItemDTO.metaFieldsNames.addAll(MetaFieldBL.getMetaFieldsByType(1,EntityType.PRODUCT));
        
        else if(instance.getClass() == OrderDTO.class)    	OrderDTO.metaFieldsNames.addAll(MetaFieldBL.getMetaFieldsByType(1,EntityType.ORDER));
        
        else if(instance.getClass() == PartnerDTO.class)  	PartnerDTO.metaFieldsNames.addAll(MetaFieldBL.getMetaFieldsByType(1,EntityType.AGENT));
        
        else if(instance.getClass() == InvoiceDTO.class)  	InvoiceDTO.metaFieldsNames.addAll(MetaFieldBL.getMetaFieldsByType(1,EntityType.INVOICE));
        
        else if(instance.getClass() == PaymentDTO.class)  	InvoiceDTO.metaFieldsNames.addAll(MetaFieldBL.getMetaFieldsByType(1,EntityType.PAYMENT));
        
        
        if(!list.isEmpty()) {
            header = list.get(0).getFieldNames();
        } else {
            // list can be empty, instantiate a new instance of type to
            // extract the field names for the CSV header
            try {
                header = type.newInstance().getFieldNames();
            } catch (InstantiationException e) {
                LOG.debug("Could not produce a new instance of %s to build CSV header.", type.getSimpleName());
                return null;

            } catch (IllegalAccessException e) {
                LOG.debug("Constructor of %s is not accessible to build CSV header.", type.getSimpleName());
                return null;
            }
        }

        StringWriter out = new StringWriter();
        CSVWriter writer = new CSVWriter(out);
        writer.writeNext(header);
        
        for (Exportable exportable : list) {
            for (Object[] values : exportable.getFieldValues()) {
                writer.writeNext(convertToString(values));
            }
        }
        try {
			instance.metaFieldsNames.clear();

            writer.close();
            out.close();
        } catch (IOException e) {
			instance.metaFieldsNames.clear();

            LOG.debug("Writer cannot be closed, exported CSV may be missing data.");
        }
        finally {
			instance.metaFieldsNames.clear();

        }

        return out.toString();
    }

    public String[] convertToString(Object[] objects) {
        String[] strings = new String[objects.length];

        int i = 0;
        for (Object object : objects) {
            if (object != null) {
                Converter converter = ConvertUtils.lookup(object.getClass());
                if (converter != null) {
                    strings[i++] = converter.convert(object.getClass(), object).toString();
                } else {
                    strings[i++] = object.toString();
                }
            } else {
                strings[i++] = "";
            }
        }

        return strings;
    }
   /* 
    public String resolveEntityType(String className) {
    	if(className == null) return null;
    	
    	className = className.substring(0,className.lastIndexOf("DTO"));
    	String entityType = String.valueOf(className.charAt(0));
    	for(int i=1; i< className.length(); i++) {
    		if(className.charAt(i) >= 'A' && className.charAt(i) <= 'Z') {
    			entityType += '_'+className.charAt(i);
    		}
    		else {
    			entityType+= className.charAt(i);
    		}
    	}
    	
    	return entityType;
    }*/
}
