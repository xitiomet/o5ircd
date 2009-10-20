package org.openstatic.irc.gateways;

import org.openstatic.irc.GatewayConnection;
import org.openstatic.irc.PreparedCommand;
import org.openstatic.irc.IrcServer;
import org.openstatic.irc.IrcUser;
import org.openstatic.http.HttpRequest;
import org.openstatic.http.PlaceboSession;
import java.io.PrintStream;


public class WebAdminGatewayConnection extends Thread implements GatewayConnection
{
    private IrcServer ircServer;
    private PlaceboSession connection;
    private IrcUser ircUser;
    
    public WebAdminGatewayConnection(PlaceboSession connection, IrcServer ircServer)
    {
        this.connection = connection;
        this.ircServer = ircServer;
        this.ircUser = new IrcUser(this.ircServer);
        this.ircUser.initGatewayConnection(this);
        this.ircServer.log(this.connection.getClientHostname(), 1, "*** Initialized new WebAdminGateway Connection");
    }
    
    public void run()
    {
        while (connection.isActive())
        {
            HttpRequest nr = connection.getNextRequest();
            if (nr != null)
                WebAdminGatewayConnection.this.handleRequest(nr);
        }
    }
    
    public void handleRequest(HttpRequest nr)
    {
        System.out.println(this.ircUser.toString());
        if (nr.getPath().equals("/") && !this.ircUser.isReady())
        {
            String username = nr.getPostValue("username");
            String password = nr.getPostValue("password");
            if (username != null && password != null)
            {
                this.ircUser.loginUser(username, password);
            } else {
                PrintStream out = new PrintStream(nr.getOutputStream());
                out.println("<html>");
                out.println("<body><h1>Openstatic.org Irc Server</h1>");
                out.println("<form method=\"post\"><table>");
                out.println("<tr><td>Username:</td><td><input type=\"text\" name=\"username\"></td></tr>");
                out.println("<tr><td>Password:</td><td><input type=\"password\" name=\"password\"></td></tr>");
                out.println("<tr><td></td><td align=\"right\"><input type=\"submit\" value=\"login\"></td></tr>");
                out.println("</table></form>");
                out.println("</body>");
                out.println("</html>");
                nr.sendResponse("text/html");
            }
        }
        if (nr.getPath().equals("/") && this.ircUser.isReady())
        {
                PrintStream out = new PrintStream(nr.getOutputStream());
                out.println("<html>");
                out.println("<body><h1>Openstatic.org Irc Server</h1>");
                out.println("</body>");
                out.println("</html>");
                nr.sendResponse("text/html");
        }
    }
    
    public String getServerHostname()
    {
        return this.connection.getServerHostname();
    }
    
    public String getClientHostname()
    {
        return this.connection.getClientHostname();
    }
    
    public void close()
    {
        connection.endSession();
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