# Makefile for Openstatic.org IRCD
# Please note: I Chose make over ant because of gcj

# Configuration Options
JC=gcj
CLASS_PATH=src:ircd.jar
JAR=jar
JC_FLAGS=-d build

# Where to begin....
all: build ircd

build:
	mkdir build

# Executable Rule for GCJ
# -------------------------------------------------------------------------------

ircd: ircd.jar build/IrcServerBase.class
	$(JC) $^ -O2 -fuse-boehm-gc --classpath=$(CLASS_PATH) --main=IrcServerBase -o $@

# Here are all the classes for the project
# -------------------------------------------------------------------------------

build/IrcServerBase.class: src/IrcServerBase.java ircd.jar
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

build/org/openstatic/irc/IrcServer.class: src/org/openstatic/irc/IrcServer.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

build/org/openstatic/irc/ReceivedCommand.class: src/org/openstatic/irc/ReceivedCommand.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

build/org/openstatic/irc/IrcUser.class: src/org/openstatic/irc/IrcUser.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

build/org/openstatic/irc/IrcChannel.class: src/org/openstatic/irc/IrcChannel.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

build/org/openstatic/irc/PreparedCommand.class: src/org/openstatic/irc/PreparedCommand.java
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

build/org/openstatic/irc/middleware/DefaultMiddlewareHandler.class: src/org/openstatic/irc/middleware/DefaultMiddlewareHandler.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

build/org/openstatic/irc/middleware/StreamMiddlewareHandler.class: src/org/openstatic/irc/middleware/StreamMiddlewareHandler.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

build/org/openstatic/irc/middleware/JsonHttpCH.class: build/org/json/JSONArray.class build/org/json/JSONObject.class src/org/openstatic/irc/middleware/JsonHttpCH.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

# This whole section is for building the json stuff!!!
# -------------------------------------------------------------------------------
build/org/json/JSONArray.class: build/org/json/JSONObject.class src/org/json/JSONArray.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

build/org/json/JSONException.class: src/org/json/JSONException.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

build/org/json/JSONObject.class: build/org/json/JSONException.class build/org/json/JSONTokener.class build/org/json/JSONString.class src/org/json/JSONObject.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

build/org/json/JSONString.class: src/org/json/JSONString.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

build/org/json/JSONStringer.class: src/org/json/JSONStringer.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

build/org/json/JSONTokener.class: build/org/json/JSONException.class src/org/json/JSONTokener.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

build/org/json/JSONWriter.class: build/org/json/JSONObject.class src/org/json/JSONWriter.java
	$(JC) $(JC_FLAGS) --classpath=$(CLASS_PATH) -C $<

# Main Builds
# -------------------------------------------------------------------------------

ircd.jar: build/org/openstatic/irc/IrcServer.class build/org/openstatic/irc/ReceivedCommand.class build/org/openstatic/irc/IrcUser.class build/org/openstatic/irc/IrcChannel.class build/org/openstatic/irc/MiddlewareHandler.class build/org/openstatic/irc/PreparedCommand.class build/org/openstatic/irc/middleware/DefaultMiddlewareHandler.class build/org/openstatic/irc/middleware/StreamMiddlewareHandler.class build/org/openstatic/irc/middleware/JsonHttpCH.class build/org/openstatic/irc/GatewayConnection.class build/org/openstatic/irc/Gateway.class build/org/openstatic/irc/gateways/IrcGatewayConnection.class build/org/openstatic/irc/gateways/IrcGateway.class
	$(JAR) -cvf $@ -C build org

clean:
	rm -fR build
	rm -f src/org/openstatic/irc/*.class
	rm -f src/org/openstatic/irc/middleware/*.class
	rm -f src/org/openstatic/irc/gateways/*.class
	rm -f src/*.class
	rm -f ircd
	rm -f ircd.jar
