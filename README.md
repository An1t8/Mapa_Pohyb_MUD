# Mapa_Pohyb_MUD

This repository keeps the original console game and adds a TCP multiplayer MUD server on top of the same world map.

## What is included

- `main.Main` starts the original console game by default.
- `main.Main --server --port 5555` starts the TCP MUD server.
- `main.MudClient --host 127.0.0.1 --port 5555` starts a simple Java client for the TCP server.
- The world map is loaded from `res/map.csv`.
- Planet gatekeeper questions are loaded from `res/questions.txt`.
- Multiplayer player accounts and progress are stored in `res/players/`.
- Server activity is logged to `res/server.log`.

## Main server commands

- `help`
- `rules`
- `fly <planet>`
- `talk`
- `prompter`
- `take`
- `position`
- `show`
- `hint`
- `check`
- `comet`
- `cometplan`
- `bigbang`
- `save`
- `load`
- `leave`

## Compile the project

```bash
mkdir -p out
javac -d out $(find src -path 'src/test' -prune -o -name '*.java' -print)
```

## Run the original console game

```bash
java -cp out main.Main
```

## Run the TCP MUD server

```bash
java -cp out main.Main --server --port 5555
```

## Connect to the TCP MUD

Using the custom Java client:

```bash
java -cp out main.MudClient --host 127.0.0.1 --port 5555
```

Or using `nc`:

```bash
nc localhost 5555
```

## IntelliJ IDEA

Open the project as a plain Java project.

- For the console version run `main.Main` with no arguments.
- For the server run `main.Main` with `--server --port 5555`.
- For the client run `main.MudClient` with `--host 127.0.0.1 --port 5555`.
