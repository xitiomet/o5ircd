package org.openstatic.irc.middleware;

import org.openstatic.irc.MiddlewareHandler;
import org.openstatic.irc.IRCMessage;
import org.openstatic.irc.IrcUser;

public class DefaultMiddlewareHandler implements MiddlewareHandler
{
    private MiddlewareHandler middlewareHandler;
    
    public DefaultMiddlewareHandler()
    {
        this.middlewareHandler = null;
    }
    
    public void onCommand(IRCMessage command, MiddlewareHandler middlewareHandler)
    {
        this.middlewareHandler = middlewareHandler;
        middlewareHandler.onCommand(command, this);
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
        // good for you!
    }
    
    public String getHandlerName()
    {
        return "org.openstatic.irc.middleware.DefaultMiddlewareHandler";
    }
    
    public String getHandlerDescription()
    {
        return "OpenStatic.org Standard IRC Middleware";
    }
    
    public String getHandlerDetails()
    {
        return null;
    }
}