# Makefile for Openstatic.org IRCD
# Please note: I Chose make over ant because of gcj

# Configuration Options
JC=gcj
CLASS_PATH=build:src:lib/json-gcj.jar:lib/placebohttp.jar
JAR=jar
JC_FLAGS=-d build

# Where to begin....
all: ircd

jvm:
	mkdir jvm-build
	javac -cp lib/json-gcj.jar:lib/placebohttp.jar:lib/smack.jar -d jvm-build src/org/openstatic/*.java src/org/openstatic/irc/*.java src/org/openstatic/irc/gateways/*.java src/org/openstatic/irc/middleware/*.java
	jar -cvmf res/manifest.mf o5ircd.jar -C jvm-build org -C res www

# Executable Rule for GCJ
# -------------------------------------------------------------------------------
# check it, the ordering of the jar files in gcj is important. Each dependency should be included before any lib that requires it
# example: ircd.jar dependes on json-gcj.jar and placebohttp.jar, therefore they must go first!!!
ircd: lib/placebohttp.jar o5ircd.jar
	$(JC) $^ -O2 -findirect-dispatch -fuse-boehm-gc --main=org.openstatic.irc.IrcServerBase -o $@

# Here are all the classes for the project
# -------------------------------------------------------------------------------

build/org/openstatic/irc/IrcServerBase.class: src/org/openstatic/irc/IrcServerBase.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

build/org/openstatic/irc/IrcServer.class: src/org/openstatic/irc/IrcServer.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

build/org/openstatic/irc/Base64Coder.class: src/org/openstatic/Base64Coder.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

build/org/openstatic/irc/IRCMessage.class: src/org/openstatic/irc/IRCMessage.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

build/org/openstatic/irc/IrcUser.class: src/org/openstatic/irc/IrcUser.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

build/org/openstatic/irc/IrcChannel.class: src/org/openstatic/irc/IrcChannel.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

build/org/openstatic/irc/MiddlewareHandler.class: src/org/openstatic/irc/MiddlewareHandler.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

build/org/openstatic/irc/Gateway.class: src/org/openstatic/irc/Gateway.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

build/org/openstatic/irc/GatewayConnection.class: src/org/openstatic/irc/GatewayConnection.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

build/org/openstatic/irc/gateways/IrcGateway.class: src/org/openstatic/irc/gateways/IrcGateway.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

build/org/openstatic/irc/gateways/IrcGatewayConnection.class: src/org/openstatic/irc/gateways/IrcGatewayConnection.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

build/org/openstatic/irc/gateways/WebGateway.class: src/org/openstatic/irc/gateways/WebGateway.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

build/org/openstatic/irc/gateways/WebGatewayConnection.class: src/org/openstatic/irc/gateways/WebGatewayConnection.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

build/org/openstatic/irc/gateways/CLIGateway.class: src/org/openstatic/irc/gateways/CLIGateway.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

build/org/openstatic/irc/gateways/CLIGatewayConnection.class: src/org/openstatic/irc/gateways/CLIGatewayConnection.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

build/org/openstatic/irc/middleware/DefaultMiddlewareHandler.class: src/org/openstatic/irc/middleware/DefaultMiddlewareHandler.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

build/org/openstatic/irc/middleware/StreamMiddlewareHandler.class: src/org/openstatic/irc/middleware/StreamMiddlewareHandler.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

build/org/openstatic/irc/middleware/TwitterMiddlewareHandler.class: src/org/openstatic/irc/middleware/TwitterMiddlewareHandler.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

build/org/openstatic/irc/middleware/StreamingJsonMiddlewareHandler.class: src/org/openstatic/irc/middleware/StreamingJsonMiddlewareHandler.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

build/org/openstatic/irc/middleware/JsonHttpCH.class: src/org/openstatic/irc/middleware/JsonHttpCH.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

# Main Builds
# -------------------------------------------------------------------------------

o5ircd.jar: build/org/openstatic/irc/IrcServer.class build/org/openstatic/irc/IRCMessage.class build/org/openstatic/irc/IrcUser.class build/org/openstatic/irc/IrcChannel.class build/org/openstatic/irc/MiddlewareHandler.class build/org/openstatic/irc/middleware/DefaultMiddlewareHandler.class build/org/openstatic/irc/middleware/StreamMiddlewareHandler.class build/org/openstatic/irc/middleware/JsonHttpCH.class build/org/openstatic/irc/GatewayConnection.class build/org/openstatic/irc/Gateway.class build/org/openstatic/irc/gateways/IrcGatewayConnection.class build/org/openstatic/irc/gateways/IrcGateway.class build/org/openstatic/irc/gateways/WebGatewayConnection.class build/org/openstatic/irc/gateways/WebGateway.class build/org/openstatic/irc/middleware/TwitterMiddlewareHandler.class build/org/openstatic/irc/middleware/StreamingJsonMiddlewareHandler.class build/org/openstatic/Base64Coder.class build/org/openstatic/irc/IrcServerBase.class build/org/openstatic/irc/gateways/CLIGatewayConnection.class build/org/openstatic/irc/gateways/CLIGateway.class
	$(JAR) -cvmf res/manifest.mf $@ -C build org -C res www

clean:
	rm -fR build
	rm -f ircd
	rm -f o5ircd.jar
	rm -fR jvm-build
