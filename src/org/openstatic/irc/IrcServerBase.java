package org.openstatic.irc;

import org.openstatic.irc.IrcServer;
import org.openstatic.irc.IrcChannel;
import org.openstatic.irc.gateways.IrcGateway;
import org.openstatic.irc.gateways.WebGateway;
import org.openstatic.irc.gateways.CLIGateway;
import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Properties;

//import org.openstatic.irc.middleware.DefaultMiddlewareHandler;

public class IrcServerBase
{
    public static void help()
    {
        System.out.println("Openstatic.org IRC Server 3/2010");
        System.err.println("");
        System.err.println("  --debug                        Turn debugging output on.");
        System.err.println("  --irc [port]                   Specify IRC listening port");
        System.err.println("  --web [port]                   Start web administration on given port");
        System.err.println("  --complex-chan [ini]           Create a channel from an ini file");
        System.err.println("  --chan [channel name]          create chanel (can be used multiple times)");
        System.err.println("  --motd [file]                  specify the motd filename");
        System.err.println("  --cli [port]                   Create CLI on specified port");
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
        ByteArrayOutputStream debug_target = new ByteArrayOutputStream();
        
        if (args.length == 0)
        {
            help();
        } else {
            System.out.println("Openstatic.org IRC Server 3/2010");
            boolean join_thread = true;
            
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
                
                if (arg.equals("--irc") && arg_p1 != null)
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
                        irc.addGateway(new WebGateway(web_port));
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
                    try
                    {
                        int cli_port = Integer.valueOf(arg_p1).intValue();
                        irc.addGateway(new CLIGateway(cli_port, debug_target));
                        irc.setDebugStream(debug_target);
                    } catch (Exception wa) {}
                }
                
            }
            irc.start();
            if (join_thread)
                irc.join();
        }
    }
}