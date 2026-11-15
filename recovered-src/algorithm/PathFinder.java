/*
 * Decompiled with CFR 0.152.
 */
public class PathFinder {
    public static char chooseDirection(GridMap grid, Agent agent, int goalRow, int goalCol, boolean[][] visited) {
        int row = agent.row;
        int col = agent.col;
        int ldis = Integer.MAX_VALUE;
        int udis = Integer.MAX_VALUE;
        int rdis = Integer.MAX_VALUE;
        int ddis = Integer.MAX_VALUE;
        int minDistance = Integer.MAX_VALUE;
        int bestdirection = 88;
        if (grid.isWalkable(agent.row, agent.col + 1) && !visited[agent.row][agent.col + 1]) {
            rdis = PathFinder.distance(agent, goalRow, goalCol - 1);
        }
        if (grid.isWalkable(agent.row, agent.col - 1) && !visited[agent.row][agent.col - 1]) {
            ldis = PathFinder.distance(agent, goalRow, goalCol + 1);
        }
        if (grid.isWalkable(agent.row + 1, agent.col) && !visited[agent.row + 1][agent.col]) {
            ddis = PathFinder.distance(agent, goalRow - 1, goalCol);
        }
        if (grid.isWalkable(agent.row - 1, agent.col) && !visited[agent.row - 1][agent.col]) {
            udis = PathFinder.distance(agent, goalRow + 1, goalCol);
        }
        if ((minDistance = Math.min(Math.min(rdis, ddis), Math.min(ldis, udis))) == Integer.MAX_VALUE) {
            return 'X';
        }
        if (minDistance == rdis) {
            bestdirection = 82;
        } else if (minDistance == ddis) {
            bestdirection = 68;
        } else if (minDistance == ldis) {
            bestdirection = 76;
        } else if (minDistance == udis) {
            bestdirection = 85;
        }
        return (char)bestdirection;
    }

    public static int distance(Agent agent, int goalRow, int goalCol) {
        int row = agent.row;
        int col = agent.col;
        int distance = (row - goalRow) * (row - goalRow) + (col - goalCol) * (col - goalCol);
        return distance;
    }
}

