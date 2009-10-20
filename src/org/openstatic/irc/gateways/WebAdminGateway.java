package org.openstatic.irc.gateways;

import org.openstatic.irc.IrcServer;
import org.openstatic.irc.Gateway;

import org.openstatic.http.PlaceboHttpServer;
import org.openstatic.http.PlaceboSession;

public class WebAdminGateway extends Thread implements Gateway
{
    private int port;
    private boolean keep_running;
    private IrcServer ircServer;
    private PlaceboHttpServer httpServer;
    
    public WebAdminGateway(int port)
    {
        this.port = port;
        this.ircServer = null;
    }
    
    public boolean initGateway(IrcServer ircServer)
    {
        this.ircServer = ircServer;
        if (this.ircServer != null)
        {
            this.keep_running = true;
            this.start();
            this.ircServer.log("PORT: " + String.valueOf(this.port), 1, "WebAdmin Gateway Startup!");
            return true;
        } else {
            return false;
        }
    }
    
    public void shutdownGateway()
    {
        this.keep_running = false;
    }
    
    public IrcServer getIrcServer()
    {
        return this.ircServer;
    }
    
    public void run()
    {
        httpServer = new PlaceboHttpServer(this.port);
        httpServer.setDebugStream(this.ircServer.getDebugStream());
        if (this.ircServer.getDebug() >= 2)
        {
            httpServer.setDebug(true);
        }
        httpServer.start();
        while(this.keep_running)
        {
            try
            {
                PlaceboSession new_connection = httpServer.getNextSession();
                WebAdminGatewayConnection wagc = new WebAdminGatewayConnection(new_connection, this.ircServer);
                wagc.start();
            } catch (Exception x) {}
        }
        ircServer.log("PORT: " + String.valueOf(this.port), 1, "WebAdmin Gateway Shutdown");
    }
    
    public String toString()
    {
        return "WebAdminGateway @ " + String.valueOf(this.port);
    }
}