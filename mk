#!/bin/sh
echo "compiling joml"
javac -d bin src/fi/captam/joml/*.java
echo "packing it to jar"
rm -f joml.jar
jar cvfe joml.jar fi.captam.joml.Joml -C bin fi
