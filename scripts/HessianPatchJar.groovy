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

final targetDir = "${basedir}/target"

target (clean: 'Clean out old hyperoptic classes, jar files') {
    println 'Cleaning old hyperoptic classes..'
    delete(file: "${targetDir}/${grailsAppName}-hessian-${grailsAppVersion}.jar")
}

target(hessianjar: "Packages hessian serializers n a .jar file.") {
    
    tstamp()
    println 'generating jar'
    Ant.jar(destfile: "${targetDir}/${grailsAppName}-hessian-${grailsAppVersion}.jar", basedir: "${basedir}/resources/hessian-jar") {
        manifest {
            attribute(name: "Built-By", value: System.properties.'user.name')
            attribute(name: "Built-On", value: "${DSTAMP}-${TSTAMP}")
            attribute(name: "Specification-Title", value: grailsAppName)
            attribute(name: "Specification-Version", value: grailsAppVersion)
            attribute(name: "Specification-Vendor", value: "jBilling.com")
            attribute(name: "Package-Title", value: grailsAppName)
            attribute(name: "Package-Version", value: grailsAppVersion)
            attribute(name: "Package-Vendor", value: "jBilling.com")
        }
    }
    println 'Build Successful'
}

target(main: "Create jBilling hessian patch jar file.") {
    println 'hessian patch jar  - main'
    depends(clean, hessianjar)
}

setDefaultTarget(main)
