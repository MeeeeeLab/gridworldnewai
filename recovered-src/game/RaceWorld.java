/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  GridMap
 *  SearchController
 *  SearchStatus
 */
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class RaceWorld {
    public static final int SIZE = 25;
    public static final int HZ = 20;
    public static final int MOVE_TICKS = 5;
    public static final int TURN_TICKS = 4;
    public static final int[] DX = new int[]{0, 1, 0, -1};
    public static final int[] DY = new int[]{-1, 0, 1, 0};
    public final char[][] map = new char[SIZE][SIZE];
    public final Player[] players = new Player[2];
    public final long seed;
    public int goalX;
    public int goalY;
    public final int[][] encounterGates = new int[3][2];
    public int tick;
    public int winner = -1;

    public RaceWorld() {
        this(new java.security.SecureRandom().nextLong());
    }

    public RaceWorld(long seed) {
        this.seed = seed;
        generate(new Random(seed));
        for (Player p : this.players) {
            p.visited[p.y][p.x] = true;
            this.reveal(p);
        }
    }

    public static int score(Player player) {
        return Math.max(0, 1000 - 10 * player.steps);
    }

    public void input(int id, Action action, boolean down) {
        boolean fresh;
        Player p = this.players[id];
        boolean bl = fresh = down && p.held.add(action);
        if (!down) {
            p.held.remove((Object)action);
        }
        if (this.winner >= 0 || !fresh || this.tick < p.stunnedUntil) {
            return;
        }
        if ((action == Action.TURN_LEFT || action == Action.TURN_RIGHT) && p.route.isEmpty()) {
            int n = p.pendingTurn = action == Action.TURN_LEFT ? -1 : 1;
        }
        if (action == Action.NAV && !p.navUsed) {
            p.pendingTurn = 0;
            p.navUsed = true;
            p.navUntil = this.tick + 20;
            this.plan(p);
        }
    }

    public void step() {
        if (this.winner >= 0) {
            return;
        }
        ++this.tick;
        for (int n = 0; n < 2 && this.winner < 0; ++n) {
            int id = (this.tick + n) % 2;
            Player p = this.players[id];
            if (this.tick >= p.navUntil) {
                p.route.clear();
            }
            if (this.tick < p.stunnedUntil) continue;
            if (p.held.contains((Object)Action.FIRE)) {
                this.fire(id);
            }
            if (this.winner >= 0) break;
            if (this.tick >= p.moveAt && !p.route.isEmpty()) {
                int[] next = p.route.removeFirst();
                int dx = next[1] - p.x;
                int dy = next[0] - p.y;
                for (int d = 0; d < 4; ++d) {
                    if (DX[d] != dx || DY[d] != dy) continue;
                    p.facing = d;
                }
                this.move(p, dx, dy);
                p.moveAt = this.tick + MOVE_TICKS;
            } else if (p.route.isEmpty()) {
                if (this.tick >= p.turnAt && p.pendingTurn != 0) {
                    this.turn(p, p.pendingTurn, id);
                    p.pendingTurn = 0;
                }
                if (this.tick >= p.moveAt) {
                    int d = p.facing;
                    int sign = 1;
                    boolean moving = true;
                    if (!p.held.contains((Object)Action.FORWARD)) {
                        if (p.held.contains((Object)Action.BACK)) {
                            sign = -1;
                        } else {
                            moving = false;
                        }
                    }
                    if (moving) {
                        this.move(p, DX[d] * sign, DY[d] * sign);
                        p.moveAt = this.tick + MOVE_TICKS;
                    }
                }
            }
            if (p.x != goalX || p.y != goalY) continue;
            this.winner = id;
        }
    }

    private void turn(Player p, int direction, int id) {
        p.facing = (p.facing + direction + 4) % 4;
        p.turnAt = this.tick + TURN_TICKS;
    }

    private void move(Player p, int dx, int dy) {
        if (Math.abs(dx) + Math.abs(dy) != 1 || !this.walkable(p.x + dx, p.y + dy)) {
            return;
        }
        p.x += dx;
        p.y += dy;
        ++p.steps;
        p.visited[p.y][p.x] = true;
        this.reveal(p);
    }

    private void plan(Player p) {
        p.route.clear();
        char[][] copy = (char[][])Arrays.stream(this.map).map(rec$ -> (char[])((char[])rec$).clone()).toArray(x$0 -> new char[x$0][]);
        SearchController search = new SearchController(new GridMap(copy), p.y, p.x, goalY, goalX);
        SearchStatus status = SearchStatus.RUNNING;
        for (int i = 0; i < 1875 && status == SearchStatus.RUNNING; ++i) {
            status = search.nextStep();
        }
        if (status != SearchStatus.SUCCESS) {
            throw new IllegalStateException("Generated maze is disconnected");
        }
        int[][] path = search.getPath();
        for (int i = 1; i < path.length; ++i) {
            p.route.addLast(path[i]);
        }
    }

    private void fire(int id) {
        Player p = this.players[id];
        Player enemy = this.players[1 - id];
        if (this.tick < p.fireAt) {
            return;
        }
        p.fireAt = this.tick + (id == 0 ? 60 : 70);
        p.flashUntil = this.tick + 5;
        ++p.shots;
        int dx = enemy.x - p.x;
        int dy = enemy.y - p.y;
        boolean hit = id == 0 ? dx * dx + dy * dy <= 9 && this.lineOfSight(p.x, p.y, enemy.x, enemy.y) : this.cannonHit(p, enemy);
        if (!hit || this.tick < enemy.immuneUntil) {
            return;
        }
        ++p.hits;
        enemy.stunnedUntil = this.tick + (id == 0 ? 40 : 50);
        enemy.immuneUntil = enemy.stunnedUntil + 20;
        enemy.pendingTurn = 0;
        enemy.route.clear();
        enemy.navUntil = this.tick;
        if (id == 0) {
            enemy.jammedUntil = this.tick + 100;
        } else {
            for (int distance = 0; distance < 2 && this.walkable(enemy.x + DX[p.facing], enemy.y + DY[p.facing]); ++distance) {
                this.move(enemy, DX[p.facing], DY[p.facing]);
                if (enemy.x == goalX && enemy.y == goalY) break;
            }
        }
        if (enemy.x == goalX && enemy.y == goalY) {
            this.winner = 1 - id;
        }
    }

    private boolean cannonHit(Player p, Player enemy) {
        int y;
        int x;
        for (int distance = 1; distance <= 8 && this.walkable(x = p.x + DX[p.facing] * distance, y = p.y + DY[p.facing] * distance); ++distance) {
            if (x != enemy.x || y != enemy.y) continue;
            return true;
        }
        return false;
    }

    public boolean walkable(int x, int y) {
        return x >= 0 && y >= 0 && x < 25 && y < 25 && this.map[y][x] != '#';
    }

    public boolean lineOfSight(int x0, int y0, int x1, int y1) {
        int dx = x1 - x0;
        int dy = y1 - y0;
        int steps = Math.max(Math.abs(dx), Math.abs(dy)) * 4;
        int lastX = x0;
        int lastY = y0;
        for (int i = 1; i <= steps; ++i) {
            int y;
            int x = (int)Math.floor((double)x0 + 0.5 + (double)dx * (double)i / (double)steps);
            if (!this.walkable(x, y = (int)Math.floor((double)y0 + 0.5 + (double)dy * (double)i / (double)steps))) {
                return false;
            }
            if (!(x == lastX || y == lastY || this.walkable(x, lastY) && this.walkable(lastX, y))) {
                return false;
            }
            lastX = x;
            lastY = y;
        }
        return true;
    }

    private void reveal(Player p) {
        for (int y = Math.max(0, p.y - 3); y <= Math.min(24, p.y + 3); ++y) {
            for (int x = Math.max(0, p.x - 3); x <= Math.min(24, p.x + 3); ++x) {
                if ((x - p.x) * (x - p.x) + (y - p.y) * (y - p.y) > 10 || !this.visibleTile(p, x, y)) continue;
                p.seen[y][x] = true;
            }
        }
    }

    private boolean visibleTile(Player p, int x, int y) {
        if (this.walkable(x, y)) {
            return this.lineOfSight(p.x, p.y, x, y);
        }
        double dx = x - p.x;
        double dy = y - p.y;
        int steps = (int)(Math.max(Math.abs(dx), Math.abs(dy)) * 8.0);
        for (int i = 1; i < steps; ++i) {
            double t = (double)i / (double)steps;
            int cx = (int)Math.floor((double)p.x + 0.5 + dx * t);
            int cy = (int)Math.floor((double)p.y + 0.5 + dy * t);
            if (cx == x && cy == y) {
                return true;
            }
            if (this.walkable(cx, cy)) continue;
            return false;
        }
        return true;
    }

    public RaceSnapshot snapshot(int id, boolean ready) {
        Player p = this.players[id];
        Player other = this.players[1 - id];
        StringBuilder known = new StringBuilder(625);
        for (int y = 0; y < 25; ++y) {
            for (int x = 0; x < 25; ++x) {
                known.append((char)(!p.seen[y][x] ? 63 : (this.map[y][x] == '.' && p.visited[y][x] ? 44 : this.map[y][x])));
            }
        }
        boolean visible = (p.x - other.x) * (p.x - other.x) + (p.y - other.y) * (p.y - other.y) <= 10 && this.lineOfSight(p.x, p.y, other.x, other.y);
        return new RaceSnapshot(this.tick, this.winner, ready, p.x, p.y, p.facing, p.navUsed, !p.route.isEmpty(), Math.max(0, p.stunnedUntil - this.tick), Math.max(0, p.fireAt - this.tick), Math.max(0, p.jammedUntil - this.tick), Math.max(0, p.flashUntil - this.tick), visible ? other.x : -1, visible ? other.y : -1, visible ? other.facing : -1, visible ? Math.max(0, other.flashUntil - this.tick) : 0, p.shots, p.hits, p.steps, RaceWorld.score(p), this.winner >= 0 ? other.steps : -1, this.winner >= 0 ? RaceWorld.score(other) : -1, known.toString());
    }

    // Generate mirrored random maze bands. Each separator has exactly one shared
    // crossing, so every route to the finish passes three distinct combat areas.
    private void generate(Random random) {
        for (char[] row : map) Arrays.fill(row, '#');
        int[] separators = {4 + 2 * random.nextInt(2),
                            10 + 2 * random.nextInt(2),
                            16 + 2 * random.nextInt(2)};
        int top = 1;
        for (int band = 0; band < 4; band++) {
            int bottom = band < 3 ? separators[band] - 1 : 23;
            carveBand(random, top, bottom);
            // At least one crossing within each band provides alternative routes.
            int bridge = top + 2 * random.nextInt((bottom - top) / 2 + 1);
            map[bridge][12] = '.';
            top = bottom + 2;
        }
        for (int i = 0; i < separators.length; i++) {
            int y = separators[i];
            map[y][12] = '.';
            for (int x = 9; x <= 15; x++) {
                map[y - 1][x] = '.';
                map[y + 1][x] = '.';
            }
            encounterGates[i] = new int[]{12, y};
        }
        goalX = 12;
        goalY = separators[2] + 1 + 2 * random.nextInt((23 - separators[2] + 1) / 2);
        map[goalY][goalX] = 'G';
        int spawnX = 1 + 2 * random.nextInt(4);
        int spawnY = 1 + 2 * random.nextInt(separators[0] / 2);
        players[0] = new Player(spawnX, spawnY, 1);
        players[1] = new Player(24 - spawnX, spawnY, 3);
        if (random.nextBoolean()) {
            Player swap = players[0]; players[0] = players[1]; players[1] = swap;
        }
        // Random orientation makes both the finish and spawn positions vary.
        int turns = random.nextInt(4);
        for (int turn = 0; turn < turns; turn++) {
            char[][] copy = new char[SIZE][SIZE];
            for (int y = 0; y < SIZE; y++)
                for (int x = 0; x < SIZE; x++) copy[x][24 - y] = map[y][x];
            for (int y = 0; y < SIZE; y++) System.arraycopy(copy[y], 0, map[y], 0, SIZE);
            int oldX = goalX; goalX = 24 - goalY; goalY = oldX;
            for (Player p : players) {
                oldX = p.x; p.x = 24 - p.y; p.y = oldX;
                p.facing = (p.facing + 1) % 4;
            }
            for (int[] gate : encounterGates) {
                oldX = gate[0]; gate[0] = 24 - gate[1]; gate[1] = oldX;
            }
        }
    }

    private void carveBand(Random random, int top, int bottom) {
        ArrayDeque<int[]> stack = new ArrayDeque<>();
        map[top][1] = '.';
        stack.push(new int[]{1, top});
        while (!stack.isEmpty()) {
            int[] current = stack.peek();
            List<Integer> dirs = new ArrayList<>(List.of(0, 1, 2, 3));
            Collections.shuffle(dirs, random);
            boolean carved = false;
            for (int d : dirs) {
                int x = current[0] + 2 * DX[d], y = current[1] + 2 * DY[d];
                if (x < 1 || x > 11 || y < top || y > bottom || map[y][x] != '#') continue;
                map[current[1] + DY[d]][current[0] + DX[d]] = '.';
                map[y][x] = '.';
                stack.push(new int[]{x, y});
                carved = true;
                break;
            }
            if (!carved) stack.pop();
        }
        // Randomly braid some dead ends without crossing a separator.
        for (int y = top; y <= bottom; y++) {
            for (int x = 1; x <= 11; x++) {
                if ((x % 2 != y % 2) && random.nextInt(5) == 0) map[y][x] = '.';
                map[y][24 - x] = map[y][x];
            }
        }
    }

    public static final class Player {
        public int x;
        public int y;
        public int facing;
        public int moveAt;
        public int turnAt;
        public int fireAt;
        public int stunnedUntil;
        public int immuneUntil;
        public int jammedUntil;
        public int flashUntil;
        public int shots;
        public int hits;
        public int navUntil;
        public int steps;
        public int pendingTurn;
        public boolean navUsed;
        public final boolean[][] seen = new boolean[25][25];
        public final boolean[][] visited = new boolean[25][25];
        public final EnumSet<Action> held = EnumSet.noneOf(Action.class);
        final ArrayDeque<int[]> route = new ArrayDeque();

        Player(int x, int y, int facing) {
            this.x = x;
            this.y = y;
            this.facing = facing;
        }
    }

    public static enum Action {
        FORWARD,
        BACK,
        TURN_LEFT,
        TURN_RIGHT,
        FIRE,
        NAV;

    }
}

