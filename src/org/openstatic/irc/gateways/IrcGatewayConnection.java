package org.openstatic.irc.gateways;

import org.openstatic.irc.GatewayConnection;
import org.openstatic.irc.IrcUser;
import org.openstatic.irc.IRCMessage;
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class IrcGatewayConnection extends Thread implements GatewayConnection
{
    private InputStream is;
    private OutputStream os;
    private Socket connection;
    private String serverHostname;
    private String clientHostname;
    private int ping_countdown;
    private boolean stay_connected;
    private IrcUser ircUser;
    private BufferedReader br;
    
    public IrcGatewayConnection(Socket connection, IrcUser ircUser)
    {
        this.stay_connected = true;
        try
        {
            this.is = connection.getInputStream();
            this.os = connection.getOutputStream();
        } catch (Exception n) {}
        this.ping_countdown = 60;
        this.connection = connection;
        this.ircUser = ircUser;
        this.serverHostname = this.connection.getLocalAddress().getCanonicalHostName();
        this.clientHostname = this.connection.getInetAddress().getCanonicalHostName();
        ircUser.initGatewayConnection(this);
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
        this.stay_connected = false;
        if (this.isAlive())
        {
            try
            {
                this.is.close();
                this.os.close();
                this.br.close();
                this.connection.close();
            } catch (Exception close_exception) {}
        }
    }
    
    public void run()
    {
        try
        {
            // this little pingPong thread is to make sure we arent wasting sockets.
            Thread pingPong = new Thread()
            {
                public void run()
                {
                    while(IrcGatewayConnection.this.stay_connected)
                    {
                        IrcGatewayConnection.this.ping_countdown--;
                        if (IrcGatewayConnection.this.ping_countdown == 0)
                        {
                            //IRCMessage ping = IRCMessage.prepare("PING");
                            //ping.addArg(IrcGatewayConnection.this.serverHostname);
                            //ping.addArg(IrcGatewayConnection.this.ircUser.getClientHostname());
                            //IrcGatewayConnection.this.sendCommand(ping);
                            IrcGatewayConnection.this.socketWrite("PING :" + IrcGatewayConnection.this.ircUser.getClientHostname() + "\r\n");
                        }
                        if (IrcGatewayConnection.this.ping_countdown == -15) // at 15 seconds after we send out a ping kick em
                        {
                            IrcGatewayConnection.this.ircUser.disconnect();
                            IrcGatewayConnection.this.ircUser.getIrcServer().log(IrcGatewayConnection.this.clientHostname, 1, "Ping Pong Timeout");
                        }
                        try
                        {
                            Thread.sleep(1000);
                        } catch (Exception ne) {}
                    }
                }
            };
            pingPong.start();
        } catch (Exception x) {
            this.ircUser.getIrcServer().log(this.clientHostname, 1, "IrcGatewayConnection PingPongThread Exception: " + x.toString() + " / " + x.getMessage());
            this.ircUser.disconnect();
        }
        try
        {
            // here we process input from the user
            br = new BufferedReader(new InputStreamReader(this.is));
            String cmd_line;
            do
            {
                try
                {
                    cmd_line = br.readLine();
                } catch (Exception n) {
                    cmd_line = null;
                }
                
                if (cmd_line != null)
                {
                    this.ircUser.getIrcServer().log(this.clientHostname, 5, "-> " + cmd_line);
                    IRCMessage cmd = new IRCMessage(cmd_line);
                    if (cmd.is("PONG"))
                    {
                        this.ping_countdown = 60;
                    }
                    if (cmd.is("PING"))
                    {
                        this.socketWrite("PONG " + cmd.getArg(0));
                        this.ping_countdown = 60;
                    }
                    this.ircUser.onGatewayCommand(cmd);
                }
            } while (cmd_line != null && this.stay_connected);
            this.connection.close();
	    this.ircUser.disconnect();
        } catch (Exception rex) {
            this.ircUser.getIrcServer().log(this.clientHostname, 1, "IrcGatewayConnection Exception: " + rex.toString() + " / " + rex.getMessage());
            this.ircUser.disconnect();
        }

        this.ircUser.getIrcServer().log(this.clientHostname, 1, "IrcGatewayConnection Thread Exiting!");
    }
    
    public void sendResponse(String response, String params)
    {
        socketWrite(":" + this.serverHostname + " " + response + " " + this.ircUser.getNick() + " " + params + "\r\n");
    }
    
    public void sendCommand(IRCMessage pc)
    {
        String out = null;
        if (pc.getSource() != null)
        {
            out = ":" + pc.getSource() + " " + pc.toString() + "\r\n";
        } else {
            out = ":" + this.serverHostname + " " + pc.toString() + "\r\n";
        }
        socketWrite(out);
    }
    
    private void socketWrite(String out)
    {
        try
        {
            this.os.write(out.getBytes());
            this.os.flush();
            this.ircUser.getIrcServer().log(this.clientHostname, 5, "<- " + out);
        } catch (Exception we) {}
    }
}
