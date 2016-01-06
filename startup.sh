#!/bin/bash
#
# jBilling - The Enterprise Open Source Billing System
# Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde
# 
# This file is part of jbilling.
# 
# jbilling is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
# 
# jbilling is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
# 
# You should have received a copy of the GNU Affero General Public License
# along with jbilling.  If not, see <http://www.gnu.org/licenses/>.


# Startup script for jBilling under Cruise Control. This script sets the
# server port on the running jBilling instance so that multiple jBilling 
# projects can be tested under the same Cruise Control build loop.
#
# see also 'cc-build.properties'

#This will redirect the output to the target file
exec 2>&1> nohup.out

# load properties if file exists, otherwise use port 8080
if [ -f cc-build.properties ]; then
    . cc-build.properties
else
    server_port=8080
fi

# grails runtime options
export GRAILS_OPTS="-server -Xmx1536M -Xms256M -XX:MaxPermSize=384m"

# start jbilling and record process id
$GRAILS_HOME/bin/grails $GRAILS_STARTUP_OPT -Ddisable.auto.recompile=true -Dserver.port=${server_port} -Dgrails.reload.enabled=false run-app --non-interactive
#echo $!> jbilling.pid
#
#echo "Started jBilling on port ${server_port}."
#
exit 0;
