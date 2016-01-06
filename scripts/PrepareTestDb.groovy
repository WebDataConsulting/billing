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

target(cleanDb: "Clean the test postgresql database, will drop/create the database if --hard.") {
    depends(parseArguments, initLiquibase)

    def db = getDatabaseParameters(argsMap)

    // execute the postgresql dropdb command to forcibly drop the database
    // when --drop or --hard
    if (argsMap.drop || argsMap.hard) {
        println "dropping database ${db.database}"
        exec(executable: "dropdb", failonerror: false) {
            arg(line: "-U ${db.username} -e ${db.database}")
        }
    }

    // execute postgresql createdb to create the database
    // when --create or --hard
    if (argsMap.create || argsMap.hard) {
        println "creating database ${db.database}"
        exec(executable: "createdb", failonerror: true) {
            arg(line: "-U ${db.username} -O ${db.username} -E UTF-8 -e ${db.database}")
        }
    }

    // default, just use liquibase to drop all existing objects within the database
    if (!argsMap.drop && !argsMap.create && !argsMap.hard) {
        println "dropping all objects in ${db.database}"
        ant.dropAllDatabaseObjects(liquibaseTaskAttrs())
    }

    /** Delete the ActiveMQ Data folder
        This stores the JMS message queues for payments, and causes
        tests to fail if the queue gains a considerable size */
    println "Cleaning the ActiveMQ data directory..."
    def amqDataDir = new File("")
    if (amqDataDir.exists()) {
        def result = amqDataDir.deleteDir()
        if (!result) {
            throw new RuntimeException("The ActiveMQ directory couldn't be deleted!!")
        }
    }

}

target(prepareTestDb: "Import the test postgresql database.") {
    depends(parseArguments, initLiquibase)

    def version = getApplicationMinorVersion(argsMap)
    println "Loading database version ${version}"
    echoDatabaseArgs()
    //dump liquibase.classpath for closer inspection
    //echo(message: '\${toString:liquibase.classpath}')

    // clean the db
    cleanDb()

    // changelog files to load
    def schema = "descriptors/database/jbilling-schema.xml"
    def init = "descriptors/database/jbilling-init_data.xml"
    def client = "client-data.xml"
    def test = "descriptors/database/jbilling-test_data.xml"

    def versionHierarchy = getApplicationVersionsHierarchy(argsMap)
    versionHierarchy.add(version)
    versionHierarchy.sort()

    // load the jbilling database
    // by default this will load the testing data
    // if the -init argument is given then only the base jbilling data will be loaded
    // if the -client argument is given then the client reference data will be loaded
    if (argsMap.init) {
        println "updating with context = base. Loading init jBilling data"
        ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: schema, contexts: 'base'))
        ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: init))
        ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: schema, contexts: 'FKs'))

        versionHierarchy.eachWithIndex { dbVersion, versionIndex ->
            def upgrade = "descriptors/database/jbilling-upgrade-${dbVersion}.xml"
            if (new File(upgrade).exists()) {
            	println "Loading upgrade ${upgrade}"
                ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: 'base'))
                ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: 'post_base'))
            }
        };

    } else if (argsMap.client) {
        println "updating with context = base. Loading client reference Db"
        ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: schema, contexts: 'base'))
        ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: client))

        versionHierarchy.eachWithIndex { dbVersion, versionIndex ->
            def upgrade = "descriptors/database/jbilling-upgrade-${dbVersion}.xml"
            if (new File(upgrade).exists()) {
                ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: 'base'))
                ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: 'client'))
                ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: 'post_base'))
            }
        };
    }

    if ((argsMap.test && !argsMap.init && !argsMap.client) || (!argsMap.init && !argsMap.client)) {
        println "updating with context = test. Loading test data"
        ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: schema, contexts: 'base'))
        ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: test))
        ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: schema, contexts: 'FKs'))

        versionHierarchy.eachWithIndex { dbVersion, versionIndex ->
            def upgrade = "descriptors/database/jbilling-upgrade-${dbVersion}.xml"
            if (new File(upgrade).exists()) {
            	println "Loading upgrade ${upgrade}"
                ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: 'base'))
                ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: 'test'))
                ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: 'post_base'))
            }
        };
    }
}

setDefaultTarget(prepareTestDb)
