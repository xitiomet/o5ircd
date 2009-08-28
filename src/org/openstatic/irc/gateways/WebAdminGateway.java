package org.openstatic.irc.gateways;

import org.openstatic.irc.IrcServer;
import org.openstatic.irc.IrcUser;
import org.openstatic.irc.Gateway;

import java.net.ServerSocket;
import java.net.Socket;

public class WebAdminGateway extends Thread implements Gateway
{
    private int port;
    private boolean keep_running;
    private IrcServer ircServer;
    
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
            this.ircServer.logln("PORT: " + String.valueOf(this.port), "WebAdmin Gateway Startup!");
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
        ServerSocket ss = null;
        try
        {
            ss = new ServerSocket(this.port);
        } catch (Exception n) {}
        while(this.keep_running)
        {
            try
            {
                Socket new_connection = ss.accept();
                WebAdminGatewayConnection wagc = new WebAdminGatewayConnection(new_connection, this.ircServer);
                wagc.start();
            } catch (Exception x) {}
        }
        ircServer.logln("PORT: " + String.valueOf(this.port), "WebAdmin Gateway Shutdown");
    }
}