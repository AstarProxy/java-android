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
import java.nio.ByteBuffer;
public class Socks5ProxyClient extends Socket {
    protected String username;
    protected String password;
    protected String remoteHost;
    protected int remotePort;
    protected InputStream in;
    protected OutputStream out;
    public Socks5ProxyClient(String hostname, int port, String username, String password, String remoteHost, int remotePort) throws
            IOException,
            UnknownHostException,
            UpstreamBadResponseException,
            UpstreamResponseStatusException,
            UpstreamSocks5AuthMethodException,
            UpstreamAuthException,
            HostUnreachableException
    {
        super(resolveIP4(hostname), port);
        this.username = username;
        this.password = password;
        this.remoteHost = resolveIP4(remoteHost);
        this.remotePort = remotePort;
        this.in = this.getInputStream();
        this.out = this.getOutputStream();
        handshake();
        connect();
    }
    public void connect() throws
            IOException,
            UpstreamBadResponseException,
            UpstreamResponseStatusException,
            HostUnreachableException
    {
        ByteBuffer buffer = ByteBuffer.allocate(10);
        buffer.put((byte)0x05);
        buffer.put((byte)0x01);
        buffer.put((byte)0x00);
        buffer.put((byte)0x01);
        String[] ipParts = remoteHost.split(".");
        for(int x = 0, l = ipParts.length; x < l; x++) {
            buffer.put((byte)Integer.valueOf(ipParts[0]).intValue());
        }
        buffer.put((byte)(remotePort / 256));
        buffer.put((byte)(remotePort % 256));
        out.write(buffer.array());
        if (in.read() != 0x05) {
            throw new UpstreamBadResponseException();
        }
        byte[] response = new byte[10];
        in.read(response);
        if (response[0] != 0x05) {
            throw new UpstreamBadResponseException();
        }
        if (response[1] == 0x03 || response[1] == 0x04) {
            throw new HostUnreachableException(AddressType.IPv4, remoteHost, remotePort);
        } else if (response[1] != 0x00) {
            throw new UpstreamResponseStatusException();
        }
    }
    protected void handshake() throws
            IOException,
            UpstreamBadResponseException,
            UpstreamSocks5AuthMethodException,
            UpstreamAuthException
    {
        out.write(new byte[]{0x05, 1, 0x02});
        if (in.read() != 0x05) {
            throw new UpstreamBadResponseException();
        }
        int authMethod = in.read();
        auth(authMethod);
    }
    protected void auth(int method) throws
            IOException,
            UpstreamBadResponseException,
            UpstreamSocks5AuthMethodException,
            UpstreamAuthException
    {
        if (method == 0x00) {
            return;
        }
        if (method != 0x02) {
            throw new UpstreamSocks5AuthMethodException();
        }
        if (username == null || password == null || username.length() == 0) {
            throw new UpstreamAuthException();
        }
        int userLength = username.length();
        int passLength = username.length();
        ByteBuffer buffer = ByteBuffer.allocate(userLength + passLength + 3);
        buffer.put((byte)0x01);
        buffer.put((byte)userLength);
        buffer.put(username.getBytes());
        buffer.put((byte)passLength);
        buffer.put(password.getBytes());
        out.write(buffer.array());
        if (in.read() != 0x01) {
            throw new UpstreamBadResponseException();
        }
        if (in.read() != 0x00) {
            throw new UpstreamAuthException();
        }
    }
    public static String resolveIP4(String hostname) throws UnknownHostException {
        InetAddress address = InetAddress.getByName(hostname);
        return address.getHostAddress();
    }
}
