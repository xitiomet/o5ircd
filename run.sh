#!/bin/bash
java -cp lib/json-gcj.jar:lib/placebohttp.jar:lib/smack.jar -jar osircd.jar $@
