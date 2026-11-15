/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  GridMap
 *  RaceConnection
 *  RaceServer
 *  RaceSnapshot
 *  RaceWorld
 *  RaceWorld$Action
 *  RaceWorld$Player
 *  SearchController
 *  SearchStatus
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;
import java.util.function.BooleanSupplier;

public final class RaceChecks {
    private static int checks;

    private static void check(boolean condition, String message) {
        ++checks;
        if (!condition) {
            throw new AssertionError((Object)message);
        }
    }

    private static RaceWorld arena() {
        RaceWorld w = new RaceWorld(0);
        w.goalX = 12; w.goalY = 12;
        for (int y = 1; y < 24; ++y) {
            for (int x = 1; x < 24; ++x) {
                w.map[y][x] = 46;
            }
        }
        w.map[12][12] = 71;
        w.players[0].x = 5;
        w.players[0].y = 5;
        w.players[0].facing = 1;
        w.players[1].x = 7;
        w.players[1].y = 5;
        w.players[1].facing = 3;
        return w;
    }

    private static void press(RaceWorld w, int id, RaceWorld.Action action) {
        w.input(id, action, true);
        w.step();
        w.input(id, action, false);
    }

    private static void await(BooleanSupplier predicate, String message) throws Exception {
        long deadline = System.nanoTime() + 3000000000L;
        while (!predicate.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(15L);
        }
        RaceChecks.check(predicate.getAsBoolean(), message);
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && args[0].equals("--startup-probe")) {
            if (!args[1].equals("-")) {
                Files.writeString(Path.of(args[1], new String[0]), (CharSequence)"ready", new OpenOption[0]);
            }
            Thread.sleep(30000L);
            return;
        }
        RaceChecks.fogAndSearch();
        RaceChecks.movement();
        RaceChecks.combat();
        RaceChecks.navigation();
        RaceChecks.networking();
        RaceChecks.startup();
        RaceChecks.scoringAndTurns();
        System.out.println("PASS: " + checks + " checks (original search, asymmetric rules, fog, victory, loopback TCP)");
    }

    private static void fogAndSearch() throws Exception {
        RaceWorld w = new RaceWorld();
        RaceSnapshot a = w.snapshot(0, true);
        RaceSnapshot b = w.snapshot(1, true);
        RaceChecks.check(a.known().length() == 625 && a.known().chars().filter(c -> c == 63).count() > 580L, "Initial fog exposes maze");
        RaceChecks.check(a.enemyX() == -1 && b.enemyX() == -1, "Remote spawn leaks through fog");
        RaceChecks.check(a.known().charAt(w.players[1].y * 25 + w.players[1].x) == '?' && b.known().charAt(w.players[0].y * 25 + w.players[0].x) == '?', "Players share exploration");
        for (int id = 0; id < 2; ++id) {
            RaceWorld.Player p = w.players[id];
            char[][] copy = (char[][])Arrays.stream(w.map).map(rec$ -> (char[])((char[])rec$).clone()).toArray(x$0 -> new char[x$0][]);
            SearchController c2 = new SearchController(new GridMap(copy), p.y, p.x, w.goalY, w.goalX);
            SearchStatus status = SearchStatus.RUNNING;
            for (int i = 0; i < 2000 && status == SearchStatus.RUNNING; ++i) {
                status = c2.nextStep();
            }
            RaceChecks.check(status == SearchStatus.SUCCESS, "Original search cannot reach goal from player " + id);
            int[][] route = c2.getPath();
            RaceChecks.check(route[0][0] == p.y && route[0][1] == p.x, "Search ignores arbitrary start");
            RaceChecks.check(route[route.length - 1][0] == w.goalY && route[route.length - 1][1] == w.goalX, "Route has wrong goal");
            for (int i = 1; i < route.length; ++i) {
                RaceChecks.check(w.walkable(route[i][1], route[i][0]) && Math.abs(route[i][0] - route[i - 1][0]) + Math.abs(route[i][1] - route[i - 1][1]) == 1, "Illegal path segment");
            }
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        a.write(new DataOutputStream(bytes));
        RaceChecks.check(a.equals((Object)RaceSnapshot.read((DataInputStream)new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())))), "Snapshot roundtrip");
        RaceWorld blocked = RaceChecks.arena();
        blocked.map[5][6] = 35;
        RaceChecks.check(!blocked.lineOfSight(5, 5, 7, 5), "Vision crosses wall");
        RaceChecks.check(blocked.snapshot(0, true).enemyX() == -1, "Enemy leaks through wall");
        blocked.map[5][6] = 46;
        blocked.map[5][7] = 35;
        RaceChecks.check(!blocked.lineOfSight(6, 5, 7, 6), "Vision cuts diagonal corner");
    }

    private static void movement() {
        RaceWorld w = RaceChecks.arena();
        RaceChecks.press(w, 0, RaceWorld.Action.TURN_LEFT);
        RaceChecks.check(w.players[0].y == 5 && w.players[0].x == 5 && w.players[0].facing == 0, "Scout left turn must rotate without moving");
        int x = w.players[1].x;
        int y = w.players[1].y;
        RaceChecks.press(w, 1, RaceWorld.Action.TURN_LEFT);
        RaceChecks.check(w.players[1].x == x && w.players[1].y == y && w.players[1].facing == 2, "Tank must turn in place");
        w = RaceChecks.arena();
        w.input(0, RaceWorld.Action.FORWARD, true);
        w.input(1, RaceWorld.Action.FORWARD, true);
        for (int i = 0; i < 14; ++i) {
            w.step();
        }
        RaceChecks.check(w.players[0].x - 5 == 7 - w.players[1].x, "Both roles must move at the same speed");
        w = RaceChecks.arena();
        w.map[5][6] = 35;
        RaceChecks.press(w, 0, RaceWorld.Action.FORWARD);
        RaceChecks.check(w.players[0].x == 5, "Movement crosses wall");
    }

    private static void combat() {
        RaceWorld w = RaceChecks.arena();
        w.players[0].facing = 0;
        RaceChecks.press(w, 0, RaceWorld.Action.FIRE);
        RaceChecks.check(w.players[0].hits == 1 && w.players[1].stunnedUntil > w.tick, "EMP must hit sideways within radius");
        RaceChecks.check(w.players[1].jammedUntil - w.tick == 100 && w.players[1].stunnedUntil - w.tick == 40 && w.players[1].x == 7, "EMP must jam without knockback");
        int until = w.players[1].stunnedUntil;
        w.players[0].fireAt = 0;
        RaceChecks.press(w, 0, RaceWorld.Action.FIRE);
        RaceChecks.check(w.players[1].stunnedUntil == until, "Recovery immunity must prevent stun lock");
        w = RaceChecks.arena();
        w.map[5][6] = 35;
        RaceChecks.press(w, 0, RaceWorld.Action.FIRE);
        RaceChecks.check(w.players[0].hits == 0, "EMP must not cross cover");
        w = RaceChecks.arena();
        w.players[1].x = 10;
        RaceChecks.press(w, 0, RaceWorld.Action.FIRE);
        RaceChecks.check(w.players[0].hits == 0, "EMP range too long");
        w = RaceChecks.arena();
        w.players[0].x = 2;
        RaceChecks.press(w, 1, RaceWorld.Action.FIRE);
        RaceChecks.check(w.players[1].hits == 1 && w.players[0].x == 1, "Rail must hit at range and push away");
        RaceChecks.check(w.players[0].jammedUntil == 0 && w.players[0].stunnedUntil - w.tick == 50, "Rail effects must differ from EMP");
        int shots = w.players[1].shots;
        RaceChecks.press(w, 1, RaceWorld.Action.FIRE);
        RaceChecks.check(w.players[1].shots == shots, "Weapon cooldown ignored");
        w = RaceChecks.arena();
        w.players[0].y = 6;
        RaceChecks.press(w, 1, RaceWorld.Action.FIRE);
        RaceChecks.check(w.players[1].hits == 0, "Rail must require alignment");
        w = RaceChecks.arena();
        w.map[5][6] = 35;
        RaceChecks.press(w, 1, RaceWorld.Action.FIRE);
        RaceChecks.check(w.players[1].hits == 0, "Rail must stop at wall");
        w = RaceChecks.arena();
        w.players[1].x = 11;
        w.players[1].y = 12;
        w.players[1].facing = 1;
        w.players[0].x = 10;
        w.players[0].y = 12;
        w.players[0].facing = 1;
        RaceChecks.press(w, 1, RaceWorld.Action.FIRE);
        RaceChecks.check(w.players[1].hits == 0, "Cannon cannot fire backwards");
    }

    private static void navigation() {
        RaceWorld w;
        for (int id = 0; id < 2; ++id) {
            w = new RaceWorld();
            w.input(id, RaceWorld.Action.NAV, true);
            RaceChecks.check(w.players[id].navUsed && !w.players[id].route.isEmpty(), "Auto failed to activate");
            for (int i = 0; i < 19; ++i) {
                w.step();
            }
            RaceChecks.check(w.snapshot(id, true).navigating(), "Navigation ended before its one-second window");
            w.step();
            RaceChecks.check(w.tick == 20 && !w.snapshot(id, true).navigating(), "Navigation must expire at exactly one second");
            int x = w.players[id].x;
            int y = w.players[id].y;
            for (int i = 0; i < 80; ++i) {
                w.step();
            }
            RaceChecks.check(w.players[id].x == x && w.players[id].y == y && w.winner < 0, "Navigation continued after expiry");
            w.input(id, RaceWorld.Action.NAV, false);
            RaceChecks.press(w, id, RaceWorld.Action.NAV);
            RaceChecks.check(!w.snapshot(id, true).navigating(), "Second navigation use was granted");
            int facing = w.players[id].facing;
            RaceChecks.press(w, id, RaceWorld.Action.TURN_LEFT);
            RaceChecks.check(w.players[id].facing == (facing + 3) % 4, "Manual control did not return after expiry");
            w = new RaceWorld();
            RaceChecks.press(w, id, RaceWorld.Action.NAV);
            x = w.players[id].x;
            y = w.players[id].y;
            w.players[id].stunnedUntil = 50;
            while (w.tick < 60) {
                w.step();
            }
            RaceChecks.check(w.players[id].x == x && w.players[id].y == y && !w.snapshot(id, true).navigating(), "Stun extended the navigation deadline");
        }
        for (int attacker = 0; attacker < 2; ++attacker) {
            w = RaceChecks.arena();
            int victim = 1 - attacker;
            w.players[victim].moveAt = 100;
            RaceChecks.press(w, victim, RaceWorld.Action.NAV);
            RaceChecks.press(w, attacker, RaceWorld.Action.FIRE);
            RaceChecks.check(w.players[attacker].hits == 1 && w.players[victim].navUsed && !w.snapshot(victim, true).navigating(), "Attack must cancel navigation without refund");
            while (w.tick < 110) {
                w.step();
            }
            RaceChecks.press(w, victim, RaceWorld.Action.NAV);
            RaceChecks.check(w.players[victim].route.isEmpty(), "Interrupted navigation resumed or was refunded");
        }
        RaceWorld w2 = RaceChecks.arena();
        RaceChecks.press(w2, 1, RaceWorld.Action.FIRE);
        RaceChecks.check(w2.players[0].x == 3, "Cannon must push two free cells");
        w2 = RaceChecks.arena();
        w2.map[5][3] = 35;
        RaceChecks.press(w2, 1, RaceWorld.Action.FIRE);
        RaceChecks.check(w2.players[0].x == 4, "Two-cell push crossed a wall");
        w2 = RaceChecks.arena();
        w2.players[1].x = 10;
        RaceChecks.press(w2, 0, RaceWorld.Action.FIRE);
        RaceChecks.check(w2.players[0].hits == 0, "EMP reached beyond radius three");
        w2 = RaceChecks.arena();
        w2.players[1].x = 8;
        RaceChecks.press(w2, 0, RaceWorld.Action.FIRE);
        RaceChecks.check(w2.players[0].hits == 1, "EMP must reach radius three");
        w2 = RaceChecks.arena();
        w2.players[1].x = 10;
        w2.players[1].y = 12;
        w2.players[1].facing = 1;
        w2.players[0].x = 11;
        w2.players[0].y = 12;
        RaceChecks.press(w2, 1, RaceWorld.Action.FIRE);
        RaceChecks.check(w2.winner == 0 && w2.players[0].x == 12, "Knockback must stop and award victory on the goal");
        int tick = w2.tick;
        w2.step();
        RaceChecks.check(w2.tick == tick, "Finished match must freeze");
    }

    private static void scoringAndTurns() {
        for (int id = 0; id < 2; ++id) {
            RaceWorld w = RaceChecks.arena();
            RaceWorld.Player p = w.players[id];
            int facing = p.facing;
            w.input(id, RaceWorld.Action.TURN_LEFT, true);
            w.input(id, RaceWorld.Action.TURN_LEFT, false);
            w.step();
            RaceChecks.check(p.facing == (facing + 3) % 4 && p.steps == 0, "Quick turn lost or charged a movement step");
            while (w.tick < p.turnAt) {
                w.step();
            }
            w.input(id, RaceWorld.Action.TURN_RIGHT, true);
            for (int i = 0; i < 40; ++i) {
                w.step();
            }
            RaceChecks.check(p.facing == facing, "Holding turn rotated more than once");
            RaceChecks.check(RaceWorld.score((RaceWorld.Player)p) == 1000, "Turning changed efficiency score");
            w.input(id, RaceWorld.Action.TURN_RIGHT, false);
            RaceChecks.press(w, id, RaceWorld.Action.FORWARD);
            RaceChecks.check(p.steps == 1 && RaceWorld.score((RaceWorld.Player)p) == 990, "Forward movement did not deduct ten points");
            int x = p.x;
            int y = p.y;
            RaceChecks.check(w.snapshot(id, true).known().charAt(y * 25 + x) == ',', "Walked tile missing from trail");
            while (w.tick < p.moveAt) {
                w.step();
            }
            RaceChecks.press(w, id, RaceWorld.Action.BACK);
            RaceChecks.check(p.steps == 2 && RaceWorld.score((RaceWorld.Player)p) == 980, "Backtracking must cost another step");
            w.map[p.y + RaceWorld.DY[p.facing]][p.x + RaceWorld.DX[p.facing]] = 35;
            while (w.tick < p.moveAt) {
                w.step();
            }
            RaceChecks.press(w, id, RaceWorld.Action.FORWARD);
            RaceChecks.check(p.steps == 2, "Blocked movement charged a step");
        }
        RaceWorld w = RaceChecks.arena();
        RaceChecks.press(w, 1, RaceWorld.Action.FIRE);
        RaceChecks.check(w.players[0].steps == 2 && RaceWorld.score((RaceWorld.Player)w.players[0]) == 980, "Knockback steps not scored");
        RaceChecks.check(w.players[1].steps == 0 && RaceWorld.score((RaceWorld.Player)w.players[1]) == 1000, "Shooting should not charge movement steps");
        w = new RaceWorld();
        RaceChecks.press(w, 0, RaceWorld.Action.NAV);
        while (w.tick < 25) {
            w.step();
        }
        RaceChecks.check(w.players[0].steps > 0 && RaceWorld.score((RaceWorld.Player)w.players[0]) == 1000 - 10 * w.players[0].steps, "Navigation movement bypassed scoring");
        RaceChecks.check(w.snapshot(0, true).opponentSteps() == -1 && w.snapshot(0, true).opponentScore() == -1, "Live opponent progress leaked");
        w = RaceChecks.arena();
        w.players[0].x = 11;
        w.players[0].y = 12;
        w.players[0].facing = 1;
        RaceChecks.press(w, 0, RaceWorld.Action.FORWARD);
        RaceSnapshot result = w.snapshot(0, true);
        RaceChecks.check(w.winner == 0 && result.steps() == 1 && result.score() == 990 && result.opponentSteps() == 0 && result.opponentScore() == 1000, "Finish scoreboard incorrect");
        RaceChecks.press(w, 0, RaceWorld.Action.BACK);
        RaceChecks.check(w.players[0].steps == 1, "Final score changed after match end");
        w.players[0].steps = 200;
        RaceChecks.check(RaceWorld.score((RaceWorld.Player)w.players[0]) == 0, "Score fell below zero");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void startup() throws Exception {
        Path folder = Files.createTempDirectory("race-startup-check-", new FileAttribute[0]);
        Path ready = folder.resolve("ready");
        Path log = folder.resolve("player.log");
        String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        try {
            Process exited = new ProcessBuilder(javaExecutable, "-version").redirectErrorStream(true).redirectOutput(log.toFile()).start();
            exited.waitFor();
            boolean rejected = false;
            try {
                RaceLauncher.awaitWindow(exited, ready, log, 5);
            }
            catch (IOException expected) {
                rejected = true;
            }
            RaceChecks.check(rejected, "Launcher accepted an exited process without a window");
            for (boolean announces : new boolean[]{true, false}) {
                Process child = new ProcessBuilder(javaExecutable, "-cp", System.getProperty("java.class.path"), "RaceChecks", "--startup-probe", announces ? ready.toString() : "-").redirectErrorStream(true).redirectOutput(log.toFile()).start();
                try {
                    if (announces) {
                        RaceLauncher.awaitWindow(child, ready, log, 5);
                        RaceChecks.check(child.isAlive(), "Launcher did not wait for a live ready process");
                        continue;
                    }
                    rejected = false;
                    try {
                        RaceLauncher.awaitWindow(child, ready, log, 0);
                    }
                    catch (IOException expected) {
                        rejected = true;
                    }
                    RaceChecks.check(rejected, "Launcher did not time out an unready process");
                }
                finally {
                    child.destroy();
                    child.waitFor();
                    Files.deleteIfExists(ready);
                }
            }
        }
        finally {
            Files.deleteIfExists(log);
            Files.deleteIfExists(folder);
        }
    }

    private static void networking() throws Exception {
        try (RaceServer server = new RaceServer(0);
             RaceConnection a = new RaceConnection(server.port());
             RaceConnection b = new RaceConnection(server.port());){
            RaceChecks.await(() -> a.latest != null && b.latest != null && a.latest.ready() && b.latest.ready(), "TCP clients never became ready");
            RaceChecks.check(a.id == 0 && b.id == 1, "Server assigned duplicate roles");
            RaceChecks.check(a.latest.enemyX() == -1 && b.latest.enemyX() == -1, "Network leaked opponent spawn");
            int ax = a.latest.x(), ay = a.latest.y(), bx = b.latest.x(), by = b.latest.y();
            a.send(0, RaceWorld.Action.NAV, true);
            a.send(0, RaceWorld.Action.NAV, false);
            a.send(1, RaceWorld.Action.NAV, true);
            a.send(1, RaceWorld.Action.NAV, false);
            RaceChecks.await(() -> a.latest.navUsed() && b.latest.navUsed(), "Shared keyboard did not control both clients");
            RaceChecks.await(() -> !(a.latest.x() == ax && a.latest.y() == ay || b.latest.x() == bx && b.latest.y() == by), "Network snapshots did not update positions");
            RaceChecks.await(() -> !a.latest.navigating() && !b.latest.navigating(), "Navigation expiry did not reach both clients");
            RaceChecks.check(a.latest.navUsed() && b.latest.navUsed(), "Network restored navigation charges");
            a.releaseAll();
            b.close();
            RaceChecks.await(() -> a.error != null, "Disconnect did not stop the other client");
        }
    }
}

