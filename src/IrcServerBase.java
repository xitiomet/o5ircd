import org.openstatic.irc.IrcServer;
import org.openstatic.irc.IrcChannel;
import org.openstatic.irc.middleware.JsonHttpCH;
import org.openstatic.irc.gateways.IrcGateway;
import java.io.File;
import java.net.URL;
//import org.openstatic.irc.middleware.DefaultMiddlewareHandler;

public class IrcServerBase
{
    public static void main(String[] args) throws Exception
    {
        IrcServer irc = new IrcServer();
        
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
                irc.setDebug(true);
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
            
            if (arg.equals("--json-chan") && arg_p1 != null && arg_p2 != null)
            {
                IrcChannel chan = new IrcChannel(arg_p1, new JsonHttpCH(new URL(arg_p2), 10));
                irc.addChannel(chan);
            }
            
            if (arg.equals("--motd") && arg_p1 != null)
            {
                irc.setMotd(new File(arg_p1));
            }
            
        }
        
        irc.start();
        irc.join();
    }
}