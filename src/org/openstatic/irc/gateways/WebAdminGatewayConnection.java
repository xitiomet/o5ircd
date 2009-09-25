package org.openstatic.irc.gateways;

import org.openstatic.irc.GatewayConnection;
import org.openstatic.irc.PreparedCommand;
import org.openstatic.irc.IrcServer;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.StringTokenizer;
import java.util.Date;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Hashtable;

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
        this.ircServer = ircServer;
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
    
    // This thread is to work with the incomming data. And parses http requests very roughly
    // most of the fun stuff happens at the handlehttpRequest part.
    public void run()
    {
        try
        {            
            // here we process input from the Browser
            BufferedReader br = new BufferedReader(new InputStreamReader(this.is));
            
            String cmd_line = null;
            
            String request_type = null;
            String request_path = null;
            Hashtable<String, String> headers = new Hashtable<String, String>();
            Hashtable<String, String> formContent = new Hashtable<String, String>();
            
            try
            {
                String each_line = null;;
                
                // Recieve Request header and process
                while (!"".equals(cmd_line))
                {
                    cmd_line = br.readLine();
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
                        // lets store the request headers just incase
                        if (currentToken.indexOf(":") > -1)
                        {
                            headers.put(currentToken.replaceAll(":",""), request.nextToken());
                        }
                    }
                }
                
                // what do do if there was a post!
                if ("application/x-www-form-urlencoded".equals(headers.get("Content-Type")))
                {
                    int content_length = Integer.valueOf(headers.get("Content-Length")).intValue();
                    this.ircServer.logln(this.clientHostname, "***  application/x-www-form-urlencoded");
                    int bytein = -2;
                    int byteCount = 0;
                    StringBuffer form_raw = new StringBuffer("");
                    while (bytein != -1 && byteCount < content_length)
                    {
                        bytein = br.read();
                        byteCount++;
                        if (bytein > -1) form_raw.append((char) bytein);
                    }
                    cmd_line = form_raw.toString();
                    
                    StringTokenizer form_data = new StringTokenizer(cmd_line, "&");
                    while (form_data.hasMoreTokens())
                    {
                        String currentToken = form_data.nextToken();
                        if (currentToken.indexOf("=") > -1)
                        {
                            StringTokenizer form_entry = new StringTokenizer(currentToken, "=");
                            String f_key = form_entry.nextToken();
                            String f_value = URLDecoder.decode(form_entry.nextToken(),"UTF-8");
                            formContent.put(f_key, f_value);
                            this.ircServer.logln(this.clientHostname, "-> (FORMDATA) {" + f_key + "} " + f_value);
                        }
                    }
                }
            } catch (Exception rex) {
                this.ircServer.logln("WebAdmin", "Exception: " + rex.toString() + " / " + rex.getMessage());
            }
            
            if (request_type != null)
            {
                this.handleHttpRequest(request_type, request_path, headers, formContent);
            }
        } catch (Exception x) {}
        try
        {
            this.connection.close();
        } catch (Exception closeout_exception) {}
    }
    
    // this is rather sloppy but it only needs basic functionality!
    private void handleHttpRequest(String request_type, String request_path, Hashtable<String, String> headers, Hastbale<String, String> formContent)
    {
        sendHttpResponse("<html><body><h1>It Works! Yeah it does!</h1><form method=\"post\"><input type=\"text\" name=\"what\"><input type=\"submit\"></form></body></html>\r\n");
    }
    
    private void sendHttpResponse(String response)
    {
        socketWrite("HTTP/1.1 200 OK\r\n");
        socketWrite("Server: OpenstaticIRC/1.0\r\n");
        socketWrite("Date: " + (new Date()).toString() + "\r\n");
        socketWrite("Content-Type: text/html\r\n");
        socketWrite("Content-Length: " + String.valueOf(response.length()) + "\r\n");
        //out.print("Connection: keep-alive\r\n");
        socketWrite("\r\n");
        socketWrite(response);
        try
        {
            this.os.close();
        } catch (Exception cs_exc) {}
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