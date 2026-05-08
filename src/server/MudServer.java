package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MudServer {
    private final int port;
    private final MudWorld world;
    private final PlayerAccountStore accountStore;
    private final ServerLogger logger;

    public MudServer(int port, String mapFile) {
        this.port = port;
        this.world = new MudWorld(mapFile);
        this.accountStore = new PlayerAccountStore("res/players");
        this.logger = new ServerLogger("res/server.log");
    }

    public void start() throws IOException {
        ExecutorService executorService = Executors.newCachedThreadPool();
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(port));
            System.out.println("MUD server listening on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(new MudClientSession(clientSocket, world, accountStore, logger));
            }
        } finally {
            executorService.shutdown();
        }
    }
}
