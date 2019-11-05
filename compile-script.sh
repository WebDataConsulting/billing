#!/bin/bash

echo "Cleaning the project";
grails clean --offline

echo "Compiling the project first time";
grails compile --offline

echo "Compiling the project for second time";
grails compile --offline

echo "Preparing test database";
grails prepare-test --offline

echo "Running app";
grails run-app --offline
