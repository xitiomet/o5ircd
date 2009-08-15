package org.openstatic.irc.middleware;

import org.openstatic.irc.MiddlewareHandler;
import org.openstatic.irc.ReceivedCommand;
import org.openstatic.irc.IrcUser;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;


public class StreamMiddlewareHandler implements MiddlewareHandler
{
    private InputStream is;
    private PrintStream os;
    private MiddlewareHandler middlewareHandler;
    
    public StreamMiddlewareHandler(InputStream is, OutputStream os)
    {
        this.is = is;
        this.os = new PrintStream(os);
        this.middlewareHandler = null;
        
        final BufferedReader br = new BufferedReader(new InputStreamReader(this.is));
        Thread t = new Thread()
        {
            public void run()
            {
                try
                {
                    String cmd_line;
                    while ((cmd_line = br.readLine()) != null)
                    {
                        ReceivedCommand rc = new ReceivedCommand(cmd_line);
                        if (rc.getSource() != null && StreamMiddlewareHandler.this.middlewareHandler != null)
                        {
                            StreamMiddlewareHandler.this.middlewareHandler.onCommand(rc, StreamMiddlewareHandler.this);
                        }
                    }
                } catch (Exception n) {
                    System.err.println("Exception:" + n.getMessage());
                }
            }
        };
        t.start();
    }
    
    public void onCommand(ReceivedCommand command, MiddlewareHandler middlewareHandler)
    {
        this.os.println(":" + command.getSource() + " " + command.toString());
        if (this.middlewareHandler == null)
        {
            this.middlewareHandler = middlewareHandler;
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
        return "org.openstatic.irc.middleware.StreamMiddlewareHandler";
    }
    
    public String getHandlerDescription()
    {
        return "OpenStatic.org System Stream Middleware";
    }
    
    public String getHandlerDetails()
    {
        return null;
    }

}