package alife;

public record Position(int row, int col) {
    public Position move(int dRow, int dCol) {
        return new Position(row + dRow, col + dCol);
    }
}
