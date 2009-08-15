package org.openstatic.irc;

import org.openstatic.irc.PreparedCommand;

public interface GatewayConnection
{
    public String getServerHostname();
    public String getClientHostname();
    public void close();
    public void sendCommand(PreparedCommand pc);
    public void sendResponse(String response, String params);
}