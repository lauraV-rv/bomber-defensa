package com.example.bombermario.model;

import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * An engineer controlled by a player. It moves smoothly with help to turn corners,
 * builds walls on the tile in front of it, places dynamite and can be knocked out,
 * which sends it back to its start tile after a short while. Its keys and outfit
 * are configurable so two players can share one keyboard.
 */
public class Avatar extends Character {
    /** Movement speed in pixels per second. */
    public static final double SPEED = 135;
    /** Half the size of the square hitbox, in pixels. */
    public static final double HALF = 12;
    /** Seconds of invulnerability after coming back. */
    public static final double INVULNERABLE_TIME = 2.0;
    /** Seconds the engineer stays knocked out. */
    public static final double KNOCKOUT_TIME = 1.6;

    /** The keys of one player. */
    public record Controls(Set<KeyCode> up, Set<KeyCode> down, Set<KeyCode> left, Set<KeyCode> right,
                           Set<KeyCode> build, Set<KeyCode> bomb) {
        /** Single player: arrows or WASD, E/K builds, SPACE places dynamite. */
        public static final Controls SOLO = new Controls(EnumSet.of(KeyCode.UP, KeyCode.W), EnumSet.of(KeyCode.DOWN, KeyCode.S),
                EnumSet.of(KeyCode.LEFT, KeyCode.A), EnumSet.of(KeyCode.RIGHT, KeyCode.D),
                EnumSet.of(KeyCode.E, KeyCode.K), EnumSet.of(KeyCode.SPACE));
        /** Co-op player 1: WASD, E builds, Q or F places dynamite. */
        public static final Controls P1 = new Controls(EnumSet.of(KeyCode.W), EnumSet.of(KeyCode.S),
                EnumSet.of(KeyCode.A), EnumSet.of(KeyCode.D),
                EnumSet.of(KeyCode.E), EnumSet.of(KeyCode.Q, KeyCode.F));
        /** Co-op player 2: arrows, M or SHIFT builds, SPACE or / places dynamite. */
        public static final Controls P2 = new Controls(EnumSet.of(KeyCode.UP), EnumSet.of(KeyCode.DOWN),
                EnumSet.of(KeyCode.LEFT), EnumSet.of(KeyCode.RIGHT),
                EnumSet.of(KeyCode.M, KeyCode.SHIFT), EnumSet.of(KeyCode.SPACE, KeyCode.SLASH, KeyCode.DIVIDE));

        Direction direction(KeyCode code) {
            if (up.contains(code)) {
                return Direction.UP;
            }
            if (down.contains(code)) {
                return Direction.DOWN;
            }
            if (left.contains(code)) {
                return Direction.LEFT;
            }
            if (right.contains(code)) {
                return Direction.RIGHT;
            }
            return null;
        }

        /** @return true if this player uses the key */
        public boolean uses(KeyCode code) {
            return direction(code) != null || build.contains(code) || bomb.contains(code);
        }
    }

    private final int index;
    private final Controls controls;
    private final Art.Outfit outfit;
    private final int startCol;
    private final int startRow;
    private final Deque<Direction> pressed = new ArrayDeque<>();
    private State state = State.IDLE;
    private Direction facing = Direction.LEFT;
    private Direction lastDir = Direction.LEFT;
    private boolean bombRequested;
    private boolean buildRequested;
    private boolean bombHeld;
    private boolean buildHeld;
    private double invulnerableTimer;
    private double knockoutTimer;
    private double animTime;
    private Bomb ignoredBomb;
    private boolean respawned;

    /**
     * Instantiates the single-player engineer at the start tile.
     *
     * @param canvas the canvas
     */
    public Avatar(Canvas canvas) {
        this(canvas, 0, Controls.SOLO, Art.Outfit.AMBER, Board.START_COL, Board.START_ROW);
    }

    /**
     * Instantiates an engineer.
     *
     * @param canvas   the canvas
     * @param index    player index (0 or 1)
     * @param controls the player's keys
     * @param outfit   the player's colors
     * @param startCol start / respawn column
     * @param startRow start / respawn row
     */
    public Avatar(Canvas canvas, int index, Controls controls, Art.Outfit outfit, int startCol, int startRow) {
        super(canvas);
        this.index = index;
        this.controls = controls;
        this.outfit = outfit;
        this.startCol = startCol;
        this.startRow = startRow;
        respawn();
        invulnerableTimer = 0;
    }

    /** Places the engineer back on its start tile, briefly invulnerable. */
    public void respawn() {
        position = new Position(Board.centerX(startCol), Board.centerY(startRow));
        pressed.clear();
        state = State.IDLE;
        facing = Direction.LEFT;
        lastDir = Direction.LEFT;
        ignoredBomb = null;
        bombRequested = false;
        buildRequested = false;
        invulnerableTimer = INVULNERABLE_TIME;
    }

    /**
     * Handles a key press if it belongs to this player.
     *
     * @param event the key event
     */
    public void onKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();
        Direction d = controls.direction(code);
        if (d != null) {
            pressed.remove(d);
            pressed.addFirst(d);
        } else if (controls.bomb().contains(code) && !bombHeld) {
            bombHeld = true;
            bombRequested = true;
        } else if (controls.build().contains(code) && !buildHeld) {
            buildHeld = true;
            buildRequested = true;
        }
    }

    /**
     * Handles a key release if it belongs to this player.
     *
     * @param event the key event
     */
    public void onKeyReleased(KeyEvent event) {
        KeyCode code = event.getCode();
        Direction d = controls.direction(code);
        if (d != null) {
            pressed.remove(d);
        } else if (controls.bomb().contains(code)) {
            bombHeld = false;
        } else if (controls.build().contains(code)) {
            buildHeld = false;
        }
    }

    /** @return true (once) if the player asked for dynamite */
    public boolean consumeBombRequest() {
        boolean requested = bombRequested;
        bombRequested = false;
        return requested;
    }

    /** @return true (once) if the player asked to build */
    public boolean consumeBuildRequest() {
        boolean requested = buildRequested;
        buildRequested = false;
        return requested;
    }

    /**
     * Moves the engineer according to the keys being held.
     *
     * @param dt    elapsed seconds
     * @param board the board
     * @param bombs the active bombs
     */
    public void update(double dt, Board board, List<Bomb> bombs) {
        animTime += dt;
        if (knockoutTimer > 0) {
            knockoutTimer -= dt;
            state = State.DEAD;
            if (knockoutTimer <= 0) {
                respawn();
                respawned = true;
            }
            return;
        }
        if (invulnerableTimer > 0) {
            invulnerableTimer -= dt;
        }
        if (ignoredBomb != null && (!bombs.contains(ignoredBomb) || ignoredBomb.isExploded()
                || !overlapsTile(position.getX(), position.getY(), ignoredBomb.getCol(), ignoredBomb.getRow()))) {
            ignoredBomb = null;
        }
        Direction d = pressed.peekFirst();
        if (d == null) {
            state = State.IDLE;
            return;
        }
        state = State.RUN;
        lastDir = d;
        if (d == Direction.LEFT || d == Direction.RIGHT) {
            facing = d;
        }
        move(d, SPEED * dt, board, bombs);
    }

    private void move(Direction d, double step, Board board, List<Bomb> bombs) {
        double x = position.getX();
        double y = position.getY();
        double nx = x + d.getDx() * step;
        double ny = y + d.getDy() * step;
        if (!collides(nx, ny, board, bombs)) {
            position.setX(nx);
            position.setY(ny);
            return;
        }
        // Corner assist: when blocked, slide toward the center of the lane if the way ahead is open
        if (d.isHorizontal()) {
            int row = Board.rowOf(y);
            int ahead = Board.colOf(x) + d.getDx();
            if (!blocks(ahead, row, board, bombs)) {
                double diff = Board.centerY(row) - y;
                double ty = y + Math.signum(diff) * Math.min(step, Math.abs(diff));
                if (!collides(x, ty, board, bombs)) {
                    position.setY(ty);
                }
            }
        } else {
            int col = Board.colOf(x);
            int ahead = Board.rowOf(y) + d.getDy();
            if (!blocks(col, ahead, board, bombs)) {
                double diff = Board.centerX(col) - x;
                double tx = x + Math.signum(diff) * Math.min(step, Math.abs(diff));
                if (!collides(tx, y, board, bombs)) {
                    position.setX(tx);
                }
            }
        }
    }

    private boolean blocks(int c, int r, Board board, List<Bomb> bombs) {
        if (board.blocksAvatar(c, r)) {
            return true;
        }
        for (Bomb b : bombs) {
            if (b != ignoredBomb && Board.isBombAt(b, c, r)) {
                return true;
            }
        }
        return false;
    }

    private boolean collides(double x, double y, Board board, List<Bomb> bombs) {
        int c1 = Board.colOf(x - HALF);
        int c2 = Board.colOf(x + HALF - 0.001);
        int r1 = Board.rowOf(y - HALF);
        int r2 = Board.rowOf(y + HALF - 0.001);
        for (int c = c1; c <= c2; c++) {
            for (int r = r1; r <= r2; r++) {
                if (blocks(c, r, board, bombs)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean overlapsTile(double x, double y, int c, int r) {
        double tx = Board.tileX(c);
        double ty = Board.tileY(r);
        return x - HALF < tx + Board.TILE && x + HALF > tx && y - HALF < ty + Board.TILE && y + HALF > ty;
    }

    /**
     * Whether the engineer's body overlaps a tile (so a wall can't be built on it).
     *
     * @param c column
     * @param r row
     * @return true if overlapping
     */
    public boolean overlaps(int c, int r) {
        return overlapsTile(position.getX(), position.getY(), c, r);
    }

    /** Moves the engineer to the center of its tile, so a wall can be built right in front. */
    public void snapToTile() {
        position.setX(Board.centerX(getCol()));
        position.setY(Board.centerY(getRow()));
    }

    /**
     * Lets the engineer stand on dynamite it has just placed until it walks off it.
     *
     * @param bomb the dynamite just placed
     */
    public void ignoreBomb(Bomb bomb) {
        this.ignoredBomb = bomb;
    }

    /**
     * Knocks the engineer out unless it is invulnerable or already down.
     *
     * @return true if it was knocked out now
     */
    public boolean hit() {
        if (isKnockedOut() || invulnerableTimer > 0) {
            return false;
        }
        knockoutTimer = KNOCKOUT_TIME;
        animTime = 0;
        pressed.clear();
        return true;
    }

    @Override
    public void paint() {
        boolean down = isKnockedOut();
        boolean blinking = !down && invulnerableTimer > 0 && ((int) (invulnerableTimer * 10)) % 2 == 0;
        if (blinking) {
            graphicsContext.setGlobalAlpha(0.35);
        }
        Art.builder(graphicsContext, position.getX(), position.getY(), 1, facing == Direction.LEFT ? -1 : 1,
                animTime, state == State.RUN, down, outfit);
        graphicsContext.setGlobalAlpha(1);
    }

    /** @return true (once) right after the engineer comes back from a knock-out */
    public boolean consumeRespawned() {
        boolean r = respawned;
        respawned = false;
        return r;
    }

    /** @return seconds left until the engineer comes back, 0 if not knocked out */
    public double getKnockoutLeft() {
        return Math.max(0, knockoutTimer);
    }

    /** @return true while knocked out */
    public boolean isKnockedOut() {
        return knockoutTimer > 0;
    }

    /** @return true if the engineer can act (not knocked out) */
    public boolean isAlive() {
        return !isKnockedOut();
    }

    public boolean isInvulnerable() {
        return invulnerableTimer > 0;
    }

    public State getState() {
        return state;
    }

    /** @return the direction the engineer last walked in; walls are built that way */
    public Direction getLastDir() {
        return lastDir;
    }

    /** @return player index (0 or 1) */
    public int getIndex() {
        return index;
    }

    public Art.Outfit getOutfit() {
        return outfit;
    }

    public Controls getControls() {
        return controls;
    }

    /** @return x of the center in pixels */
    public double getX() {
        return position.getX();
    }

    /** @return y of the center in pixels */
    public double getY() {
        return position.getY();
    }
}
