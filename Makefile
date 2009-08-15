JC=gcj
CLASS_PATH=src:ircd.jar
JAR=jar

all: ircd

ircd: ircd.jar src/IrcServerBase.class
	$(JC) $^ -O2 -fuse-boehm-gc --classpath=$(CLASS_PATH) --main=IrcServerBase -o $@

src/IrcServerBase.class: src/IrcServerBase.java ircd.jar
	$(JC) --classpath=$(CLASS_PATH) -C $<

src/org/openstatic/irc/IrcServer.class: src/org/openstatic/irc/IrcServer.java
	$(JC) --classpath=$(CLASS_PATH) -C $<

src/org/openstatic/irc/ReceivedCommand.class: src/org/openstatic/irc/ReceivedCommand.java
	$(JC) --classpath=$(CLASS_PATH) -C $<

src/org/openstatic/irc/IrcUser.class: src/org/openstatic/irc/IrcUser.java
	$(JC) --classpath=$(CLASS_PATH) -C $<

src/org/openstatic/irc/IrcChannel.class: src/org/openstatic/irc/IrcChannel.java
	$(JC) --classpath=$(CLASS_PATH) -C $<

src/org/openstatic/irc/PreparedCommand.class: src/org/openstatic/irc/PreparedCommand.java
	$(JC) --classpath=$(CLASS_PATH) -C $<

src/org/openstatic/irc/MiddlewareHandler.class: src/org/openstatic/irc/MiddlewareHandler.java
	$(JC) --classpath=$(CLASS_PATH) -C $<

src/org/openstatic/irc/Gateway.class: src/org/openstatic/irc/Gateway.java
	$(JC) --classpath=$(CLASS_PATH) -C $<

src/org/openstatic/irc/GatewayConnection.class: src/org/openstatic/irc/GatewayConnection.java
	$(JC) --classpath=$(CLASS_PATH) -C $<

src/org/openstatic/irc/gateways/IrcGateway.class: src/org/openstatic/irc/gateways/IrcGateway.java
	$(JC) --classpath=$(CLASS_PATH) -C $<

src/org/openstatic/irc/gateways/IrcGatewayConnection.class: src/org/openstatic/irc/gateways/IrcGatewayConnection.java
	$(JC) --classpath=$(CLASS_PATH) -C $<

src/org/openstatic/irc/middleware/DefaultMiddlewareHandler.class: src/org/openstatic/irc/middleware/DefaultMiddlewareHandler.java
	$(JC) --classpath=$(CLASS_PATH) -C $<

src/org/openstatic/irc/middleware/StreamMiddlewareHandler.class: src/org/openstatic/irc/middleware/StreamMiddlewareHandler.java
	$(JC) --classpath=$(CLASS_PATH) -C $<

src/org/openstatic/irc/middleware/JsonHttpCH.class: src/org/openstatic/irc/middleware/JsonHttpCH.java
	$(JC) --classpath=$(CLASS_PATH) -C $<

ircd.jar: src/org/openstatic/irc/IrcServer.class src/org/openstatic/irc/ReceivedCommand.class src/org/openstatic/irc/IrcUser.class src/org/openstatic/irc/IrcChannel.class src/org/openstatic/irc/MiddlewareHandler.class src/org/openstatic/irc/PreparedCommand.class src/org/openstatic/irc/middleware/DefaultMiddlewareHandler.class src/org/openstatic/irc/middleware/StreamMiddlewareHandler.class src/org/openstatic/irc/middleware/JsonHttpCH.class src/org/openstatic/irc/GatewayConnection.class src/org/openstatic/irc/Gateway.class src/org/openstatic/irc/gateways/IrcGatewayConnection.class src/org/openstatic/irc/gateways/IrcGateway.class
	$(JAR) -cvf $@ -C src org

clean:
	rm -f src/org/openstatic/irc/*.class
	rm -f src/org/openstatic/irc/middleware/*.class
	rm -f src/org/openstatic/irc/gateways/*.class
	rm -f src/*.class
	rm -f ircd
	rm -f ircd.jar
