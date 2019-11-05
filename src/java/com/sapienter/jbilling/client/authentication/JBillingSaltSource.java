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

package com.sapienter.jbilling.client.authentication;

import org.springframework.security.authentication.dao.SaltSource;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Implementation of {@link org.springframework.security.authentication.dao.SaltSource}
 * that relies on user detail information.
 *
 * @author: Panche.Isajeski
 */
public class JBillingSaltSource implements SaltSource {

    @Override
    public Object getSalt(UserDetails userDetails) {
        return userDetails;
    }
}
