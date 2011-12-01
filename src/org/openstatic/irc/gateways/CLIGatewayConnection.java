package org.openstatic.irc.gateways;

import org.openstatic.irc.GatewayConnection;
import org.openstatic.irc.IrcUser;
import org.openstatic.irc.IrcChannel;
import org.openstatic.irc.IRCMessage;
import org.openstatic.irc.IRCResponse;
import org.openstatic.irc.IrcServer;
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.File;
import java.util.Properties;
import java.util.Vector;
import java.util.Enumeration;

public class CLIGatewayConnection extends Thread implements GatewayConnection
{
    private InputStream is;
    private OutputStream os;
    private Socket connection;
    private String serverHostname;
    private String clientHostname;
    private IrcServer ircServer;
    private boolean stay_connected;
    private BufferedReader br;

    
    // Sloppy function for sending messages from the CLI
    public void findAndSend(String command,  String source, String target, String message)
    {
        IRCMessage cmd = new IRCMessage(command, source);
        cmd.addArg(target);
        cmd.addArg(message);
        
        if (target.startsWith("#") || target.startsWith("&") || target.startsWith("!") || target.startsWith("+"))
        {
            IrcChannel possible_target = this.ircServer.findChannel(target);
            if (possible_target != null)
            {
                possible_target.getHandler().onCommand(cmd);
            } else {
                this.println("No such channel: " + target);
            }
        } else {
            IrcUser possible_target2 = this.ircServer.findUser(target);
            if (possible_target2 != null)
            {
                if (possible_target2.getAway() != null)
                {
                    this.println(possible_target2.getNick() + " :" + possible_target2.getAway());
                }
                possible_target2.sendCommand(cmd);
            } else {
                this.println("No such nick: " + target);
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
        this.socketWrite("\033[2K\033[6D");
    }
    
    public void showPrompt()
    {
        this.socketWrite("@irc >");
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
                    
                    if (cmd_ary[0].equals("shutdown"))
                    {
                        this.ircServer.shutdown();
                    }

                    if (cmd_ary[0].equals("exit") || cmd_ary[0].equals("quit"))
                    {
                        this.close();
                    }
                    
                    if (cmd_ary[0].equals("help"))
                    {
                        this.println("Openstatic IRC Server CLI Help");
                        this.println("--------------------------------------------------------------");
                        this.println("load [filename]                    Load settings file");
                        this.println("channels                           Display all channels");
                        this.println("users                              Display all users");
                        this.println("notice [target] [msg]              Send a notice to target");
                        this.println("msg [target] [msg]                 Send a privmsg to target");
                        this.println("nick [target] [new nick]           Change somebodys nickname");
                        this.println("drop [target]                      Remove target channel/user from server");
                        this.println("debug [0-10]                       Set the verbosity");
                        this.println("motd                               Show the message of the day");
                        this.println("inject [cmd] [src] [target] [msg]  Inject Irc Packet");
                        this.println("shutdown                           Shutdown the Irc Server");
                        this.println("exit                               Exit the CLI");
                        this.println("");
                        this.println("[cmd] = An IRC Command");
                        this.println("[target] = A channel or user");
                        this.println("[src] = A user source (example: nick!user@host)");
                        this.println("[msg] = A String representing the command body");
                    }
                    
                    if (cmd_ary[0].equals("debug") && cmd_ary.length == 2)
                    {
                        this.ircServer.setDebug(Integer.valueOf(cmd_ary[1]).intValue());
                        clearPrompt();
                        this.println("Debug Level Changed to " + cmd_ary[1]);
                    }
                    
                    if (cmd_ary[0].equals("channel") && cmd_ary.length == 2)
                    {
                        if (cmd_ary[1].startsWith("#") || cmd_ary[1].startsWith("&") || cmd_ary[1].startsWith("!") || cmd_ary[1].startsWith("+"))
                        {
                            IrcChannel chan = new IrcChannel(cmd_ary[1]);
                            this.ircServer.addChannel(chan);
                            clearPrompt();
                            this.println("Created Channel \"" + chan.toString() + "\"");
                        } else {
                            this.println("Channels must start with #, &, !, or +");
                        }
                    }
                    
                    if (cmd_ary[0].equals("load") && cmd_ary.length == 2)
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

                    if (cmd_ary[0].equals("drop") && cmd_ary.length == 2)
                    {
                        if (cmd_ary[1].startsWith("#") || cmd_ary[1].startsWith("&") || cmd_ary[1].startsWith("!") || cmd_ary[1].startsWith("+"))
                        {
                            IrcChannel possible_target = this.ircServer.findChannel(cmd_ary[1]);
                            if (possible_target != null)
                            {
                                possible_target.shutdown();
                                this.ircServer.removeChannel(possible_target);
                            } else {
                                this.println("No such channel: " + cmd_ary[1]);
                            }
                        } else {
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
                    }

                    if (cmd_ary[0].equals("nick") && cmd_ary.length == 3)
                    {
                        if (cmd_ary[1].startsWith("#") || cmd_ary[1].startsWith("&") || cmd_ary[1].startsWith("!") || cmd_ary[1].startsWith("+"))
                        {
                            this.println("Sorry this command only works on users");
                        } else {
                            IrcUser u = this.ircServer.findUser(cmd_ary[1]);
                            if (u != null)
                            {
                                IRCMessage cmd = new IRCMessage("NICK", u);
                                cmd.addArg(cmd_ary[2]);
                                u.onGatewayCommand(cmd);
                                clearPrompt();
                                this.println("Nick Changed " + u.toString());
                            } else {
                                clearPrompt();
                                this.println("Could not find" + cmd_ary[1]);
                            }
                        }
                    }
                    
                    if (cmd_ary[0].equals("motd"))
                    {
                        
                        Vector<String> motd = this.ircServer.getMotd();
                        if (motd != null)
                        {
                            for(Enumeration<String> m = motd.elements(); m.hasMoreElements(); )
                            {
                                this.println(m.nextElement());
                            }
                        }
                    }
                    
                    if (cmd_ary[0].equals("users"))
                    {
                        
                        Vector<IrcUser> users = this.ircServer.getUsers();
                        for(Enumeration<IrcUser> u = users.elements(); u.hasMoreElements(); )
                        {
                            IrcUser x = u.nextElement();
                            String away_msg = "";
                            if (x.getAway() != null)
                                away_msg = x.getAway();
                            this.println(x.toString() + " (" + x.getRealName() + ") : " + away_msg);
                        }
                    }
                    
                    if (cmd_ary[0].equals("channels"))
                    {
                        
                        Vector<IrcChannel> channels = this.ircServer.getChannels();
                        for(Enumeration<IrcChannel> c = channels.elements(); c.hasMoreElements(); )
                        {
                            IrcChannel x = c.nextElement();
                            this.println(x.toString() + " " + String.valueOf(x.getMemberCount()) + " " + x.getTopic());
                        }
                    }
                    
                    if (cmd_ary[0].equals("notice") && cmd_ary.length >= 3)
                    {
                        findAndSend("NOTICE",  "CLI", cmd_ary[1], string_join(cmd_ary, 2, cmd_ary.length));
                    }
                    
                    if (cmd_ary[0].equals("msg") && cmd_ary.length >= 3)
                    {
                        findAndSend("PRIVMSG", "CLI", cmd_ary[1], string_join(cmd_ary, 2, cmd_ary.length));
                    }

                    if (cmd_ary[0].equals("inject") && cmd_ary.length >= 5)
                    {
                        findAndSend(cmd_ary[1].toUpperCase(), cmd_ary[2], cmd_ary[3], string_join(cmd_ary, 4, cmd_ary.length));
                    }
                    
                    if (cmd_ary[0].equals("privmsg") && cmd_ary.length >= 4)
                    {
                        findAndSend("PRIVMSG",  cmd_ary[1], cmd_ary[2], string_join(cmd_ary, 3, cmd_ary.length));
                    }

                }
            } while (cmd_line != null && this.stay_connected);
            this.connection.close();
        } catch (Exception rex) {
            this.println(rex.toString() + " / " + rex.getMessage());
        }

    }
    
    public void sendResponse(IRCResponse response)
    {
        socketWrite(response.getResponseCode() + " : " + response.getData() + "\r\n");
    }
    
    public void sendCommand(IRCMessage message)
    {
        String out = null;
        if (message.getSource() != null)
        {
            out = ":" + message.getSource() + " " + message.toString() + "\r\n";
        } else {
            out = ":" + this.serverHostname + " " + message.toString() + "\r\n";
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
    
    public OutputStream getOutputStream()
    {
        return this.os;
    }

    public void println(String out)
    {
        socketWrite(out + "\n");
    }
}
