package org.openstatic.irc;

import java.net.InetAddress;
import java.util.Date;
import java.util.Vector;
import java.util.Enumeration;
import java.io.PrintStream;
import java.io.OutputStream;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;

public class IrcServer extends Thread
{
    private Vector<IrcChannel> rooms;
    private Vector<IrcUser> users;
    private Vector<Gateway> gateways;
    private PrintStream debug_stream;
    private int debug_mode;
    private boolean keep_running;
    private int minute_tracker;
    private Vector<String> motd;
    private long boot_time;
    private String server_name;
    
    public IrcServer()
    {
        this.rooms = new Vector<IrcChannel>();
        this.users = new Vector<IrcUser>();
        this.gateways = new Vector<Gateway>();
        this.debug_mode = 0;
        this.debug_stream = System.err;
        this.keep_running = true;
        this.motd = null;
        this.boot_time = 0;
        try
        {
            InetAddress addr = InetAddress.getLocalHost();
            this.server_name = addr.getHostName();
        } catch (Exception e) {
            this.server_name = "localhost";
        }
    }
    
    public void run()
    {
        this.boot_time = System.currentTimeMillis();
        for(Enumeration<Gateway> e = gateways.elements(); e.hasMoreElements(); )
        {
            Gateway gate = e.nextElement();
            gate.initGateway(this);
        }
        log(this.server_name, 1, "Completed Init Gateway");
        while (this.keep_running)
        {
            try
            {
                minute_tracker++;
                if (minute_tracker == 60)
                {
                    minute_tracker = 0;
                    System.gc();
                }
                Thread.sleep(1000);
            } catch (Exception z) {}
        }
        log(this.server_name, 1, "Full Shutdown, killing gateways");
        for(Enumeration<Gateway> e2 = gateways.elements(); e2.hasMoreElements(); )
        {
            Gateway gate = e2.nextElement();
            gate.shutdownGateway();
        }
        log(this.server_name, 1, "Gatway Shutdown Complete!");
        
        log(this.server_name, 1, "Killing User Connections");
        for(Enumeration<IrcUser> e3 = users.elements(); e3.hasMoreElements(); )
        {
            IrcUser cu = e3.nextElement();
            cu.disconnect();
        }
        
        log(this.server_name, 1, "Detaching Channels and Shutting Down Middleware");
        for(Enumeration<IrcChannel> e4 = rooms.elements(); e4.hasMoreElements(); )
        {
            IrcChannel cc = e4.nextElement();
            cc.shutdown();
        }
    }
    
    public String getBootTime()
    {
        return (new Date(this.boot_time)).toString();
    }
    
    public void addGateway(Gateway g)
    {
        log(this.server_name, 1, "Gateway Added \"" + g.toString() + "\"");
        gateways.add(g);
    }
    
    public void setDebug(int value)
    {
        this.debug_mode = value;
    }

    public void setServerName(String value)
    {
        this.server_name = value;
    }
    
    public int getDebug()
    {
        return this.debug_mode;
    }
    
    public void setDebugStream(OutputStream value)
    {
        this.debug_stream = new PrintStream(value);
    }
    
    public PrintStream getDebugStream()
    {
        return this.debug_stream;
    }
    
    public void log(String caption, int level, String msg)
    {
        if (this.debug_mode >= level)
        {
            this.debug_stream.println("[ " + setSizedString(caption, 16) + " ] " + msg.trim());
        }
    }
    
    private static String setSizedString(String value, int size)
    {
        if (value == null)
        {
            return getPaddingSpace(size);
        } else if (value.length() == size) {
            return value;
        } else if (value.length() > size) {
            return value.substring(0, size);
        } else if (value.length() < size) {
            return value + getPaddingSpace(size - value.length());
        } else {
            return null;
        }
    }
    
    private static String getPaddingSpace(int value)
    {
        StringBuffer x = new StringBuffer("");
        for (int n = 0; n < value; n++)
        {
            x.append(" ");
        }
        return x.toString();
    }
    
    
    //--------------------User Management---------------------------
    // Nobody should ever touch the users vector, outside of here
    
    public void addUser(IrcUser u)
    {
        this.users.add(u);
    }
    
    // Should only be called by the disconnecting user
    public void removeUser(IrcUser conn)
    {
        int idx = users.indexOf(conn);
        if (idx != -1)
        {
            users.remove(idx);
            for(Enumeration<IrcChannel> e = rooms.elements(); e.hasMoreElements(); )
            {
                IrcChannel chan = e.nextElement();
                chan.removeMember(conn);
            }
            log(this.server_name, 1, "Connection Removed \"" + conn.toString() + "\"");
        }
    }
    
    public IrcUser findUser(String nick)
    {
        for (Enumeration<IrcUser> e = users.elements(); e.hasMoreElements(); )
        {
            IrcUser isc = e.nextElement();
            if (isc.isWelcomed())
            {
                if (isc.is(nick))
                {
                    return isc;
                }
            }
        }
        return null;
    }
    
    public int getUserCount()
    {
        return this.users.size();
    }
    
    //--------------------------------------------------------------
    
    public IrcChannel findChannel(String chan)
    {
        for (Enumeration<IrcChannel> e = rooms.elements(); e.hasMoreElements(); )
        {
            IrcChannel ic = e.nextElement();
            if (ic.getName().equals(chan))
            {
                return ic;
            }
        }
        return null;
    }
    
    public void removeChannel(IrcChannel chan)
    {
        int idx = rooms.indexOf(chan);
        if (idx != -1)
        {
            rooms.remove(idx);
            log(this.server_name, 1, "Channel Removed \"" + chan.toString() + "\"");
        }
    }
    
    public int getChannelCount()
    {
        return this.rooms.size();
    }
    
    public void setMotd(File motd_file)
    {
        String thisLine;
        BufferedReader br = null;
        this.motd = new Vector<String>();
        try {
            br = new BufferedReader(new FileReader(motd_file));
            while ((thisLine = br.readLine()) != null)
                this.motd.add(thisLine);
            log(this.server_name, 1, "Loaded MOTD from file \"" + motd_file.toString() + "\"");
        } catch (Exception ioe) {
            // who cares
        } finally {
            if(br!=null)
            {
                try {
                    br.close();
                } catch (IOException e) {}
            }
        }
    }
    
    public void setMotd(Vector<String> motd)
    {
        this.motd = motd;
    }
    
    public Vector<String> getMotd()
    {
        return this.motd;
    }
    
    public void addChannel(IrcChannel chan)
    {
        this.rooms.add(chan);
        log(this.server_name, 1, "Channel Added \"" + chan.getName() + "\"");
    }
    
    public Vector<IrcChannel> getChannels()
    {
        return this.rooms;
    }

    public Vector<IrcUser> getUsers()
    {
        return this.users;
    }

    public String getServerName()
    {
        return this.server_name;
    }
    
    public void shutdown()
    {
        this.keep_running = false;
    }
}
