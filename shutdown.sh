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


# Shutdown script for jBilling under Cruise Control. This script kills the
# jBilling instance identified by the process ID from the 'jbilling.pid' file
# created by the startup script.

touch .kill-run-app

PID_FILE=jbilling.pid

# get the PID output from the startup script
if [ -f $PID_FILE ]; then
    JBILLING_PID=`cat $PID_FILE`
    echo "Shutting down jBilling PID $JBILLING_PID"
else
    echo "$PID_FILE not found, jBilling is not running."
    exit 0;
fi

# kill the process if it's running
if [ -n "$JBILLING_PID" ] && ps -p ${JBILLING_PID} > /dev/null ; then
    kill -9 ${JBILLING_PID}
else
    echo "jBilling is not running."
fi

# remove the pid file
if [ -f $PID_FILE ]; then
    rm $PID_FILE
fi

exit 0;
