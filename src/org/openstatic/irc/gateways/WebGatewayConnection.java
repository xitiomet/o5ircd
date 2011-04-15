package org.openstatic.irc.gateways;

import org.openstatic.irc.GatewayConnection;
import org.openstatic.irc.IRCMessage;
import org.openstatic.irc.IrcServer;
import org.openstatic.irc.IrcUser;
import org.openstatic.irc.IrcChannel;
import org.openstatic.http.HttpRequest;
import org.openstatic.http.HttpResponse;
import org.openstatic.http.PlaceboSession;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Hashtable;
import java.net.URLEncoder;

public class WebGatewayConnection extends Thread implements GatewayConnection
{
    private IrcServer ircServer;
    private PlaceboSession connection;
    private IrcUser ircUser;
    private Hashtable<String, ByteArrayOutputStream> ajax_buffer;
    private Hashtable<String, Integer> room_timeout;
    private int lastRequestDelay;    
    private String clientHostname;

    public WebGatewayConnection(PlaceboSession connection, IrcServer ircServer)
    {
        this.connection = connection;
        this.ircServer = ircServer;
        this.ircUser = new IrcUser(this.ircServer);
        this.ajax_buffer = new Hashtable<String, ByteArrayOutputStream>();
        this.ircUser.initGatewayConnection(this);
        this.ircServer.addUser(this.ircUser);
        this.ircServer.log(this.connection.getClientHostname(), 1, "*** Initialized new WebGateway Connection");
        this.lastRequestDelay = 0;
    }
    
    private String generateBigAlphaKey(int key_length)
    {
        Random n = new Random(System.currentTimeMillis());
        String alpha = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuffer return_key = new StringBuffer();
        for (int i = 0; i < key_length; i++)
        {
            return_key.append(alpha.charAt(n.nextInt(alpha.length())));
        }
        return return_key.toString();
    }

    public void run()
    {
        Thread pingPong = new Thread()
        {
            public void run()
            {
                while (connection.isActive())
                {
                    WebGatewayConnection.this.lastRequestDelay++;
                    try
                    {
                        Thread.sleep(1000);
                    } catch (Exception e) {}
                    if (WebGatewayConnection.this.lastRequestDelay > 20)
                    {
                        WebGatewayConnection.this.ircUser.disconnect();
                    }
                }
            }
        };
        pingPong.start();
        while (connection.isActive())
        {
            HttpRequest nr = connection.getNextRequest();
            if (nr != null)
            {
                this.lastRequestDelay = 0;
                WebGatewayConnection.this.handleRequest(nr);
            }
        }
        this.ircUser.getIrcServer().log(this.clientHostname, 1, "WebGatewayConnection Thread Exiting!");
    }

    public static String osTemplate(String title, String head, String body, String post_body)
    {
        return "<html>\r\n" +
                             "<head>\r\n" +
                             "<style type=\"text/css\">\r\n" +
                             "p, li, blockquote, table\r\n" +
                             "{\r\n" +
                             "    margin-left: 12px;\r\n" +
                             "    margin-right: 8px;\r\n" +
                             "    margin-top: 0px;\r\n" +
                             "    font-family: \"terminal\", monospace;\r\n" +
                             "    font-size: .9em;\r\n" +
                             "    line-height: 150%;\r\n" +
                             "    text-align: left;\r\n" +
                             "    width: 800px;\r\n" +
                             "}\r\n" +
                             "body\r\n" +
                             "{\r\n" +
                             "    padding: 0px 0px 0px 0px;\r\n" +
                             "    margin: 0px 0px 0px 0px;\r\n" +
                             "    background-color: #FFFFEE;\r\n" +
                             "}\r\n" +
                             "h1\r\n" +
                             "{\r\n" +
                             "    font-family: \"terminal\", monospace;\r\n" +
                             "    font-size: 36px;\r\n" +
                             "    text-shadow: 0.1em 0.1em 0.6em #FFFFFF;\r\n" +
                             "    color: #EBEB00;\r\n" +
                             "    line-height: 80%;\r\n" +
                             "    letter-spacing: 4px;\r\n" +
                             "    margin: 0px 0px 8px 0px;\r\n" +
                             "    text-align: left;\r\n" +
                             "}\r\n" +
                             "div.headbar\r\n" +
                             "{\r\n" +
                             "    background-color: black;\r\n" +
                             "    padding: 12px 12px 12px 12px;\r\n" +
                             "    margin: 0px 0px 0px 0px;\r\n" +
                             "    border-style: none;\r\n" +
                             "    width: 800px;\r\n" +
                             "}\r\n" +
                             "div.nav\r\n" +
                             "{\r\n" +
                             "    background-color: #555555;\r\n" +
                             "    padding: 4px 0px 4px 0px;\r\n" +
                             "    margin: 0px 0px 0px 0px;\r\n" +
                             "    border-style: solid;\r\n" +
                             "    border-color: #777777;\r\n" +
                             "    border-width: 1px;\r\n" +
                             "    height: 23px;\r\n" +
                             "    width: 822px;\r\n" +
                             "}\r\n" +
                             "div.nav a\r\n" +
                             "{\r\n" +
                             "    color: #FFFFFF;\r\n" +
                             "    font-family: \"terminal\", monospace;\r\n" +
                             "    text-decoration: none;\r\n" +
                             "    border-style: solid;\r\n" +
                             "    border-color: #BBBBBB;\r\n" +
                             "    border-width: 1px;\r\n" +
                             "    padding: 2px 6px 2px 6px;\r\n" +
                             "}\r\n" +
                             "div.nav a:hover\r\n" +
                             "{\r\n" +
                             "    background-color: #BBBBBB;\r\n" +
                             "    color: #FF0000;\r\n" +
                             "    font-family: \"terminal\", monospace;\r\n" +
                             "    text-decoration: none;\r\n" +
                             "    border-style: solid;\r\n" +
                             "    border-color: #BBBBBB;\r\n" +
                             "    border-width: 1px;\r\n" +
                             "    padding: 2px 6px 2px 6px;\r\n" +
                             "}\r\n" +
                             "div.falsebody\r\n" +
                             "{\r\n" +
                             "    background-color: #F1F1F1;\r\n" +
                             "    width: 818px;\r\n" +
                             "    margin: 0px 0px 0px 0px;\r\n" +
                             "    padding: 5px 3px 5px 3px;\r\n" +
                             "    text-align: left;\r\n" +
                             "}\r\n" +
                             "</style>\r\n" + head +
                             "</head>\r\n" +
                             "<body onLoad=\"setTimeout('callAjax()', 3000); document.getElementById('message').focus();\">" +
                             "<div align=\"center\"><br />" +
                             "<div style=\"width: 824px; border-style: solid; border-width: 1px; border-color: black;\">" +
                             "<div class=\"headbar\"><h1>" + title + "</h1></div>\r\n" +
                             "<div class=\"nav\">\r\n" +
                             "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">\r\n" +
                             "<tr><td align=\"left\">\r\n" +
                             "</td><td align=\"right\">\r\n" +
                             "<a href=\"http://openstatic.org/irc/\">o5ircd</a>\r\n" +
                             "</td></tr></table>\r\n" +
                             "</div>\r\n" +
                             "<div class=\"falsebody\" id=\"chat_scroll\">" + body + "</div>\r\n" + post_body +
                             "</div></div></body>\r\n" +
                             "</html>\r\n";
    }
    
    public void handleRequest(HttpRequest nr)
    {
        this.clientHostname = nr.getHttpHeader("X-Real-IP");
        if (this.clientHostname == null)
            this.clientHostname = this.connection.getClientHostname();

        if (this.clientHostname == null)
            this.clientHostname = "unknown-host";
            
        if (nr.getPath().equals("/") && !this.ircUser.isReady())
        {
            String username = nr.getPostValue("username");
            String password = nr.getPostValue("password");
            if (username != null)
            {
                this.ircUser.loginUser(username, password);
                this.ircUser.setClientHost(this.clientHostname);
                Vector<String> motd = this.ircServer.getMotd();
                if (motd != null)
                {
                    for (Enumeration<String> e = motd.elements(); e.hasMoreElements();)
                    {
                        writeToBuffer("<span style=\"font-family: 'terminal', monospace;\">" + e.nextElement().replaceAll(" ","&nbsp;") + "</span><br />");
                    }
                }
            } else {
                HttpResponse response = new HttpResponse();
                response.setContentType("text/html");
                response.setData(osTemplate("O5 IRC Server","",
                                 "<form method=\"post\"><table style=\"width: 250px;\">" +
                                 "<tr><td>Username:</td><td><input type=\"text\" name=\"username\"></td></tr>" +
                                 "<tr><td></td><td align=\"right\"><input type=\"submit\" value=\"login\"></td></tr>" +
                                 "</table></form>",""));
                nr.sendResponse(response);
            }
        }

        if (nr.getPath().equals("/") && this.ircUser.isReady())
        {
            HttpResponse response = new HttpResponse();
            response.setContentType("text/html");
            String body = "<p>";
            for (Enumeration<IrcChannel> e = this.ircServer.getChannels().elements(); e.hasMoreElements(); )
            {
                IrcChannel room = e.nextElement();
                try
                {
                    body += "<a href=\"/chat/" + URLEncoder.encode(room.getName(), "UTF-8") + "/\">" + room.getName() + "</a> " + room.getTopic() + "<br />";
                } catch (Exception url_enc_exc) {}
            }
            body += "</p>";
            response.setData(osTemplate("Channels","",body,""));
            nr.sendResponse(response);
        }

        if (nr.getPath().endsWith("/ajax_updates/"))
        {
            HttpResponse response = new HttpResponse();
            response.setContentType("text/html");
            response.setData(readBuffer(nr.getGetValue("target")));
            nr.sendResponse(response);
        } else if (nr.getPath().endsWith("/part/")) {
            String target = nr.getGetValue("target");
            IRCMessage imessage = new IRCMessage("PART " + target);
            imessage.setSource(this.ircUser);
            this.ircUser.onGatewayCommand(imessage);
            
            HttpResponse response = new HttpResponse();
            response.setContentType("text/html");
            response.setData("");
            nr.sendResponse(response);
        } else if (nr.getPath().endsWith("/post_action/")) {
            String command = nr.getPostValue("command");
            String target = nr.getPostValue("target");
            String message = nr.getPostValue("message");
            IRCMessage imessage = new IRCMessage(command + " " + target + " :" + message);
            imessage.setSource(this.ircUser);
            this.ircUser.onGatewayCommand(imessage);
            HttpResponse response = new HttpResponse();
            response.setContentType("text/html");
            response.setData("");
            nr.sendResponse(response);
        } else if (nr.getPath().startsWith("/chat/") || nr.getPath().startsWith("/priv/")) {
            if (!this.ircUser.isReady())
            {
                String username = nr.getGetValue("username");
                if (username == null)
                    username = "webuser-" + generateBigAlphaKey(5);
                this.ircUser.loginUser(username, null);
                this.ircUser.setClientHost(this.clientHostname);
            }
            String path_tokens = nr.getPath().substring(6);
            StringTokenizer st = new StringTokenizer(path_tokens, "/");
            String target = "";
            String encoded_target = "";
            if (st.hasMoreTokens())
            {
                target = st.nextToken();
                try
                {
                    encoded_target = URLEncoder.encode(target, "UTF-8");
                } catch (Exception encoding_exc) {
                    encoded_target = target;
                }
                if (nr.getPath().startsWith("/chat/"))
                {
                    IRCMessage join_m = new IRCMessage("JOIN", this.ircUser);
                    join_m.setSource(this.ircUser);
                    join_m.addArg(target);
                    this.ircUser.onGatewayCommand(join_m);
                }
            }
            HttpResponse response = new HttpResponse();
            response.setContentType("text/html");
            response.setData(osTemplate(target,
                             "<script type=\"text/javascript\">\r\n" +
                             "window.onbeforeunload = function()"+
                             "{\r\n" +
                             "    var xmlhttp;\r\n" +
                             "    if (window.XMLHttpRequest) {\r\n" +
                             "        xmlhttp=new XMLHttpRequest();\r\n" +
                             "    } else if (window.ActiveXObject) {\r\n" +
                             "        xmlhttp=new ActiveXObject(\"Microsoft.XMLHTTP\");\r\n" +
                             "    } else {\r\n" +
                             "        alert(\"Your browser does not support XMLHTTP!\");\r\n" +
                             "    }\r\n" +
                             "    xmlhttp.open(\"GET\", \"part/?target=" + encoded_target + "&PLACEBO_SESSIONID=" + this.connection.getSessionId() + "\", true);\r\n" +
                             "    xmlhttp.send(null);\r\n" +
                             "}\r\n" +
                             "function callAjax()\r\n" +
                             "{\r\n" +
                             "    ajaxFunction()\r\n" +
                             "}\r\n" +
                             "function tothebottom()\r\n" +
                             "{\r\n" +
                             "    dh=document.body.scrollHeight;\r\n" +
                             "    ch=document.body.clientHeight;\r\n" +
                             "    if(dh>ch)\r\n" +
                             "    {\r\n" +
                             "        moveme=dh-ch;\r\n" +
                             "        window.scrollTo(0,moveme)\r\n" +
                             "    }\r\n" +
                             "}" +
                             "function entsub(myform, event)\r\n" +
                             "{\r\n" +
                             "    if (event.keyCode == 13)\r\n" +
                             "    {\r\n" +
                             "      myform.submit();\r\n" +
                             "      chatarea = document.getElementById('chat_scroll');\r\n" +
                             "      msg = document.getElementById('message');\r\n" +
                             "      chatarea.innerHTML += \"<b>" + this.ircUser.getNick() + "</b>: \" + msg.value + \"<br />\";\r\n" +
                             "      msg.value = '';\r\n" +
                             "      tothebottom();\r\n" +
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
                             "            if (xmlhttp.responseText != '')\r\n" +
                             "            {\r\n" +
                             "              chatarea.innerHTML += xmlhttp.responseText\r\n" +
                             "              //document.getElementById('message').focus();\r\n" +
                             "              tothebottom()\r\n" +
                             "            }\r\n" +
                             "            setTimeout('callAjax()', 10000);\r\n" +
                             "        }\r\n" +
                             "    }\r\n" +
                             "    xmlhttp.open(\"GET\", \"ajax_updates/?target=" + encoded_target + "&PLACEBO_SESSIONID=" + this.connection.getSessionId() + "\", true);\r\n" +
                             "    xmlhttp.send(null);\r\n" +
                             "}\r\n" +
                             "</script>", "",
                             "<form style=\"margin-bottom: 0;\" method=\"post\" target=\"hidden_iframe\" action=\"post_action/?PLACEBO_SESSIONID=" + nr.getPlaceboSession().getSessionId() + "\" id=\"chat_form\">" +
                             "<div id=\"action_area\" style=\"border-top-style: solid; border-top-color: black; border-top-width: 1px; padding: 0px 0px 0px 0px;\">\r\n" +
                             "<input name=\"command\" type=\"hidden\" value=\"PRIVMSG\">\r\n" +
                             "<input name=\"target\" readonly=\"true\" style=\"border-style: none; text-align: right;\" id=\"target\" size=\"5\" value=\"" + target + "\" type=\"hidden\">\r\n" +
                             "<input style=\"border-style: none; width: 100%;\" type=\"text\" id=\"message\" name=\"message\" onkeypress=\"return entsub(this.form, event);\">\r\n" +
                             "</div></form>\r\n" +
                             "<iframe name=\"hidden_iframe\" id=\"hidden_iframe\" style=\"width: 0px; height: 0px; display: none;\"></iframe>\r\n"));
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
        this.connection.endSession();
    }
    
    public void sendResponse(String response, String params)
    {
        // Do Nothing
    }

    public void writeToBuffer(String buffer_name, String data)
    {
        if (ajax_buffer.containsKey(buffer_name))
        {
            PrintStream out = new PrintStream(this.ajax_buffer.get(buffer_name));
            out.println(data);
            out.flush();
        } else {
            ByteArrayOutputStream z = new ByteArrayOutputStream();
            ajax_buffer.put(buffer_name, z);
            PrintStream out = new PrintStream(z);
            out.println(data);
            out.flush();
        }
    }

    public void writeToBuffer(String data)
    {
        for (Enumeration<String> keys = this.ajax_buffer.keys(); keys.hasMoreElements(); )
        {
            String key = keys.nextElement();
            PrintStream out = new PrintStream(this.ajax_buffer.get(key));
            out.println(data);
            out.flush();
        }
    }

    public byte[] readBuffer(String buffer_name)
    {
        byte[] return_data;
        if (ajax_buffer.containsKey(buffer_name))
        {
            ByteArrayOutputStream baos = ajax_buffer.get(buffer_name);
            return_data = baos.toByteArray();
            baos.reset();
        } else {
            ByteArrayOutputStream z = new ByteArrayOutputStream();
            ajax_buffer.put(buffer_name, z);
            return_data = z.toByteArray();
        }
        return return_data;
    }
    
    public void sendCommand(IRCMessage pc)
    {
        this.ircUser.getIrcServer().log(this.clientHostname, 5, "<- " + pc.toString());
        String nc = pc.getSourceNick();
        if (pc.is("PRIVMSG"))
        {
            if (pc.getArg(0).equals(this.ircUser.getNick()))
            {
                writeToBuffer(nc, "<b>" + nc + "</b>: " + pc.getArg(1) + "<br />");
            } else {
                writeToBuffer(pc.getArg(0), "<b><a href=\"/priv/" + nc + "/?PLACEBO_SESSIONID=" + this.connection.getSessionId() + "\" target=\"__blank\">" + nc + "</a></b>: " + pc.getArg(1) + "<br />");
            }
        }
        if (pc.is("NOTICE"))
        {
            if (pc.getArg(0).equals(this.ircUser.getNick()))
            {
                writeToBuffer(nc, "NOTICE (<b>" + nc + "</b>): " + pc.getArg(1) + "<br />");
            } else {
                writeToBuffer(pc.getArg(0), "NOTICE (<b><a href=\"/priv/" + nc + "/?PLACEBO_SESSIONID=" + this.connection.getSessionId() + "\" target=\"__blank\">" + nc + "</a></b>): " + pc.getArg(1) + "<br />");
            }
        }
        if (pc.is("JOIN"))
        {
            writeToBuffer(pc.getArg(0), "<b style=\"color: #005500;\">*** " + nc + " Joined Channel :" + pc.getArg(0) + "</b><br />");
        }
        if (pc.is("PART"))
        {
            writeToBuffer(pc.getArg(0), "<b style=\"color: #550000;\">*** " + nc + " Left Channel :" + pc.getArg(0) + "</b><br />");
        }
        if (pc.is("QUIT"))
        {
            writeToBuffer(pc.getArg(0), "<b style=\"color: #550000;\">*** " + nc + " QUIT IRC :" + pc.getArg(0) + "</b><br />");
        }
        if (pc.is("TOPIC"))
        {
            writeToBuffer(pc.getArg(0), "<b style=\"color: #000055;\">*** " + nc + " changed the topic to: " + pc.getArg(1) + "</b><br />");
        }
    }
}
