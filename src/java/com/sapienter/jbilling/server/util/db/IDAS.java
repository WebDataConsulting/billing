/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.util.db;

import java.io.Serializable;
import java.util.List;

/**
 * Author: Emiliano Conde
 * Date: 12-12-26
 * Time: 4:07 PM
 */
@SuppressWarnings("unchecked")
public interface IDAS<T> {

    public T save(T newEntity);
    public void delete(T entity);
    public T find(Serializable id);
    public T findNow(Serializable id);
    public List<T> findAll();
}
