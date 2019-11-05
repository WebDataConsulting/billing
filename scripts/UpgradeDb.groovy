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

includeTargets << grailsScript("Init")
includeTargets << new File("${basedir}/scripts/Liquibase.groovy")

target(upgradeDb: "Upgrades database to the latest version") {
    depends(parseArguments, initLiquibase)

    def version = getApplicationMinorVersion(argsMap)

    println "Upgrading database to version ${version}"
    echoDatabaseArgs()

    // changelog files to load
    def upgrade = "descriptors/database/jbilling-upgrade-${version}.xml"

    // run the upgrade scripts
    // by default this will run the upgrade context
    // if the -test argument is given then the test data will be updated
    // if the -client argument is given then the client data will be updated
    def context = "base"
    if (argsMap.test) {
        context = "test"
    } else if (argsMap.client) {
        context = "client"
    } else if (argsMap.demo) {
        context = "demo"
    } else if (argsMap.post_base) {
        context = "post_base"
    }

    println "updating with context = $context"
    ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: context))
}

setDefaultTarget(upgradeDb)
