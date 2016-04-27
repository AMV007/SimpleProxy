// This example is from _Java Examples in a Nutshell_. (http://www.oreilly.com)
// Copyright (c) 1997 by David Flanagan
// This example is provided WITHOUT ANY WARRANTY either expressed or implied.
// You may study, use, modify, and distribute it for non-commercial purposes.
// For any commercial use, see http://www.davidflanagan.com/javaexamples

import com.sun.corba.se.impl.orbutil.concurrent.Mutex;
import java.io.*;
import java.net.*;

/**
 * This class implements a simple single-threaded proxy server.
 *
 */
public class SimpleProxyServer
{

    /**
     * The main method parses arguments and passes them to runServer
     */
    public static void main(String[] args) throws IOException
    {
        try
        {
            // Check the number of arguments
            if (args.length != 3)
            {
                throw new IllegalArgumentException("Wrong number of arguments.");
            }

            // Get the command-line arguments: the host and port we are proxy for
            // and the local port that we listen for connections on
            String host = args[0];
            int remoteport = Integer.parseInt(args[1]);
            int localport = Integer.parseInt(args[2]);
            // Print a start-up message
            System.out.println("Starting proxy for " + host + ":" + remoteport
                    + " on port " + localport);
            // And start running the server
            runServer(host, remoteport, localport);   // never returns
        }
        catch (Exception e)
        {
            System.err.println(e);
            System.err.println("Usage: java SimpleProxyServer "
                    + "<host> <remoteport> <localport>");
        }
    }

    static Mutex mutex = new Mutex();
    
    static void Log(String log)
    {
        try
        {
            mutex.acquire();
            try
            {
                System.out.println(log);
                System.out.flush();
            }
            finally
            {
                mutex.release();
            }
        }
        catch (InterruptedException ie)
        {
            // ...
        }
    }

    static void LogError(String error)
    {
        try
        {
            mutex.acquire();
            try
            {
                System.err.println(error);
                System.err.flush();
            }
            finally
            {
                mutex.release();
            }
        }
        catch (InterruptedException ie)
        {
            // ...
        }
    }

    


    /**
     * This method runs a single-threaded proxy server for host:remoteport on
     * the specified local port. It never returns.
     *
     */
    public static void runServer(String host, int remoteport, int localport)
            throws IOException
    {
        try
        {
            // Create a ServerSocket to listen for connections with
            ServerSocket ss = new ServerSocket(localport);
            
            while (true)
            {
                
                try
                {
                    // Wait for a connection on the local port
                    Socket client = ss.accept();
                    (new ProxyThread(host,remoteport,client)).start();
                }
                catch (Exception ex)
                {
                    
                }                   
            }
        }
        catch (Exception e)
        {
            LogError("global err: " + e);
        }
    }
}