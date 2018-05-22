package io.github.astarProxy;

/**
 * Created by maryam on 5/19/2018.
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
public class HttpProxyClient extends Socket {
    protected String username;
    protected String password;
    public HttpProxyClient(String hostname, int port, String username, String password) throws
            IOException,
            UnknownHostException
    {
        super(resolveIP4(hostname), port);
        this.username = username;
        this.password = password;
    }
    public void connectTo(String hostname, int port) throws
            IOException,
            UpstreamBadResponseException,
            UpstreamResponseStatusException,
            InterruptedException
    {
        String request = "CONNECT " + hostname + ":" + port + " HTTP/1.0\r\n";
        request += "Host: " + hostname + ":" + port + "\r\n";
        if (username != null && username.length() > 0){
            request += "Proxy-Authorization: basic " + Base64.encodeToString((new String(username + ":" + password)).getBytes(),Base64.DEFAULT) + "\r\n";
        }
        request += "\r\n";
        InputStream in = this.getInputStream();
        OutputStream out = this.getOutputStream();
        out.write(request.getBytes());
        String header = "";
        boolean fullHeader = false;
        while (!fullHeader) {
            while (in.available() > 0) {
                header += (char)in.read();
                if (header.endsWith("\r\n\r\n")) {
                    fullHeader = true;
                    break;
                }
            }
            Thread.sleep(50);
        }
        if (!fullHeader) {
            throw new UpstreamBadResponseException();
        }
        String[] lines = header.split("\r\n");
        if (!lines[0].contains("200")) {
            throw new UpstreamResponseStatusException();
        }
    }
    public static String resolveIP4(String hostname) throws UnknownHostException {
        InetAddress address = InetAddress.getByName(hostname);
        return address.getHostAddress();
    }
}
