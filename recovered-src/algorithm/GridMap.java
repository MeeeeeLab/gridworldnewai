/*
 * Decompiled with CFR 0.152.
 */
public class GridMap {
    private char[][] grid;

    public GridMap(char[][] grid) {
        this.grid = grid;
    }

    public int getRows() {
        return this.grid.length;
    }

    public boolean inBounds(int row, int col) {
        return row >= 0 && row < this.getRows() && col >= 0 && col < this.grid[row].length;
    }

    public boolean isWalkable(int row, int col) {
        return this.inBounds(row, col) && this.grid[row][col] != '#';
    }

    public int getRowLength(int row) {
        return this.grid[row].length;
    }

    public void setCell(int row, int col, char value) {
        this.grid[row][col] = value;
    }
}

