package org.openstatic.irc;

public interface IRCMessageListener
{
    public void onIRCMessage(IRCMessage message, Object source);
    public void onIRCResponse(IRCResponse response, Object source);
}
