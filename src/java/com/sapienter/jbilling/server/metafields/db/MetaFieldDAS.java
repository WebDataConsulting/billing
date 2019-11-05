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

package com.sapienter.jbilling.server.metafields.db;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.db.value.IntegerMetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.value.StringMetaFieldValue;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import java.util.*;

/**
 * @author Brian Cowdery
 * @since 03-Oct-2011
 */
public class MetaFieldDAS extends AbstractDAS<MetaField> {
	
	public static final FormatLogger LOG = new FormatLogger(MetaFieldDAS.class);
			
    private static final String findCountByDTypeName =
            "SELECT count(*) " +
                    "  FROM MetaField a " +
                    " WHERE a.dataType = :dataType "+
                    " AND a.name = :name";

    private static final String findAllIdsByDataTypeNameSQL =
            "SELECT id " +
                    "  FROM MetaField a " +
                    " WHERE a.dataType = :dataType "+
                    " AND a.name = :name";
    
    @SuppressWarnings("unchecked")
    public List<MetaField> getAvailableFields(Integer entityId, EntityType[] entityType, Boolean primary) {
        DetachedCriteria query = DetachedCriteria.forClass(MetaField.class);
        query.add(Restrictions.eq("entity.id", entityId));
        query.add(Restrictions.in("entityType", entityType));
        if(null != primary){
            query.add(Restrictions.eq("primary", primary.booleanValue()));
        }
        query.addOrder(Order.asc("displayOrder"));
        List<MetaField> result = null;
        try {
			
        	result= (List<MetaField>)getHibernateTemplate().findByCriteria(query);
		} catch (Exception e) {
			LOG.error(e.getMessage());
			e.printStackTrace();
			LOG.error(e);
		}
		return result;
    }

    @SuppressWarnings("unchecked")
    public MetaField getFieldByName(Integer entityId, EntityType[] entityType, String name) {
        return getFieldByName(entityId, entityType, name, null);
    }
    
    @SuppressWarnings("unchecked")
    public boolean getValueByMetaFieldId(Integer metaFieldId, DataType type, MetaFieldValue value) {
        if (null == type || null == value.getValue() || null == metaFieldId) {
            throw new IllegalArgumentException("arguments type/value/fields can not be null");
        }

        StringBuilder queryBuilder = findMetafieldValueIdsByQueryBuilder(type, value);
        if (null != value.getId()) {
            queryBuilder.append(" and id != :metaFieldValueId");
        }
        queryBuilder.append(" and meta_field_name_id = :metaFieldId");

        SQLQuery query = getSession().createSQLQuery(queryBuilder.toString());
        if (null != value.getId()) {
            query.setInteger("metaFieldValueId", value.getId());
        }
        query.setInteger("metaFieldId", metaFieldId);

        return query.list().size() > 0 ? Boolean.FALSE : Boolean.TRUE;
    }
    
    @SuppressWarnings("unchecked")
    public MetaField getFieldByName(Integer entityId, EntityType[] entityType, String name, Boolean primary) {
        DetachedCriteria query = DetachedCriteria.forClass(MetaField.class);
        query.add(Restrictions.eq("entity.id", entityId));
        query.add(Restrictions.in("entityType", entityType));
        query.add(Restrictions.eq("name", name));

        if(null != primary){
            query.add(Restrictions.eq("primary", primary.booleanValue()));
        }

        List<MetaField> fields = (List<MetaField>)getHibernateTemplate().findByCriteria(query);
        return !fields.isEmpty() ? fields.get(0) : null;
    }

    public MetaField getFieldByNameTypeAndGroup(Integer entityId, EntityType[] entityType, String name, Integer groupId) {
        DetachedCriteria query = DetachedCriteria.forClass(MetaField.class);
        query.add(Restrictions.eq("entity.id", entityId));
        query.add(Restrictions.in("entityType", entityType));
        query.add(Restrictions.eq("name", name));
        query.createAlias("metaFieldGroups", "groups", CriteriaSpecification.LEFT_JOIN);
        query.add(Restrictions.eq("groups.id", groupId));
        query.add(Restrictions.eq("groups.entityType", EntityType.ACCOUNT_TYPE));
        query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);

        List<MetaField> fields = (List<MetaField>)getHibernateTemplate().findByCriteria(query);
        return !fields.isEmpty() ? fields.get(0) : null;
    }

    public MetaField getFieldByNameAndGroup(Integer entityId, String name, Integer groupId) {
        DetachedCriteria query = DetachedCriteria.forClass(MetaField.class);
        query.add(Restrictions.eq("entity.id", entityId));
        query.add(Restrictions.eq("name", name));
        query.createAlias("metaFieldGroups", "groups", CriteriaSpecification.LEFT_JOIN);
        query.add(Restrictions.eq("groups.id", groupId));
        query.add(Restrictions.eq("groups.entityType", EntityType.ACCOUNT_TYPE));
        query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);

        List<MetaField> fields = (List<MetaField>)getHibernateTemplate().findByCriteria(query);
        return !fields.isEmpty() ? fields.get(0) : null;
    }


    public void deleteMetaFieldValuesForEntity(EntityType entityType, int metaFieldId) {
        Session session = getSession();
        List<String> deleteEntitiesList = new ArrayList<String>();

        switch (entityType) {
            case INVOICE:
                deleteEntitiesList.add(" invoice_meta_field_map ");
                break;
            case CUSTOMER:
                deleteEntitiesList.add(" customer_meta_field_map ");
                break;
            case AGENT:
                deleteEntitiesList.add(" partner_meta_field_map ");
                break;
            case ACCOUNT_TYPE:
                deleteEntitiesList.add(" customer_meta_field_map ");
                break;
            case PRODUCT:
                deleteEntitiesList.add(" item_meta_field_map ");
                break;
            case ORDER:
                deleteEntitiesList.add(" order_meta_field_map ");
                break;
            case PAYMENT:
                deleteEntitiesList.add(" payment_meta_field_map ");
                break;
            case ASSET:
                deleteEntitiesList.add(" asset_meta_field_map ");
                break;
            case ORDER_LINE:
                deleteEntitiesList.add(" order_line_meta_field_map ");
                deleteEntitiesList.add(" order_change_meta_field_map ");
                break;
            case DISCOUNT:
            	deleteEntitiesList.add(" discount_meta_field_map ");
            	break;
        }

        String deleteFromSql = "delete from ";
        String deleteWhereSql = " where meta_field_value_id in " +
                "(select val.id from meta_field_value val where meta_field_name_id = :metaFieldId  )";
        for (String deleteSingleEntity : deleteEntitiesList) {
        	
        	StringBuilder sqlBuilder = new StringBuilder();
        	sqlBuilder.append(deleteFromSql).append(deleteSingleEntity).append(deleteWhereSql);
        	session.createSQLQuery(sqlBuilder.toString())
                    .setParameter("metaFieldId", metaFieldId)
                    .executeUpdate();
        }

        String deleteValuesHql = "delete from " + MetaFieldValue.class.getSimpleName() + " where field.id = ?";
        getHibernateTemplate().bulkUpdate(deleteValuesHql, metaFieldId);
    }

    /**
     * Useful to delete meta field values for a given {@link EntityType} entityType and ID id
     * @param id
     * @param entityType
     * @param values
     */
    /*TODO: This method is no longer use in any methods. We may delete it.*/
    public void deleteMetaFieldValues(Integer id, EntityType entityType, List<MetaFieldValue> values) {
        Session session = getSession();
        List<String> deleteEntitiesList = new ArrayList<String>();
        
        String metaFieldValuesToDelete= "delete from meta_field_value where id in (";
        
        StringBuffer csvID= new StringBuffer();
        for(MetaFieldValue value: values) {
            csvID.append(value.getId()).append(',');
        }
        metaFieldValuesToDelete += csvID.substring(0, csvID.length()-1) + ")";
        
        switch (entityType) {
           case INVOICE:
               deleteEntitiesList.add(" invoice_meta_field_map where invoice_id = " + id);
               break;
           case CUSTOMER:
               deleteEntitiesList.add(" customer_meta_field_map where customer_id = " + id);
               break;
            case AGENT:
                deleteEntitiesList.add(" partner_meta_field_map where partner_id = " + id);
                break;
            case ACCOUNT_TYPE:
               deleteEntitiesList.add(" customer_meta_field_map where customer_id = " + id);
               break;
           case PRODUCT:
               deleteEntitiesList.add(" item_meta_field_map where item_id =" + id);
               break;
           case ORDER:
               deleteEntitiesList.add(" order_meta_field_map where order_id = " + id);
               break;
           case PAYMENT:
               deleteEntitiesList.add(" payment_meta_field_map where payment_id = " + id);
               break;
           case ASSET:
               deleteEntitiesList.add(" asset_meta_field_map where asset_id = " + id);
               break;
           case ORDER_LINE:
               deleteEntitiesList.add(" order_line_meta_field_map where asset_id = " + id);
               break;
           case DISCOUNT:
        	   deleteEntitiesList.add(" discount_meta_field_map where discount_id = " + id);
        	   break;
        }
        
        String deleteFromSql = "delete from ";
        for (String deleteSingleEntity : deleteEntitiesList) {
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append(deleteFromSql).append(deleteSingleEntity);
            session.createSQLQuery(sqlBuilder.toString()).executeUpdate();
        }
        session.createSQLQuery(metaFieldValuesToDelete).executeUpdate();
    }    

    public Long getFieldCountByDataTypeAndName(DataType dataType, String name,Integer entityId ){
        Query query;
        if(entityId!=null) {
           query = getSession().createQuery(findCountByDTypeName+" AND a.entity.id = :entityId");
           query.setParameter("entityId",entityId);
       }
        else{
           query = getSession().createQuery(findCountByDTypeName);
        }
        query.setParameter("dataType", dataType);
        query.setParameter("name", name);
        return (Long) query.uniqueResult();
    }

    /**
     * Method to search entities (Customer, Order, Product, Invoice etc) with matching Meta Field values
     * @param metaField
     * @param value - currently supported to search a string value, can be extended for others.
     * @return
     */
    /*TODO: This method is no longer use in application. We may delete this method.*/
    public final List<Integer> findEntitiesByMetaFieldValue(MetaField metaField,
			String value) {
		List<Integer> customizedEntityList = null;
		Session session = getSession();
		try {
			String temp = "select val.id from meta_field_value val where meta_field_name_id="
					+ metaField.getId();
			switch (metaField.getDataType()) {
				case STRING:
					System.out.println("Data type is string.");
					temp += " and string_value= :value";
					break;
			}
			System.out.println("Query is: " + temp);
			List<Integer> values = session.createSQLQuery(temp).setString("value",value).list();

			List<String> queries = new ArrayList<String>();
			if (!values.isEmpty()) {
				for (Integer id : values) {
					switch (metaField.getEntityType()) {
					case INVOICE:
						queries.add("select map.invoice_id from invoice_meta_field_map map, invoice i where map.meta_field_value_id = "
										+ id + " and map.invoice_id = i.id and i.deleted = 0");
						break;
					case CUSTOMER:
						queries.add("select customer_id from customer_meta_field_map where meta_field_value_id = "
										+ id + " and customer_id not in (select c.id from customer c, base_user bu where c.user_id = bu.id and bu.deleted > 0)");
						// queries.add("select partner_id from partner_meta_field_map where meta_field_value_id="
						// + id);
						break;
                    case AGENT:
                        queries.add("select partner_id from partner_meta_field_map where meta_field_value_id = "
                                + id + " and partner_id not in (select p.id from partner p, base_user bu where p.user_id = bu.id and bu.deleted > 0)");
                        break;
					case PRODUCT:
						queries.add("select map.item_id from item_meta_field_map map, item i where map.meta_field_value_id="
										+ id + " i.id = map.item_id and i.deleted = 0");
						break;
					case ORDER:
						queries.add("select map.order_id from order_meta_field_map map, purchase_order po where meta_field_value_id="
										+ id + " po.id = map.order_id and po.deleted = 0");
						break;
					case PAYMENT:
						queries.add("select map.payment_id from payment_meta_field_map map, payment p where meta_field_value_id="
										+ id + " p.id = map.payment_id and p.deleted = 0");
						break;
					}
				}
				customizedEntityList = new ArrayList<Integer>();
				for (String query : queries) {
					customizedEntityList.addAll(session.createSQLQuery(query)
							.list());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			// do something esle?
		}

		return customizedEntityList;
	}
    
    public Long countMetaFieldValuesForEntity(EntityType entityType, int metaFieldId) {
        Session session = getSession();
        Set<String> entityTypes = new HashSet<String>();

        switch (entityType) {
            case INVOICE:
                entityTypes.add(" invoice_meta_field_map ");
                break;
            case CUSTOMER:
                entityTypes.add(" customer_meta_field_map ");
                break;
            case AGENT:
                entityTypes.add(" partner_meta_field_map ");
                break;
            case ACCOUNT_TYPE:
                entityTypes.add(" customer_meta_field_map ");
                break;
            case PRODUCT:
                entityTypes.add(" item_meta_field_map ");
                break;
            case ORDER:
                entityTypes.add(" order_meta_field_map ");
                break;
            case PAYMENT:
                entityTypes.add(" payment_meta_field_map ");
                break;
            case ASSET:
                entityTypes.add(" asset_meta_field_map ");
                break;
            case ORDER_LINE:
                entityTypes.add(" order_line_meta_field_map ");
                break;
            case DISCOUNT:
            	entityTypes.add(" discount_meta_field_map ");
            	break;
            case PAYMENT_METHOD_TYPE:
	        case PAYMENT_METHOD_TEMPLATE:
                entityTypes.add(" payment_information_meta_fields_map ");
                break;
        }

        Long count = 0L;
        String sql;
        String countSql = "select count(*) from ";
        String countWhereSql =
		        " where meta_field_value_id in " +
                "(select val.id " +
		        "   from meta_field_value val " +
		        "       where meta_field_name_id = :metaFieldId " +
                "           and (boolean_value is not null " +
		        "               or date_value is not null " +
		        "               or decimal_value is not null " +
		        "               or integer_value is not null " +
		        "               or (string_value is not null and string_value <> ''))) " +
                "or meta_field_value_id in " +
                "(select distinct val.id " +
		        "   from meta_field_value val join list_meta_field_values lmv on lmv.meta_field_value_id = val.id " +
                "   where val.meta_field_name_id = :metaFieldId " +
                ")";

        for (String entity : entityTypes) {
            sql = countSql+entity+countWhereSql;
            Number temp= (Number) session.createSQLQuery(sql)
                    .setParameter("metaFieldId", metaFieldId)
                    .uniqueResult();
            count  = count + (temp == null ? 0L : temp.longValue());
        }
        
        return count;
    }
    
    public Long getTotalFieldCount(int metaFieldId){
        long totalCount = 0L;
        for(EntityType entityType : EntityType.values()){
            totalCount = totalCount + countMetaFieldValuesForEntity(entityType, metaFieldId);
        }

        return totalCount;
    }

    private static final String findCustomerValuesSQL =
            "select this.id " +
            " from meta_field_value this " +
            " inner join meta_field_name field on this.meta_field_name_id=field.id " +
            " inner join customer_meta_field_map cmap on cmap.meta_field_value_id = this.id" +
            " where field.field_usage = :type " +
            "   and cmap.customer_id = :customer " +
            "   order by field.id asc";

    public List<Integer> getCustomerFieldValues(Integer customerId, MetaFieldType type){
        if(null == customerId || null == type){
            throw new IllegalArgumentException("can have null arguments for customer or type");
        }

        SQLQuery query = getSession().createSQLQuery(findCustomerValuesSQL);
        query.setParameter("type", type.toString());
        query.setParameter("customer", customerId);
        return query.list();
    }


    private static final String findCustomerValuesByGroupSQL =
            "(select this.id " +
                    " from meta_field_value this " +
                    " inner join meta_field_name field on this.meta_field_name_id=field.id " +
                    " inner join metafield_group_meta_field_map mgmfm on field.id = mgmfm.meta_field_value_id " +
                    " inner join customer_meta_field_map cmap on cmap.meta_field_value_id = this.id" +
                    " where field.field_usage = :type " +
                    "   and cmap.customer_id = :customer " +
                    "   and mgmfm.metafield_group_id = :groupId " +
                    "   order by field.id asc)" +
    		"UNION" +
    		"(select this.id " +  
    				"from meta_field_value this " +
    				"inner join meta_field_name field on this.meta_field_name_id=field.id " +
    				"inner join metafield_group_meta_field_map mgmfm on field.id = mgmfm.meta_field_value_id " +
    				"inner join customer_account_info_type_timeline timeline on timeline.meta_field_value_id = this.id " +
    				"where field.field_usage = :type " +
    				"and timeline.customer_id = :customer " +
    				"and mgmfm.metafield_group_id = :groupId " +
    				"and effective_date = (select max(effective_date) from customer_account_info_type_timeline where customer_id = :customer and effective_date <= :startDate) " +
    				"order by field.id asc)";

    public List<Integer> getCustomerFieldValues(Integer customerId, MetaFieldType type, Integer groupId, Date effectiveDate){
        if(null == customerId || null == type || null == groupId){
            throw new IllegalArgumentException("can have null arguments for customer, type or group");
        }

        SQLQuery query = getSession().createSQLQuery(findCustomerValuesByGroupSQL);
        query.setParameter("type", type.toString());
        query.setParameter("customer", customerId);
        query.setDate("startDate", effectiveDate);
        query.setParameter("groupId", groupId);
        return query.list();
    }
    
    /**
     * Returns All IDs with matching criteria
     * @param dataType
     * @param name
     * @return
     */
    public List<Integer> getAllIdsByDataTypeAndName(DataType dataType, String name){
        Query query = getSession().createQuery(findAllIdsByDataTypeNameSQL);
        query.setParameter("dataType", dataType);
        query.setParameter("name", name);
        return   query.list();
    }

    public MetaFieldValue getStringMetaFieldValue(Integer valueId){
        Criteria criteria = getSession().createCriteria(StringMetaFieldValue.class);
        criteria.add(Restrictions.eq("id", valueId));
        return (MetaFieldValue) criteria.uniqueResult();
    }

    public MetaFieldValue getIntegerMetaFieldValue(Integer valueId){
        Criteria criteria = getSession().createCriteria(IntegerMetaFieldValue.class);
        criteria.add(Restrictions.eq("id", valueId));
        return (MetaFieldValue) criteria.uniqueResult();
    }

    public static final String getByFieldTypes =
    		 "select mf.id " +
             " from meta_field_name mf" +
             " where mf.field_usage in (:types) " +
             " and mf.entity_id = :entity ";

    @SuppressWarnings("unchecked")
	public List<Integer> getByFieldType(Integer entityId, MetaFieldType[] types){
        if(null == entityId || null == types || types.length == 0){
            throw new IllegalArgumentException("entity and types must be defined");
        }
        String strTypes[] = toStringArray(types);
        SQLQuery query = getSession().createSQLQuery(getByFieldTypes);
        query.setParameter("entity", entityId);
        query.setParameterList("types", strTypes);
        return query.list();
    }

    private String[] toStringArray(MetaFieldType[] types){
        String result[] = new String[types.length];
        for(int i=0; i< types.length; i++){
            result[i] = types[i].toString();
        }
        return result;
    }


    public List<Integer> findByValue(MetaField field, Object value, Boolean sensitive){
        if(null == field || null == value){
            throw new IllegalArgumentException("arguments field and/or value can not be null");
        }

        StringBuilder queryBuilder = getFindByValueQueryBuilder(field.getDataType(), value, sensitive);
        SQLQuery query = getSession().createSQLQuery(queryBuilder.toString());
        return query.list();
    }

    public List<Integer> findByValueAndField(DataType type, Object value, Boolean sensitive, List<Integer> fields){
        if(null == type || null == value || null == fields){
            throw new IllegalArgumentException("arguments type/value/fields can not be null");
        }

        StringBuilder queryBuilder = getFindByValueQueryBuilder(type, value, sensitive);
        queryBuilder.append(" and meta_field_name_id in (:fields)");

        SQLQuery query = getSession().createSQLQuery(queryBuilder.toString());
        query.setParameterList("fields", fields);
        return query.list();
    }

    
    private StringBuilder findMetafieldValueIdsByQueryBuilder(DataType type, MetaFieldValue value) {
    	
        StringBuilder queryBuilder = new StringBuilder(
                "select mfv.id from meta_field_value mfv where ");
        
	    switch(type) {
	    	case STRING:
	    		queryBuilder.append("mfv.string_value = '").append(value.getValue()).append("' ");
	    		break;
	    	case BOOLEAN:
	    		queryBuilder.append("mfv.boolean_value = '").append(value.getValue()).append("' ");
	    		break;
	    	case DATE:
	    		queryBuilder.append("mfv.date_value = '").append(value.getValue()).append("' ");
	    		break;
	    	case INTEGER:
	    		queryBuilder.append("mfv.integer_value = '").append(value.getValue()).append("' ");
	    		break;
	    	case DECIMAL:
	    		queryBuilder.append("mfv.decimal_value = '").append(value.getValue()).append("' ");
	    		break;
	    }
           
        return queryBuilder;
    }

    private StringBuilder getFindByValueQueryBuilder(DataType type, Object value, Boolean sensitive){
        StringBuilder queryBuilder = new StringBuilder(
                "select mfv.id from meta_field_value mfv where ");

        if(type.equals(DataType.STRING)){
            if(null == sensitive || sensitive.booleanValue()){
                queryBuilder.append("mfv.string_value = '").append((String) value).append("' ");
            } else {
                queryBuilder.append("lower(mfv.string_value) = '").append(((String) value).toLowerCase()).append("' ");
            }
        }
        
        return queryBuilder;
    }

    private static final String getValuesByCustomerFields =
            "(select mv.id " +
                    " from meta_field_value mv, customer_meta_field_map cmfm " +
                    " where cmfm.customer_id = :customer " +
                    "   and cmfm.meta_field_value_id = mv.id" +
                    "   and mv.meta_field_name_id in (:fields))" +
              " UNION " +
              "(select mv.id " +
                     " from meta_field_value mv, customer_account_info_type_timeline cmfm " +                   
                     " where cmfm.customer_id = :customer " +
                     "  and cmfm.meta_field_value_id = mv.id " +
                     "  and mv.meta_field_name_id in (:fields) " +
                     "  and cmfm.effective_date = (select max(effective_date) from customer_account_info_type_timeline where customer_id = :customer and effective_date <= :startDate))";

    public List<Integer> getValuesByCustomerAndFields(Integer customerId, List<Integer> fields) {
        if (null == customerId || null == fields || fields.size() == 0) {
            throw new IllegalArgumentException(" customer and fields can not be null");
        }

        SQLQuery query = getSession().createSQLQuery(getValuesByCustomerFields);
        query.setParameter("customer", customerId);
        query.setParameterList("fields", fields);
        
        query.setDate("startDate", new Date());
        

        return query.list();
    }
    
    public List<String> getMetaFieldsByType(Integer entityId, EntityType type) {
        if (null == entityId || null == type ) {
            throw new IllegalArgumentException(" entity and entity type can not be null");
        }

        SQLQuery query = getSession().createSQLQuery("select name from meta_field_name where entity_id =:entity and entity_type=:type group by name");
        query.setParameter("entity", entityId);
        query.setParameter("type", type.toString());
        

        return query.list();
    }
    
    public List<String> getMetaFieldsByCustomerype(Integer entityId, EntityType type) {
        if (null == entityId || null == type ) {
            throw new IllegalArgumentException(" entity and entity type can not be null");
        }

        SQLQuery query = getSession().createSQLQuery("select name from meta_field_name where entity_id =:entity and entity_type=:type");
        query.setParameter("entity", entityId);
        query.setParameter("type", type.toString());
        

        return query.list();
    }
    
    
    


}
