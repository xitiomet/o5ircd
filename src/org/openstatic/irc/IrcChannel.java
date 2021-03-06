package org.openstatic.irc;

import java.util.Vector;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.lang.reflect.Constructor;

public class IrcChannel implements MiddlewareHandler
{
    private String channel_name;
    private String topic;
    private Vector<IrcUser> members;
    private Hashtable<IrcUser, String> member_modes;
    
    private Vector<IrcUser> pending_joins;
    private MiddlewareHandler myHandler;
    
    private Vector<MiddlewareHandler> myMiddleware;
    
    public IrcChannel(Properties setup)
    {
        this.channel_name = setup.getProperty("channel_name");
        this.topic = setup.getProperty("channel_topic");
        this.myHandler = null;
        this.myMiddleware = new Vector<MiddlewareHandler>();
        this.buildMiddlewareStack();
        if (setup.getProperty("middleware") != null)
        {
            try
            {
                Class<?> c = Class.forName(setup.getProperty("middleware"));
                Constructor<?> cons = c.getDeclaredConstructor(Properties.class);
                this.addHandler((MiddlewareHandler) cons.newInstance(setup));
            } catch (Exception e) {}
        }
        this.members = new Vector<IrcUser>();
        this.pending_joins = new Vector<IrcUser>();
        this.member_modes = new Hashtable<IrcUser, String>();
    }
    
    public IrcChannel(String name)
    {
        this.channel_name = name;
        this.topic = null;
        this.myHandler = null;
        this.myMiddleware = new Vector<MiddlewareHandler>();
        this.buildMiddlewareStack();
        this.members = new Vector<IrcUser>();
        this.pending_joins = new Vector<IrcUser>();
        this.member_modes = new Hashtable<IrcUser, String>();
    }
    
    public IrcChannel(String name, String topic)
    {
        this.channel_name = name;
        this.topic = topic;
        this.myHandler = null;
        this.myMiddleware = new Vector<MiddlewareHandler>();
        this.buildMiddlewareStack();
        this.members = new Vector<IrcUser>();
        this.pending_joins = new Vector<IrcUser>();
        this.member_modes = new Hashtable<IrcUser, String>();
    }
    
    public IrcChannel(String name, MiddlewareHandler handler)
    {
        this(name, null, handler);
    }
    
    public IrcChannel(String name, String topic, MiddlewareHandler handler)
    {
        this.channel_name = name;
        this.topic = topic;
        this.myHandler = handler;
        this.myMiddleware = new Vector<MiddlewareHandler>();
        this.addHandler(handler);
        this.members = new Vector<IrcUser>();
        this.pending_joins = new Vector<IrcUser>();
        this.member_modes = new Hashtable<IrcUser, String>();
    }
    
    public void shutdown()
    {
        for (Enumeration<IrcUser> e = this.members.elements(); e.hasMoreElements(); )
        {
            IrcUser cu = e.nextElement();
            removeMember(cu);
        }
        if (this.myHandler != null && this.myHandler != this)
        {
            this.myHandler.shutdown();
        }
    }
    
    public String getName()
    {
        return this.channel_name;
    }
    
    public String getTopic()
    {
        if (this.topic != null)
        {
            return this.topic;
        } else {
            return "";
        }
    }
    
    public int getMemberCount()
    {
        return this.members.size();
    }
    
    public boolean removeMember(IrcUser u)
    {
        IRCMessage rc = new IRCMessage("PART", u);
        rc.addArg(this.channel_name);
        return removeMember(rc);
    }
    
    public boolean removeMember(IRCMessage receivedCommand)
    {
        IrcUser u = findMember(receivedCommand.getSource());
        int idx = members.indexOf(u);
        if (idx != -1)
        {
            members.remove(idx);
            
            receivedCommand.clearArgs();
            receivedCommand.addArg(this.channel_name);
            
            u.sendCommand(receivedCommand);
            broadcast(receivedCommand, false);
            
            return true;
        } else {
            return false;
        }
    }
    
    public boolean kickMember(String src, IrcUser u)
    {
        int idx = members.indexOf(u);
        if (idx != -1)
        {
            members.remove(idx);
            
            IRCMessage kick_message = IRCMessage.prepare("KICK");
            kick_message.addArg(this.channel_name);
            kick_message.addArg(u.getNick());
            kick_message.setSource(src);
            
            u.sendCommand(kick_message);
            broadcast(kick_message, false);
            
            return true;
        } else {
            return false;
        }
    }
    
    public boolean isMember(IrcUser u)
    {
        int idx = members.indexOf(u);
        if (idx == -1)
        {
            return false;
        } else {
            return true;
        }
    }
    
    public boolean updateNick(IrcUser u, String newNickname)
    {
        int idx = members.indexOf(u);
        if (idx != -1)
        {
            IRCMessage new_nick = IRCMessage.prepare("NICK");
            new_nick.addArg(newNickname);
            new_nick.setSource(u);
            broadcast(new_nick, false);
            return true;
        } else {
            return false;
        }
    }
    
    public void pendingJoin(IrcUser u)
    {
        int idx = pending_joins.indexOf(u);
        if (idx == -1)
        {
            pending_joins.add(u);
        }
    }
    
    public IrcUser getPendingJoin(String value)
    {
        for (Enumeration<IrcUser> e = this.pending_joins.elements(); e.hasMoreElements(); )
        {
            IrcUser cu = e.nextElement();
            if (cu.is(value) == true)
            {
                pending_joins.remove(cu);
                return cu;
            }
        }
        return null;        
    }
    
    public boolean addMember(IrcUser u, IRCMessage receivedCommand)
    {
        int idx = members.indexOf(u);
        if (idx == -1)
        {
            members.add(u);
            IRCMessage join_message = receivedCommand;           
            
            join_message.clearArgs();
            join_message.addArg(this.channel_name);
            
            u.sendCommand(join_message);
            broadcast(join_message, false);
            
            if (this.topic != null)
            {
                u.sendResponse("332", this.channel_name + " :" + this.topic);
            } else {
                u.sendResponse("331", this.channel_name + " :No topic is set");
            }
            u.sendResponse("353", "= " + this.channel_name + " :" + getMembersSerial());
            u.sendResponse("366", this.channel_name + " :End of Names List");
            return true;
        } else {
            return false;
        }
    }
        
    private void broadcast(IRCMessage message, boolean withEcho)
    {
        for (Enumeration<IrcUser> e = this.members.elements(); e.hasMoreElements(); )
        {
            IrcUser cu = e.nextElement();
            if (cu.toString().equals(message.getSource()) == false || withEcho == true)
            {
                if (message.inDestination(cu))
                {
                    cu.sendCommand(message);
                }
            }
        }
    }
    
    private void whoResponse(IrcUser u)
    {
        for (Enumeration<IrcUser> e = this.members.elements(); e.hasMoreElements(); )
        {
            IrcUser cu = e.nextElement();
            u.sendResponse("352", this.channel_name + " " + cu.getUserName() + " " + cu.getClientHostname() + " " + cu.getServerHostname() + " " + cu.getNick() + " H :0 " + cu.getRealName());
        }
        u.sendResponse("315", ":End of Who List");
    }
    
    public IrcUser findMember(String value)
    {
        for (Enumeration<IrcUser> e = this.members.elements(); e.hasMoreElements(); )
        {
            IrcUser cu = e.nextElement();
            if (cu.is(value) == true)
            {
                return cu;
            }
        }
        return null;
    }
    
    public void onCommand(IRCMessage receivedCommand)
    {
        IrcUser u = findMember(receivedCommand.getSource());
        if (receivedCommand.is("JOIN")) {
            IrcUser pj = getPendingJoin(receivedCommand.getSource());
            if (pj != null)
            {
                addMember(pj, receivedCommand);
            }
        } else if (receivedCommand.is("PART") && u != null) {
            removeMember(receivedCommand);
        } else if (receivedCommand.is("KICK")) {
            if (receivedCommand.argCount() >= 2)
            {
                IrcUser intended_target = findMember(receivedCommand.getArg(1));
                if (intended_target != null)
                {
                    kickMember(receivedCommand.getSource(), intended_target);
                }
            }
        } else if (receivedCommand.is("MODE")) {
            IrcUser mt = findMember(receivedCommand.getArg(2));
            if (mt != null)
            {
                String m_mode = member_modes.get(mt);
                StringBuffer current_mode = new StringBuffer();
                if (m_mode == null)
                {
                    current_mode = new StringBuffer("");
                } else {
                    current_mode = new StringBuffer(m_mode);
                }
                
                String mode_line = receivedCommand.getArg(1);
                boolean add_mode = false;
                boolean remove_mode = false;
                for (int n = 0; n < mode_line.length(); n++)
                {
                    char action = mode_line.charAt(n);
                    String action_str = (new Character(action)).toString();
                    if (action == '+')
                    {
                        add_mode = true;
                        remove_mode = false;
                    } else if (action == '-') {
                        add_mode = false;
                        remove_mode = true;
                    } else if (add_mode) {
                        if (current_mode.indexOf(action_str) == -1)
                        {
                            current_mode.append(action);
                        }
                    } else if (remove_mode) {
                        int mode_loc = current_mode.indexOf(action_str);
                        if (mode_loc != -1)
                        {
                            current_mode.deleteCharAt(mode_loc);
                        }
                    }
                    
                }
                member_modes.put(mt, current_mode.toString());
                broadcast(receivedCommand, true);
            }
        } else if (receivedCommand.is("INVITE") && u != null) {
            IrcUser usr = u.getIrcServer().findUser(receivedCommand.getArg(0));
            if (usr != null && receivedCommand.argCount() == 2)
            {
                IRCMessage pc = IRCMessage.prepare("INVITE");
                pc.addArg(receivedCommand.getArg(0));
                pc.addArg(receivedCommand.getArg(1));
                usr.sendCommand(pc);
                if (u != null) u.sendResponse("341", receivedCommand.getArg(1) + " " + receivedCommand.getArg(0));
            } else {
                if (u != null) u.sendResponse("461", "INVITE :Failed");
            }
        } else if (receivedCommand.is("WHO")) {
            if (u != null) whoResponse(u);
        } else if (receivedCommand.is("PRIVMSG") || receivedCommand.is("NOTICE")) {
            IRCMessage outbound_message = receivedCommand;
            if (receivedCommand.getArg(0).equals(this.channel_name))
            {
                broadcast(outbound_message, u.shouldEcho());
            } else {
                IrcUser intended_target = findMember(receivedCommand.getArg(0));
                if (intended_target != null)
                {
                    intended_target.sendCommand(outbound_message);
                }
            }
        } else if (receivedCommand.is("TOPIC")) {
            broadcast(receivedCommand, true);
            this.topic = receivedCommand.getArg(1);
        } else {
            if (u != null)
            {
                u.sendCommand(receivedCommand);
            } else {
                IrcUser pj = getPendingJoin(receivedCommand.getSource());
                if (pj != null)
                {
                    pj.sendCommand(receivedCommand);
                }
            }
        }
    }
    
    private String getMembersSerial()
    {
        StringBuffer returnMembers = new StringBuffer("");
        for (Enumeration<IrcUser> e = this.members.elements(); e.hasMoreElements(); )
        {
            IrcUser u = e.nextElement();
            String nick = u.getNick();
            if (hasMode(u, "o"))
            {
                nick = "@" + nick;
            } else if (hasMode(u, "v")) {
                nick = "+" + nick;
            }
            returnMembers.append(nick);
            if (e.hasMoreElements())
            {
                returnMembers.append(" ");
            }
        }
        return returnMembers.toString();
    }

    public String[] getMemberStringArray()
    {
        Vector<String> returnMembers = new Vector<String>();
        for (Enumeration<IrcUser> e = this.members.elements(); e.hasMoreElements(); )
        {
            IrcUser u = e.nextElement();
            String nick = u.getNick();
            if (hasMode(u, "o"))
            {
                nick = "@" + nick;
            } else if (hasMode(u, "v")) {
                nick = "+" + nick;
            }
            returnMembers.add(nick);
        }
        return returnMembers.toArray(new String[returnMembers.size()]);
    }
    
    private boolean hasMode(IrcUser member, String mode)
    {
        String m_mode = member_modes.get(member);
        StringBuffer current_mode;
        if (m_mode == null)
        {
            current_mode = new StringBuffer("");
        } else {
            current_mode = new StringBuffer(m_mode);
        }
        if (current_mode.indexOf(mode) != -1)
        {
            return true;
        } else {
            return false;
        }
    }

    public void setHandler(MiddlewareHandler mh)
    {
        this.myMiddleware.clear();
        this.myMiddleware.add(mh);
        this.buildMiddlewareStack();
    }
    
    public void addHandler(MiddlewareHandler mh)
    {
        this.myMiddleware.add(mh);
        this.buildMiddlewareStack();
    }
    
    /* so IrcChannels are a special kind of middleware since they have
       built in functionality for ordering and enumerating other middleware
       for this very reason, IrcChannels could get cranky if this isn't done
       properly
       */
    private void buildMiddlewareStack()
    {
        Enumeration<MiddlewareHandler> e = this.myMiddleware.elements();
        
        if (e.hasMoreElements())
        {
            this.myHandler = e.nextElement();
            
            System.err.println("MSTACK (0):" + this.myHandler.toString());
            MiddlewareHandler mx = this.myHandler;
            while (e.hasMoreElements())
            {
                MiddlewareHandler m = e.nextElement();
                System.err.println("MSTACK (*):" + m.toString());
                if (mx != null)
                    mx.setNextHandler(m);
                mx = m;
            }
            mx.setNextHandler(this);
        } else {
            System.err.println("No Middleware stack to create");
            this.myHandler = this;
        }
    }
    
    // I hope this never happens.
    public void setNextHandler(MiddlewareHandler middlewareHandler)
    {
        //this.myHandler = middlewareHandler;
    }
    
    public MiddlewareHandler getHandler()
    {
        return this.myHandler;
    }
    
    public String getHandlerName()
    {
        return "org.openstatic.irc.IrcChannel";
    }

    public String getHandlerDescription()
    {
        return "OpenStatic.org Irc Channel";
    }
    
    public String getHandlerDetails()
    {
        return this.channel_name;
    }
    
    public String toString()
    {
        return this.channel_name;
    }

}
