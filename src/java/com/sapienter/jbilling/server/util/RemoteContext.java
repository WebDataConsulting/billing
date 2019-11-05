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

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Static factory for accessing remote Spring beans.
 */
public class RemoteContext {

    // spring application context for remote beans
    private static final ApplicationContext spring
            = new ClassPathXmlApplicationContext( new String[] {"/jbilling-remote-beans.xml"});

    // defined bean names
    public enum Name {
        API_CLIENT                  ("apiClient"),
        API_CLIENT_SOAP				("apiClient2"),
        API_CHILD_CLIENT			("apiClient4"),
        API_CLIENT_MORDOR			("apiClientMordor");

        private String name;
        Name(String name) { this.name = name; }
        public String getName() { return name; }
    }

    // static factory cannot be instantiated
    private RemoteContext() {
    }

    public static ApplicationContext getApplicationContext() {
        return spring;
    }

    /**
     * Returns a Spring Bean of type T for the given RemoteContext.Name
     *
     * @param bean remote context name
     * @param <T> bean type
     * @return bean from remote context
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBean(Name bean) {
        return (T) getApplicationContext().getBean(bean.getName());
    }

    /**
     * Returns a Spring Bean of type T for the given name
     *
     * @param beanName bean name
     * @param <T> bean type
     * @return bean from remote context
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBean(String beanName) {
        return (T)  getApplicationContext().getBean(beanName);
    }
}
