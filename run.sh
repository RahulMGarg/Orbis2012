#!/bin/bash
set -o errexit
# Compile and Run the pacman game with java ai player

DIR_PATH=`pwd`
JAR_PATH="$DIR_PATH/pacman-java.jar"
CLASS_PATH="$DIR_PATH/player"
BUILD_CLASS_PATH="$JAR_PATH:$CLASS_PATH"
SRC_PATH="$DIR_PATH/player/PacPlayer.java"

# Compile
javac -classpath $BUILD_CLASS_PATH $SRC_PATH || { echo "Compilation failed for AI player"; exit 1; }

# Run
java -DCHASE_CONSTANT=$1 -DTHRESHOLD_TILES=$2 -jar $JAR_PATH $CLASS_PATH PacPlayer -ai -speed 30 -nonui
