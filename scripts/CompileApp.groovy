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

/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.tools.ant.BuildException
import org.codehaus.groovy.control.MultipleCompilationErrorsException
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException

/**
 *
 * This is a duplicated of the GrailsCompile script to implement double compilation
 *
 */

includeTargets << grailsScript("_GrailsCompile")

ant.taskdef (name: 'groovyc', classname : 'org.codehaus.groovy.grails.compiler.Grailsc')
ant.path(id: "grails.compile.classpath", compileClasspath)

target(compileIgnoreFail : "Implementation of compilation phase ignoring failures") {
    depends(compilePlugins)
    profile("Compiling sources to location [$classesDirPath]") {
        withCompilationErrorHandling (true) {
            projectCompiler.compile()
        }
        classLoader.addURL(grailsSettings.classesDir.toURI().toURL())
        classLoader.addURL(grailsSettings.pluginClassesDir.toURI().toURL())
    }
}

private withCompilationErrorHandling(boolean ignoreFailure, Closure callable) {
    try {
        callable.call()
    }
    catch (BuildException e) {
        if (e.cause instanceof MultipleCompilationErrorsException) {
            event("StatusError", ["Compilation error: ${e.cause.message}"])
        }
        else {
            grailsConsole.error "Fatal error during compilation ${e.class.name}: ${e.message}", e
        }
        if (!ignoreFailure){
            exit 1
        }
    }
    catch(Throwable e) {
        grailsConsole.error "Fatal error during compilation ${e.class.name}: ${e.message}", e
        if (!ignoreFailure){
            exit 1
        }
    }
}

target(compileApplication: "Compiles the entire application including the double compile issue") {
    println "Compiling the application with error handling..."

    println "First compilation (Will fail...)"
    compileIgnoreFail()
    println "Second compilation (Should not fail...)"
    compile()

    println "Application compiled"
}

setDefaultTarget(compileApplication)
