package io.github.astarProxy;

/**
 * Created by maryam on 5/19/2018.
 */
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
public class AstarProxyClient extends HttpProxyClient {
    protected String username;
    protected String password;
    public AstarProxyClient(String hostname, int port, String username, String password) throws
            IOException,
            UnknownHostException
    {
        super(hostname, port, username, password);
        this.username = username;
        this.password = password;
    }
    @Override
    public void connectTo(String hostname, int port) throws
            IOException,
            UpstreamBadResponseException,
            UpstreamResponseStatusException,
            InterruptedException
    {
        String decodedHeader = "CONNECT " + hostname + ":" + port + " HTTP/1.1\r\n";
        if (username != null && username.length() > 0){
            decodedHeader += "Proxy-Authorization: basic " + Base64.encodeToString((new String(username + ":" + password)).getBytes(),Base64.DEFAULT) + "\r\n";
        }
        decodedHeader += "\r\n";
        String request = "GET / HTTP/1.1\r\n";
        request += "X-Encoded: " + Base64.encodeToString(decodedHeader.getBytes(),Base64.DEFAULT) + "\r\n";
        request += "\r\n";
        // System.out.println(request);
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
        // System.out.println(header);
        if (!lines[0].contains("200")) {
            throw new UpstreamResponseStatusException();
        }
    }
}
