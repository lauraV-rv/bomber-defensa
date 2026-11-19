package com.example.bombermario.model;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * An invader. It comes out of a portal and walks tile by tile along the shortest
 * path to the core's gate, recomputing the path at every tile so it reacts to the
 * walls the player builds or blows up. Dynamite does not block it: it keeps marching
 * over it, so it never stands still. Reaching the gate damages the core.
 */
public class Enemy extends Character {
    /** Seconds the defeated body stays on screen. */
    public static final double DEATH_TIME = 0.6;
    /** Distance between centers at which it knocks out the player. */
    public static final double TOUCH_DISTANCE = 22;

    private static final double HIT_INVULNERABLE = 0.75;
    private static final double SPAWN_TIME = 0.4;
    private static final int SEARCH = Board.COLS * Board.ROWS;
    /** Seconds of walking after which an automaton overloads and speeds up. */
    public static final double OVERLOAD_TIME = 30;
    private static final double OVERLOAD_BOOST = 1.6;

    private final EnemyType type;
    private final double speed;
    private int health;

    private int col;
    private int row;
    private int targetCol;
    private int targetRow;
    private Direction dir = Direction.RIGHT;
    private boolean moving;
    private boolean alive = true;
    private boolean reachedCore;
    private boolean facingLeft;
    private double deadTimer;
    private double animTime;
    private double waitTimer;
    private double hitTimer;
    private double spawnTimer = SPAWN_TIME;
    private double walkTime;
    private boolean overloadAnnounced;

    /**
     * Instantiates a new invader on a tile.
     *
     * @param canvas     the canvas
     * @param col        the starting column
     * @param row        the starting row
     * @param type       the kind of invader
     * @param speedBoost multiplier applied to the type's speed
     */
    public Enemy(Canvas canvas, int col, int row, EnemyType type, double speedBoost) {
        super(canvas);
        this.type = type;
        this.speed = type.getSpeed() * speedBoost;
        this.health = type.getHealth();
        this.col = col;
        this.row = row;
        this.targetCol = col;
        this.targetRow = row;
        this.position = new Position(Board.centerX(col), Board.centerY(row));
        this.animTime = Math.random();
    }

    /**
     * Moves the invader one frame.
     *
     * @param dt    elapsed seconds
     * @param board the board
     * @param bombs the active bombs
     */
    public void update(double dt, Board board, java.util.List<Bomb> bombs) {
        animTime += dt;
        if (hitTimer > 0) {
            hitTimer -= dt;
        }
        if (!alive) {
            deadTimer += dt;
            return;
        }
        if (spawnTimer > 0) {
            spawnTimer -= dt;
            return;
        }
        if (waitTimer > 0) {
            waitTimer -= dt;
            return;
        }
        if (!moving) {
            chooseNext(board, bombs);
            if (!moving) {
                waitTimer = 0.2;
                return;
            }
        }
        // Dynamite never stops automatons (they walk right over it); only a solid block makes them go back
        if (board.isWall(targetCol, targetRow)) {
            turnAround();
        }
        double tx = Board.centerX(targetCol);
        double ty = Board.centerY(targetRow);
        double dx = tx - position.getX();
        double dy = ty - position.getY();
        double dist = Math.hypot(dx, dy);
        walkTime += dt;
        double step = speed * (isOverloaded() ? OVERLOAD_BOOST : 1) * dt;
        if (step >= dist) {
            position.setX(tx);
            position.setY(ty);
            col = targetCol;
            row = targetRow;
            moving = false;
            if (col == Board.GATE_COL && row == Board.GATE_ROW) {
                reachedCore = true;
                alive = false;
                return;
            }
            chooseNext(board, bombs);
        } else {
            position.setX(position.getX() + dx / dist * step);
            position.setY(position.getY() + dy / dist * step);
        }
    }

    private void turnAround() {
        int c = targetCol;
        int r = targetRow;
        targetCol = col;
        targetRow = row;
        col = c;
        row = r;
        dir = dir.opposite();
        updateFacing();
    }

    private void chooseNext(Board board, java.util.List<Bomb> bombs) {
        Direction next = board.firstStepTowards(col, row, Board.GATE_COL, Board.GATE_ROW, java.util.List.of(), SEARCH);
        if (next == null) {
            moving = false;
            return;
        }
        dir = next;
        targetCol = col + next.getDx();
        targetRow = row + next.getDy();
        moving = true;
        updateFacing();
    }

    private void updateFacing() {
        if (dir == Direction.LEFT) {
            facingLeft = true;
        } else if (dir == Direction.RIGHT) {
            facingLeft = false;
        }
    }

    @Override
    public void paint() {
        GraphicsContext gc = graphicsContext;
        double x = position.getX();
        double y = position.getY();
        double size = type.getSize();
        if (reachedCore) {
            return;
        }
        if (!alive) {
            double p = Math.min(1, deadTimer / DEATH_TIME);
            gc.setGlobalAlpha(1 - p);
            gc.setFill(type.getColor());
            gc.fillOval(x - size / 2 * (1 + p), y + 8, size * (1 + p), size * 0.3);
            gc.setGlobalAlpha(1);
            return;
        }
        double appear = spawnTimer > 0 ? 1 - spawnTimer / SPAWN_TIME : 1;
        if (isOverloaded()) {
            double pulse = 0.5 + 0.5 * Math.sin(animTime * 14);
            gc.setFill(Color.rgb(255, 60, 40, 0.25 + 0.25 * pulse));
            gc.fillOval(x - size * 0.75, y - size * 0.55, size * 1.5, size * 1.4);
        }
        boolean flashing = hitTimer > 0 && ((int) (hitTimer * 20)) % 2 == 0;
        Art.invader(gc, x, y, size * appear, type.getColor(), facingLeft ? -1 : 1, animTime, moving, flashing);
        double top = y + 16 - size * 0.9 - 4;
        if (type.getHealth() > 1) {
            for (int i = 0; i < type.getHealth(); i++) {
                gc.setFill(i < health ? Color.rgb(255, 80, 80) : Color.rgb(60, 60, 60));
                gc.fillRect(x - 13 + i * 9, top - 10, 8, 4);
            }
        }
    }

    /**
     * Hits the invader with a blast. It can't be hit again for a moment, so one
     * explosion counts once.
     *
     * @return true if this hit defeated it
     */
    public boolean damage() {
        if (!alive || hitTimer > 0) {
            return false;
        }
        health--;
        hitTimer = HIT_INVULNERABLE;
        if (health <= 0) {
            alive = false;
            deadTimer = 0;
            return true;
        }
        return false;
    }

    /** @return true once it has walked long enough to overload (faster, glowing red) */
    public boolean isOverloaded() {
        return walkTime >= OVERLOAD_TIME;
    }

    /** @return true (once) the moment it overloads */
    public boolean consumeOverloadStart() {
        if (isOverloaded() && alive && !overloadAnnounced) {
            overloadAnnounced = true;
            return true;
        }
        return false;
    }

    /** @return true if a blast can hurt it right now */
    public boolean canBeHit() {
        return alive && hitTimer <= 0;
    }

    /** @return true when it can be removed from the game */
    public boolean isRemovable() {
        return reachedCore || (!alive && deadTimer >= DEATH_TIME);
    }

    /**
     * Whether it touches the player.
     *
     * @param avatar the player
     * @return true if close enough to knock the player out
     */
    public boolean touches(Avatar avatar) {
        return alive && spawnTimer <= 0
                && Math.hypot(avatar.getX() - position.getX(), avatar.getY() - position.getY()) < TOUCH_DISTANCE;
    }

    /** @return true if it got to the core this frame or earlier */
    public boolean hasReachedCore() {
        return reachedCore;
    }

    public boolean isAlive() {
        return alive;
    }

    public EnemyType getType() {
        return type;
    }

    /** @return the tile it stands on (or is leaving) */
    public int[] getTile() {
        return new int[]{col, row};
    }

    /** @return the tile it is walking to */
    public int[] getTargetTile() {
        return new int[]{targetCol, targetRow};
    }
}
