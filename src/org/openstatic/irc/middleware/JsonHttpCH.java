package org.openstatic.irc.middleware;

import org.openstatic.irc.IrcUser;
import org.openstatic.irc.MiddlewareHandler;
import org.openstatic.irc.ReceivedCommand;
import org.json.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

public class JsonHttpCH implements MiddlewareHandler
{
    private URL url;
    private int polling_time;
    private int polling_cycle;
    private MiddlewareHandler middlewareHandler;

    public JsonHttpCH(URL url, int polling_cycle)
    {
        this.url = url;
        this.polling_time = 0;
        this.polling_cycle = polling_cycle;
        this.middlewareHandler = null;
        if (polling_cycle > 0)
        {
            Thread polling_thread = new Thread()
            {
                public void run()
                {
                    while (true)
                    {
                        if (JsonHttpCH.this.polling_time >= JsonHttpCH.this.polling_cycle && JsonHttpCH.this.middlewareHandler != null)
                        {
                            JsonHttpCH.this.polling_time = 0;
                            JsonHttpCH.this.poll();
                        }
                        try
                        {
                            Thread.sleep(1000);
                            JsonHttpCH.this.polling_time++;
                        } catch (Exception e) {}
                    }
                }
            };
            polling_thread.start();
        }
    }

    public void poll()
    {
        try
        {
            URLConnection conn = this.url.openConnection();
            String data = URLEncoder.encode("poll", "UTF-8") + "=" + URLEncoder.encode("true", "UTF-8");
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            wr.close();
            processResponse(conn);
        } catch (Exception e) {
            System.err.println("+JsonHttpCH(poll) : " + e.toString() + " / " + e.getMessage());
            e.printStackTrace(System.err);            
        }
    }
    
    public void onCommand(ReceivedCommand command, MiddlewareHandler middlewareHandler)
    {
        this.middlewareHandler = middlewareHandler;
        try
        {
            URLConnection conn = this.url.openConnection();
            JSONObject job = new JSONObject();
            IrcUser user = this.findMember(command.getSource());
            
            String cmd_line = ":" + command.getSource() + " " + command.toString();
            
            job.put("source", command.getSource());
            job.put("command", command.getCommand());
            job.put("args", command.getArgs());
            
            JSONObject meta = new JSONObject();
            
            meta.put("raw_irc", cmd_line);
            
            if (user != null)
            {
                JSONObject j_user = new JSONObject();
                j_user.put("nick", user.getNick());
                j_user.put("realname", user.getRealName());
                j_user.put("hostname", user.getClientHostname());
                j_user.put("username", user.getUserName());
                meta.put("user", j_user);
            }
            
            job.put("meta", meta);
            
            String data = URLEncoder.encode("json", "UTF-8") + "=" + URLEncoder.encode(job.toString(), "UTF-8");
            
            conn.setDoOutput(true);
            
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            wr.close();
            
            processResponse(conn);
        } catch (Exception e) {
            System.err.println("+JsonHttpCH : " + e.toString() + " / " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }
    
    private void processResponse(URLConnection conn) throws Exception
    {
        try
        {
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            StringBuffer response = new StringBuffer("");
            while ((line = br.readLine()) != null)
            {
                response.append(line);
            }
            br.close();
            
            // process the json callback array
            JSONArray callback = new JSONArray(response.toString());
            for (int n = 0; n < callback.length(); n++)
            {
                JSONObject cmd = callback.getJSONObject(n);
                JSONArray args = cmd.getJSONArray("args");
                StringBuffer s_args = new StringBuffer("");
                if (args != null)
                {
                    for (int a = 0; a < args.length(); a++)
                    {
                        String c_arg = args.getString(a);
                        if (c_arg.contains(" "))
                        {
                            c_arg = ":" + c_arg;
                        }
                        s_args.append(" " + c_arg);
                    }
                }
                String raw_irc = ":" + cmd.getString("source") + " " + cmd.getString("command") + s_args.toString();
                
                ReceivedCommand rc = new ReceivedCommand(raw_irc);
                JSONArray dest = null;
                try
                {   
                    dest = cmd.getJSONArray("destination");
                } catch (Exception nfos) {}
                if (dest != null)
                {
                    for (int da = 0; da < dest.length(); da++)
                    {
                        rc.addDestination(dest.getString(da));
                    }
                }
                middlewareHandler.onCommand(rc, this);
            }
        } catch (Exception x) {}
    }
    
    public IrcUser findMember(String value)
    {
        if (this.middlewareHandler != null)
        {
            return this.middlewareHandler.findMember(value);
        } else {
            return null;
        }
    }
    
    public String getHandlerName()
    {
        return "org.openstatic.irc.middleware.JsonHttpCH";
    }

    public String getHandlerDescription()
    {
        return "OpenStatic.org JSON/HTTP Middleware";
    }
    
    public String getHandlerDetails()
    {
        return this.url.toString();
    }

}