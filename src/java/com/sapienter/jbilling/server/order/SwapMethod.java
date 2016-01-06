/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2014] Enterprise jBilling Software Ltd.
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
package com.sapienter.jbilling.server.order;

/**
 * Method of Plan Swap calculation
 *   - DEFAULT swap method will calculate the order changes as the existing plan was removed
 *             and the new plan item was added to the order
 *   - DIFF    swap method will calculate the difference between the existing plan item and
 *             swap plan item and generate order changes only for that difference.
 *
 * @author: Alexander Aksenov
 * @since: 28.02.14
 */
public enum SwapMethod {
    DEFAULT, DIFF
}
