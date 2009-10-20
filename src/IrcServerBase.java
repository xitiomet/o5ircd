import org.openstatic.irc.IrcServer;
import org.openstatic.irc.IrcUser;
import org.openstatic.irc.IrcChannel;
import org.openstatic.irc.ReceivedCommand;
import org.openstatic.irc.PreparedCommand;
import org.openstatic.irc.gateways.IrcGateway;
import org.openstatic.irc.gateways.WebAdminGateway;
import java.io.File;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.util.Properties;

//import org.openstatic.irc.middleware.DefaultMiddlewareHandler;

public class IrcServerBase
{
    public static void help()
    {
        System.err.println("Openstatic.org IRC Server");
        System.err.println("");
        System.err.println("  --debug                        Turn debugging output on.");
        System.err.println("  --irc-port [port]              Specify IRC listening port");
        System.err.println("  --web [port]                   Start web administration on given port");
        System.err.println("  --complex-chan [ini]           Create a channel from an ini file");
        System.err.println("  --chan [channel name]          create chanel (can be used multiple times)");
        System.err.println("  --motd [file]                  specify the motd filename");
        System.err.println("  --help                         display this menu");
        System.err.println("");
        System.err.println("");
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
    
    // Sloppy function for sending messages from the CLI
    public static void findAndSend(String command, IrcServer irc,  String source, String target, String message)
    {
        ReceivedCommand cmd = new ReceivedCommand(command, source);
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
                possible_target2.sendCommand(new PreparedCommand(cmd));
            } else {
                System.out.println("No such nick: " + target);
            }
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
    
    public static void main(String[] args) throws Exception
    {
        IrcServer irc = new IrcServer();
        
        if (args.length == 0)
        {
            help();
        } else {
            System.out.println("Openstatic.org IRC Server");
            System.out.println("-------------------------");
            boolean join_thread = true;
            boolean run_cli = false;
            
            for (int i = 0; i < args.length; i++)
            {
                String arg = args[i].toLowerCase();
                String arg_p1 = null;
                String arg_p2 = null;
                if (i + 1 < args.length)
                    arg_p1 = args[i+1];
                if (i + 2 < args.length)
                    arg_p2 = args[i+2];
                
                if (arg.equals("--debug"))
                {
                    irc.setDebug(Integer.valueOf(arg_p1).intValue());
                }
                
                if (arg.equals("--irc-port") && arg_p1 != null)
                {
                    try
                    {
                        int irc_port = Integer.valueOf(arg_p1).intValue();
                        irc.addGateway(new IrcGateway(irc_port));
                    } catch (Exception bi) {}
                }
                
                if (arg.equals("--chan") && arg_p1 != null)
                {
                    IrcChannel chan = new IrcChannel(arg_p1);
                    irc.addChannel(chan);
                }
                
                if (arg.equals("--web") && arg_p1 != null)
                {
                    try
                    {
                        int web_port = Integer.valueOf(arg_p1).intValue();
                        irc.addGateway(new WebAdminGateway(web_port));
                    } catch (Exception wa) {}
                }
                
                if (arg.equals("--complex-chan") && arg_p1 != null)
                {
                    IrcChannel chan = new IrcChannel(loadProperties(arg_p1));
                    irc.addChannel(chan);
                }
                
                if (arg.equals("--motd") && arg_p1 != null)
                {
                    irc.setMotd(new File(arg_p1));
                }
                
                if (arg.equals("--help"))
                {
                    help();
                    System.exit(0);
                }
                
                if (arg.equals("--cli"))
                {
                    run_cli = true;
                    join_thread = false;
                }
                
            }
            irc.start();
            Thread.sleep(1000);
            if (run_cli)
            {
                boolean do_input = true;
                BufferedReader std_in = new BufferedReader(new InputStreamReader(System.in));
                String cl_mode = "@CLI";
                while (do_input)
                {
                    System.out.print("[" + cl_mode + "] >");
                    String cmd_line = std_in.readLine();
                    String[] cmd_ary = cmd_line.split(" ");
                    
                    if (cmd_ary[0].equals("quit"))
                    {
                        do_input = false;
                        irc.shutdown();
                    }
                    
                    if (cmd_ary[0].equals("channel") && cmd_ary.length == 2)
                    {
                        IrcChannel chan = new IrcChannel(cmd_ary[1]);
                        irc.addChannel(chan);
                        System.out.println("Created Channel \"" + chan.toString() + "\"");
                    }
                    
                    if (cmd_ary[0].equals("load"))
                    {
                        Properties l = loadProperties(cmd_ary[1]);
                        if ("channel".equals(l.getProperty("config")))
                        {
                            IrcChannel chan = new IrcChannel(l);
                            irc.addChannel(chan);
                            System.out.println("Created Channel \"" + chan.toString() + "\"");
                        }
                    }
                    
                    if (cmd_ary[0].equals("notice"))
                    {
                        findAndSend("NOTICE", irc, "CLI", cmd_ary[1], string_join(cmd_ary, 2, cmd_ary.length));
                    }
                    
                    if (cmd_ary[0].equals("msg"))
                    {
                        findAndSend("PRIVMSG", irc, "CLI", cmd_ary[1], string_join(cmd_ary, 2, cmd_ary.length));
                    }                    
                }
            }

            if (join_thread)
                irc.join();
        }
    }
}