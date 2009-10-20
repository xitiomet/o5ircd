package org.openstatic.irc.gateways;

import org.openstatic.irc.IrcServer;
import org.openstatic.irc.IrcUser;
import org.openstatic.irc.Gateway;

import java.net.ServerSocket;
import java.net.Socket;

public class IrcGateway extends Thread implements Gateway
{
    private int port;
    private boolean keep_running;
    private IrcServer ircServer;
    
    public IrcGateway(int port)
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
            this.ircServer.log("PORT: " + String.valueOf(this.port), 1, "Irc Gateway Startup!");
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
                IrcUser ircUser = new IrcUser(this.ircServer);
                IrcGatewayConnection igc = new IrcGatewayConnection(new_connection, ircUser);
                igc.start();
                ircServer.addUser(ircUser);
            } catch (Exception x) {}
        }
        ircServer.log("PORT: " + String.valueOf(this.port), 1, "Irc Gateway Shutdown");
    }
}