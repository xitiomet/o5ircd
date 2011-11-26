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

public class APIGatewayConnection extends Thread implements GatewayConnection
{
    private IrcServer ircServer;
    private PlaceboSession connection;
    private IrcUser ircUser;
    private Hashtable<String, ByteArrayOutputStream> ajax_buffer;
    private Hashtable<String, Integer> room_timeout;
    private int lastRequestDelay;    
    private String clientHostname;

    public APIGatewayConnection(PlaceboSession connection, IrcServer ircServer)
    {
        this.connection = connection;
        this.ircServer = ircServer;
        this.ircUser = new IrcUser(this.ircServer);
        this.ajax_buffer = new Hashtable<String, ByteArrayOutputStream>();
        this.ircUser.initGatewayConnection(this);
        this.ircServer.addUser(this.ircUser);
        this.ircServer.log(this.connection.getClientHostname(), 1, "*** Initialized new APIGateway Connection");
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
                    APIGatewayConnection.this.lastRequestDelay++;
                    try
                    {
                        Thread.sleep(1000);
                    } catch (Exception e) {}
                    if (APIGatewayConnection.this.lastRequestDelay > 40)
                    {
                        APIGatewayConnection.this.ircUser.disconnect();
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
                APIGatewayConnection.this.handleRequest(nr);
            }
        }
        this.ircUser.getIrcServer().log(this.clientHostname, 1, "APIGatewayConnection Thread Exiting!");
    }

    
    public void handleRequest(HttpRequest nr)
    {
        this.clientHostname = nr.getHttpHeader("X-Real-IP");
        if (this.clientHostname == null)
            this.clientHostname = this.connection.getClientHostname();

        if (this.clientHostname == null)
            this.clientHostname = "unknown-host";
            
        if (nr.getPath().equals("/api/user/connect/") && !this.ircUser.isReady())
        {
            String username = nr.getGetValue("username");
            String password = nr.getGetValue("password");
            if (username != null)
            {
                this.ircUser.loginUser(username, password);
                this.ircUser.setClientHost(this.clientHostname);
                Vector<String> motd = this.ircServer.getMotd();
            }
        }

        if (nr.getPath().equals("/api/channel/") && this.ircUser.isReady())
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

        if (nr.getPath().endsWith("/wgc.css"))
        {
            HttpResponse response = new HttpResponse();
            response.setData(getClass().getResourceAsStream("/www/openstatic.css"),"text/css");
            nr.sendResponse(response);
        } else if (nr.getPath().endsWith("/updates/")) {
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
                writeToBuffer(pc.getArg(0), "<b><a href=\"/priv/" + nc + "/?PLACEBO_SESSIONID=" + this.connection.getSessionId() + "\" target=\"_blank\">" + nc + "</a></b>: " + pc.getArg(1) + "<br />");
            }
        }
        if (pc.is("NOTICE"))
        {
            if (pc.getArg(0).equals(this.ircUser.getNick()))
            {
                writeToBuffer(nc, "NOTICE (<b>" + nc + "</b>): " + pc.getArg(1) + "<br />");
            } else {
                writeToBuffer(pc.getArg(0), "NOTICE (<b><a href=\"/priv/" + nc + "/?PLACEBO_SESSIONID=" + this.connection.getSessionId() + "\" target=\"_blank\">" + nc + "</a></b>): " + pc.getArg(1) + "<br />");
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
