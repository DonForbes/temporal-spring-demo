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
SCRIPT_LOCATION=$(dirname $(realpath $0))
MAIN_MAVEN_POM_FILE=$(realpath "$SCRIPT_LOCATION/..")/${MAIN_MAVEN_POM_FILENAME}
} # End set_location of main_pom

function validation() {
	if ! command -v temporal 2>&1 > /dev/null 
	then
		echo "Please install the temporal command line onto your path."
		exit 1
	fi
 	
	if [ $(temporal env list | grep ${NAMESPACE} | wc -l) -eq 1 ]
	then
		echo "environment setup"
	else
		echo "Environment for CLI not setup."
		echo "Using the command line please setup an environment that will allow the temporal"
		echo "command line to connect to your Temporal service and namespace."
		echo "The namespace you are trying to connect to is defined in the .env file and is "
		echo "currently set to [" ${NAMESPACE} "]"
		exit 1
	fi
	
	temporal workflow list --env ${NAMESPACE} 2>&1 > /dev/null
	if [ $? -eq 0 ]
 	then
		echo "Successfully connected to temporal service."
	else
		echo "Failed to connect to the temporal service.  Please check operations from the command line."
		exit 1
	fi
	
} # End Validation

function set_assignment_rule_to_current_version() {
# Using temporal cli we connect to the service and set all traffic to go to the current version in the POM.

        WORKER_VERSION=$(cat $(dirname ${MAIN_MAVEN_POM_FILE})/temporal-demo-server/pom.xml| grep -A 1 temporal-demo-server|grep version | awk 'BEGIN{FS="<[A-z]+>|</[A-z]+>"}{print $2}')
	echo "Setting versioning assignment rules for task queues using version [" ${WORKER_VERSION} "]"
	
	for TASK_QUEUE in ${VERSIONED_TASK_QUEUES}
	do
		echo "Checking task queue [" ${TASK_QUEUE} "] for versioned workers"
		if [ $(temporal task-queue versioning get-rules -t ${TASK_QUEUE} --env ${NAMESPACE} | grep -c ${WORKER_VERSION}) -gt 0 ]
		then
			echo "Task queue [" ${TASK_QUEUE} "] already configured - Assuming OK"
		else
			echo "Task queue [" ${TASK_QUEUE} "] has no assignment rules.  Specifying to send 100% to worker [" ${WORKER_VERSION} "]"
			temporal task-queue versioning insert-assignment-rule -t ${TASK_QUEUE} --build-id ${WORKER_VERSION} --percentage 100 --env ${NAMESPACE} -y
		fi
		
	done

} # end set_assignment_rule_to_current_version


# *******************************************************************************
# Main running program
# *******************************************************************************
#
source $(dirname $(realpath $0))/.env
validation
set_location_of_main_pom

set_assignment_rule_to_current_version

# Run the Demo UI service in the background and trap the kill signal to stop 
# when this script stops.
mvn spring-boot:run -pl temporal-demo-ui -f ${MAIN_MAVEN_POM_FILE} &
trap onexit INT

# Run the worker service.  (ctrl C to finish)
mvn spring-boot:run -pl temporal-demo-server -f ${MAIN_MAVEN_POM_FILE}

