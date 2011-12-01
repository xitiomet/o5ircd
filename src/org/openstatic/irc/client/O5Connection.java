package org.openstatic.irc.client;

import java.net.URL;
import java.net.URLEncoder;
import java.net.HttpURLConnection;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.json.*;

import org.openstatic.irc.IRCMessage;
import org.openstatic.irc.IRCResponse;
import org.openstatic.irc.IRCMessageListener;


public class O5Connection
{
    private String token_id;
    private String nick;
    private String server;
    private String server_name;
    private PrintStream ips;

    public O5Connection(String server)
    {
        this.server = server;
    }

    public JSONObject login(String nick, String password)
    {
        try
        {
            JSONObject login_response = apiCall("http://" + server + "/irc/connect/?nick=" + URLEncoder.encode(nick, "UTF-8") + "&password=" + URLEncoder.encode(password, "UTF-8"));
            this.token_id = login_response.getString("token_id");
            this.server_name = login_response.getString("server");
            this.nick = login_response.getString("nick");
            return login_response;
        } catch (Exception e) {
            return null;
        }
    }

    public void startInteractive(final IRCMessageListener iml)
    {
        Thread t = new Thread()
        {
            public void run()
            {
                try
                {
                    URL url = new URL("http://" + O5Connection.this.server + "/irc/interactive/?token_id=" + O5Connection.this.token_id);
                    InputStream is = url.openStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    String cmd_line;
                    do
                    {
                        try
                        {
                            cmd_line = br.readLine();
                        } catch (Exception n) {
                            System.err.println("READLINE: " + n.toString() + " / " + n.getMessage());
                            n.printStackTrace(System.err);                            
                            cmd_line = null;
                        }
                        if (cmd_line != null)
                        {
                            System.err.println(cmd_line);
                            try
                            {
                                JSONObject event = new JSONObject(cmd_line);
                                if (event.has("command"))
                                    iml.onIRCMessage(new IRCMessage(event), O5Connection.this);
                                if (event.has("response"))
                                    iml.onIRCResponse(new IRCResponse(event), O5Connection.this);
                            } catch (Exception cmd_exception) {
                                System.err.println(cmd_exception.toString() + " / " + cmd_exception.getMessage());
                                cmd_exception.printStackTrace(System.err);
                            }
                        }
                    } while (cmd_line != null);
                } catch (Exception e) {
                    System.err.println(e.toString() + " / " + e.getMessage());
                    e.printStackTrace(System.err);
                }
            }
        };
        t.start();
    }

    public void onIRCMessage(IRCMessage message)
    {
        // stub for event.
    }

    public void sendIRCMessage(IRCMessage message)
    {
        apiCall("http://" + this.server + "/irc/direct/?token_id=" + this.token_id, message.toJSONObject().toString());
    }

    public String getNick()
    {
        return this.nick;
    }

    public String getServerName()
    {
        return this.server_name;
    }

    public String getServerAddress()
    {
        return this.server;
    }
    
    public String getTokenId()
    {
        return this.token_id;
    }

    private static JSONObject apiCall(String url_string)
    {
        return apiCall(url_string, null);
    }

    private static JSONObject apiCall(String url_string, String post_data)
    {
        try
        {
            URL url = new URL(url_string);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            if (post_data != null)
            {
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "text/plain");
                conn.setDoOutput(true);
                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                wr.write(post_data);
                wr.flush();
                wr.close();
            }
            InputStream is = conn.getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int inputByte;
            while ((inputByte = is.read()) > -1)
            {
                baos.write(inputByte);
            }
            is.close();
            String response = new String(baos.toByteArray());
            return new JSONObject(response);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

}
