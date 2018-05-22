package io.github.astarProxy;

/**
 * Created by maryam on 5/19/2018.
 */

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class SocksServer implements Runnable {
    protected AuthenticationListener authenticationListener;
    protected UpstreamProxy upstreamProxy;
    protected ServerSocket socket;
    protected int port = 1080;
    protected ArrayList<ClientReception> counters;
    protected ArrayList<Thread> connections;

    public SocksServer() {
        this(1080);
    }
    public SocksServer(int port) {
        this.port = port;
        this.counters = new ArrayList<ClientReception>();
        this.connections = new ArrayList<Thread>();
    }
    public void setAuthenticationListener(AuthenticationListener authenticationListener){
        this.authenticationListener = authenticationListener;
    }
    public AuthenticationListener getAuthenticationListener() {
        return this.authenticationListener;
    }
    public void setUpstreamProxy(UpstreamProxy upstreamProxy){
        this.upstreamProxy = upstreamProxy;
    }
    public UpstreamProxy getUpstreamProxy() {
        return this.upstreamProxy;
    }
    public void run() {
        try {
            socket = new ServerSocket(this.port);
            while (true) {
                Socket client = socket.accept();
                ClientReception reception = new ClientReception(client, this);
                Thread connection = new Thread(reception);
                connections.add(connection);
                connection.start();
            }
        } catch (Exception e) {
            System.out.println("S: Error");
            e.printStackTrace();
        }
    }
    public interface AuthenticationListener {
        boolean byPassword(String username, String password);
    }
    public interface UpstreamProxy {
        public enum ProxyType {
            HTTP,
            SOCKS5,
            Astar,
        };
        ProxyType getType();
        String getHostname();
        int getPort();
        String getUsername();
        String getPassword();
    }
}
