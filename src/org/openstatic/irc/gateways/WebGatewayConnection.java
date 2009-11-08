package org.openstatic.irc.gateways;

import org.openstatic.irc.GatewayConnection;
import org.openstatic.irc.IRCMessage;
import org.openstatic.irc.IrcServer;
import org.openstatic.irc.IrcUser;
import org.openstatic.http.HttpRequest;
import org.openstatic.http.HttpResponse;
import org.openstatic.http.PlaceboSession;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Vector;
import java.util.Enumeration;

public class WebGatewayConnection extends Thread implements GatewayConnection
{
    private IrcServer ircServer;
    private PlaceboSession connection;
    private IrcUser ircUser;
    private ByteArrayOutputStream ajax_buffer;
    
    public WebGatewayConnection(PlaceboSession connection, IrcServer ircServer)
    {
        this.connection = connection;
        this.ircServer = ircServer;
        this.ircUser = new IrcUser(this.ircServer);
        this.ajax_buffer = new ByteArrayOutputStream();
        this.ircUser.initGatewayConnection(this);
        this.ircServer.addUser(this.ircUser);
        this.ircServer.log(this.connection.getClientHostname(), 1, "*** Initialized new WebGateway Connection");
    }
    
    public void run()
    {
        while (connection.isActive())
        {
            HttpRequest nr = connection.getNextRequest();
            if (nr != null)
                WebGatewayConnection.this.handleRequest(nr);
        }
    }
    
    public void handleRequest(HttpRequest nr)
    {
        if (nr.getPath().equals("/") && !this.ircUser.isReady())
        {
            String username = nr.getPostValue("username");
            String password = nr.getPostValue("password");
            if (username != null)
            {
                this.ircUser.loginUser(username, password);
                PrintStream out = new PrintStream(this.ajax_buffer);
                Vector<String> motd = this.ircServer.getMotd();
                if (motd != null)
                {
                    for (Enumeration<String> e = motd.elements(); e.hasMoreElements();)
                    {
                        out.println("<span style=\"font-family: 'terminal', monospace;\">" + e.nextElement().replaceAll(" ","&nbsp;") + "</span><br />");
                    }
                }
            } else {
                HttpResponse response = new HttpResponse();
                response.setContentType("text/html");
                response.setData("<html>" +
                                 "<body><h1>Openstatic.org Irc Server</h1>" +
                                 "<form method=\"post\"><table>" +
                                 "<tr><td>Username:</td><td><input type=\"text\" name=\"username\"></td></tr>" +
                                 "<tr><td>Password:</td><td><input type=\"password\" name=\"password\"></td></tr>" +
                                 "<tr><td></td><td align=\"right\"><input type=\"submit\" value=\"login\"></td></tr>" +
                                 "</table></form>" +
                                 "</body>" +
                                 "</html>");
                nr.sendResponse(response);
            }
        }
        if (nr.getPath().equals("/ajax_updates/"))
        {
            HttpResponse response = new HttpResponse();
            response.setContentType("text/html");
            response.setData(ajax_buffer.toByteArray());
            nr.sendResponse(response);
            ajax_buffer.reset();
        }
        if (nr.getPath().equals("/post_action/"))
        {
            String command = nr.getPostValue("command");
            String target = nr.getPostValue("target");
            String message = nr.getPostValue("message");
            IRCMessage imessage = new IRCMessage(command + " " + target + " :" + message);
            
            this.ircUser.onGatewayCommand(imessage);
            
            HttpResponse response = new HttpResponse();
            response.setContentType("text/html");
            response.setData("");
            nr.sendResponse(response);
        }
        if (nr.getPath().equals("/") && this.ircUser.isReady())
        {
                HttpResponse response = new HttpResponse();
                response.setContentType("text/html");
                response.setData("<html>" +
                                 "<head><script type=\"text/javascript\">\r\n" +
                                 "function callAjax()\r\n" +
                                 "{\r\n" +
                                 "    ajaxFunction()\r\n" +
                                 "}\r\n" +
                                 "function entsub(myform, event)\r\n" +
                                 "{\r\n" +
                                 "    if (event.keyCode == 13)\r\n" +
                                 "    {\r\n" +
                                 "      myform.submit();\r\n" +
                                 "      chatarea = document.getElementById('chat_scroll');\r\n" +
                                 "      msg = document.getElementById('message');\r\n" +
                                 "      chatarea.innerHTML += \"<b>" + this.ircUser.getNick() + "</b> -> <i>\" + document.getElementById('target').value + \"</i>: \" + msg.value + \"<br />\";\r\n" +
                                 "      msg.value = '';\r\n" +
                                 "      return false;\r\n" +
                                 "    } else {\r\n"+
                                 "      return true;\r\n" +
                                 "    }\r\n" +
                                 "}\r\n" +
                                 "function ajaxFunction()\r\n" +
                                 "{\r\n" +
                                 "    var xmlhttp;\r\n" +
                                 "    if (window.XMLHttpRequest) {\r\n" +
                                 "        xmlhttp=new XMLHttpRequest();\r\n" +
                                 "    } else if (window.ActiveXObject) {\r\n" +
                                 "        xmlhttp=new ActiveXObject(\"Microsoft.XMLHTTP\");\r\n" +
                                 "    } else {\r\n" +
                                 "        alert(\"Your browser does not support XMLHTTP!\");\r\n" +
                                 "    }\r\n" +
                                 "    xmlhttp.onreadystatechange=function()\r\n" +
                                 "    {\r\n" +
                                 "        if (xmlhttp.readyState == 4)\r\n" +
                                 "        {\r\n" +
                                 "            chatarea = document.getElementById('chat_scroll');\r\n" +
                                 "            chatarea.innerHTML += xmlhttp.responseText\r\n" +
                                 "            document.getElementById('message').focus();\r\n" +
                                 "            setTimeout('callAjax()', 10000);\r\n" +
                                 "        }\r\n" +
                                 "    }\r\n" +
                                 "    xmlhttp.open(\"GET\", \"/ajax_updates/\", true);\r\n" +
                                 "    xmlhttp.send(null);\r\n" +
                                 "}\r\n" +
                                 "</script></head>\r\n" +
                                 "<body onLoad=\"ajaxFunction()\"><h1>Openstatic.org Irc Server</h1>\r\n" +
                                 "<div id=\"chat_scroll\"></div>\r\n" +
                                 "<form method=\"post\" target=\"hidden_iframe\" action=\"/post_action/\" id=\"chat_form\"><input name=\"command\" type=\"hidden\" value=\"PRIVMSG\"><input name=\"target\" id=\"target\" size=\"12\" value=\"\" type=\"text\"><input size=\"50\" type=\"text\" id=\"message\" name=\"message\" onkeypress=\"return entsub(this.form, event);\"><iframe name=\"hidden_iframe\" style=\"width: 0px; height: 0px; display: none;\"></iframe></form>\r\n" +
                                 "</body>\r\n" +
                                 "</html>\r\n");
                nr.sendResponse(response);
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
    
    public void sendCommand(IRCMessage pc)
    {
        // Do Nothing
        PrintStream out = new PrintStream(this.ajax_buffer);
        if (pc.is("PRIVMSG"))
        {
            out.println("<b>" + pc.getSourceNick() + "</b> -> <i>" + pc.getArg(0) + "</i>: " + pc.getArg(1) + "<br />");
        }
        out.flush();
    }
    

}