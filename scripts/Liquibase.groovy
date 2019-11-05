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

includeTargets << grailsScript("_GrailsInit")
includeTargets << grailsScript("_GrailsArgParsing")

/**
 * Classpath containing all the Grails runtime dependencies, compiled classes
 * and plug-in classes for the liquibase ant task.
 *
 * Example:
 *     ant.taskdef(resource: "liquibasetasks.properties")
 *     ant.path(id: "liquibase.classpath", liquibaseClasspath)
 *
 *     updateDatabase(classpathref: "liquibase.classpath", args...)
 */
liquibaseClasspath = {
    commonClasspath.delegate = delegate
    commonClasspath.call()

    def dependencies = grailsSettings.runtimeDependencies
    if (dependencies) {
        for (File f in dependencies) {
            pathelement(location: f.absolutePath)
        }
    }

    pathelement(location: "${pluginClassesDir.absolutePath}")
    pathelement(location: "${classesDir.absolutePath}")
}

/**
 * Returns the application version as a numeric [major].[minor] version number.
 *
 * Can be explicitly set using the -dbVersion command line argument.
 *
 * Example:
 *      "enterprise-3.2.0" => 3.2
 */
getApplicationMinorVersion = { argsMap ->
    def version = argsMap.dbVersion ? argsMap.dbVersion : grailsAppVersion

    // strip all alphanumeric characters, then trim the string down to the first dotted pair
    def number = version.replaceAll(/[^0-9\.]/, '')
    return number.count('.') > 1 ? number.substring(0, number.lastIndexOf('.')) : number;
}

/**
 * Return list of application versions that are preceding the current version.
 * The list of application version is order by the oldest first
 *
 * The hierarchy map is hardcoded
 *
 * Example: current version: 3.3 => 3.1, 3.2
 *
 */
getApplicationVersionsHierarchy = { argsMap ->

    def versionsHierarchy = argsMap.appHierarchy ? argsMap.appHierarchy : [
            '4.1': [3.1, 3.2, 3.3, 3.4, 4.0],
            '4.0': [3.1, 3.2, 3.3, 3.4],
            '3.3': [3.1, 3.2],
            '3.2': [3.1]
    ]

    def version = getApplicationMinorVersion(argsMap)
    return versionsHierarchy["${version}"]
}

/**
 * Parses the command line arguments and builds a map of database parameters required
 * by all liquibase ant tasks. If no arguments are provided the defaults will be used.
 * When running PrepareTest, you must be in the Groovy shell if you want to specify
 * there parameters as in the example below.
 *
 *      -user   = Database username,   defaults to config.dataSource.username
 *      -pass   = Database password,   defaults to config.dataSource.password
 *      -db     = Database name, defaults to 'jbilling_test'
 *      -url    = Database url,        defaults to config.dataSource.url
 *      -driver = JDBC Driver class,   defaults to config.dataSource.driverClassName
 *      -schema = Default schema name, defaults to 'public'
 *
 * Example:
 *      grails liquibase -user=[username] -pass=[password] \
 *          -db=[db name] -url=[jdbc url] -driver=[driver class] -schema=[defalut schema name]
 */
getDatabaseParameters = { argsMap ->
    def db = [
        username: argsMap.user   ?: config.dataSource.username,
        password: argsMap.pass   ?: config.dataSource.password,
        database: argsMap.db     ?: "jbilling_test",
        url:      argsMap.url    ?: config.dataSource.url,
        driver:   argsMap.driver ?: config.dataSource.driverClassName,
        schema:   argsMap.schema ?: "public"
    ]

    return db
}

echoDatabaseArgs = {
    def db = getDatabaseParameters(argsMap)
    println "${db.url} ${db.username}/${db.password ?: '[no password]'} (schema: ${db.schema}) (driver ${db.driver})"
}

/**
 * set default params for use in ant.updateDatabase and other liquibase ant tasks.
 * supports adding extra params map
 *
 * Examples:
 *      ant.dropAllDatabaseObjects(liquibaseTaskAttrs())
 *      ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: 'test'))
 */
liquibaseTaskAttrs = { extraAttrsMap ->
    def resultMap = getDatabaseParameters(argsMap)
    resultMap["classpathref"] = "liquibase.classpath"
    resultMap.remove("database")
    resultMap["defaultSchemaName"] = resultMap.remove("schema")

    extraAttrsMap.each { entry ->
        resultMap[entry.key] = entry.value
    }
    resultMap
}

target(initLiquibase: "Initialized the liquibase ant tasks") {
    depends(createConfig)
    // see http://www.liquibase.org/manual/ant
    ant.taskdef(resource: "liquibasetasks.properties")
    ant.path(id: "liquibase.classpath", liquibaseClasspath)
}

target(echoArgs: "Prints the parsed liquibase parameters to the screen.") {
    depends(parseArguments, createConfig)

    println "This grails script does not have an executable target."

    def version = getApplicationMinorVersion(argsMap)

    println "jBilling minor version = ${version}"
    echoDatabaseArgs()

    def hierarchy = getApplicationVersionsHierarchy(argsMap)
    println "jBilling versions hierarchy = ${hierarchy}"
}

setDefaultTarget(echoArgs)
