package org.openstatic.irc.gateways;

import org.openstatic.irc.GatewayConnection;
import org.openstatic.irc.PreparedCommand;
import org.openstatic.irc.IrcServer;
import java.net.Socket;
import java.util.StringTokenizer;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class WebAdminGatewayConnection extends Thread implements GatewayConnection
{
    private InputStream is;
    private OutputStream os;
    private Socket connection;
    private String serverHostname;
    private String clientHostname;
    private int ping_countdown;
    private boolean stay_connected;
    private IrcServer ircServer;
    
    public WebAdminGatewayConnection(Socket connection, IrcServer ircServer)
    {
        this.stay_connected = true;
        try
        {
            this.is = connection.getInputStream();
            this.os = connection.getOutputStream();
        } catch (Exception n) {}
        this.ping_countdown = 60;
        this.connection = connection;
        this.ircServer = ircServer
        this.serverHostname = this.connection.getLocalAddress().getCanonicalHostName();
        this.clientHostname = this.connection.getInetAddress().getCanonicalHostName();
    }
    
    public String getServerHostname()
    {
        return this.serverHostname;
    }
    
    public String getClientHostname()
    {
        return this.clientHostname;
    }
    
    public void close()
    {
        this.stay_connected = false;
    }
    
    // This thread is to work with the incomming data.
    public void run()
    {
        try
        {            
            // here we process input from the Browser
            BufferedReader br = new BufferedReader(new InputStreamReader(this.is));
            String cmd_line;
            
            String request_type = null;
            String request_path = null;
            
            try
            {
                while ((cmd_line = br.readLine()) != null)
                {
                    this.ircServer.logln(this.clientHostname, "-> " + cmd_line);
                    StringTokenizer request = new StringTokenizer(cmd_line);
                    while (request.hasMoreTokens())
                    {
                        String currentToken = request.nextToken();
                        if ("GET".equals(currentToken) && request.hasMoreTokens())
                        {
                            request_path = request.nextToken();
                            request_type = "GET";
                        }
                        if ("POST".equals(currentToken) && request.hasMoreTokens())
                        {
                            request_path = request.nextToken();
                            request_type = "POST";
                        }
                    }
                }
            } catch (Exception rex) {}
            
            if (request_type != null)
            {
                this.handleRequest(request_type, request_path);
            }
        } catch (Exception x) {}
        this.connection.close();
    }
    
    public void handleRequest(String request_type, String request_path)
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
    
    private void socketWrite(String out)
    {
        try
        {
            this.os.write(out.getBytes());
            this.os.flush();
            this.ircServer.log(this.clientHostname, "<- " + out);
        } catch (Exception we) {}
    }
}