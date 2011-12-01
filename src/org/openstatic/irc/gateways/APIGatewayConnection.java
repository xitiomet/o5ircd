package org.openstatic.irc.gateways;

import org.openstatic.irc.GatewayConnection;
import org.openstatic.irc.IRCMessage;
import org.openstatic.irc.IRCResponse;
import org.openstatic.irc.IrcServer;
import org.openstatic.irc.IrcUser;
import org.openstatic.irc.IrcChannel;
import org.openstatic.http.HttpRequest;
import org.openstatic.http.HttpResponse;
import org.openstatic.http.InteractiveResponse;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Vector;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Hashtable;
import java.util.concurrent.LinkedBlockingQueue;
import java.net.URLEncoder;
import org.json.*;

public class APIGatewayConnection extends Thread implements GatewayConnection
{
    private IrcServer ircServer;
    private IrcUser ircUser;
    private Hashtable<String, Integer> room_timeout;
    private LinkedBlockingQueue<JSONObject> message_queue;
    private Vector<InteractiveResponse> ir_list;
    private int lastRequestDelay;
    private int timeout;
    private String clientHostname;
    private String serverHostname;
    private String token_id;
    private boolean keep_running;

    public APIGatewayConnection(IrcServer ircServer, String token_id, int timeout, String client_ip, String server_hostname)
    {
        this.ircServer = ircServer;
        this.ircUser = new IrcUser(this.ircServer);
        this.ircUser.setEcho(true);
        this.message_queue = new LinkedBlockingQueue<JSONObject>();
        this.ir_list = new Vector<InteractiveResponse>();
        this.ircUser.initGatewayConnection(this);
        this.ircServer.addUser(this.ircUser);
        this.clientHostname = client_ip;
        this.serverHostname = server_hostname;
        this.ircServer.log(client_ip, 1, "*** Initialized new APIGateway Connection");
        this.lastRequestDelay = 0;
        this.timeout = timeout;
        this.keep_running = true;
        this.token_id = token_id;
        Thread pingPong = new Thread()
        {
            public void run()
            {
                while (APIGatewayConnection.this.keep_running)
                {
                    APIGatewayConnection.this.lastRequestDelay++;
                    try
                    {
                        Thread.sleep(1000);
                    } catch (Exception e) {}
                    if (APIGatewayConnection.this.lastRequestDelay > APIGatewayConnection.this.timeout)
                    {
                        APIGatewayConnection.this.ircUser.disconnect();
                    }
                }
            }
        };
        pingPong.start();
    }
    
    public void run()
    {
        this.ircUser.getIrcServer().log(this.clientHostname, 1, "APIGatewayConnection Thread Exiting!");
    }

    public void connect(String username, String password)
    {
        if (username != null)
        {
            this.ircUser.loginUser(username, password);
            this.ircUser.setClientHost(this.clientHostname);
        }
    }

    public void join(String channel_name)
    {
        IRCMessage imessage = new IRCMessage("JOIN " + channel_name);
        imessage.setSource(this.ircUser);
        this.ircUser.onGatewayCommand(imessage);
    }

    public void part(String channel_name)
    {
        IRCMessage imessage = new IRCMessage("PART " + channel_name);
        imessage.setSource(this.ircUser);
        this.ircUser.onGatewayCommand(imessage);
    }

    public void privmsg(String target, String message)
    {
        IRCMessage imessage = new IRCMessage("PRIVMSG " + target + " :" + message);
        imessage.setSource(this.ircUser);
        this.ircUser.onGatewayCommand(imessage);
    }

    public void handleMessage(IRCMessage imessage)
    {
        imessage.setSource(this.ircUser);
        this.ircUser.onGatewayCommand(imessage);
    }

    public void rawCommand(String raw)
    {
        IRCMessage imessage = new IRCMessage(raw);
        imessage.setSource(this.ircUser);
        this.ircUser.onGatewayCommand(imessage);
    }

    public String getTokenId()
    {
        return this.token_id;
    }
    
    public int getTimeout()
    {
        return this.timeout;
    }

    public int getDelay()
    {
        return this.lastRequestDelay;
    }

    public void resetPingRemaining()
    {
        this.lastRequestDelay = 0;
    }

    public int getPingRemaining()
    {
        return this.timeout - this.lastRequestDelay;
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
        this.keep_running = false;
    }

    public IrcUser getIrcUser()
    {
        return this.ircUser;
    }
    
    public void addInteractiveResponse(InteractiveResponse ir)
    {
        this.ir_list.add(ir);
    }

    public void sendResponse(IRCResponse response)
    {
        try
        {
            this.ircUser.getIrcServer().log(this.clientHostname, 5, "<- " + response.toString());
            if (this.ir_list.size() > 0)
            {
                resetPingRemaining();
                for (Enumeration<InteractiveResponse> e = this.ir_list.elements(); e.hasMoreElements(); )
                {
                    InteractiveResponse ir = e.nextElement();
                    if (ir.isConnected())
                        ir.push(response.toJSONObject());
                    else
                        this.ir_list.remove(ir);
                }
            } else {
                this.message_queue.put(response.toJSONObject());
            }
        } catch (Exception e) {

        }
    }

    public void sendCommand(IRCMessage message)
    {
        try
        {
            this.ircUser.getIrcServer().log(this.clientHostname, 5, "<- " + message.toString());
            if (this.ir_list.size() > 0)
            {
                resetPingRemaining();
                for (Enumeration<InteractiveResponse> e = this.ir_list.elements(); e.hasMoreElements(); )
                {
                    InteractiveResponse ir = e.nextElement();
                    if (ir.isConnected())
                        ir.push(message.toJSONObject());
                    else
                        this.ir_list.remove(ir);
                }
            } else {
                this.message_queue.put(message.toJSONObject());
            }
        } catch (Exception e) {
            System.err.println(e.toString() + " / " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    public JSONObject pollMessageQueue()
    {
        return this.message_queue.poll();
    }

    public JSONArray getMessageQueue()
    {
        JSONArray return_array = new JSONArray();
        Vector<JSONObject> drain_results = new Vector<JSONObject>();
        this.message_queue.drainTo(drain_results);
        for(Enumeration<JSONObject> e = drain_results.elements(); e.hasMoreElements(); )
        {
            JSONObject jo = e.nextElement();
            return_array.put(jo);
        }
        this.message_queue.clear();
        return return_array;
    }
}
