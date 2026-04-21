# Mapa_Pohyb_MUD

This repository keeps the original planet-based project and adds a TCP MUD server on top of the same world map.

## What changed

- The planet map from `res/map.csv` is reused as the room graph.
- `main.Main` now starts the TCP server by default.
- Each connected client gets its own player session and inventory.
- Rooms show exits, items, NPCs, and other connected players.
- Czech and English command aliases are both supported.

## Supported commands

- `pomoc` or `help`
- `prozkoumej` or `look`
- `jdi <mistnost>` or `fly <planet>`
- `vezmi <predmet>` or `take <item>`
- `odloz <predmet>` or `drop <item>`
- `inventar` or `inventory`
- `mluv <npc>` or `talk <npc>`
- `konec` or `quit`

## Run the server

```bash
mkdir -p out
javac -d out $(find src -path 'src/test' -prune -o -name '*.java' -print)
java -cp out main.Main --port 5555
```

Then connect from another terminal or a TCP client:

```bash
nc localhost 5555
```

## Optional legacy console mode

The original single-player console can still be started with:

```bash
java -cp out main.Main --console
```

## IntelliJ IDEA

The project can be opened and run directly as a plain Java project. Run `main.Main` and pass `--port 5555` if you want a specific port.
