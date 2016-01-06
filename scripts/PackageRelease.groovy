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

includeTargets << grailsScript("War")

includeTargets << new File("${basedir}/scripts/CopyResources.groovy")
includeTargets << new File("${basedir}/scripts/CompileDesigns.groovy")
includeTargets << new File("${basedir}/scripts/CompileReports.groovy")
includeTargets << new File("${basedir}/scripts/Jar.groovy")

resourcesDir = "${basedir}/resources"
descriptorsDir = "${basedir}/descriptors"
configDir = "${basedir}/grails-app/conf"
sqlDir = "${basedir}/sql"
javaDir = "${basedir}/src/java"
targetDir = "${basedir}/target"

timestamp = String.format("%tF-%<tH%<tM", new Date())
releaseName = "${grailsAppName}-${grailsAppVersion}"
packageName = "${targetDir}/${releaseName}-${timestamp}.zip"

target(prepareRelease: "Builds the war and all necessary resources.") {
    copyResources()
    compileDesigns()
    compileReports()
    jar()
    war()
}

target(packageRelease: "Builds the war and packages all the necessary config files and resources in a release zip file.") {
    depends(prepareRelease)

    // ship the data.sql file if it exists, otherwise use jbilling_test.sql
    def testDb = new File("${basedir}/sql/jbilling_test.sql")
    def referenceDb = new File("${basedir}/data.sql")
    File sqlFile = referenceDb.exists() ? referenceDb : testDb

    // zip up resources into a release package
    delete(dir: targetDir, includes: "${grailsAppName}-*.zip")

    // zip into a timestamped archive for delivery to customers
    zip(filesonly: false, update: false, destfile: packageName) {
        zipfileset(dir: resourcesDir, prefix: "jbilling/resources")
        zipfileset(dir: targetDir, includes: "${grailsAppName}.jar", prefix: "jbilling/resources/api")
        zipfileset(dir: javaDir, includes: "jbilling.properties", fullpath: "jbilling/jbilling.properties")
        zipfileset(dir: configDir, includes: "Config.groovy", fullpath: "jbilling/${grailsAppName}-Config.groovy")
        zipfileset(dir: configDir, includes: "DataSource.groovy", fullpath: "jbilling/${grailsAppName}-DataSource.groovy")
        zipfileset(dir: targetDir, includes: "${grailsAppName}.war")
        zipfileset(file: sqlFile.absolutePath, includes: sqlFile.name)
        zipfileset(dir: sqlDir, includes: "upgrade.sql")
        zipfileset(file: "UPGRADE-NOTES")
    }

    println "Packaged release to ${packageName}"
}

setDefaultTarget(packageRelease)
