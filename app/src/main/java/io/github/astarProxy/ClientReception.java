package io.github.astarProxy;

/**
 * Created by maryam on 5/19/2018.
 */

import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;


enum ClientProtocol {
    Socks4,
    Socksa4,
    Socks5,
};
enum AuthMethod {
    NoAuthentication,
    GSSAPI,
    Password,
};
enum Socks5Command {
    Connect,
    Bind,
    UDP_ASSOCIATE,
};
enum AddressType {
    IPv4,
    Domain,
    IPv6
};

public class ClientReception implements Runnable {
    protected Socket socket;
    protected SocksServer server;
    protected OutputStream outputStream;
    protected InputStream inputStream;
    protected ClientProtocol protocol;
    protected AuthMethod authMethod;
    protected Socket remote;
    protected ArrayList<Thread> pool;
    public ClientReception(Socket socket, SocksServer server) throws IOException {
        this.socket = socket;
        this.server = server;
        outputStream = socket.getOutputStream();
        inputStream = socket.getInputStream();
        pool = new ArrayList<Thread>();
    }
    @Override
    public void run() {
        try {
            try {
                handshake();
                if (authMethod != AuthMethod.NoAuthentication) {
                    authentication();
                }
                handleRequest();
            }catch(UnsupportedProcotolException e) {
                PrintWriter textport = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream)), true);
                textport.print("UnsupportedProcotolException");
                textport.flush();
            }catch(UnacceptableAuthMethodException e) {
                outputStream.write(new byte[]{0x05, 0x10});
            }catch(UnsupportedAuthMethodException e) {
                outputStream.write(new byte[]{0x05, 0x10});
            }catch(AuthenticationException e) {
                outputStream.write(new byte[]{0x01, 0x01});
            }catch(HostUnreachableException e) {
                outputStream.write(sendRequestResponse((byte)0x04, e.addressType, e.address, e.port));
            }catch(EOFException e) {
            }catch(Exception e) {
                e.printStackTrace();
            }
            socket.close();
        }catch(Exception e) {
            e.printStackTrace();
        }
        for (int x = 0, l = pool.size(); x < l; x++) {
            pool.get(x).interrupt();
        }
    }
    protected void handshake() throws
            UnsupportedProcotolException,
            UnsupportedAuthMethodException,
            UnacceptableAuthMethodException,
            IOException
    {
        byte protocolByte = (byte)readByte();
        if (protocolByte == 0x05) {
            this.protocol = ClientProtocol.Socks5;
        } else {
            throw new UnsupportedProcotolException();
        }
        int numberOfAuthMethods = readByte();
        ArrayList<AuthMethod> methods = new ArrayList<AuthMethod>();
        for (int x = 0; x < numberOfAuthMethods; x++) {
            int method = readByte();
            AuthMethod authMethod;
            switch(method) {
                case(0x00):
                    authMethod = AuthMethod.NoAuthentication;
                    break;
                case(0x01):
                    authMethod = AuthMethod.GSSAPI;
                    break;
                case(0x02):
                    authMethod = AuthMethod.Password;
                    break;
                default:
                    throw new UnsupportedAuthMethodException();
            }
            methods.add(authMethod);
        }
        if (server.getAuthenticationListener() != null) {
            if (methods.contains(AuthMethod.Password)) {
                this.authMethod = AuthMethod.Password;
            } else {
                throw new UnacceptableAuthMethodException();
            }
        }
        byte authMethodByte = 0x00;
        if (this.authMethod == AuthMethod.NoAuthentication) {
            authMethodByte = 0x00;
        } else if (this.authMethod == AuthMethod.Password) {
            authMethodByte = 0x02;
        }
        outputStream.write(new byte[]{protocolByte, authMethodByte});
    }
    protected void authentication() throws
            AuthenticationException,
            IOException
    {
        if (authMethod == AuthMethod.Password) {
            authenticationByPassword();
        }
    }
    protected void authenticationByPassword() throws
            AuthenticationException,
            IOException
    {
        byte version = (byte)readByte();
        if (version != 0x01) {
            throw new AuthenticationException();
        }
        int usernameLength = readByte();
        String username = "";
        for(int x = 0;x < usernameLength; x++) {
            username += (char)readByte();
        }
        int passwordLength = readByte();
        String password = "";
        for(int x = 0;x < passwordLength; x++) {
            password += (char)readByte();
        }
        boolean check = server.getAuthenticationListener().byPassword(username, password);
        if (!check){
            throw new AuthenticationException();
        }
        outputStream.write(new byte[]{version, 0x00});
    }
    protected void handleRequest() throws
            UnsupportedProcotolException,
            HostUnreachableException,
            IOException,
            Exception
    {
        int version = readByte();
        if (version != 0x05) {
            throw new UnsupportedProcotolException();
        }
        Socks5Command command;
        switch(readByte()) {
            case(0x01):
                command = Socks5Command.Connect;
                break;
            case(0x02):
                command = Socks5Command.Bind;
                break;
            case(0x03):
                command = Socks5Command.UDP_ASSOCIATE;
                break;
            default:
                throw new Exception();
        }
        readByte();

        AddressType addressType;
        switch(readByte()) {
            case(0x01):
                addressType = AddressType.IPv4;
                break;
            case(0x03):
                addressType = AddressType.Domain;
                break;
            case(0x04):
                addressType = AddressType.IPv6;
                break;
            default:
                throw new Exception();
        }
        String address = "";
        if (addressType == AddressType.IPv4) {
            address = Integer.toString(readByte()) + "." +  Integer.toString(readByte()) + "." +  Integer.toString(readByte()) + "." + Integer.toString(readByte());
        } else if (addressType == AddressType.Domain) {
            int domainSize = readByte();
            for (int x = 0; x < domainSize; x++) {
                address += (char) readByte();
            }
        } else if(addressType == AddressType.IPv6) {
            throw new Exception();
        }
        int port = readByte() * 256 + readByte();
        // System.out.println("address = " + address);
        // System.out.println("port = " + port);
        if (command == Socks5Command.Connect) {
            connectTo(address, port);
            this.outputStream.write(sendRequestResponse((byte)0x00, addressType, address, port));
            InputStream inputStream = remote.getInputStream();
            final OutputStream outputStream = remote.getOutputStream();
            InputStreamReader clientReader = new InputStreamReader(this.inputStream);
            clientReader.setAvailableListener(new InputStreamReader.AvailableListener(){
                public void onAvailable(byte[] bytes, int length) throws IOException {
                    outputStream.write(bytes, 0, length);
                }
            });
            final ClientReception that = this;
            InputStreamReader remoteReader = new InputStreamReader(inputStream);
            remoteReader.setAvailableListener(new InputStreamReader.AvailableListener(){
                public void onAvailable(byte[] bytes, int length) throws IOException {
                    that.outputStream.write(bytes, 0, length);
                }
            });
            pool.add(clientReader);
            pool.add(remoteReader);
            clientReader.start();
            remoteReader.start();
            while(clientReader.isAlive() && remoteReader.isAlive()) {
                Thread.sleep(500);
            }
            socket.close();
            remote.close();
        }
    }
    protected void connectTo(String ipv4, int port) throws
            HostUnreachableException,
            IOException
    {
        SocksServer.UpstreamProxy upstream = server.getUpstreamProxy();
        try{
            if (upstream != null) {
                String upstreamHostname = upstream.getHostname();
                int upstreamPort = upstream.getPort();
                String username = upstream.getUsername();
                String password = upstream.getPassword();
                SocksServer.UpstreamProxy.ProxyType type = upstream.getType();
                if (type == SocksServer.UpstreamProxy.ProxyType.HTTP) {
                    HttpProxyClient proxyClient = new HttpProxyClient(upstreamHostname, upstreamPort, username, password);
                    proxyClient.connectTo(ipv4, port);
                    remote = proxyClient;
                } else if (type == SocksServer.UpstreamProxy.ProxyType.Astar) {
                    AstarProxyClient proxyClient = new AstarProxyClient(upstreamHostname, upstreamPort, username, password);
                    proxyClient.connectTo(ipv4, port);
                    remote = proxyClient;
                } else if (type == SocksServer.UpstreamProxy.ProxyType.SOCKS5) {
                    remote = new Socks5ProxyClient(upstreamHostname, upstreamPort, username, password, ipv4, port);
                }
            } else {
                remote = new Socket(ipv4, port);
            }
        }catch(UnknownHostException e) {
            throw new HostUnreachableException(AddressType.IPv4, ipv4, port);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    protected int readByte() throws
            EOFException,
            IOException
    {
        return readByte(inputStream);
    }
    protected int readByte(InputStream inputStream) throws
            EOFException,
            IOException
    {
        int b = inputStream.read();
        if (b == -1) {
            throw new EOFException();
        }
        // System.out.println("Read from inputSteam: 0x" + Integer.toHexString(b));
        return b;
    }
    protected byte[] sendRequestResponse(byte status, AddressType addressType, String address, int port) {
        byte addressTypeByte = 0x00;
        int size = 4;
        if (addressType == AddressType.IPv4) {
            addressTypeByte = 0x01;
            size += 4;
        } else if(addressType == AddressType.Domain) {
            addressTypeByte = 0x03;
            size += 1 + address.length();
        } else if(addressType == AddressType.IPv6) {
            addressTypeByte = 0x04;
            size += 16;
        }
        byte[] bytes = new byte[size + 2];
        if (protocol == ClientProtocol.Socks5) {
            bytes[0] = 0x05;
        }
        bytes[1] = status;
        bytes[2] = 0x00;
        bytes[3] = addressTypeByte;
        if (addressType == AddressType.IPv4) {
            String[] parts = address.split("\\.");
            // System.out.println(address);
            // System.out.println(parts.length);
            for (int x = 0, l = parts.length; x < l; x++) {
                int part = Integer.valueOf(parts[x]);
                bytes[4 + x] = (byte)part;
                // System.out.println(bytes[4 + x]);
            }
        } else if(addressType == AddressType.Domain) {
            bytes[4] = (byte)address.length();
            for (int x = 0;x < bytes[4]; x++) {
                bytes[5 + x] = (byte)address.charAt(x);
            }
        } else if(addressType == AddressType.IPv6) {
            addressTypeByte = 0x04;
            size += 16;
        }
        bytes[size] = (byte)(port / 256);
        bytes[size + 1] = (byte)(port % 256);
        return bytes;
    }
}

