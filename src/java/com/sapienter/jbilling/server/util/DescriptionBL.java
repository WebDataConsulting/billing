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

package com.sapienter.jbilling.server.util;

import com.sapienter.jbilling.server.util.db.InternationalDescriptionDAS;
import com.sapienter.jbilling.server.util.db.InternationalDescriptionDTO;
import com.sapienter.jbilling.server.util.db.LanguageDTO;

import java.util.Collection;
import java.util.List;

import static com.sapienter.jbilling.server.util.ServerConstants.LANGUAGE_ENGLISH_ID;


public class DescriptionBL {
    private InternationalDescriptionDAS descriptionDas;
    
    public DescriptionBL() {
        init(); 
    }
    
    void init()  {
        descriptionDas = Context.getBean(Context.Name.DESCRIPTION_DAS);
    }
    
    public static final  InternationalDescriptionWS getInternationalDescriptionWS(InternationalDescriptionDTO description) {
    	
    	InternationalDescriptionWS ws = new InternationalDescriptionWS();
    	if (description.getId() != null) {
            ws.setPsudoColumn(description.getId().getPsudoColumn());
            ws.setLanguageId(description.getId().getLanguageId());
        }
        ws.setContent(description.getContent());
        return ws;
    }
    
    public void delete(String table, Integer foreignId) {
        Collection toDelete = descriptionDas.findByTable_Row(table, 
                foreignId);
                
        toDelete.clear(); // this would be cool if it worked.
    }

    public static void setDescriptionForMultiLangBeans(List<InternationalDescriptionWS> beanDescriptions,
                                                       String newDescription) {
        for (InternationalDescriptionWS description : beanDescriptions) {
            if (description != null &&
            		LANGUAGE_ENGLISH_ID.intValue() == description.getLanguageId()) {
                description.setContent(newDescription);
                return;
            }
        }
        beanDescriptions.add(
                new InternationalDescriptionWS("description", LANGUAGE_ENGLISH_ID, newDescription));
    }

    public static String getDescriptionForMultiLangBeans(List<InternationalDescriptionWS> descriptions) {
        for (InternationalDescriptionWS description : descriptions) {
            if (description != null &&
            		LANGUAGE_ENGLISH_ID.intValue() == description.getLanguageId()) {
                return description.getContent();
            }
        }
        return "";
    }
    
    public static final LanguageDTO getLanguageDTO(LanguageWS ws){
        LanguageDTO languageDTO= new LanguageDTO();
        languageDTO.setId(ws.getId());
        languageDTO.setDescription(ws.getDescription());
        languageDTO.setCode(ws.getCode());
        return languageDTO;
    }
}
