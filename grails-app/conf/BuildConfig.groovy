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

grails.work.dir = "${userHome}/.grails/${grailsVersion}"
grails.project.work.dir = "${grails.work.dir}/projects/${appName}-${appVersion}"

grails.servlet.version             = "3.0"
grails.project.class.dir           = "target/classes"
grails.project.test.class.dir      = "target/test-classes"
grails.project.test.reports.dir    = "target/test-reports"
grails.project.target.level        = 1.8
grails.project.source.level        = 1.8
grails.project.war.file            = "target/${appName}.war"
grails.project.dependency.resolver = "maven" // or ivy

grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
    }

    // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    log "debug"

    // repositories for dependency resolution
    repositories {
        inherits true       // inherit repositories from plugins
        grailsPlugins()
        grailsHome()
        grailsCentral()

        mavenLocal()        // maven repositories
        mavenCentral()
        mavenRepo "http://snapshots.repository.codehaus.org"
        mavenRepo "http://repository.codehaus.org"
        mavenRepo "http://download.java.net/maven/2/"
        mavenRepo "http://repository.jboss.org/nexus/content/groups/public-jboss/"
        mavenRepo "http://jasperreports.sourceforge.net/maven2/"
        mavenRepo "http://jaspersoft.artifactoryonline.com/jaspersoft/third-party-ce-artifacts/"
    }

    dependencies {
        compile('org.springmodules:spring-modules-cache:0.8') {
            transitive = false
        }
        compile('org.osgi:org.osgi.core:4.1.0')
        compile('org.apache.xbean:xbean-spring:3.5') {
            excludes 'commons-logging'
        }

        compile 'org.apache.activemq:activemq-all:5.3.2'
        compile('org.apache.activemq:activemq-pool:5.3.2') {
            excludes 'junit', 'commons-logging', 'log4j'
        }

        compile('org.apache.xmlrpc:xmlrpc-client:3.1') {
            excludes 'junit', 'xml-apis'
        }

        compile('org.apache.geronimo.javamail:geronimo-javamail_1.4_mail:1.8.4')
        compile('org.apache.geronimo.javamail:geronimo-javamail_1.4_provider:1.8.4')
        compile('org.apache.geronimo.specs:geronimo-javamail_1.4_spec:1.7.1')

        compile ('org.drools:drools-core:5.0.1') {
            excludes 'joda-time'
        }
        compile ('org.drools:drools-compiler:5.0.1') {
            excludes 'joda-time', 'core'
        }
        build ('org.drools:drools-decisiontables:5.0.1') {
            excludes 'joda-time'
        }
        build 'org.drools:drools-templates:5.0.1'
        build 'org.drools:drools-ant:5.0.1'
        build 'org.eclipse.jdt:core:3.3.0-v_771'

		runtime 'org.eclipse.jdt.core.compiler:ecj:4.4'
		
        compile 'org.quartz-scheduler:quartz:2.2.1'
        compile 'joda-time:joda-time:2.5'

        compile('net.sf.opencsv:opencsv:2.3') {
            excludes 'junit'
        }

        compile('commons-httpclient:commons-httpclient:3.0.1') {
            excludes 'junit'
        }
        compile 'commons-net:commons-net:3.3'
        compile 'commons-codec:commons-codec:1.9'
        compile 'commons-beanutils:commons-beanutils:1.9.2'

        compile 'org.hibernate:hibernate-validator:5.1.2.Final'
        compile 'javax.validation:validation-api:1.1.0.Final'

        compile 'org.springframework.data:spring-data-hadoop:1.0.2.RELEASE'
        compile('org.apache.hadoop:hadoop-core:1.1.1') {
            excludes 'slf4j', 'hsqldb','servlet-api','servlet-api-2.5'
        }
        compile('org.apache.hbase:hbase:0.94.2') {
            excludes 'slf4j', 'slf4j-log4j12', 'log4j', 'commons-logging', 'jaxb-api', 'stax-api','servlet-api','servlet-api-2.5'
            excludes 'jruby-complete'
            excludes 'slf4j', 'slf4j-log4j12', 'log4j', 'commons-logging', 'jaxb-api', 'stax-api', 'jaxb-impl'
        }

        //needed by the jDiameter library
        runtime 'org.picocontainer:picocontainer:2.13.5'
        runtime 'commons-pool:commons-pool:1.5.6'

        compile 'org.codehaus.jackson:jackson-core-asl:1.9.13'
        compile 'org.codehaus.jackson:jackson-mapper-asl:1.9.13'

        compile 'org.apache.velocity:velocity:1.7'
        compile('org.apache.velocity:velocity-tools:2.0') {
            excludes 'struts-core', 'struts-taglib', 'struts-tiles'
        }

        compile('net.sf.jasperreports:jasperreports:5.6.1') {
            excludes 'jaxen', 'xalan', 'xml-apis', 'jdtcore'
        }

        build 'com.lowagie:itext:2.1.7'
        build 'org.eclipse.jdt.core.compiler:ecj:4.4'

        compile 'net.sf.jasperreports:jasperreports-fonts:5.6.1'
        compile 'org.apache.poi:poi:3.6'

        compile('net.sf.barcode4j:barcode4j:2.1') {
            excludes 'xerces', 'xalan', 'xml-apis'
        }

        compile 'org.liquibase:liquibase-core:3.2.3'
        compile 'com.mchange:c3p0:0.9.2.1'

        compile 'org.springframework:spring-core:4.0.5.RELEASE'
        compile 'org.springframework:spring-beans:4.0.5.RELEASE'
        compile 'org.springframework:spring-jms:4.0.5.RELEASE'
        compile 'org.springframework:spring-webmvc:4.0.5.RELEASE'
        compile 'org.springframework:spring-orm:4.0.5.RELEASE'

        compile 'org.springframework.integration:spring-integration-core:4.0.4.RELEASE'
        compile 'org.springframework.integration:spring-integration-ftp:4.0.4.RELEASE'
        compile 'org.springframework.integration:spring-integration-sftp:4.0.4.RELEASE'
        compile 'org.springframework.integration:spring-integration-file:4.0.4.RELEASE'
        compile 'org.springframework.batch:spring-batch-core:3.0.1.RELEASE'

        runtime 'xerces:xercesImpl:2.11.0'  // for paypal payment

        runtime 'org.postgresql:postgresql:9.3-1102-jdbc41'
        runtime 'mysql:mysql-connector-java:5.1.26'
        runtime 'org.hsqldb:hsqldb:2.3.2'

        compile('org.codehaus.groovy.modules.http-builder:http-builder:0.5.2') {
            excludes "commons-logging", "xml-apis", "groovy"
        }

        compile('com.thoughtworks.xstream:xstream:1.4.4')
        compile("org.mockftpserver:MockFtpServer:2.3")

        // lombok dependency for boiler plate, getter,setter, toString and hashCode method
        compile 'org.projectlombok:lombok:1.12.2'
        compile 'com.lowagie:itext:4.2.1'

        // Test dependencies

        // override junit bundled with grails
        build('junit:junit:4.11') {
            transitive = false
        }
        test ('junit:junit:4.11') {
            transitive = false // excludes "hamcrest-core"
        }
        test    'org.hamcrest:hamcrest-all:1.3'

        test    ('org.testng:testng:6.8.8') {
            transitive = false // excludes "junit", "snakeyaml", "jcommander", "bsh"
        }
        test    ('org.easymock:easymockclassextension:3.2') {
            excludes "cglib-nodep"
        }
        compile 'org.grails:grails-datastore-core:3.1.2.RELEASE'
    }

    plugins {
        build ":tomcat8:8.0.5"

        runtime ":hibernate:3.6.10.14"
        runtime ":jquery:1.11.1"
        runtime ":resources:1.2.8"

        compile ":jquery-ui:1.10.4"
        compile ':webflow:2.1.0'
        compile ":cookie:0.51"
        compile ":cxf:2.0.1"
        compile ":remote-pagination:0.4.8"
        compile ":remoting:1.3"
        compile ":spring-security-core:2.0-RC4"
        runtime ":webxml:1.4.1"
        runtime (':cxf:1.1.1') {
            excludes 'jaxb-impl'
        }
    }
}
