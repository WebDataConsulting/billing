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
package com.sapienter.jbilling.server.util.db;

import java.util.Collection;

import com.sapienter.jbilling.server.util.ServerConstants;
import org.hibernate.Query;
import com.sapienter.jbilling.server.util.Context;


/**
 * 
 * @author abimael
 *
 */
public class InternationalDescriptionDAS extends AbstractDAS<InternationalDescriptionDTO> {

    private JbillingTableDAS jtDAS; // injected by Spring

    // should only be created from Spring
    protected InternationalDescriptionDAS() {
        super();
    }

    public void setJbDAS(JbillingTableDAS util) {
        this.jtDAS = util;
    }

    public InternationalDescriptionDTO findIt(String table,
            Integer foreignId, String column, Integer language) {

        if (foreignId == null || foreignId == 0) {
            return null;
        }
        
        InternationalDescriptionId idi =
                new InternationalDescriptionId(jtDAS.findByName(table).getId(),
                (foreignId == null) ? 0 : foreignId, column, (language == null) ? 0 : language);

        return find(idi); // this should cache ok
    }
    
    public Collection<InternationalDescriptionDTO> exists(String table, String column, String content, Integer language) {

        jtDAS = (JbillingTableDAS) Context.getBean(Context.Name.JBILLING_TABLE_DAS);
        
        final String QUERY = "SELECT a " +
                "FROM InternationalDescriptionDTO a " +
                "WHERE a.id.tableId = :tableId " +
                "AND a.id.psudoColumn = :psudoColumn " +
                "AND a.id.languageId = :languageId " +
                "AND UPPER(a.content) = UPPER(:content)";

            Query query = getSession().createQuery(QUERY);
            query.setParameter("tableId", jtDAS.findByName(table).getId());
            query.setParameter("psudoColumn", column);
            query.setParameter("languageId", language);
            query.setParameter("content", content);
            return query.list();
    }

    public Collection<InternationalDescriptionDTO> roleExists(String table, String column, String content, Integer language, Integer companyId) {
        jtDAS = (JbillingTableDAS) Context.getBean(Context.Name.JBILLING_TABLE_DAS);
        final String ROLEIDS_QUERY = "SELECT a.id " +
                "FROM RoleDTO a " +
                "WHERE a.company.id = :companyId";
        Query roleIdsQuery = getSession().createQuery(ROLEIDS_QUERY);
        roleIdsQuery.setParameter("companyId", companyId);

        final String QUERY = "SELECT a " +
                "FROM InternationalDescriptionDTO a " +
                "WHERE a.id.tableId = :tableId " +
                "AND a.id.psudoColumn = :psudoColumn " +
                "AND a.id.languageId = :languageId " +
                "AND UPPER(a.content) = UPPER(:content)" +
                "AND a.id.foreignId in (:foreignIds)";

        Query query = getSession().createQuery(QUERY);
        query.setParameter("tableId", jtDAS.findByName(table).getId());
        query.setParameter("psudoColumn", column);
        query.setParameter("languageId", language);
        query.setParameter("content", content);
        query.setParameterList("foreignIds", roleIdsQuery.list());

        return query.list();
    }

    public InternationalDescriptionDTO create(String table, Integer foreignId, String column,
            Integer language, String message) {

        InternationalDescriptionId idi = new InternationalDescriptionId(
                jtDAS.findByName(table).getId(), foreignId, column, language);

        InternationalDescriptionDTO inter = new InternationalDescriptionDTO();
        inter.setId(idi);
        inter.setContent(message);

        return save(inter);

    }

    public Collection<InternationalDescriptionDTO> findByTable_Row(String table, Integer foreignId) {
        final String QUERY = "SELECT a " +
            "FROM InternationalDescriptionDTO a, JbillingTable b " +
            "WHERE a.id.tableId = b.id " +
            "AND b.name = :table " +
            "AND a.id.foreignId = :foreing ";

        Query query = getSession().createQuery(QUERY);
        query.setParameter("table", table);
        query.setParameter("foreing", foreignId);
        return query.list();
    }


    public Collection<InternationalDescriptionDTO> findAll(int tableId,int foreignId,String psudoColumn) {
        final String QUERY = "SELECT a " + "FROM InternationalDescriptionDTO a " + "WHERE a.id.tableId = :tableId "
                + "AND a.id.foreignId = :foreignId " + "AND a.id.psudoColumn = :psudoColumn ";

        Query query = getSession().createQuery(QUERY);
        query.setParameter("tableId", tableId);
        query.setParameter("foreignId", foreignId);
        query.setParameter("psudoColumn", psudoColumn);
        return query.list();
    }


    public void delete(int tableId,int foreignId,String psudoColumn,int languageId) {
        final String QUERY = "DELETE " + "InternationalDescriptionDTO a " + "WHERE a.id.tableId = :tableId "
                + "AND a.id.foreignId = :foreignId " + "AND a.id.psudoColumn = :psudoColumn "
                + "AND a.id.languageId = :languageId ";

        Query query = getSession().createQuery(QUERY);
        query.setParameter("tableId", tableId);
        query.setParameter("foreignId", foreignId);
        query.setParameter("psudoColumn", psudoColumn);
        query.setParameter("languageId", languageId);
        query.executeUpdate();
    }

    public static InternationalDescriptionDAS getInstance() {
        return new InternationalDescriptionDAS();
    }
    
    public Collection<InternationalDescriptionDTO> findOrderPeriodByDescription(String descrption) {

        final String QUERY = "SELECT a " +
                "FROM InternationalDescriptionDTO a, JbillingTable b " +
                "WHERE  b.name = :table " +
                "AND a.content = :description ";

        Query query = getSession().createQuery(QUERY);
        query.setParameter("table", ServerConstants.TABLE_ORDER_PERIOD);
        query.setParameter("description", descrption);
        return query.list();
    }
}
