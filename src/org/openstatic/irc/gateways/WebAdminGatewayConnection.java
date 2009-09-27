package org.openstatic.irc.gateways;

import org.openstatic.irc.GatewayConnection;
import org.openstatic.irc.PreparedCommand;
import org.openstatic.irc.IrcServer;
import org.openstatic.http.HttpRequest;
import org.openstatic.http.PlaceboSession;


public class WebAdminGatewayConnection extends Thread implements GatewayConnection
{
    private IrcServer ircServer;
    private PlaceboSession connection;
    
    public WebAdminGatewayConnection(PlaceboSession connection, IrcServer ircServer)
    {
        this.connection = connection;
        this.ircServer = ircServer;
    }
    
    public String getServerHostname()
    {
        return "";
    }
    
    public String getClientHostname()
    {
        return "";
    }
    
    public void close()
    {

    }
    
    
    public void sendResponse(String response, String params)
    {
        // Do Nothing
    }
    
    public void sendCommand(PreparedCommand pc)
    {
        // Do Nothing
    }
    

}