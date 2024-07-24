#!/bin/bash

APP_DIR=$(dirname "$0")
JAVA_HOME=$(/usr/libexec/java_home)

$JAVA_HOME/bin/java -jar "$APP_DIR/../Resources/osxiec.jar"
