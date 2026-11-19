package com.example.bombermario.model;

/**
 * The four movement directions on the board grid.
 */
public enum Direction {
    LEFT(-1, 0), RIGHT(1, 0), UP(0, -1), DOWN(0, 1);

    private final int dx;
    private final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    /**
     * Gets the column offset of this direction.
     *
     * @return -1, 0 or 1
     */
    public int getDx() {
        return dx;
    }

    /**
     * Gets the row offset of this direction.
     *
     * @return -1, 0 or 1
     */
    public int getDy() {
        return dy;
    }

    /**
     * Whether this direction moves along the X axis.
     *
     * @return true for LEFT and RIGHT
     */
    public boolean isHorizontal() {
        return dy == 0;
    }

    /**
     * Gets the opposite direction.
     *
     * @return the opposite direction
     */
    public Direction opposite() {
        return switch (this) {
            case LEFT -> RIGHT;
            case RIGHT -> LEFT;
            case UP -> DOWN;
            case DOWN -> UP;
        };
    }
}
