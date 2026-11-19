package com.example.bombermario.model;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * A bomb placed on a tile. It burns its fuse for {@link #FUSE_TIME} seconds and then
 * explodes in a cross of {@link #RANGE} tiles. Flames stop at walls and break the first
 * rubble or player-built wall they reach (yes, your own walls too); they stay dangerous
 * for {@link #BLAST_TIME} seconds.
 */
public class Bomb {
    /** Seconds from placement to explosion. */
    public static final double FUSE_TIME = 2.0;
    /** Seconds the flames stay on screen and hurt. */
    public static final double BLAST_TIME = 0.6;
    /** Tiles the flames reach in each direction. */
    public static final int RANGE = 2;

    private static final int CENTER = 0;
    private static final int HORIZONTAL = 1;
    private static final int VERTICAL = 2;

    private final int owner;
    private final int col;
    private final int row;
    private final List<int[]> flames = new ArrayList<>();
    private double timer;
    private boolean exploded;

    /**
     * Instantiates a new Bomb.
     *
     * @param col the board column
     * @param row the board row
     */
    public Bomb(int col, int row) {
        this(col, row, 0);
    }

    /**
     * Instantiates a new Bomb placed by a player.
     *
     * @param col   the board column
     * @param row   the board row
     * @param owner index of the player who placed it
     */
    public Bomb(int col, int row, int owner) {
        this.owner = owner;
        this.col = col;
        this.row = row;
    }

    /**
     * Advances the fuse or the explosion.
     *
     * @param dt elapsed seconds
     */
    public void update(double dt) {
        timer += dt;
    }

    /**
     * Whether the fuse has burnt out and the bomb must explode now.
     *
     * @return true when it is time to explode
     */
    public boolean isReadyToExplode() {
        return !exploded && timer >= FUSE_TIME;
    }

    /**
     * Explodes the bomb, computing the flames and breaking the blocks they reach.
     *
     * @param board the board
     * @return the blocks broken, as {col, row, TypeBox ordinal}
     */
    public List<int[]> explode(Board board) {
        exploded = true;
        timer = FUSE_TIME;
        List<int[]> destroyed = new ArrayList<>();
        flames.add(new int[]{col, row, CENTER});
        for (Direction d : Direction.values()) {
            for (int i = 1; i <= RANGE; i++) {
                int c = col + d.getDx() * i;
                int r = row + d.getDy() * i;
                int kind = d.isHorizontal() ? HORIZONTAL : VERTICAL;
                if (!board.isWall(c, r)) {
                    flames.add(new int[]{c, r, kind});
                    continue;
                }
                TypeBox broken = board.destroy(c, r);
                if (broken != null) {
                    flames.add(new int[]{c, r, kind});
                    destroyed.add(new int[]{c, r, broken.ordinal()});
                }
                break;
            }
        }
        return destroyed;
    }

    /**
     * Whether the bomb has exploded.
     *
     * @return true after the explosion started
     */
    public boolean isExploded() {
        return exploded;
    }

    /**
     * Whether the flames are on screen and dangerous.
     *
     * @return true during the blast
     */
    public boolean isExploding() {
        return exploded && timer < FUSE_TIME + BLAST_TIME;
    }

    /**
     * Whether the explosion has completely finished.
     *
     * @return true when the bomb can be discarded
     */
    public boolean isFinished() {
        return exploded && timer >= FUSE_TIME + BLAST_TIME;
    }

    /**
     * Whether the flames touch a rectangle.
     *
     * @param left   left edge in pixels
     * @param top    top edge in pixels
     * @param right  right edge in pixels
     * @param bottom bottom edge in pixels
     * @return true if any flame tile overlaps the rectangle
     */
    public boolean hits(double left, double top, double right, double bottom) {
        if (!isExploding()) {
            return false;
        }
        for (int[] f : flames) {
            double fx = Board.tileX(f[0]);
            double fy = Board.tileY(f[1]);
            if (left < fx + Board.TILE && right > fx && top < fy + Board.TILE && bottom > fy) {
                return true;
            }
        }
        return false;
    }

    /**
     * Draws the bomb while its fuse burns, or the flames while it explodes.
     *
     * @param gc the graphics context
     */
    public void paint(GraphicsContext gc) {
        if (!exploded) {
            paintFuse(gc);
        } else if (isExploding()) {
            paintBlast(gc);
        }
    }

    private void paintFuse(GraphicsContext gc) {
        double cx = Board.centerX(col);
        double cy = Board.centerY(row);
        double progress = timer / FUSE_TIME;
        // The bomb throbs faster as the fuse runs out
        double speed = 6 + 22 * progress;
        double scale = 1 + (0.06 + 0.1 * progress) * Math.sin(timer * speed);
        double radius = 12 * scale;
        gc.setFill(Color.rgb(0, 0, 0, 0.3));
        gc.fillOval(cx - 13, cy + 10, 26, 8);
        drawBomb(gc, cx, cy + 2, radius, timer);
        if (progress > 0.6) {
            double flash = 0.5 + 0.5 * Math.sin(timer * 30);
            gc.setFill(Color.rgb(255, 40, 0, 0.45 * flash * (progress - 0.6) / 0.4));
            gc.fillOval(cx - radius - 2, cy + 2 - radius - 2, radius * 2 + 4, radius * 2 + 4);
        }
    }

    /**
     * Draws a round bomb with a lit fuse.
     *
     * @param gc     the graphics context
     * @param cx     center x
     * @param cy     center y
     * @param radius body radius
     * @param time   seconds, animates the spark
     */
    public static void drawBomb(GraphicsContext gc, double cx, double cy, double radius, double time) {
        Art.bomb(gc, cx, cy, radius, time);
    }

    private void paintBlast(GraphicsContext gc) {
        double p = (timer - FUSE_TIME) / BLAST_TIME;
        double alpha = p < 0.2 ? 1 : 1 - (p - 0.2) / 0.8;
        double grow = p < 0.15 ? 0.55 + 3 * p : 1 - 0.35 * (p - 0.15);
        gc.setGlobalAlpha(Math.max(0, alpha));
        paintLayer(gc, Color.rgb(255, 110, 0), 0.95 * grow);
        paintLayer(gc, Color.rgb(255, 205, 40), 0.65 * grow);
        paintLayer(gc, Color.rgb(255, 255, 220), 0.32 * grow);
        gc.setGlobalAlpha(1);
    }

    private void paintLayer(GraphicsContext gc, Color color, double thickness) {
        double t = Board.TILE * thickness;
        gc.setFill(color);
        for (int[] f : flames) {
            double x = Board.tileX(f[0]);
            double y = Board.tileY(f[1]);
            double cx = x + Board.TILE / 2.0;
            double cy = y + Board.TILE / 2.0;
            switch (f[2]) {
                case HORIZONTAL -> gc.fillRoundRect(x - 1, cy - t / 2, Board.TILE + 2, t, t, t);
                case VERTICAL -> gc.fillRoundRect(cx - t / 2, y - 1, t, Board.TILE + 2, t, t);
                default -> gc.fillOval(cx - t * 0.65, cy - t * 0.65, t * 1.3, t * 1.3);
            }
        }
    }

    /** @return the flame tiles {col, row, kind}, filled once the bomb explodes */
    public List<int[]> getFlames() {
        return flames;
    }

    /** @return index of the player who placed it */
    public int getOwner() {
        return owner;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }
}
