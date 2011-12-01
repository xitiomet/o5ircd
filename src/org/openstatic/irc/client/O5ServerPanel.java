package org.openstatic.irc.client;

import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.Document;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.openstatic.irc.IRCMessage;
import org.openstatic.irc.IRCResponse;

import java.util.StringTokenizer;

public class O5ServerPanel extends JPanel implements ActionListener
{
    private O5Connection connection;
    private JTextPane server_box;
    private StringBuffer server_buffer;
    private JTextField server_input;

    public O5ServerPanel(O5Connection oc)
    {
        super(new BorderLayout());
        this.connection = oc;
        this.server_buffer = new StringBuffer();
        server_box = new JTextPane();
        server_box.setContentType("text/html");
        server_box.setEditable(false);
        Font font = new Font("Monospaced", Font.BOLD, 12);
        server_box.setBackground(Color.BLACK);
        server_box.setFont(font);
        server_box.setForeground(Color.WHITE);
        server_box.setText("");
        JScrollPane scroller = new JScrollPane(server_box);
        scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        server_input = new JTextField(255);
        server_input.setBackground(Color.BLACK);
        server_input.setFont(font);
        server_input.setForeground(Color.WHITE);
        server_input.addActionListener(this);
        JPanel page_start = new JPanel(new BorderLayout());
        this.add(scroller, BorderLayout.CENTER);
        this.add(server_input, BorderLayout.PAGE_END);
    }

    public void addText(String message, String color)
    {
        server_buffer.append("<font color=\"" + color + "\">" + message + "</font><br />");
        server_box.setText("<html><body color=\"white\">" + server_buffer.toString() + "</body></html>");
    }

    public void handleResponse(IRCResponse response)
    {
        addText(response.toString(), "white");
    }

    public void serverCommand(String command)
    {
        StringTokenizer st = new StringTokenizer(command);
        String cmd = st.nextToken().toLowerCase();
        if (cmd.equals("/join"))
        {
            IRCMessage im = new IRCMessage(IRCMessage.JOIN);
            im.addArg(st.nextToken());
            this.connection.sendIRCMessage(im);
        }
        if (cmd.equals("/msg"))
        {
            IRCMessage im = new IRCMessage(IRCMessage.PRIVMSG);
            im.addArg(st.nextToken());
            im.addArg(remainingTokens(st));
            this.connection.sendIRCMessage(im);
        }
    }

    public String remainingTokens(StringTokenizer st)
    {
        StringBuffer sb = new StringBuffer();
        while (st.hasMoreTokens())
        {
            sb.append(st.nextToken());
            if (st.hasMoreTokens())
                sb.append(" ");
        }
        return sb.toString();
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
                    O5ServerPanel.this.serverCommand(server_input.getText());
                    server_input.setText("");
                } catch (Exception e) {

                }
            }

        };
        t.start();
    }

}
