package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class MudClient {
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 5555;

    public static void main(String[] args) {
        String host = resolveHost(args);
        int port = resolvePort(args);

        try (Socket socket = new Socket(host, port);
             BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter serverWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {

            Thread readerThread = new Thread(() -> pumpServerOutput(serverReader), "mud-client-reader");
            readerThread.setDaemon(true);
            readerThread.start();

            String line;
            while ((line = consoleReader.readLine()) != null) {
                serverWriter.write(line);
                serverWriter.write("\n");
                serverWriter.flush();
            }
        } catch (IOException exception) {
            System.out.println("Unable to connect to the MUD server: " + exception.getMessage());
        }
    }

    private static void pumpServerOutput(BufferedReader reader) {
        try {
            int ch;
            while ((ch = reader.read()) != -1) {
                System.out.print((char) ch);
            }
        } catch (IOException ignored) {
        }
    }

    private static String resolveHost(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if ("--host".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                return args[i + 1].trim();
            }
        }
        return DEFAULT_HOST;
    }

    private static int resolvePort(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if ("--port".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                return parsePort(args[i + 1]);
            }
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
