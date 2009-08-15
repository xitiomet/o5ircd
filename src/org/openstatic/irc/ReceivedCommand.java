package org.openstatic.irc;

import java.util.Vector;
import java.util.Enumeration;

public class ReceivedCommand
{
    protected String cmd;
    protected String source;
    private boolean arg_end;
    protected Vector<String> args;
    protected Vector<String> destination;
    
    // constructor
    public ReceivedCommand(String line)
    {
        this.args = new Vector<String>();
        this.destination = null;
        String[] cmdArray = line.split(" ");
        if (cmdArray[0].startsWith(":"))
        {
            this.source = cmdArray[0].substring(1);
            this.cmd = cmdArray[1];
            this.args = doArgs(cmdArray,2);
        } else {
            this.cmd = cmdArray[0];
            this.args = doArgs(cmdArray,1);
        }
    }
    
    public ReceivedCommand(String cmd, String source)
    {
        this.cmd = cmd;
        this.args = new Vector<String>();
        this.arg_end = false;
        this.source = source;
        this.destination = null;
    }

    public ReceivedCommand(String cmd, IrcUser source)
    {
        this.cmd = cmd;
        this.args = new Vector<String>();
        this.arg_end = false;
        this.source = source.toString();
        this.destination = null;
    }
    
    // add arg for command handler recreation
    public void addArg(String value)
    {
        if (!arg_end)
        {
            if (value.contains(" "))
            {
                this.args.add(":" + value);
                this.arg_end = true;
            } else {
                this.args.add(value);
            }
        }
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
    
    // return the source part of a line
    public String getSource()
    {
        return this.source;
    }
    
    public void setSource(String value)
    {
        this.source = value;
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
    
    // return the command like JOIN, PART, etc
    public String getCommand()
    {
        return this.cmd;
    }
    
    // The Echo Response is for some commands that need to be echoed back to the client
    public String getEchoResponse()
    {
        return this.toString();
    }
    
    public String toString()
    {
        StringBuffer args_string = new StringBuffer("");
        for (Enumeration<String> e = args.elements(); e.hasMoreElements(); )
        {
            args_string.append(e.nextElement());
            if (e.hasMoreElements())
            {
                args_string.append(" ");
            }
        }
        return this.cmd + " " + args_string.toString();
    }
    
    // check to see if a command matches a string
    public boolean is(String text)
    {
        if (this.cmd.equals(text))
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
    
    public int argCount()
    {
        return this.args.size();
    }
    
}