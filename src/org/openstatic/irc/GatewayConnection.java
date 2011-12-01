package org.openstatic.irc;

import org.openstatic.irc.IRCMessage;

public interface GatewayConnection
{
    public String getServerHostname();
    public String getClientHostname();
    public void close();
    public void sendCommand(IRCMessage message);
    public void sendResponse(IRCResponse response);
}
