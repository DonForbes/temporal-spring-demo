#!/bin/bash
#
# Script to start the two Temporal demo apps running and on closure both shutdown.
#
# Global Variables
MAIN_MAVEN_POM_FILENAME="pom.xml"

# Functions
#
# Capture the exit signal and kill the (Maven) UI process running in the background.
function onexit() {
	DEMO_UI_PID=$(ps -ef | grep "spring-boot:run" | grep temporal-demo-ui | awk '{printf $2}')
	echo "Killing pid " $DEMO_UI_PID
	kill $DEMO_UI_PID
} # End onexit function
function set_location_of_main_pom() {


echo "pom location|"
SCRIPT_LOCATION=$(dirname $(realpath $0))
MAIN_MAVEN_POM_FILE=$(realpath "$SCRIPT_LOCATION/..")/${MAIN_MAVEN_POM_FILENAME}
} # End set_location of main_pom

# Main running program
set_location_of_main_pom


# Run the Demo UI service
mvn spring-boot:run -pl temporal-demo-ui -f ${MAIN_MAVEN_POM_FILE} &
trap onexit INT

# Run the worker service.  (ctrl C to finish)
mvn spring-boot:run -pl temporal-demo-server -f ${MAIN_MAVEN_POM_FILE}

