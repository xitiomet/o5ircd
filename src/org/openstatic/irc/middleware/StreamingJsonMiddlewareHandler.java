package org.openstatic.irc.middleware;

import org.openstatic.irc.MiddlewareHandler;
import org.openstatic.irc.IRCMessage;
import org.openstatic.irc.IrcUser;
import org.openstatic.Base64Coder;
import org.json.*;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Properties;
import java.util.StringTokenizer;

public class StreamingJsonMiddlewareHandler implements MiddlewareHandler
{
    private MiddlewareHandler middlewareHandler;
    private Properties setup;
    private Thread read_json;
    private boolean keep_running;
    private BufferedReader bread;
    
    public StreamingJsonMiddlewareHandler(Properties setup)
    {
        this.keep_running = true;
        this.setup = setup;
        this.middlewareHandler = null;
        this.bread = null;
        
        read_json = new Thread()
        {
            public void run()
            {
                String privmsg_to = StreamingJsonMiddlewareHandler.this.setup.getProperty("channel_name");
                String username = StreamingJsonMiddlewareHandler.this.setup.getProperty("username");
                String password = StreamingJsonMiddlewareHandler.this.setup.getProperty("password");
                String url = StreamingJsonMiddlewareHandler.this.setup.getProperty("stream_url");

                try
                {
                    HttpURLConnection connection = null;
                    if (username != null && password != null)
                    {
                        connection = authenticate(url, username, password);
                    } else {
                        connection = (HttpURLConnection) (new URL(url)).openConnection();
                    }
                    bread = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String newLine = null;
                    do
                    {
                        try
                        {
                            newLine = bread.readLine();
                            JSONObject json_object = new JSONObject(newLine);
                            
                            String stream_nickname = StreamingJsonMiddlewareHandler.this.resolveField(json_object, "stream_data_nickname");
                            String stream_hostname = StreamingJsonMiddlewareHandler.this.resolveField(json_object, "stream_data_hostname");
                            String stream_username = StreamingJsonMiddlewareHandler.this.resolveField(json_object, "stream_data_username");
                            String stream_message = StreamingJsonMiddlewareHandler.this.resolveField(json_object, "stream_data_message");
                            String raw_irc = ":" + stream_nickname + "!" + stream_username + "@" + stream_hostname + " PRIVMSG " + privmsg_to + " :" + stream_message;
                            IRCMessage rc = new IRCMessage(raw_irc);
                            if (StreamingJsonMiddlewareHandler.this.middlewareHandler != null)
                                StreamingJsonMiddlewareHandler.this.middlewareHandler.onCommand(rc);
                        } catch (Exception ve) {}
                    } while (newLine != null && StreamingJsonMiddlewareHandler.this.keep_running);
                    bread.close();
                    
                } catch (Exception nex) {
                    System.err.println("Streaming JSON Middleware Failed: " + nex.getMessage());
                }
            }
        };
    }
    
    public String resolveField(JSONObject data, String field) throws JSONException
    {
        if (setup.getProperty(field) != null)
        {
            return setup.getProperty(field);
        } else {
            if (setup.getProperty(field + "_JSONPATH") != null) {
                String jsonPath =  setup.getProperty(field + "_JSONPATH");
                StringTokenizer json_path = new StringTokenizer(jsonPath, ".");
                JSONObject co = data;
                String return_var = null;
                while (json_path.hasMoreTokens())
                {
                    String currentToken = json_path.nextToken();
                    if (json_path.hasMoreTokens())
                    {
                        co = co.getJSONObject(currentToken);
                    } else {
                        return_var =  co.getString(currentToken);
                    }
                }
                return return_var;
            } else {
                return null;
            }
        }
    }
    
    public static HttpURLConnection authenticate(String url, String username, String password) throws Exception
    {
        HttpURLConnection c = (HttpURLConnection) (new URL(url)).openConnection();
        String plain_auth = username + ":" + password;
        String auth = Base64Coder.encodeString(plain_auth);
        c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        c.setRequestProperty("Authorization", "Basic " + auth);
        return c;
    }
    
    public void setNextHandler(MiddlewareHandler middlewareHandler)
    {
        this.middlewareHandler = middlewareHandler;
    }
    
    public void onCommand(IRCMessage command)
    {
        if (command.is("PRIVMSG"))
        {
            String privmsg_to = this.setup.getProperty("channel_name");
            if (privmsg_to.equals(command.getArg(0)))
            {
                String username = this.setup.getProperty("username");
                String password = this.setup.getProperty("password");
                String reply_url = this.setup.getProperty("reply_url");
                String reply_message_field = this.setup.getProperty("reply_message_field");

                try
                {
                    HttpURLConnection connection = null;
                    if (username != null && password != null)
                    {
                        connection = authenticate(reply_url, username, password);
                    } else {
                        connection = (HttpURLConnection) (new URL(reply_url)).openConnection();
                    }
                    String data = URLEncoder.encode(reply_message_field, "UTF-8") + "=" + URLEncoder.encode(command.getArg(1), "UTF-8");
                    connection.setDoOutput(true);
                    OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
                    wr.write(data);
                    wr.flush();
                    wr.close();
                    int resp = connection.getResponseCode();
                } catch (Exception n) {}
            }
        } else {
            this.middlewareHandler.onCommand(command);
        }
        
        if (read_json.isAlive() == false)
        {
            read_json.start();
        }
    }
    
    public IrcUser findMember(String value)
    {
        if (this.middlewareHandler != null)
        {
            return this.middlewareHandler.findMember(value);
        } else {
            return null;
        }
    }
    
    public void shutdown()
    {
        this.keep_running = false;
        try
        {
            bread.close();
        } catch (Exception e) {
            //couldnt close buffered reader
        }
    }
    
    public String getHandlerName()
    {
        return "org.openstatic.irc.middleware.StreamingJsonMiddlewareHandler";
    }
    
    public String getHandlerDescription()
    {
        return "OpenStatic.org Streaming JSON IRC Middleware";
    }
    
    public String getHandlerDetails()
    {
        return null;
    }
}