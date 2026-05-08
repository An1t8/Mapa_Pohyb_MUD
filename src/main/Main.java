package main;

import server.MudServer;

public class Main {

    private static final int DEFAULT_PORT = 5555;

    public static void main(String[] args) {
        if (containsArg(args, "--server")) {
            int port = resolvePort(args);
            try {
                MudServer mudServer = new MudServer(port, "res/map.csv");
                mudServer.start();
            } catch (Exception exception) {
                System.out.println("Unable to start the MUD server: " + exception.getMessage());
            }
            return;
        }

        GameConsole gameConsole = new GameConsole();
        gameConsole.start();
    }

    private static boolean containsArg(String[] args, String target) {
        for (String arg : args) {
            if (target.equalsIgnoreCase(arg)) {
                return true;
            }
        }
        return false;
    }

    private static int resolvePort(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if ("--port".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                return parsePort(args[i + 1]);
            }
            if (args[i].matches("\\d+")) {
                return parsePort(args[i]);
            }
        }

        String propertyPort = System.getProperty("mud.port");
        if (propertyPort != null && !propertyPort.isBlank()) {
            return parsePort(propertyPort);
        }

        String environmentPort = System.getenv("MUD_PORT");
        if (environmentPort != null && !environmentPort.isBlank()) {
            return parsePort(environmentPort);
        }

        return DEFAULT_PORT;
    }

    private static int parsePort(String portText) {
        try {
            int port = Integer.parseInt(portText.trim());
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("Port must be between 1 and 65535.");
            }
            return port;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid port: " + portText, exception);
        }
    }
}
