package org.openstatic.irc.gateways;

import org.openstatic.irc.IrcServer;
import org.openstatic.irc.IrcUser;
import org.openstatic.irc.IrcChannel;
import org.openstatic.irc.Gateway;
import org.openstatic.irc.IRCMessage;

import org.openstatic.http.*;

import java.util.Vector;
import java.util.Enumeration;
import java.util.Random;

import org.json.*;

public class APIGateway extends Thread implements Gateway
{
    private int port;
    private boolean keep_running;
    private IrcServer ircServer;
    private PlaceboHttpServer httpServer;
    private Vector<APIGatewayConnection> clients;
    
    public APIGateway(int port)
    {
        this.port = port;
        this.ircServer = null;
        this.clients = new Vector<APIGatewayConnection>();
    }

    private static String generateBigAlphaKey(int key_length)
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

    public boolean initGateway(IrcServer ircServer)
    {
        this.ircServer = ircServer;
        if (this.ircServer != null)
        {
            this.keep_running = true;
            this.start();
            this.ircServer.log("PORT: " + String.valueOf(this.port), 1, "APIGateway Startup!");
            return true;
        } else {
            return false;
        }
    }

    public APIGatewayConnection findClient(String token_id)
    {
        for(Enumeration<APIGatewayConnection> e = clients.elements(); e.hasMoreElements(); )
        {
            APIGatewayConnection ce = e.nextElement();
            if (ce.getTokenId().equals(token_id))
                return ce;
        }
        return null;
    }

    public APIGatewayConnection findClient(IrcUser u)
    {
        for(Enumeration<APIGatewayConnection> e = clients.elements(); e.hasMoreElements(); )
        {
            APIGatewayConnection ce = e.nextElement();
            if (ce.getIrcUser() == u)
                return ce;
        }
        return null;
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
        if (this.ircServer.getDebug() >= 10)
        {
            httpServer.setDebug(true);
            httpServer.setShowData(true);
        }
        httpServer.setSessionVariable("token_id");
        httpServer.setSessionMode(PlaceboHttpServer.GET_SESSION);
        httpServer.start();
        while(this.keep_running)
        {
            try
            {
                final HttpRequest nr = httpServer.getNextRequest();
                Thread t = new Thread()
                {
                    public void run()
                    {
                        HttpResponse response = new HttpResponse();
                        response.setContentType("text/javascript");
                        try
                        {
                            JSONObject response_json = APIGateway.this.doRequest(nr);
                            response.setData(response_json);
                        } catch (Exception e) {
                            System.err.println(e.toString() + " / " + e.getMessage());
                            e.printStackTrace(System.err);
                            response.setData(e.toString() + " / " + e.getMessage());
                        }
                        nr.sendResponse(response);
                    }
                };
                t.start();
            } catch (Exception x) {
                System.err.println(x.toString() + " / " + x.getMessage());
                x.printStackTrace(System.err);
            }
        }
        ircServer.log("PORT: " + String.valueOf(this.port), 1, "APIGateway Shutdown");
    }

    public JSONObject doRequest(HttpRequest request) throws Exception
    {
        JSONObject response = new JSONObject();
        String client_ip = request.getClientHostname();
        String x_real_ip = request.getHttpHeader("X-Real-IP");
        if (x_real_ip != null)
            client_ip = x_real_ip;
        String token_id = request.getGetValue("token_id");
        String timeout_string = request.getGetValue("timeout");
        int timeout = 60;
        if (timeout_string != null)
            timeout = Integer.valueOf(timeout_string).intValue();
        APIGatewayConnection connection = null;
        if (token_id != null)
        {
            connection = findClient(token_id);
            if (connection != null)
                connection.resetPingRemaining();
        }

        if (request.getPath().equals("/irc/connect/"))
        {
            if (token_id == null)
                token_id = generateBigAlphaKey(15);
            String nickname = request.getGetValue("nick");
            String pass = request.getGetValue("password");
            IrcUser existing_user = this.ircServer.findUser(nickname);
            if (existing_user == null)
            {
                connection = new APIGatewayConnection(this.ircServer, token_id, timeout, client_ip, request.getServerHostname());
                //connection.start();
                this.clients.add(connection);
                connection.connect(nickname, pass);
                response.put("token_id", token_id);
            } else {
                APIGatewayConnection ec = findClient(existing_user);
                if (ec != null)
                {
                    if (existing_user.checkPassword(pass))
                    {
                        response.put("token_id", ec.getTokenId());
                    } else {
                        response.put("error", "INVALID_PASSWORD");
                    }

                } else {
                    response.put("error", "NICK_IN_USE");
                }
            }
        } else if (request.getPath().equals("/irc/queue/")) {
            if (connection != null)
                response.put("events", connection.getMessageQueue());
        } else if (request.getPath().equals("/irc/interactive/")) {
            if (connection != null)
            {
                InteractiveResponse ir = new InteractiveResponse(request);
                connection.addInteractiveResponse(ir);
            }
        } else if (request.getPath().equals("/irc/direct/")) {
            if (connection != null)
            {
                connection.handleMessage(new IRCMessage(request.getJSONObjectPost()));
            }
        } else if (request.getPath().equals("/irc/raw/")) {
            if (connection != null)
            {
                connection.rawCommand(request.getRawPost());
            }
        } else if (request.getPath().startsWith("/irc/channel/")) {
            String channel_name = request.getPathArray()[2];
            IrcChannel channel = this.ircServer.findChannel(channel_name);
            if (channel != null)
            {
                response.put("topic", channel.getTopic());
                response.put("users", new JSONArray(channel.getMemberStringArray()));
            }
            if (connection != null)
            {
                if (request.getPath().endsWith("/join/"))
                {
                    connection.join(channel_name);
                } else if (request.getPath().endsWith("/part/")) {
                    connection.part(channel_name);
                } else if (request.getPath().endsWith("/message/")) {
                    connection.privmsg(channel_name, request.getRawPost());
                }
            }
        } else if (request.getPath().startsWith("/irc/user/")) {
            String nick_name = request.getPathArray()[2];
            IrcUser nick = this.ircServer.findUser(nick_name);
            if (nick != null)
            {
                response.put("user", nick.toString());
            }
            if (connection != null)
            {
                if (request.getPath().endsWith("/message/"))
                {
                    connection.privmsg(nick_name, request.getRawPost());
                }
            }
        } else {
            response.put("error", "API_NOT_FOUND");
        }
        if (connection != null)
        {
            response.put("nick", connection.getIrcUser().getNick());
        }
        response.put("server", this.ircServer.getServerName());
        return response;
    }
    
    public String toString()
    {
        return "APIGateway @ " + String.valueOf(this.port);
    }
}
