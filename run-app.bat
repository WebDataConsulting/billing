rem
rem jBilling - The Enterprise Open Source Billing System
rem Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde
rem 
rem This file is part of jbilling.
rem 
rem jbilling is free software: you can redistribute it and/or modify
rem it under the terms of the GNU Affero General Public License as published by
rem the Free Software Foundation, either version 3 of the License, or
rem (at your option) any later version.
rem 
rem jbilling is distributed in the hope that it will be useful,
rem but WITHOUT ANY WARRANTY; without even the implied warranty of
rem MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
rem GNU Affero General Public License for more details.
rem 
rem You should have received a copy of the GNU Affero General Public License
rem along with jbilling.  If not, see <http://www.gnu.org/licenses/>.

set JAVA_OPTS=%JAVA_OPTS% -Xms256m -Xmx512m -XX:PermSize=512m -XX:MaxPermSize=512m
set GRAILS_OPTS=-server -Xmx1024M -Xms256M -XX:PermSize=512m -XX:MaxPermSize=512m

grails -noreloading -Ddisable.auto.recompile=true run-app
