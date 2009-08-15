package org.openstatic.irc;

public interface Gateway
{
    public boolean initGateway(IrcServer ircServer);
    public void shutdownGateway();
    public IrcServer getIrcServer();
}