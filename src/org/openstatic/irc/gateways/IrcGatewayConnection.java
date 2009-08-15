package org.openstatic.irc.gateways;

import org.openstatic.irc.GatewayConnection;
import org.openstatic.irc.PreparedCommand;
import org.openstatic.irc.IrcUser;
import org.openstatic.irc.ReceivedCommand;
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
                            PreparedCommand ping = new PreparedCommand("PING");
                            ping.addArg(IrcGatewayConnection.this.serverHostname);
                            IrcGatewayConnection.this.sendCommand(ping);
                        }
                        if (IrcGatewayConnection.this.ping_countdown == -15) // at 15 seconds after we send out a ping kick em
                            IrcGatewayConnection.this.stay_connected = false;
                        try
                        {
                            if (!IrcGatewayConnection.this.stay_connected)
                                IrcGatewayConnection.this.connection.close();
                            Thread.sleep(1000);
                        } catch (Exception ne) {}
                    }
                }
            };
            pingPong.start();
            
            // here we process input from the user
            BufferedReader br = new BufferedReader(new InputStreamReader(this.is));
            String cmd_line;
            try
            {
                while ((cmd_line = br.readLine()) != null)
                {
                    this.ircUser.getIrcServer().logln(this.clientHostname, "-> " + cmd_line);
                    ReceivedCommand cmd = new ReceivedCommand(cmd_line);
                    if (cmd.is("PONG"))
                    {
                        this.ping_countdown = 60;
                    }
                    this.ircUser.onGatewayCommand(cmd);
                }
            } catch (Exception rex) {
            }
        } catch (Exception x) {
        }
        this.ircUser.disconnect();
    }
    
    public void sendResponse(String response, String params)
    {
        socketWrite(":" + this.serverHostname + " " + response + " " + this.ircUser.getNick() + " " + params + "\r\n");
    }
    
    public void sendCommand(PreparedCommand pc)
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
            this.ircUser.getIrcServer().log(this.clientHostname, "<- " + out);
        } catch (Exception we) {}
    }
}