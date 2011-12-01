package org.openstatic.irc;

import java.util.Vector;
import java.util.Enumeration;
import org.json.*;

public class IRCResponse
{
    private String response_code;
    private String server_host;
    private String target_nick;
    private Vector<String> args;

    public IRCResponse(String line)
    {
        String[] cmdArray = line.split(" ");
        if (cmdArray[0].startsWith(":"))
        {
            this.server_host = cmdArray[0].substring(1);
            this.response_code = cmdArray[1];
            this.args = doArgs(cmdArray,2);
        } else {
            this.response_code = cmdArray[0];
            this.args = doArgs(cmdArray, 1);
        }
    }

    public IRCResponse(String resp_code, String data)
    {
        this.response_code = resp_code;
        String[] data_array = data.split(" ");
        this.args = doArgs(data_array, 0);
    }

    public IRCResponse(JSONObject job) throws JSONException
    {
        this.args = new Vector<String>();
        this.response_code = job.getString("response");
        if (job.has("data"))
        {
            JSONArray arguments = job.getJSONArray("data");
            for (int a = 0; a < arguments.length(); a++)
            {
                String c_arg = arguments.getString(a);
                this.args.add(c_arg);
            }
        }
    }

    // add arg for command handler recreation
    public void addArg(String value)
    {
        this.args.add(value);
    }
    
    public void clearArgs()
    {
        this.args = new Vector<String>();
    }

    
    // process that funny argument layout
    private Vector<String> doArgs(String[] ary, int start)
    {
        Vector<String> return_vector = new Vector<String>();
        for (int i = start; i < ary.length; i++)
        {
            if (ary[i].startsWith(":"))
            {
                StringBuffer lastarg = new StringBuffer(ary[i].substring(1));
                i++;
                while (i < ary.length)
                {
                    lastarg.append(" " + ary[i]);
                    i++;
                }
                return_vector.add(lastarg.toString());
            } else {
                return_vector.add(ary[i]);
            }
        }
        return return_vector;
    }

    // check to see if a command matches a string
    public boolean is(String text)
    {
        if (this.response_code.equals(text))
        {
            return true;
        } else {
            return false;
        }
    }

    // retrieve the arguments by index to a command
    public String getArg(int index)
    {
        try
        {
            return this.args.elementAt(index);
        } catch (Exception e) {
            return null;
        }
    }
    
    public Vector<String> getArgs()
    {
        return this.args;
    }

    public String getResponseCode()
    {
        return this.response_code;
    }

    public String getData()
    {
        StringBuffer args_string = new StringBuffer();
        for (Enumeration<String> e = args.elements(); e.hasMoreElements(); )
        {
            String ca = e.nextElement();
            if (ca.contains(" "))
            {
                ca = ":" + ca;
            }
            args_string.append(ca);
            if (e.hasMoreElements())
            {
                args_string.append(" ");
            }
        }
        return args_string.toString();
    }

    public JSONObject toJSONObject()
    {
        JSONObject job = new JSONObject();
        try
        {
            job.put("response", this.response_code);
            job.put("data", this.args);
        } catch (Exception e) {}
        return job;
    }

    public String toString()
    {

        return this.response_code + " " + this.getData();
    }

}
