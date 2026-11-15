import java.util.*;

/** Run with java -cp 'lib/*' RandomMapChecks. */
public final class RandomMapChecks {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    private static int[][] distances(RaceWorld w, int x, int y, int[] blocked) {
        int[][] d = new int[25][25];
        for (int[] row : d) Arrays.fill(row, -1);
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        d[y][x] = 0;
        queue.add(new int[]{x, y});
        while (!queue.isEmpty()) {
            int[] p = queue.remove();
            for (int i = 0; i < 4; i++) {
                int nx = p[0] + RaceWorld.DX[i], ny = p[1] + RaceWorld.DY[i];
                if (!w.walkable(nx, ny) || d[ny][nx] >= 0 ||
                    blocked != null && nx == blocked[0] && ny == blocked[1]) continue;
                d[ny][nx] = d[p[1]][p[0]] + 1;
                queue.add(new int[]{nx, ny});
            }
        }
        return d;
    }

    public static void main(String[] args) {
        Set<String> layouts = new HashSet<>(), spawns = new HashSet<>(), goals = new HashSet<>();
        for (int seed = 0; seed < 1000; seed++) {
            RaceWorld w = new RaceWorld(seed);
            RaceWorld again = new RaceWorld(seed);
            check(Arrays.deepEquals(w.map, again.map), "Seed cannot reproduce map " + seed);
            layouts.add(Arrays.deepToString(w.map));
            spawns.add(w.players[0].x + "," + w.players[0].y);
            goals.add(w.goalX + "," + w.goalY);
            int[][] a = distances(w, w.players[0].x, w.players[0].y, null);
            int[][] b = distances(w, w.players[1].x, w.players[1].y, null);
            int goalCount = 0;
            for (int y = 0; y < 25; y++) for (int x = 0; x < 25; x++) {
                if (w.map[y][x] == 'G') goalCount++;
                if (w.walkable(x, y)) check(a[y][x] >= 0, "Disconnected floor seed " + seed);
                if (x == 0 || y == 0 || x == 24 || y == 24)
                    check(!w.walkable(x, y), "Open boundary");
            }
            check(goalCount == 1 && w.map[w.goalY][w.goalX] == 'G', "Invalid finish");
            check(a[w.goalY][w.goalX] >= 20 && a[w.goalY][w.goalX] == b[w.goalY][w.goalX], "Unfair finish distance seed " + seed);
            check(w.snapshot(0, true).enemyX() == -1 && w.snapshot(1, true).enemyX() == -1, "Spawns visible to opponents");
            int last = -1;
            for (int[] gate : w.encounterGates) {
                check(a[gate[1]][gate[0]] == b[gate[1]][gate[0]], "Unfair gate distance");
                check(a[gate[1]][gate[0]] > last, "Gates not sequential");
                last = a[gate[1]][gate[0]];
                for (RaceWorld.Player p : w.players)
                    check(distances(w, p.x, p.y, gate)[w.goalY][w.goalX] == -1, "Gate bypass seed " + seed);
            }
            for (int id = 0; id < 2; id++) {
                RaceWorld nav = new RaceWorld(seed);
                nav.input(id, RaceWorld.Action.NAV, true);
                check(!nav.players[id].route.isEmpty(), "Original search failed random seed " + seed);
                int[] finish = nav.players[id].route.getLast();
                check(finish[0] == nav.goalY && finish[1] == nav.goalX, "Navigation uses fixed goal");
            }
            // Winning must depend on the generated finish, not the old center.
            RaceWorld finish = new RaceWorld(seed);
            RaceWorld.Player p = finish.players[0];
            p.x = finish.goalX; p.y = finish.goalY;
            finish.step();
            check(finish.winner == 0, "Random finish not awarded");
            RaceWorld push = new RaceWorld(seed);
            for (int direction = 0; direction < 4; direction++) {
                int dx = RaceWorld.DX[direction], dy = RaceWorld.DY[direction];
                int ax = push.goalX - 2 * dx, ay = push.goalY - 2 * dy;
                if (ax < 1 || ay < 1 || ax > 23 || ay > 23) continue;
                push.players[1].x = ax; push.players[1].y = ay;
                push.players[1].facing = direction;
                push.players[0].x = ax + dx; push.players[0].y = ay + dy;
                push.map[ay][ax] = '.'; push.map[ay + dy][ax + dx] = '.';
                push.input(1, RaceWorld.Action.FIRE, true);
                push.step();
                check(push.winner == 0 && push.players[0].x == push.goalX && push.players[0].y == push.goalY,
                      "Knockback missed random finish " + seed);
                break;
            }
            RaceWorld center = new RaceWorld(seed);
            center.players[0].x = 12; center.players[0].y = 12;
            center.step();
            check(center.winner == -1, "Old center still awards victory");
        }
        check(layouts.size() == 1000 && spawns.size() > 50 && goals.size() >= 16, "Insufficient map/spawn/finish variety");
        Set<String> fresh = new HashSet<>();
        for (int i = 0; i < 20; i++) fresh.add(Arrays.deepToString(new RaceWorld().map));
        check(fresh.size() == 20, "Default matches reuse a fixed seed");
        System.out.println("PASS: 1000 seeds: connected maps, equal routes, three unavoidable gates, dynamic goals, original navigation; 20 fresh random matches");
    }
}
