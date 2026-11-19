package com.example.bombermario.model;

import javafx.scene.canvas.GraphicsContext;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * The battlefield grid: border walls, fixed pillars, rubble, the walls the player
 * builds, the two invader portals on the left and the core on the right.
 * <p>
 * The key rule of the mod lives here: a wall can only be built if, afterwards, every
 * portal and every invader can still walk to the core ({@link #canBuild}). The check is
 * a single breadth-first search that starts at the core's gate.
 */
public class Board {
    /** Size of a tile in pixels. */
    public static final int TILE = 40;
    /** Number of columns. */
    public static final int COLS = 21;
    /** Number of rows. */
    public static final int ROWS = 13;
    /** Pixels reserved at the top of the canvas for the HUD. */
    public static final int OFFSET_Y = 120;

    /** The core sits in the right border, in the middle row. */
    public static final int CORE_COL = COLS - 1;
    public static final int CORE_ROW = 6;
    /** The tile in front of the core: invaders that step on it damage the core. */
    public static final int GATE_COL = COLS - 2;
    public static final int GATE_ROW = CORE_ROW;

    /** Portals sit in the left border; invaders appear on the tile in front of them. */
    public static final int[][] PORTALS = {{0, 1}, {0, ROWS - 2}};
    public static final int[][] SPAWNS = {{1, 1}, {1, ROWS - 2}};

    /** Where the player starts and respawns, close to the core. */
    public static final int START_COL = COLS - 3;
    public static final int START_ROW = CORE_ROW - 1;
    /** Where player 2 starts and respawns in co-op. */
    public static final int START2_COL = COLS - 3;
    public static final int START2_ROW = CORE_ROW + 1;

    /** Result of trying to build a wall. */
    public enum BuildResult {
        OK, SOLID, RESERVED, OCCUPIED, BLOCKS_PATH
    }

    private final Box[][] cells = new Box[COLS][ROWS];
    private final List<Box> breaking = new ArrayList<>();
    private final boolean[] portalOpen = new boolean[PORTALS.length];

    /**
     * Builds the battlefield with some random rubble that never cuts the path.
     *
     * @param rubble how many rubble blocks to try to place
     * @param random the random generator
     */
    public Board(int rubble, Random random) {
        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r < ROWS; r++) {
                boolean border = c == 0 || r == 0 || c == COLS - 1 || r == ROWS - 1;
                boolean pillar = c % 2 == 0 && r % 2 == 0;
                if (border || pillar) {
                    cells[c][r] = new Box(c, r, TypeBox.NON_DESTROYABLE);
                }
            }
        }
        cells[CORE_COL][CORE_ROW] = new Box(CORE_COL, CORE_ROW, TypeBox.CORE);
        for (int[] p : PORTALS) {
            cells[p[0]][p[1]] = new Box(p[0], p[1], TypeBox.PORTAL);
        }
        portalOpen[0] = true;

        List<int[]> free = new ArrayList<>();
        for (int c = 1; c < COLS - 1; c++) {
            for (int r = 1; r < ROWS - 1; r++) {
                if (cells[c][r] == null && !isReserved(c, r) && !nearStart(c, r)) {
                    free.add(new int[]{c, r});
                }
            }
        }
        Collections.shuffle(free, random);
        int placed = 0;
        List<int[]> sources = new ArrayList<>(Arrays.asList(SPAWNS));
        sources.add(new int[]{START_COL, START_ROW});
        sources.add(new int[]{START2_COL, START2_ROW});
        for (int[] cell : free) {
            if (placed >= rubble) {
                break;
            }
            if (canBuild(cell[0], cell[1], sources) == BuildResult.OK) {
                cells[cell[0]][cell[1]] = new Box(cell[0], cell[1], TypeBox.DESTROYABLE);
                placed++;
            }
        }
    }

    // Spawn tiles, the gate and the tiles right next to them must stay free
    private static boolean isReserved(int c, int r) {
        if (Math.abs(c - GATE_COL) + Math.abs(r - GATE_ROW) <= 1) {
            return true;
        }
        for (int[] s : SPAWNS) {
            if (Math.abs(c - s[0]) + Math.abs(r - s[1]) <= 1) {
                return true;
            }
        }
        return false;
    }

    private static boolean nearStart(int c, int r) {
        return Math.abs(c - START_COL) + Math.abs(r - START_ROW) <= 2
                || Math.abs(c - START2_COL) + Math.abs(r - START2_ROW) <= 2;
    }

    /** @param c column @return x of the tile's left edge */
    public static double tileX(int c) {
        return c * TILE;
    }

    /** @param r row @return y of the tile's top edge */
    public static double tileY(int r) {
        return OFFSET_Y + r * TILE;
    }

    /** @param c column @return x of the tile's center */
    public static double centerX(int c) {
        return tileX(c) + TILE / 2.0;
    }

    /** @param r row @return y of the tile's center */
    public static double centerY(int r) {
        return tileY(r) + TILE / 2.0;
    }

    /** @param x pixel x @return the column containing x */
    public static int colOf(double x) {
        return (int) Math.floor(x / TILE);
    }

    /** @param y pixel y @return the row containing y */
    public static int rowOf(double y) {
        return (int) Math.floor((y - OFFSET_Y) / TILE);
    }

    /**
     * Whether a cell is inside the grid.
     *
     * @param c column
     * @param r row
     * @return true if inside
     */
    public boolean inside(int c, int r) {
        return c >= 0 && r >= 0 && c < COLS && r < ROWS;
    }

    /**
     * Gets the solid block at a cell (blocks that are breaking are no longer solid).
     *
     * @param c column
     * @param r row
     * @return the block or null
     */
    public Box get(int c, int r) {
        return inside(c, r) ? cells[c][r] : null;
    }

    /**
     * Whether a cell is solid (walls, rubble, built walls, core, portals, outside).
     *
     * @param c column
     * @param r row
     * @return true if solid
     */
    public boolean isWall(int c, int r) {
        return !inside(c, r) || cells[c][r] != null;
    }

    /**
     * Whether a cell blocks the player. Same as {@link #isWall}.
     *
     * @param c column
     * @param r row
     * @return true if solid for the player
     */
    public boolean blocksAvatar(int c, int r) {
        return isWall(c, r);
    }

    /**
     * Checks whether a wall may be built on a cell. The wall is placed temporarily and a
     * breadth-first search runs from the core's gate; every source (open portals' spawn
     * tiles and the tiles of every invader) must still be reachable.
     *
     * @param c       column
     * @param r       row
     * @param sources cells {col, row} that must keep a path to the core
     * @return OK, or why the wall can't go there
     */
    public BuildResult canBuild(int c, int r, List<int[]> sources) {
        if (isWall(c, r)) {
            return BuildResult.SOLID;
        }
        if (isReserved(c, r)) {
            return BuildResult.RESERVED;
        }
        for (int[] s : sources) {
            if (s[0] == c && s[1] == r) {
                return BuildResult.OCCUPIED;
            }
        }
        boolean[][] reach = reachableFromGate(c, r);
        for (int[] s : sources) {
            if (inside(s[0], s[1]) && !isWall(s[0], s[1]) && !reach[s[0]][s[1]]) {
                return BuildResult.BLOCKS_PATH;
            }
        }
        return BuildResult.OK;
    }

    /**
     * Breadth-first search from the core's gate over open tiles.
     *
     * @param blockC column to treat as solid (the wall being tested), or -1
     * @param blockR row to treat as solid, or -1
     * @return reach[c][r] true if that tile can walk to the core
     */
    public boolean[][] reachableFromGate(int blockC, int blockR) {
        boolean[][] seen = new boolean[COLS][ROWS];
        if (isWall(GATE_COL, GATE_ROW) || (GATE_COL == blockC && GATE_ROW == blockR)) {
            return seen;
        }
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{GATE_COL, GATE_ROW});
        seen[GATE_COL][GATE_ROW] = true;
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            for (Direction d : Direction.values()) {
                int nc = cur[0] + d.getDx();
                int nr = cur[1] + d.getDy();
                if (inside(nc, nr) && !seen[nc][nr] && !isWall(nc, nr) && !(nc == blockC && nr == blockR)) {
                    seen[nc][nr] = true;
                    queue.add(new int[]{nc, nr});
                }
            }
        }
        return seen;
    }

    /**
     * Builds a player wall. Call {@link #canBuild} first.
     *
     * @param c column
     * @param r row
     */
    public void build(int c, int r) {
        cells[c][r] = new Box(c, r, TypeBox.BUILT);
        cells[c][r].startBuilding();
    }

    /**
     * Destroys rubble or a player wall, starting its break animation.
     *
     * @param c column
     * @param r row
     * @return the type destroyed, or null if nothing breakable was there
     */
    public TypeBox destroy(int c, int r) {
        Box box = get(c, r);
        if (box == null || (box.getTypeBox() != TypeBox.DESTROYABLE && box.getTypeBox() != TypeBox.BUILT)) {
            return null;
        }
        cells[c][r] = null;
        box.startDestroying();
        breaking.add(box);
        return box.getTypeBox();
    }

    /**
     * Advances the block animations.
     *
     * @param dt elapsed seconds
     */
    public void update(double dt) {
        Iterator<Box> it = breaking.iterator();
        while (it.hasNext()) {
            Box box = it.next();
            box.update(dt);
            if (box.isGone()) {
                it.remove();
            }
        }
        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r < ROWS; r++) {
                if (cells[c][r] != null) {
                    cells[c][r].update(dt);
                }
            }
        }
    }

    /**
     * Draws the ground and every block.
     *
     * @param gc        the graphics context
     * @param time      seconds since the game started
     * @param coreRatio core health from 0 to 1, for its glow
     */
    public void paint(GraphicsContext gc, double time, double coreRatio) {
        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r < ROWS; r++) {
                Art.ground(gc, c, r, tileX(c), tileY(r), TILE);
            }
        }
        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r < ROWS; r++) {
                Box box = cells[c][r];
                if (box != null) {
                    box.paint(gc, time, box.getTypeBox() == TypeBox.PORTAL ? (isPortalOpen(portalIndex(c, r)) ? 1 : 0) : coreRatio);
                }
            }
        }
        for (Box box : breaking) {
            box.paint(gc, time, 1);
        }
    }

    private static int portalIndex(int c, int r) {
        for (int i = 0; i < PORTALS.length; i++) {
            if (PORTALS[i][0] == c && PORTALS[i][1] == r) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Finds the first step of the shortest path between two cells, avoiding walls
     * and the active bombs.
     *
     * @param fromC    start column
     * @param fromR    start row
     * @param toC      target column
     * @param toR      target row
     * @param bombs    the active bombs
     * @param maxSteps maximum path length to explore
     * @return the direction of the first step, or null if unreachable
     */
    public Direction firstStepTowards(int fromC, int fromR, int toC, int toR, List<Bomb> bombs, int maxSteps) {
        if (!inside(fromC, fromR) || (fromC == toC && fromR == toR)) {
            return null;
        }
        int[][] dist = new int[COLS][ROWS];
        for (int[] column : dist) {
            Arrays.fill(column, -1);
        }
        Direction[][] first = new Direction[COLS][ROWS];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{fromC, fromR});
        dist[fromC][fromR] = 0;
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int c = cur[0];
            int r = cur[1];
            if (dist[c][r] >= maxSteps) {
                continue;
            }
            for (Direction d : Direction.values()) {
                int nc = c + d.getDx();
                int nr = r + d.getDy();
                boolean target = nc == toC && nr == toR;
                if (!inside(nc, nr) || dist[nc][nr] != -1 || isWall(nc, nr) || (!target && isBombAt(bombs, nc, nr))) {
                    continue;
                }
                dist[nc][nr] = dist[c][r] + 1;
                first[nc][nr] = (c == fromC && r == fromR) ? d : first[c][r];
                if (target) {
                    return first[nc][nr];
                }
                queue.add(new int[]{nc, nr});
            }
        }
        return null;
    }

    /**
     * Length of the current shortest walk from a cell to the gate, ignoring bombs.
     *
     * @param c column
     * @param r row
     * @return number of steps, or -1 if unreachable
     */
    public int pathLength(int c, int r) {
        if (isWall(c, r)) {
            return -1;
        }
        int[][] dist = new int[COLS][ROWS];
        for (int[] column : dist) {
            Arrays.fill(column, -1);
        }
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{c, r});
        dist[c][r] = 0;
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            if (cur[0] == GATE_COL && cur[1] == GATE_ROW) {
                return dist[cur[0]][cur[1]];
            }
            for (Direction d : Direction.values()) {
                int nc = cur[0] + d.getDx();
                int nr = cur[1] + d.getDy();
                if (inside(nc, nr) && dist[nc][nr] == -1 && !isWall(nc, nr)) {
                    dist[nc][nr] = dist[cur[0]][cur[1]] + 1;
                    queue.add(new int[]{nc, nr});
                }
            }
        }
        return -1;
    }

    /**
     * The tiles of the current shortest walk from a cell to the gate, ignoring bombs.
     * Used to show the player the route the invaders will take.
     *
     * @param c column
     * @param r row
     * @return the tiles {col, row} from the cell to the gate, or an empty list
     */
    public List<int[]> pathToGate(int c, int r) {
        List<int[]> path = new ArrayList<>();
        if (isWall(c, r) || isWall(GATE_COL, GATE_ROW)) {
            return path;
        }
        int[][] dist = new int[COLS][ROWS];
        for (int[] column : dist) {
            Arrays.fill(column, -1);
        }
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{GATE_COL, GATE_ROW});
        dist[GATE_COL][GATE_ROW] = 0;
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            for (Direction d : Direction.values()) {
                int nc = cur[0] + d.getDx();
                int nr = cur[1] + d.getDy();
                if (inside(nc, nr) && dist[nc][nr] == -1 && !isWall(nc, nr)) {
                    dist[nc][nr] = dist[cur[0]][cur[1]] + 1;
                    queue.add(new int[]{nc, nr});
                }
            }
        }
        if (dist[c][r] < 0) {
            return path;
        }
        int cc = c;
        int cr = r;
        path.add(new int[]{cc, cr});
        while (dist[cc][cr] > 0) {
            for (Direction d : Direction.values()) {
                int nc = cc + d.getDx();
                int nr = cr + d.getDy();
                if (inside(nc, nr) && dist[nc][nr] == dist[cc][cr] - 1) {
                    cc = nc;
                    cr = nr;
                    break;
                }
            }
            path.add(new int[]{cc, cr});
        }
        return path;
    }

    /**
     * Whether the active, unexploded bomb sits on a cell.
     *
     * @param bomb the bomb, may be null
     * @param c    column
     * @param r    row
     * @return true if the bomb blocks that cell
     */
    public static boolean isBombAt(Bomb bomb, int c, int r) {
        return bomb != null && !bomb.isExploded() && bomb.getCol() == c && bomb.getRow() == r;
    }

    /**
     * Whether any active, unexploded bomb sits on a cell.
     *
     * @param bombs the bombs
     * @param c     column
     * @param r     row
     * @return true if a bomb blocks that cell
     */
    public static boolean isBombAt(List<Bomb> bombs, int c, int r) {
        for (Bomb b : bombs) {
            if (isBombAt(b, c, r)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether a portal is open.
     *
     * @param index portal index
     * @return true if invaders can come out of it
     */
    public boolean isPortalOpen(int index) {
        return index >= 0 && index < portalOpen.length && portalOpen[index];
    }

    /**
     * Opens a portal.
     *
     * @param index portal index
     */
    public void openPortal(int index) {
        portalOpen[index] = true;
    }
}
