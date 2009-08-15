package org.openstatic.irc;

import java.util.Vector;
import java.util.Enumeration;

public class PreparedCommand
{
    private String cmd;
    private Vector<String> args;
    private boolean arg_end;
    private String source;
    private Vector<String> destination;
    
    public PreparedCommand(String cmd)
    {
        this.cmd = cmd;
        this.args = new Vector<String>();
        this.arg_end = false;
        this.source = null;
        this.destination = null;
    }
    
    public PreparedCommand(String cmd, String source, Vector<String> args)
    {
        this.cmd = cmd;
        this.source = source;
        this.args = args;
        this.destination = null;
    }
    
    public PreparedCommand(ReceivedCommand rc, String source)
    {
        this.cmd = rc.cmd;
        this.args = rc.args;
        this.arg_end = true;
        this.source = source;
        this.destination = rc.destination;
    }

    public PreparedCommand(ReceivedCommand rc, IrcUser u)
    {
        this.cmd = rc.cmd;
        this.args = rc.args;
        this.arg_end = true;
        this.source = u.toString();
        this.destination = rc.destination;
    }
    
    public PreparedCommand(ReceivedCommand rc)
    {
        this.cmd = rc.cmd;
        this.args = rc.args;
        this.arg_end = true;
        this.source = rc.getSource();
        this.destination = rc.destination;
    }
    
    public void addArg(String value)
    {
        if (!arg_end)
        {
            if (value.contains(" "))
            {
                this.addLastArg(value);
            } else {
                this.args.add(value);
            }
        }
    }
    
    public void clearArgs()
    {
        this.args = new Vector<String>();
        this.arg_end = false;
    }
    
    public void addLastArg(String value)
    {
        this.args.add(value);
        this.arg_end = true;
    }
    
    public void setSource(IrcUser value)
    {
        this.source = value.toString();
    }
    
    public void setSource(String value)
    {
        this.source = value;
    }
    
    public String getSource()
    {
        return this.source;
    }
    
    public void addDestination(IrcUser value)
    {
        if (this.destination == null)
        {
            this.destination = new Vector<String>();
        }
        this.destination.add(value.toString());
    }
    
    public void addDestination(String value)
    {
        if (this.destination == null)
        {
            this.destination = new Vector<String>();
        }
        this.destination.add(value);
    }
    
    public void clearDestination()
    {
        this.destination = null;
    }
    
    public boolean inDestination(String value)
    {
        boolean ret_bool = false;
        if (this.destination != null)
        {
            for (Enumeration<String> e = this.destination.elements(); e.hasMoreElements(); )
            {
                if (e.nextElement().equals(value))
                {
                    ret_bool = true;
                }
            }
        } else {
            ret_bool = true;
        }
        return ret_bool;
    }

    public boolean inDestination(IrcUser value)
    {
        boolean ret_bool = false;
        if (this.destination != null)
        {
            for (Enumeration<String> e = this.destination.elements(); e.hasMoreElements(); )
            {
                if (value.is(e.nextElement()))
                {
                    ret_bool = true;
                }
            }
        } else {
            ret_bool = true;
        }
        return ret_bool;
    }
    
    public String getDestination()
    {
        if (this.destination != null)
        {
            StringBuffer destinations = new StringBuffer("");
            for (Enumeration<String> e = this.destination.elements(); e.hasMoreElements(); )
            {
                destinations.append(e.nextElement());
                if (e.hasMoreElements())
                {
                    destinations.append(",");
                }
            }
            return destinations.toString();
        } else {
            return null;
        }
    }
    
    public String toString()
    {
        StringBuffer args_string = new StringBuffer("");
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
        return this.cmd + " " + args_string.toString();
    }
    
}