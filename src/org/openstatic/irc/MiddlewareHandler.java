package org.openstatic.irc;

/* The MiddlewareHandler Interface
   This interface is designed to allow mangling of irc commands
   passed from a client to the server. After the command has been
   modified it should be passed along to ircHandler.onCommand this
   will allow any client-needed packets to be sent out in accordance
   with the command.
*/

public interface MiddlewareHandler
{
    public void onCommand(ReceivedCommand command, MiddlewareHandler middlewareHandler);
    public String getHandlerName();
    public String getHandlerDetails();
    public String getHandlerDescription();
    public IrcUser findMember(String value);
    public void shutdown();
}