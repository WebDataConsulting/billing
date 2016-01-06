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

/**
 * @author Vladimir Carevski
 * @since 13-APR-2014
 *
 * This script will parse all the jbilling-upgrade-x.xml files
 * and look for customChange. It will extract the class name from
 * the customChange convert the class to a file path and generate
 * an upgrade.jar in Liquidbase folder with all Java classes
 * required for Liquibase upgrade. This jar is especially useful
 * when upgrading environment where there is no Grails installed
 * (ex. production) or we need manual update via console.
 */
includeTargets << grailsScript("Init")
includeTargets << new File("${basedir}/scripts/Liquibase.groovy")

final targetDir = "${basedir}/target"
final jarDir = 'liquibase-3.2.3'
final upgradeLib = 'upgrade.jar'

target(generateLbJar: "Generates a JAR with all customChange class dependencies") {
    depends(parseArguments, initLiquibase)

    def version = getApplicationMinorVersion(argsMap)
    def versionHierarchy = getApplicationVersionsHierarchy(argsMap)
    versionHierarchy.add(version)
    versionHierarchy.sort()
    def classes = []

    versionHierarchy.eachWithIndex { dbVersion, versionIndex ->
        def upgrade = "descriptors/database/jbilling-upgrade-${dbVersion}.xml"
        def File upgradeFile = new File(upgrade)
        if (upgradeFile.exists()) {
            println 'Checking file: ' + upgradeFile.absolutePath
            def databaseChangeLog = new XmlParser().parse(upgradeFile)
            classes.addAll(databaseChangeLog.changeSet.customChange.'@class' as List)
        }
    }

    delete(dir: targetDir, includes: "${upgradeLib}")

    println 'Found Classes: ' + classes.toString().replaceAll(',','\n')
    println 'JBilling Version: ' + version

    ant.pathconvert( property : 'change.class.path'){
        classes.each {_class ->
            string(value:_class)
        }
        unpackagemapper( from:'*', to:'*.class')
    }

    def changeClassList = ant.antProject.getProperty(
            'change.class.path')?.replaceAll(';',',')
                                .replaceAll('\\.class','*').split(':')

    println "Target dir: ${targetDir}"
    println "Building Jar:${jarDir}/${upgradeLib}"

    ant.jar(destfile: "${jarDir}/${upgradeLib}",
            basedir: "${targetDir}/classes",
            includes: changeClassList,
            excludes: '*') {

        for(def changeClass: changeClassList){
            fileset( dir: "${targetDir}/classes", includes: changeClass)
        }

        manifest {
            attribute(name: "Built-By", value: System.properties.'user.name')
            attribute(name: "Specification-Vendor", value: "jBilling.com")

            attribute(name: "Package-Title", value: 'Upgrade-Lib')
            attribute(name: "Package-Version", value: version)
            attribute(name: "Package-Vendor", value: "jBilling.com")
        }
    }

    println 'Done'
}

setDefaultTarget(generateLbJar)
