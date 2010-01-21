package org.openstatic.irc.gateways;

import org.openstatic.irc.GatewayConnection;
import org.openstatic.irc.IrcUser;
import org.openstatic.irc.IrcChannel;
import org.openstatic.irc.IRCMessage;
import org.openstatic.irc.IrcServer;
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.File;
import java.util.Properties;

public class CLIGatewayConnection extends Thread implements GatewayConnection
{
    private InputStream is;
    private OutputStream os;
    private Socket connection;
    private String serverHostname;
    private String clientHostname;
    private int ping_countdown;
    private IrcServer ircServer;
    private boolean stay_connected;
    private BufferedReader br;

    
    // Sloppy function for sending messages from the CLI
    public static void findAndSend(String command, IrcServer irc,  String source, String target, String message)
    {
        IRCMessage cmd = new IRCMessage(command, source);
        cmd.addArg(target);
        cmd.addArg(message);
        
        if (target.startsWith("#") || target.startsWith("&") || target.startsWith("!") || target.startsWith("+"))
        {
            IrcChannel possible_target = irc.findChannel(target);
            if (possible_target != null)
            {
                possible_target.getHandler().onCommand(cmd, possible_target);
            } else {
                System.out.println("No such channel: " + target);
            }
        } else {
            IrcUser possible_target2 = irc.findUser(target);
            if (possible_target2 != null)
            {
                if (possible_target2.getAway() != null)
                {
                    System.out.println(possible_target2.getNick() + " :" + possible_target2.getAway());
                }
                possible_target2.sendCommand(cmd);
            } else {
                System.out.println("No such nick: " + target);
            }
        }
    }
    
    public static Properties loadProperties(String filename)
    {
        try
        {
            File load_file = new File(filename);
            Properties props = new Properties();
            FileInputStream fis = new FileInputStream(load_file);
            props.load(fis);
            fis.close();
            return props;
        } catch (Exception e) {
            return new Properties();
        }
    }
    
    // Sloppy join function
    public static String string_join(String[] ary, int from, int to)
    {
        StringBuffer return_string = new StringBuffer("");
        for (int i = from; i < to; i++)
        {
            return_string.append(ary[i]);
            if (i  < to)
            {
                return_string.append(" ");
            }
        }
        return return_string.toString();
    }    
    
    public CLIGatewayConnection(Socket connection, IrcServer ircServer)
    {
        this.stay_connected = true;
        try
        {
            this.is = connection.getInputStream();
            this.os = connection.getOutputStream();
        } catch (Exception n) {}
        this.ircServer = ircServer;
        this.ping_countdown = 60;
        this.connection = connection;
        this.serverHostname = this.connection.getLocalAddress().getCanonicalHostName();
        this.clientHostname = this.connection.getInetAddress().getCanonicalHostName();
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
    
    public void clearPrompt()
    {
        this.socketWrite("\033[A\033[2K");
    }
    
    public void showPrompt()
    {
        this.socketWrite("[@CLI] >");
    }
    
    public void run()
    {
        try
        {
            // here we process input from the user
            br = new BufferedReader(new InputStreamReader(this.is));
            String cmd_line;
            do
            {
                
                try
                {
                    showPrompt();
                    cmd_line = br.readLine();
                } catch (Exception n) {
                    cmd_line = null;
                }
                
                if (cmd_line != null)
                {
                    String[] cmd_ary = cmd_line.split(" ");
                    
                    if (cmd_ary[0].equals("quit"))
                    {
                        this.ircServer.shutdown();
                    }
                    
                    if (cmd_ary[0].equals("channel") && cmd_ary.length == 2)
                    {
                        IrcChannel chan = new IrcChannel(cmd_ary[1]);
                        this.ircServer.addChannel(chan);
                        clearPrompt();
                        this.println("Created Channel \"" + chan.toString() + "\"");
                    }
                    
                    if (cmd_ary[0].equals("load"))
                    {
                        Properties l = loadProperties(cmd_ary[1]);
                        if ("channel".equals(l.getProperty("config")))
                        {
                            IrcChannel chan = new IrcChannel(l);
                            this.ircServer.addChannel(chan);
                            clearPrompt();
                            this.println("Created Channel \"" + chan.toString() + "\"");
                        }
                    }

                    if (cmd_ary[0].equals("drop"))
                    {
                        IrcUser u = this.ircServer.findUser(cmd_ary[1]);
                        if (u != null)
                        {
                            u.disconnect();
                            clearPrompt();
                            this.println("Dropped Connection " + u.toString());
                        } else {
                            clearPrompt();
                            this.println("Could not find" + cmd_ary[1]);
                        }
                    }
                            
                    if (cmd_ary[0].equals("notice"))
                    {
                        findAndSend("NOTICE", this.ircServer, "CLI", cmd_ary[1], string_join(cmd_ary, 2, cmd_ary.length));
                    }
                    
                    if (cmd_ary[0].equals("msg"))
                    {
                        findAndSend("PRIVMSG", this.ircServer, "CLI", cmd_ary[1], string_join(cmd_ary, 2, cmd_ary.length));
                    }                    

                }
            } while (cmd_line != null && this.stay_connected);
            this.connection.close();
        } catch (Exception rex) {
            System.err.println(rex.toString() + " / " + rex.getMessage());
            rex.printStackTrace(System.err);
        }

    }
    
    public void sendResponse(String response, String params)
    {
        socketWrite(response + " : " + params + "\r\n");
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
    
    public void socketWrite(String out)
    {
        try
        {
            this.os.write(out.getBytes());
            this.os.flush();
        } catch (Exception we) {}
    }

    public void println(String out)
    {
        socketWrite(out + "\n");
    }
}