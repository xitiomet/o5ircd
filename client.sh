#!/bin/bash
java -cp lib/json-gcj.jar:lib/placebohttp.jar:lib/smack.jar:o5ircd.jar org.openstatic.irc.client.O5Client $@
