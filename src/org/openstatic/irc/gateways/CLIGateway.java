package org.openstatic.irc.gateways;

import org.openstatic.irc.IrcServer;
import org.openstatic.irc.IrcUser;
import org.openstatic.irc.Gateway;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.ByteArrayOutputStream;
import java.util.Vector;
import java.util.Enumeration;

public class CLIGateway extends Thread implements Gateway
{
    private int port;
    private boolean keep_running;
    private IrcServer ircServer;
    private ServerSocket ss;
    private ByteArrayOutputStream debug_feed;
    private Vector<CLIGatewayConnection> mySockets;
    
    public void watchByteStream(final ByteArrayOutputStream baos)
    {
        Thread t = new Thread()
        {
            public void run()
            {
                while (CLIGateway.this.keep_running)
                {
                    try
                    {
                        String debug_line = baos.toString("UTF-8");
                        if (baos.size() > 0 && debug_line.indexOf("\n") != -1)
                        {
                            for (Enumeration<CLIGatewayConnection> e = CLIGateway.this.mySockets.elements() ; e.hasMoreElements() ;)
                            {
                                CLIGatewayConnection cgc = e.nextElement();
                                cgc.clearPrompt();
                                try
                                {
                                    baos.writeTo(cgc.getOutputStream());
                                } catch (Exception ne) {
                                    System.err.println(ne.getMessage());
                                }
                                cgc.showPrompt();
                            }
                            baos.reset();
                        }
                        Thread.sleep(500);
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                    }
                }
            }
        };
        t.start();
    }
    
    public CLIGateway(int port, ByteArrayOutputStream debug_feed)
    {
        this.port = port;
        this.ircServer = null;
        this.ss = null;
        this.debug_feed = debug_feed;
        this.mySockets = new Vector<CLIGatewayConnection>();
    }
    
    public boolean initGateway(IrcServer ircServer)
    {
        this.ircServer = ircServer;
        if (this.ircServer != null)
        {
            this.keep_running = true;
            this.start();
            this.ircServer.log("PORT: " + String.valueOf(this.port), 1, "CLI Gateway Startup!");
            watchByteStream(debug_feed);
            return true;
        } else {
            return false;
        }
    }
    
    public void shutdownGateway()
    {
        this.keep_running = false;
        try
        {
            for (Enumeration<CLIGatewayConnection> e = CLIGateway.this.mySockets.elements() ; e.hasMoreElements() ;)
            {
                CLIGatewayConnection cgc = e.nextElement();
                cgc.close();
            }
            this.ss.close();
            join();
        } catch (Exception ss_close) {}
    }
    
    public IrcServer getIrcServer()
    {
        return this.ircServer;
    }
    
    public void run()
    {
        
        try
        {
            ss = new ServerSocket(this.port);
        } catch (Exception n) {}
        while(this.keep_running)
        {
            try
            {
                Socket new_connection = ss.accept();
                ircServer.log("PORT: " + String.valueOf(this.port), 1, "CLI Gateway new connection");
                CLIGatewayConnection cligc = new CLIGatewayConnection(new_connection, this.ircServer);
                cligc.start();
                this.mySockets.addElement(cligc);
            } catch (Exception x) {}
        }
        ircServer.log("PORT: " + String.valueOf(this.port), 1, "CLI Gateway Shutdown");
    }    
}