package org.openstatic.irc;

import java.util.Properties;

/* The MiddlewareHandler Interface
   This interface is designed to allow mangling of irc commands
   passed from a client to the server. After the command has been
   modified it should be passed along to ircHandler.onCommand this
   will allow any client-needed packets to be sent out in accordance
   with the command.
*/

public interface MiddlewareHandler
{
    // When a handler recieves and processes a command it needs to know what to do with it next
    // thats where we set the next MiddlewareHandler.
    public void setNextHandler(MiddlewareHandler middlewareHandler);
    
    // All Handlers recieve their messages as an onCommand
    public void onCommand(IRCMessage command);
    
    // just for meta info
    public String getHandlerName();
    public String getHandlerDetails();
    public String getHandlerDescription();
    
    // kind of a hack to find members of a handler
    public IrcUser findMember(String value);
    
    // shutdown the handlers interfaces.
    public void shutdown();
}