/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  RaceConnection
 *  RaceGame
 *  RaceServer
 *  RaceSnapshot
 *  RaceWorld$Action
 *  com.jme3.app.state.AppState
 *  com.jme3.app.state.ScreenshotAppState
 *  com.jme3.system.AppSettings
 */
import com.jme3.app.state.AppState;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.system.AppSettings;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

public final class RaceLauncher {
    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void main(String[] args) throws Exception {
        boolean check;
        if (args.length >= 1 && args[0].equals("--garage")) {
            int exit;
            ArrayList<String> command = new ArrayList<String>();
            command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
            if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                command.add("-XstartOnFirstThread");
            }
            command.addAll(List.of("-cp", System.getProperty("java.class.path"), "VehicleGarage"));
            if (args.length == 2) {
                command.add(args[1]);
            }
            if ((exit = new ProcessBuilder(command).inheritIO().start().waitFor()) != 0) {
                throw new IOException("Vehicle workshop failed: " + exit);
            }
            return;
        }
        if (args.length >= 5 && args[0].equals("--client")) {
            System.out.println("Runtime: Java " + System.getProperty("java.version") + " / " + System.getProperty("os.name") + " / " + System.getProperty("os.arch"));
            RaceConnection connection = new RaceConnection(Integer.parseInt(args[1]));
            if (connection.id != Integer.parseInt(args[2])) {
                throw new IOException("Unexpected player assignment");
            }
            Path ready = Path.of(args[3], new String[0]);
            RaceGame game = new RaceGame(connection, () -> {
                try {
                    Files.writeString(ready, (CharSequence)("First frame rendered for player " + (connection.id + 1)), new OpenOption[0]);
                }
                catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            AppSettings settings = new AppSettings(true);
            settings.setTitle(connection.id == 0 ? "GRID//RIFT  |  01 SCOUT" : "GRID//RIFT  |  02 SIEGE");
            settings.setResolution(800, 640);
            settings.setResizable(false);
            settings.setFrameRate(60);
            settings.setAudioRenderer(null);
            settings.setVSync(true);
            settings.setSamples(2);
            boolean mac = System.getProperty("os.name").toLowerCase().contains("mac");
            settings.setCenterWindow(false);
            settings.setWindowXPosition(20 + connection.id * (mac ? 60 : 820));
            settings.setWindowYPosition(40 + (mac ? connection.id * 60 : 0));
            game.setSettings(settings);
            game.setShowSettings(false);
            game.start();
            if (!args[4].equals("-")) {
                RaceLauncher.visualCheck(game, connection, Path.of(args[4], new String[0]));
            }
            return;
        }
        boolean bl = check = args.length == 2 && args[0].equals("--visual-check");
        if (args.length != 0 && !check) {
            throw new IllegalArgumentException("Usage: RaceLauncher [--visual-check OUTPUT_DIR]");
        }
        if (check) {
            Path output = Path.of(args[1], new String[0]);
            Files.createDirectories(output, new FileAttribute[0]);
            try (Stream<Path> files = Files.list(output);){
                if (files.findAny().isPresent()) {
                    throw new IOException("Visual check needs a new empty output directory");
                }
            }
        }
        Path logs = Files.createDirectories(Path.of("logs", new String[0]), new FileAttribute[0]);
        Path runLogs = Files.createTempDirectory(logs, "run-", new FileAttribute[0]).toAbsolutePath();
        Path session = Files.createTempDirectory("gridrift-session-", new FileAttribute[0]);
        System.out.println("Window logs: " + String.valueOf(runLogs));
        try (RaceServer server = new RaceServer(0);){
            ArrayList<Process> clients = new ArrayList<Process>();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                server.close();
                clients.forEach(Process::destroy);
            }));
            try {
                for (int id = 0; id < 2; ++id) {
                    ArrayList<String> command = new ArrayList<String>();
                    command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
                    command.add("--enable-native-access=ALL-UNNAMED");
                    if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                        command.add("-XstartOnFirstThread");
                        command.add("-Xdock:name=GRID RIFT - Player " + (id + 1));
                    }
                    Path nativeTemp = Files.createDirectory(session.resolve("player-" + (id + 1)), new FileAttribute[0]);
                    command.add("-Djava.io.tmpdir=" + String.valueOf(nativeTemp));
                    Path ready = session.resolve("player-" + (id + 1) + ".ready");
                    Path log = runLogs.resolve("player-" + (id + 1) + ".log");
                    command.add("-XX:ErrorFile=" + String.valueOf(runLogs.resolve("player-" + (id + 1) + "-crash.log")));
                    command.addAll(List.of("-cp", System.getProperty("java.class.path"), "RaceLauncher", "--client", Integer.toString(server.port()), Integer.toString(id), ready.toString(), check ? Path.of(args[1], new String[0]).toAbsolutePath().toString() : "-"));
                    Process client = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
                    clients.add(client);
                    RaceLauncher.awaitWindow(client, ready, log, 60);
                    System.out.println("Player " + (id + 1) + " window ready.");
                }
                System.out.println("GRID//RIFT on 127.0.0.1:" + server.port() + " | close either window to end the match.");
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(45L);
                while (clients.stream().allMatch(Process::isAlive)) {
                    if (check && System.nanoTime() > deadline) {
                        throw new IOException("Visual check timed out");
                    }
                    Thread.sleep(100L);
                }
                if (!check) {
                    for (int id = 0; id < clients.size(); ++id) {
                        Process client = (Process)clients.get(id);
                        if (client.isAlive() || client.exitValue() == 0) continue;
                        throw RaceLauncher.windowFailure("Player " + (id + 1) + " exited with code " + client.exitValue(), runLogs.resolve("player-" + (id + 1) + ".log"));
                    }
                }
                if (check) {
                    for (Process client : clients) {
                        if (client.waitFor(15L, TimeUnit.SECONDS) && client.exitValue() == 0) continue;
                        throw new IOException("Visual client failed");
                    }
                    for (int id = 0; id < 2; ++id) {
                        if (Files.exists(Path.of(args[1], "player-" + id + ".pass"), new LinkOption[0])) continue;
                        throw new IOException("Missing visual result for player " + id);
                    }
                    System.out.println("PASS: two TCP clients, first-person scenes, fog maps, navigation and rendered screenshots");
                }
            }
            finally {
                clients.forEach(Process::destroy);
                for (Process client : clients) {
                    if (client.waitFor(5L, TimeUnit.SECONDS)) continue;
                    client.destroyForcibly().waitFor();
                }
                try (Stream<Path> files = Files.walk(session, new FileVisitOption[0]);){
                    for (Path file : files.sorted(Comparator.reverseOrder()).toList()) {
                        Files.deleteIfExists(file);
                    }
                }
            }
        }
    }

    static void awaitWindow(Process client, Path ready, Path log, int timeoutSeconds) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        while (!Files.isRegularFile(ready, new LinkOption[0])) {
            if (!client.isAlive()) {
                throw RaceLauncher.windowFailure("Window exited before rendering (code " + client.exitValue() + ")", log);
            }
            if (System.nanoTime() > deadline) {
                throw RaceLauncher.windowFailure("Window did not render within " + timeoutSeconds + " seconds", log);
            }
            Thread.sleep(50L);
        }
        if (!client.isAlive()) {
            throw RaceLauncher.windowFailure("Window exited during startup", log);
        }
    }

    private static IOException windowFailure(String message, Path log) throws IOException {
        System.err.println(message + ". Log: " + String.valueOf(log));
        if (Files.exists(log, new LinkOption[0])) {
            List<String> lines = Files.readAllLines(log);
            for (String line : lines.subList(Math.max(0, lines.size() - 30), lines.size())) {
                System.err.println(line);
            }
        }
        return new IOException(message + ". Please share " + String.valueOf(log));
    }

    private static void visualCheck(RaceGame game, RaceConnection connection, Path output) {
        Thread.ofPlatform().name("visual-check").start(() -> {
            try {
                Files.createDirectories(output, new FileAttribute[0]);
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(90L);
                while (!(connection.latest != null && connection.latest.ready() || System.nanoTime() >= deadline)) {
                    Thread.sleep(50L);
                }
                RaceSnapshot start = connection.latest;
                if (start == null || !start.ready()) {
                    throw new AssertionError((Object)"Server never became ready");
                }
                deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(25L);
                while (!((Boolean)game.enqueue(() -> ((RaceGame)game).isViewReady()).get(10L, TimeUnit.SECONDS)).booleanValue()) {
                    if (System.nanoTime() > deadline) {
                        throw new AssertionError((Object)"First rendered view never became ready");
                    }
                    Thread.sleep(50L);
                }
                game.enqueue(() -> {
                    game.verifyView();
                    return true;
                }).get(10L, TimeUnit.SECONDS);
                int leftKey = connection.id == 0 ? 30 : 203;
                int rightKey = connection.id == 0 ? 32 : 205;
                game.enqueue(() -> {
                    game.handleKey(leftKey, true);
                    return true;
                }).get(5L, TimeUnit.SECONDS);
                Thread.sleep(600L);
                if (connection.latest.facing() != (start.facing() + 3) % 4) {
                    throw new AssertionError((Object)"Left key mapping/one-press turn failed");
                }
                game.enqueue(() -> {
                    game.handleKey(leftKey, false);
                    game.handleKey(rightKey, true);
                    return true;
                }).get(5L, TimeUnit.SECONDS);
                Thread.sleep(600L);
                if (connection.latest.facing() != start.facing()) {
                    throw new AssertionError((Object)"Right key mapping failed");
                }
                game.enqueue(() -> {
                    game.handleKey(rightKey, false);
                    return true;
                }).get(5L, TimeUnit.SECONDS);
                connection.send(connection.id, RaceWorld.Action.NAV, true);
                connection.send(connection.id, RaceWorld.Action.NAV, false);
                Thread.sleep(2200L);
                RaceSnapshot after = connection.latest;
                if (!after.navUsed() || after.navigating() || after.x() == start.x() && after.y() == start.y()) {
                    throw new AssertionError((Object)"Network navigation did not move player");
                }
                if (after.steps() == 0 || after.score() != Math.max(0, 1000 - 10 * after.steps())) {
                    throw new AssertionError((Object)"Network score did not reflect movement");
                }
                game.enqueue(() -> {
                    game.verifyView();
                    return true;
                }).get(10L, TimeUnit.SECONDS);
                ScreenshotAppState screenshot = new ScreenshotAppState(String.valueOf(output.toAbsolutePath()) + File.separator, "player-" + connection.id);
                screenshot.setIsNumbered(false);
                game.enqueue(() -> {
                    game.getStateManager().attach((AppState)screenshot);
                    return true;
                }).get(5L, TimeUnit.SECONDS);
                Thread.sleep(250L);
                game.enqueue(() -> {
                    screenshot.takeScreenshot();
                    return true;
                }).get(5L, TimeUnit.SECONDS);
                Path picture = output.resolve("player-" + connection.id + ".png");
                BufferedImage image = null;
                while (image == null && System.nanoTime() < deadline) {
                    try {
                        if (Files.exists(picture, new LinkOption[0])) {
                            image = ImageIO.read(picture.toFile());
                        }
                    }
                    catch (IOException iOException) {
                        // empty catch block
                    }
                    if (image != null) continue;
                    Thread.sleep(50L);
                }
                if (image == null) {
                    throw new AssertionError((Object)"Screenshot was not written");
                }
                HashSet<Integer> colors = new HashSet<Integer>();
                for (int y = 0; y < image.getHeight(); y += 4) {
                    for (int x = 0; x < image.getWidth(); x += 4) {
                        colors.add(image.getRGB(x, y));
                    }
                }
                if (image.getWidth() != 800 || colors.size() < 50) {
                    throw new AssertionError((Object)"Blank or incomplete rendered frame");
                }
                Files.writeString(output.resolve("player-" + connection.id + ".pass"), (CharSequence)"scene + fog + first-person camera + network navigation passed\n", new OpenOption[0]);
                while (!(Files.exists(output.resolve("player-0.pass"), new LinkOption[0]) && Files.exists(output.resolve("player-1.pass"), new LinkOption[0]) || System.nanoTime() >= deadline)) {
                    Thread.sleep(50L);
                }
                game.stop(true);
            }
            catch (Throwable e) {
                e.printStackTrace();
                game.stop(true);
                System.exit(1);
            }
        });
    }
}

