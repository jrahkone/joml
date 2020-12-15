#!/bin/sh
set -e
mkdir -p bin
echo "compiling joml"
javac -d bin src/fi/captam/joml/*.java test/fi/captam/joml/*.java
echo "testing"
mkdir -p out
java -cp bin fi.captam.joml.JomlTest
echo "packing it to jar"
rm -f joml.jar
jar cvfe joml.jar fi.captam.joml.Joml -C bin fi
