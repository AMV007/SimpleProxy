
import com.sun.corba.se.impl.orbutil.concurrent.Mutex;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author makristiniy
 */
public class ProxyThread extends Thread
{

    String host;
    int remoteport;
    Socket client;       

    public ProxyThread(String host, int remoteport, Socket client)
    {
        this.host = host;
        this.remoteport = remoteport;
        this.client = client;
    }
    
    static byte[] StringToByte(String str)
    {
        char[] chars = str.toCharArray();
        byte[] res = new byte[chars.length];
        for (int i = 0; i < chars.length; i++)
        {
            res[i] = (byte) chars[i];
        }
        return res;
    }

    @Override
    public void run()
    {         
        Socket server = null;
        try
        {
            // Get client streams.  Make them final so they can
            // be used in the anonymous thread below.
            final InputStream from_client = client.getInputStream();
            final OutputStream to_client = client.getOutputStream();

            // Make a connection to the real server
            // If we cannot connect to the server, send an error to the 
            // client, disconnect, then continue waiting for another connection.            
            try
            {
                server = new Socket(host, remoteport);
            }
            catch (IOException e)
            {
                System.err.println("Proxy server cannot connect to " + host + ":"
                        + remoteport + ":\n" + e);
                client.close();
                return;
            }

            // Get server streams.
            final InputStream from_server = server.getInputStream();
            final OutputStream to_server = server.getOutputStream();
            final String ServerHost = host + ":" + remoteport;

            // Make a thread to read the client's requests and pass them to the 
            // server.  We have to use a separate thread because requests and
            // responses may be asynchronous.
            Thread t = new Thread()
            {
                @Override
                public void run()
                {                    
                    String req = "", reqBefore = "";
                    try
                    {
                        int bytes_read;
                        byte[] request = new byte[1024*32];
                        while ((bytes_read = from_client.read(request)) != -1)
                        {
                            String reqstr = new String(request, 0, bytes_read);
                            if (DetectDebugMode.isDebug())
                            {
                                reqBefore = reqstr;
                            }
                            byte[] Data = request;
                            if (reqstr.startsWith("GET"))
                            {
                                String[] reqlines = reqstr.split("\r\n");
                                String out = "";
                                for (int i = 0; i < reqlines.length; i++)
                                {
                                    if (reqlines[i].startsWith("Host:"))
                                    {
                                        out += "Host: " + ServerHost;
                                        out += "\r\n";
                                    }
                                    else if (reqlines[i].startsWith("Referer:"))
                                    {
                                        String[] reflines = reqlines[i].split("/");
                                        reflines[2] = ServerHost;
                                        String referer = "";
                                        for (int j = 0; j < reflines.length; j++)
                                        {
                                            referer += reflines[j] + "/";
                                        }
                                        out += referer;
                                        out += "\r\n";
                                    }
                                    else
                                    {
                                        out += reqlines[i];
                                        out += "\r\n";
                                    }
                                }
                                while (!out.endsWith("\r\n\r\n"))
                                {
                                    out += "\r\n";
                                }

                                Data = StringToByte(out);
                                bytes_read = Data.length;
                            }

                            req = "Req: " + new String(Data, 0, bytes_read);
                            if (DetectDebugMode.isDebug())
                            {
                                //Log("ReqBefore: "+reqBefore);
                                //Log("ReqAfter: " +req);
                                //Log("ReqBegin");
                            }
                            to_server.write(Data, 0, bytes_read);
                            to_server.flush();
                            if (DetectDebugMode.isDebug())
                            {
                                //Log("ReqOK");
                            }
                        }
                    }
                    catch (IOException e)
                    {
                        SimpleProxyServer.LogError("errRequestBefore: " + e + ", " + reqBefore);
                        SimpleProxyServer.LogError("errRequest: " + e + ", " + req);
                    }

                    // the client closed the connection to us, so  close our 
                    // connection to the server.  This will also cause the 
                    // server-to-client loop in the main thread exit.
                    try
                    {
                        to_server.close();
                    }
                    catch (IOException e)
                    {
                        SimpleProxyServer.Log("errRequestServerClose: " + e);
                    }
                }
            };

            // Start the client-to-server request thread running
            t.start();

            // Meanwhile, in the main thread, read the server's responses
            // and pass them back to the client.  This will be done in
            // parallel with the client-to-server request thread above.                    
            String ans = "";
            try
            {
                int bytes_read;
                byte[] reply = new byte[1024*32];
                while ((bytes_read = from_server.read(reply)) != -1)
                {
                    ans = "Ans: " + new String(reply, 0, bytes_read);
                    if (DetectDebugMode.isDebug())
                    {
                        //Log("AnsBegin");
                        //Log("Ans: "+new String(reply));
                    }
                    to_client.write(reply, 0, bytes_read);
                    to_client.flush();
                    if (DetectDebugMode.isDebug())
                    {
                        //Log("AnsOK");
                    }
                }
            }
            catch (IOException e)
            {
                SimpleProxyServer.LogError("errAnswer: " + e + ", " + ans);
            }

            // The server closed its connection to us, so close our 
            // connection to our client.  This will make the other thread exit.
            to_client.close();
        }
        catch (IOException e)
        {
            SimpleProxyServer.LogError("err4: " + e);
        }
        // Close the sockets no matter what happens each time through the loop.
        finally
        {
            try
            {
                if (server != null)
                {
                    server.close();
                }
                if (client != null)
                {
                    client.close();
                }
            }
            catch (IOException e)
            {
                SimpleProxyServer.LogError("err5: " + e);
            }
        }

    }
}
