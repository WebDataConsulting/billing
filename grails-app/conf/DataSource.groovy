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

import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsAnnotationConfiguration

def dbUser = System.getenv("JBILLING_DB_USER") ?: "jbilling"
def dbName = System.getenv("JBILLING_DB_NAME") ?: "jbilling_test"
def dbHost = System.getenv("JBILLING_DB_HOST") ?: "localhost"

dataSource {
    /*
        Database dialects

            org.hibernate.dialect.HSQLDialect
            org.hibernate.dialect.MySQLDialect
            org.hibernate.dialect.Oracle9Dialect
            org.hibernate.dialect.PostgreSQLDialect

*/
    dialect = "org.hibernate.dialect.PostgreSQLDialect"
    driverClassName = "org.postgresql.Driver"
    username = dbUser
    password = ""
    url = "jdbc:postgresql://${dbHost}:5432/${dbName}"

    /*
    dialect = "org.hibernate.dialect.MySQLDialect"
    driverClassName = "com.mysql.jdbc.Driver"
    username = "jbilling"
    password = "jbilling"
    url = "jdbc:mysql://localhost:3306/jbilling_test"
    */

    /*
        Other database configuration settings. Do not change unless you know what you are doing!
        See resources.groovy for additional configuration options
    */
    pooled = true
    configClass = GrailsAnnotationConfiguration.class
}

hibernate {
    cache.use_second_level_cache = true
    cache.use_query_cache = true
    cache.region.factory_class = 'net.sf.ehcache.hibernate.EhCacheRegionFactory'
}

environments {
    development {
        /*hibernate {
            config.location = [
                "file:grails-app/conf/hibernate/hibernate.cfg.xml",
                "file:grails-app/conf/hibernate/hibernate-debug.cfg.xml"]
            cache.use_query_cache = false
        }*/
    }
}
/*
environments {
    development {
        dataSource {
            url = "jdbc:postgresql://localhost:5432/jbilling_test"
        }
    }
    test {
        dataSource {
            url = "jdbc:postgresql://localhost:5432/jbilling_test"
        }
    }
    production {
        dataSource {
            url = "jdbc:postgresql://localhost:5432/jbilling_prod"
            username = "jbilling"
            password = "my-password"

            properties {
               maxActive = -1
               minEvictableIdleTimeMillis=1800000
               timeBetweenEvictionRunsMillis=1800000
               numTestsPerEvictionRun=3
               testOnBorrow=true
               testWhileIdle=true
               testOnReturn=true
               validationQuery="SELECT 1"
            }
        }
    }
}
*/
