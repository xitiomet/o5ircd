/*
    Copyright (C) 2010 Brian Dunigan

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.openstatic.irc.client;

import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.imageio.ImageIO;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import javax.swing.JTabbedPane;
import java.awt.Font;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Properties;

import org.json.*;

import org.openstatic.irc.IRCMessage;
import org.openstatic.irc.IRCResponse;
import org.openstatic.irc.IRCMessageListener;

public class O5Client extends JFrame implements ActionListener, IRCMessageListener
{
    private JTabbedPane tabbed;
    private JTextField server_field;
    private JTextField username_field;
    private JTextField password_field;
    private Hashtable<String, O5ChatPanel> chat_panels;
    private Hashtable<String, O5ServerPanel> server_panels;
	private JButton login_btn;
    private String token_id;

    public static void main(String[] args)
    {
        O5Client o5 = new O5Client();
    }

    public O5Client()
    {
        super("O5 IRC");
        this.chat_panels = new Hashtable<String, O5ChatPanel>();
        this.server_panels = new Hashtable<String, O5ServerPanel>();
        this.token_id = null;
        this.addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(WindowEvent winEvt)
            {
                System.exit(0); 
            }
        });
        this.tabbed = new JTabbedPane();

        JPanel main_pane = new JPanel();
        main_pane.setLayout(new BorderLayout());
        // Setup tab
        JPanel pane = new JPanel(new GridLayout(0,2,6,6));
        pane.setSize(50,100);

        JLabel server_label = new JLabel("Server:", JLabel.TRAILING);
        server_field = new JTextField(15);
        server_field.setText("127.0.0.1:4050");

        JLabel username_label = new JLabel("Username:", JLabel.TRAILING);
        username_field = new JTextField(15);

        JLabel password_label = new JLabel("Password:", JLabel.TRAILING);
        password_field = new JTextField(15);

        login_btn = new JButton("Connect");
        login_btn.setActionCommand("login");
        login_btn.addActionListener(this);

        pane.add(server_label);
        pane.add(server_field);

        pane.add(username_label);
        pane.add(username_field);
        
        pane.add(password_label);
        pane.add(password_field);

        pane.add(new JLabel(""));
        pane.add(login_btn);

        JPanel page_start = new JPanel(new BorderLayout());
        
        JLabel page_header = new JLabel("O5 Client");
        page_header.setFont(new Font("Monospaced", Font.BOLD, 36));
         
        page_start.add(page_header, BorderLayout.PAGE_START);
        page_start.add(pane, BorderLayout.PAGE_END);
        
        main_pane.add(page_start, BorderLayout.PAGE_START);
        
        tabbed.addTab("Connect", null, main_pane, "");

        // finish window
        this.add(tabbed);
        centerWindow();
        this.setVisible(true);
    }

    public void centerWindow()
    {
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screenSize = tk.getScreenSize();
        final int WIDTH = screenSize.width;
        final int HEIGHT = screenSize.height;
        this.setSize(520, 340);
        this.setLocation(WIDTH / 4 - 260, HEIGHT / 2 - 170);
    }
    
    public boolean isCentered()
    {
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screenSize = tk.getScreenSize();
        final int WIDTH = screenSize.width;
        final int HEIGHT = screenSize.height;
        if (this.getX() == ((WIDTH / 2) - (this.getWidth() / 2)) && this.getY() == ((HEIGHT / 2) - (this.getHeight() / 2)))
        {
            return true;
        } else {
            return false;
        }
    }

    public void addPanel(String panel_caption, Component panel)
    {
        final JPanel plugin_pane = new JPanel(new BorderLayout());
        plugin_pane.add(panel, BorderLayout.CENTER);
        plugin_pane.setForeground(Color.WHITE);
        plugin_pane.setBackground(Color.GRAY);
        JButton close_button = new JButton("Close Tab");
        close_button.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e)
                {
                    tabbed.remove(plugin_pane);
                }
        });
        
        
        /*
        JPanel sub_layout = new JPanel(new BorderLayout())
        {
            public void paintComponent(Graphics g)
            {
                g.drawImage(placebo_icon, 2, 2, 24, 24, null);
            }
        };
        sub_layout.add(close_button, BorderLayout.EAST);
        plugin_pane.add(sub_layout, BorderLayout.PAGE_END);
        */
        
        tabbed.addTab(panel_caption, null, plugin_pane, "");
    }

    public void onIRCMessage(IRCMessage message, Object source)
    {
        if (message.is("PRIVMSG"))
        {
            O5Connection connection = (O5Connection) source;
            String source_nick = message.getSourceNick();
            String dest_nick = message.getArgs().firstElement();
            O5ChatPanel p = null;
            //System.err.println("{" + connection.getNick() + "} [" + source_nick + "] , [" + dest_nick + "]");
            if (source_nick.equals(connection.getNick()))
            {
                p = chat_panels.get(dest_nick);
            } else {
                p = chat_panels.get(source_nick);
            }
            if (p != null)
            {
                p.handleMessage(message);
            } else {
                O5ChatPanel p_new = new O5ChatPanel((O5Connection) source, source_nick);
                chat_panels.put(source_nick, p_new);
                p_new.handleMessage(message);
                tabbed.addTab(source_nick, null, p_new, "");
            }
        }
    }
    
    public void onIRCResponse(IRCResponse response, Object source)
    {
        O5Connection connection = (O5Connection) source;
        O5ServerPanel sp = null;
        sp = server_panels.get(connection.getServerName());
        sp.handleResponse(response);
    }

    private void addServer(O5Connection oc)
    {
        O5ServerPanel osp = new O5ServerPanel(oc);
        oc.startInteractive(this);
        server_panels.put(oc.getServerName(), osp);
        tabbed.addTab(oc.getServerName(), null, osp, "");
        tabbed.setSelectedIndex(tabbed.getTabCount() - 1);
    }


    public void actionPerformed(ActionEvent e)
    {
        final ActionEvent x = e;
        Thread t = new Thread()
        {
            public void run()
            {
                try
                {
                    if ("login".equals(x.getActionCommand()))
                    {
                        String server = server_field.getText();
                        String username = username_field.getText();
                        String password = password_field.getText();
                        O5Connection oc = new O5Connection(server);
                        oc.login(username, password);
                        addServer(oc);
                    }
                } catch (Exception e) {

                }
            }

        };
        t.start();
    }
}
