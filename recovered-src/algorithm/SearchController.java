/*
 * Decompiled with CFR 0.152.
 */
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public class SearchController {
    private GridMap grid;
    private Agent agent;
    private boolean[][] visited;
    private Deque<Agent> path;
    private int goalRow;
    private int goalCol;
    private int startRow;
    private int startCol;
    private int steps;
    private int expCount;
    private int backCount;
    private int failCount;

    public SearchController(GridMap grid, int goalRow, int goalCol) {
        this(grid, 0, 0, goalRow, goalCol);
    }

    public SearchController(GridMap grid, int startRow, int startCol, int goalRow, int goalCol) {
        this.grid = grid;
        if (!grid.isWalkable(startRow, startCol) || !grid.isWalkable(goalRow, goalCol)) {
            throw new IllegalArgumentException("Start and goal must be walkable");
        }
        this.agent = new Agent(startRow, startCol);
        this.visited = new boolean[grid.getRows()][];
        for (int i = 0; i < grid.getRows(); ++i) {
            this.visited[i] = new boolean[grid.getRowLength(i)];
        }
        this.visited[startRow][startCol] = true;
        this.path = new ArrayDeque<Agent>();
        this.path.push(new Agent(this.agent.row, this.agent.col));
        this.goalRow = goalRow;
        this.goalCol = goalCol;
        this.startRow = startRow;
        this.startCol = startCol;
    }

    public int[][] getPath() {
        int[][] result = new int[this.path.size()][2];
        int i = 0;
        Iterator<Agent> it = this.path.descendingIterator();
        while (it.hasNext()) {
            Agent cell = it.next();
            result[i++] = new int[]{cell.row, cell.col};
        }
        return result;
    }

    public Agent getAgent() {
        return this.agent;
    }

    public int getPathLength() {
        return this.path.size() - 1;
    }

    public boolean isGoal() {
        return this.agent.row == this.goalRow && this.agent.col == this.goalCol;
    }

    public SearchStatus nextStep() {
        if (this.isGoal()) {
            return SearchStatus.SUCCESS;
        }
        char dir = PathFinder.chooseDirection(this.grid, this.agent, this.goalRow, this.goalCol, this.visited);
        if (dir != 'X') {
            int oldRow = this.agent.row;
            int oldCol = this.agent.col;
            AgentMove.moveAgent(this.grid, this.agent, dir);
            this.visited[this.agent.row][this.agent.col] = true;
            this.path.push(new Agent(this.agent.row, this.agent.col));
            ++this.steps;
            if (oldRow == this.startRow && oldCol == this.startCol && (this.agent.row != this.startRow || this.agent.col != this.startCol)) {
                ++this.expCount;
            }
            return SearchStatus.RUNNING;
        }
        if (this.path.size() == 1) {
            return SearchStatus.FAILED;
        }
        this.path.pop();
        Agent prev = this.path.peek();
        this.grid.setCell(this.agent.row, this.agent.col, '.');
        ++this.backCount;
        ++this.steps;
        if (prev.row == this.startRow && prev.col == this.startCol && (this.agent.row != this.startRow || this.agent.col != this.startCol)) {
            ++this.failCount;
        }
        this.agent.row = prev.row;
        this.agent.col = prev.col;
        this.grid.setCell(this.agent.row, this.agent.col, 'A');
        return SearchStatus.RUNNING;
    }

    public int getSteps() {
        return this.steps;
    }

    public int getExpCount() {
        return this.expCount;
    }

    public int getBackCount() {
        return this.backCount;
    }

    public int getFailCount() {
        return this.failCount;
    }
}

