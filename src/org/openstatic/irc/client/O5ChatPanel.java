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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import java.net.URL;
import java.net.URLEncoder;
import java.net.HttpURLConnection;

import org.json.*;
import org.openstatic.irc.IRCMessage;

public class O5ChatPanel extends JPanel implements ActionListener
{
    private JTextPane chat_box;
    private JTextField chat_input;
    private StringBuffer chat_buffer;
    private String nickname;
    private O5Connection connection;

    public O5ChatPanel(O5Connection oc, String nickname)
    {
        super(new BorderLayout());
        this.connection = oc;
        this.chat_buffer = new StringBuffer();
        this.nickname = nickname;
        chat_box = new JTextPane();
        chat_box.setContentType("text/html");
        chat_box.setEditable(false);
        Font font = new Font("Monospaced", Font.BOLD, 14);
        chat_box.setBackground(Color.WHITE);
        chat_box.setFont(font);
        chat_box.setForeground(Color.BLACK);
        chat_box.setText("");
        JScrollPane scroller = new JScrollPane(chat_box);
        scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        
        chat_input = new JTextField(255);
        chat_input.setBackground(Color.WHITE);
        chat_input.setFont(font);
        chat_input.setForeground(Color.BLACK);
        chat_input.addActionListener(this);
        JPanel page_start = new JPanel(new BorderLayout());

        this.add(scroller, BorderLayout.CENTER);
        this.add(chat_input, BorderLayout.PAGE_END);
    }

    public void handleMessage(IRCMessage im)
    {
        if (im.getSourceNick().equals(nickname))
            addText(im.getSourceNick(), "#880000", im.getArgs().lastElement());
        else
            addText(im.getSourceNick(), "#000088", im.getArgs().lastElement());
    }

    public void addText(String user, String color, String message)
    {
        chat_buffer.append("<font color=\"" + color + "\"><b>" + user + "</b></font>: " + message + "<br />");
        chat_box.setText("<html><body>" + chat_buffer.toString() + "</body></html>");
        Document d = chat_box.getDocument();
        chat_box.select(d.getLength(), d.getLength());
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
                    IRCMessage im = new IRCMessage(IRCMessage.PRIVMSG);
                    im.addArg(O5ChatPanel.this.nickname);
                    im.addArg(chat_input.getText());
                    O5ChatPanel.this.connection.sendIRCMessage(im);
                    chat_input.setText("");
                } catch (Exception e) {

                }
            }

        };
        t.start();
    }
}
