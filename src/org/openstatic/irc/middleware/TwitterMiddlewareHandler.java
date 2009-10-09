package org.openstatic.irc.middleware;

import org.openstatic.irc.MiddlewareHandler;
import org.openstatic.irc.ReceivedCommand;
import org.openstatic.irc.IrcUser;
import org.openstatic.Base64Coder;
import org.json.*;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class TwitterMiddlewareHandler implements MiddlewareHandler
{
    private MiddlewareHandler middlewareHandler;
    private String privmsg_to;
    private String twitter_username;
    private String twitter_password;
    private String twitter_topic;
    private Thread read_twitter;
    
    public TwitterMiddlewareHandler(String twitter_topic, String privmsg_to, String twitter_username, String twitter_password)
    {
        
        this.middlewareHandler = null;
        this.privmsg_to = privmsg_to;
        this.twitter_topic = twitter_topic;
        this.twitter_username = twitter_username;
        this.twitter_password = twitter_password;
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
    
    public void onCommand(ReceivedCommand command, MiddlewareHandler middlewareHandler)
    {
        middlewareHandler.onCommand(command, this);
        if (this.middlewareHandler == null)
        {
            this.middlewareHandler = middlewareHandler;
            read_twitter = new Thread()
            {
                public void run()
                {
                    try
                    {
                        String url = "http://stream.twitter.com/1/statuses/filter.json?track=" + URLEncoder.encode(TwitterMiddlewareHandler.this.twitter_topic, "UTF-8");
                        HttpURLConnection twitter_connection = authenticate(url, TwitterMiddlewareHandler.this.twitter_username, TwitterMiddlewareHandler.this.twitter_password);
                        BufferedReader bread = new BufferedReader(new InputStreamReader(twitter_connection.getInputStream()));
                        String newLine = null;
                        do
                        {
                            newLine = bread.readLine();
                            try
                            {
                                JSONObject tweet = new JSONObject(newLine);
                                String message = tweet.getString("text");
                                JSONObject user = tweet.getJSONObject("user");
                                String username = user.getString("screen_name");
                                String raw_irc = ":" + username + "!" + username + "@twitter.com PRIVMSG " + TwitterMiddlewareHandler.this.privmsg_to + " :" + message;
                                ReceivedCommand rc = new ReceivedCommand(raw_irc);
                                TwitterMiddlewareHandler.this.middlewareHandler.onCommand(rc, TwitterMiddlewareHandler.this);
                            } catch (Exception ve) {}
                        } while (newLine != null);
                        bread.close();
                        
                    } catch (Exception nex) {
                        System.err.println("Twitter Middleware Failed: " + nex.getMessage());
                    }
                }
            };
            read_twitter.start();
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
    
    public String getHandlerName()
    {
        return "org.openstatic.irc.middleware.TwitterMiddlewareHandler";
    }
    
    public String getHandlerDescription()
    {
        return "OpenStatic.org Twitter IRC Middleware";
    }
    
    public String getHandlerDetails()
    {
        return null;
    }
}