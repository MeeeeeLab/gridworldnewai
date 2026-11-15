/*
 * Decompiled with CFR 0.152.
 */
public class AgentMove {
    public static void moveAgent(GridMap grid, Agent agent, char direction) {
        int row = agent.row;
        int col = agent.col;
        if (direction == 'U' && grid.isWalkable(row - 1, col)) {
            grid.setCell(row, col, '.');
            grid.setCell(row - 1, col, 'A');
            agent.row = row - 1;
        }
        if (direction == 'D' && grid.isWalkable(row + 1, col)) {
            grid.setCell(row, col, '.');
            grid.setCell(row + 1, col, 'A');
            agent.row = row + 1;
        }
        if (direction == 'L' && grid.isWalkable(row, col - 1)) {
            grid.setCell(row, col, '.');
            grid.setCell(row, col - 1, 'A');
            agent.col = col - 1;
        }
        if (direction == 'R' && grid.isWalkable(row, col + 1)) {
            grid.setCell(row, col, '.');
            grid.setCell(row, col + 1, 'A');
            agent.col = col + 1;
        }
    }
}

