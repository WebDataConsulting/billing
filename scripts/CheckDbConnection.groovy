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

 import groovy.sql.Sql

includeTargets << grailsScript("_GrailsArgParsing")

target(checkDbConnection: "Checks the connection to the database and prints the errors if there are any.") {
    depends(createConfig)

    String url      = config.dataSource.url
    String driver   = config.dataSource.driverClassName
    String userName = config.dataSource.username
    String password = config.dataSource.password

    try {
        println "Checking the connection to the DB..."
        def sql = Sql.newInstance(url, userName, password, driver)
        println "Connected to the DB successfully!!!"
    } catch (Exception e) {
        System.out.println("An error ocurred while trying to connect to the DB...");
        e.printStackTrace();
    }
}
