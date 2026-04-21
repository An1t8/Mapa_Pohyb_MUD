package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TCP server that accepts multiple players and gives each one an independent
 * session on a virtual thread.
 */
public class MudServer {

    private final int port;
    private final MudWorld world;

    public MudServer(int port, String mapFile) {
        this.port = port;
        this.world = new MudWorld(mapFile);
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket();
             ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {

            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(port));
            System.out.println("Mapa_Pohyb_MUD server listening on port " + port);
            System.out.println("Connect with: nc localhost " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(new MudClientSession(clientSocket, world));
            }
        }
    }
}
