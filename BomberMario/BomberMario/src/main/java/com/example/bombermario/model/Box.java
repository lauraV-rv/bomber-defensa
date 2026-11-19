package com.example.bombermario.model;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * A single block of the board. Everything is drawn with code (no image files):
 * stone walls, rubble, the player's wooden walls, the core and the portals.
 * Breakable blocks play a short break animation; built walls pop in.
 */
public class Box {
    /** Seconds the break animation lasts. */
    public static final double DESTROY_TIME = 0.35;
    private static final double BUILD_TIME = 0.2;

    private final int col;
    private final int row;
    private TypeBox typeBox;
    private double destroyTimer = -1;
    private double buildTimer = -1;

    /**
     * Instantiates a new Box.
     *
     * @param col  the board column
     * @param row  the board row
     * @param type the box type
     */
    public Box(int col, int row, TypeBox type) {
        this.col = col;
        this.row = row;
        this.typeBox = type;
    }

    /** Starts the break animation. */
    public void startDestroying() {
        destroyTimer = 0;
    }

    /** Starts the "just built" pop animation. */
    public void startBuilding() {
        buildTimer = 0;
    }

    /** @return true while breaking */
    public boolean isDestroying() {
        return destroyTimer >= 0;
    }

    /** @return true when the break animation is over */
    public boolean isGone() {
        return destroyTimer >= DESTROY_TIME;
    }

    /**
     * Advances the animations.
     *
     * @param dt elapsed seconds
     */
    public void update(double dt) {
        if (destroyTimer >= 0) {
            destroyTimer += dt;
        }
        if (buildTimer >= 0) {
            buildTimer += dt;
            if (buildTimer >= BUILD_TIME) {
                buildTimer = -1;
            }
        }
    }

    /**
     * Draws the block.
     *
     * @param gc    the graphics context
     * @param time  seconds since the game started
     * @param value core health 0-1 for the core, 1/0 open/closed for portals
     */
    public void paint(GraphicsContext gc, double time, double value) {
        double x = Board.tileX(col);
        double y = Board.tileY(row);
        double s = Board.TILE;
        if (isDestroying()) {
            double p = Math.min(1, destroyTimer / DESTROY_TIME);
            double size = s * (1 + 0.3 * p);
            double off = (size - s) / 2;
            gc.setGlobalAlpha(1 - p);
            gc.save();
            gc.translate(x - off, y - off);
            gc.scale(size / s, size / s);
            paintShape(gc, 0, 0, s, time, value);
            gc.restore();
            gc.setFill(Color.rgb(255, 240, 200, 0.7 * (1 - p)));
            gc.fillRect(x - off, y - off, size, size);
            gc.setGlobalAlpha(1);
            return;
        }
        if (buildTimer >= 0) {
            double p = buildTimer / BUILD_TIME;
            double scale = p < 0.7 ? 0.6 + 0.6 * p / 0.7 : 1.2 - 0.2 * (p - 0.7) / 0.3;
            double size = s * scale;
            double off = (size - s) / 2;
            gc.save();
            gc.translate(x - off, y - off);
            gc.scale(scale, scale);
            paintShape(gc, 0, 0, s, time, value);
            gc.restore();
            return;
        }
        paintShape(gc, x, y, s, time, value);
    }

    private void paintShape(GraphicsContext gc, double x, double y, double s, double time, double value) {
        switch (typeBox) {
            case NON_DESTROYABLE -> Art.stone(gc, col, row, x, y, s);
            case DESTROYABLE -> Art.rubble(gc, col, row, x, y, s);
            case BUILT -> Art.crate(gc, x, y, s);
            case CORE -> {
                Art.stone(gc, col, row, x, y, s);
                Art.coreCrystal(gc, x + s / 2, y + s / 2, 1, time, value);
            }
            case PORTAL -> {
                Art.stone(gc, col, row, x, y, s);
                Art.portal(gc, x + s / 2, y + s / 2, 1, time, value > 0.5);
            }
        }
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public TypeBox getTypeBox() {
        return typeBox;
    }

    public void setTypeBox(TypeBox typeBox) {
        this.typeBox = typeBox;
    }
}
