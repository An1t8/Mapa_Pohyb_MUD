package server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class PlayerAccountStore {
    private final Path accountDir;

    public PlayerAccountStore(String dir) {
        this.accountDir = Path.of(dir);
    }

    public synchronized boolean accountExists(String username) {
        return Files.exists(accountFile(username));
    }

    public synchronized boolean register(String username, String password) {
        if (password == null || password.isBlank() || accountExists(username)) {
            return false;
        }
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String hash = hashPassword(password, salt);
        List<String> lines = List.of(
                "username=" + username,
                "salt=" + Base64.getEncoder().encodeToString(salt),
                "hash=" + hash,
                "planet=Station",
                "inventory=",
                "stationCrystals=",
                "comet1=",
                "comet2=",
                "finished=false"
        );
        try {
            Files.createDirectories(accountDir);
            Files.write(accountFile(username), lines, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public synchronized AccountData authenticate(String username, String password) {
        AccountData data = load(username);
        if (data == null) return null;
        String check = hashPassword(password, Base64.getDecoder().decode(data.salt));
        return check.equals(data.hash) ? data : null;
    }

    public synchronized AccountData load(String username) {
        try {
            Path file = accountFile(username);
            if (!Files.exists(file)) return null;
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            return AccountData.fromLines(lines);
        } catch (IOException e) {
            return null;
        }
    }

    public synchronized boolean saveProgress(MudPlayer player) {
        AccountData data = load(player.getName());
        if (data == null) return false;
        data.planet = player.getCurrentRoomName();
        data.inventory = new ArrayList<>(player.getInventorySnapshot());
        data.stationCrystals = new ArrayList<>(player.getStationCrystalsSnapshot());
        data.cometOne = new ArrayList<>(player.getCometOneSnapshot());
        data.cometTwo = new ArrayList<>(player.getCometTwoSnapshot());
        data.finished = player.isFinished();
        try {
            Files.write(accountFile(player.getName()), data.toLines(), StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private String hashPassword(String password, byte[] salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Path accountFile(String username) {
        return accountDir.resolve(MudWorld.normalize(username).replace(' ', '_') + ".player");
    }

    public static class AccountData {
        String username;
        String salt;
        String hash;
        String planet;
        List<String> inventory = new ArrayList<>();
        List<String> stationCrystals = new ArrayList<>();
        List<String> cometOne = new ArrayList<>();
        List<String> cometTwo = new ArrayList<>();
        boolean finished;

        static AccountData fromLines(List<String> lines) {
            AccountData d = new AccountData();
            for (String line : lines) {
                int idx = line.indexOf('=');
                if (idx < 0) continue;
                String k = line.substring(0, idx);
                String v = line.substring(idx + 1);
                switch (k) {
                    case "username" -> d.username = v;
                    case "salt" -> d.salt = v;
                    case "hash" -> d.hash = v;
                    case "planet" -> d.planet = v;
                    case "inventory" -> {
                        addDelimitedValues(v, d.inventory);
                    }
                    case "stationCrystals" -> addDelimitedValues(v, d.stationCrystals);
                    case "comet1" -> addDelimitedValues(v, d.cometOne);
                    case "comet2" -> addDelimitedValues(v, d.cometTwo);
                    case "finished" -> d.finished = Boolean.parseBoolean(v);
                }
            }
            return d;
        }

        List<String> toLines() {
            return List.of(
                    "username=" + username,
                    "salt=" + salt,
                    "hash=" + hash,
                    "planet=" + planet,
                    "inventory=" + String.join("|", inventory),
                    "stationCrystals=" + String.join("|", stationCrystals),
                    "comet1=" + String.join("|", cometOne),
                    "comet2=" + String.join("|", cometTwo),
                    "finished=" + finished
            );
        }

        private static void addDelimitedValues(String raw, List<String> target) {
            if (raw.isBlank()) {
                return;
            }
            for (String item : raw.split("\\|")) {
                if (!item.isBlank()) {
                    target.add(item);
                }
            }
        }
    }
}
