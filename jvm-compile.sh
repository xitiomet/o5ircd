#!/bin/bash
mkdir jvm-build
javac -d jvm-build src/IrcServerBase.java src/org/json/*.java src/org/openstatic/irc/*.java src/org/openstatic/irc/gateways/*.java src/org/openstatic/irc/middleware/*.java
jar -cvf jvm-osircd.jar -C jvm-build org
