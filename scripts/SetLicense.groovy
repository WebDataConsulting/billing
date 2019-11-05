

//import com.sapienter.jbilling.common.Util
import org.apache.log4j.lf5.util.Resource

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


isInternalLicensee  = { licensee, licenseKey ->
    ("jbilling.com".equals(licensee)) && licenseKey.startsWith("CxDQ") && licenseKey.endsWith("wIHM")
}


replaceLicenseTokens = { fileName ->
    ant.replace(file: fileName, propertyFile: "license.txt") {
        replacefilter(token: "licensee name", property: "licensee")
        replacefilter(token: "place license key here", property: "licenseKey")
    }
}


copyAndReplaceLicenseTokens = { dirName ->
    ant.mkdir(dir: dirName)
    ant.copy(file: "${basedir}/src/java/jbilling.properties", toDir: dirName, overwrite: "yes", verbose: "yes")
    replaceLicenseTokens("${dirName}/jbilling.properties")
}


target(setLicense: "Set the license key in jbilling.properties with whatever is in license.txt") {

    ant.available(file: "license.txt", property: "licenseAvailable")

    if (ant.project.getProperty("licenseAvailable")) {
        println "Setting license in jbilling.properties from license.txt"

        ant.loadproperties(srcFile:"license.txt")

        if (! isInternalLicensee(ant.project.getProperty("licensee"), ant.project.getProperty("licenseKey"))) {
            replaceLicenseTokens("${basedir}/src/java/jbilling.properties")
        }
        copyAndReplaceLicenseTokens("${projectWorkDir}/resources/") // for "grails run-app"
    }
}
