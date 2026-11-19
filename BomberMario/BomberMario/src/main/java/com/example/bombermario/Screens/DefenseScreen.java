package com.example.bombermario.Screens;

import com.example.bombermario.audio.SoundManager;
import com.example.bombermario.audio.SoundManager.Sfx;
import com.example.bombermario.model.Art;
import com.example.bombermario.model.Avatar;
import com.example.bombermario.model.Board;
import com.example.bombermario.model.Bomb;
import com.example.bombermario.model.Box;
import com.example.bombermario.model.Direction;
import com.example.bombermario.model.Enemy;
import com.example.bombermario.model.EnemyType;
import com.example.bombermario.model.FloatingText;
import com.example.bombermario.model.GameResult;
import com.example.bombermario.model.GameSettings;
import com.example.bombermario.model.Particles;
import com.example.bombermario.model.Pickup;
import com.example.bombermario.model.State;
import com.example.bombermario.model.TypeBox;
import com.example.bombermario.ui.PixelFont;
import com.example.bombermario.ui.PixelFont.Align;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * "Castillo en Obras": a tower-defense game built on top of the bomb-grid mechanics.
 * <p>
 * Automatons walk from the portals to the core along the shortest path. The players
 * build walls to make that path longer (but may never close it completely) and
 * place dynamite to defeat them. Dynamite also breaks the players' own walls and
 * knocks out any engineer caught in the blast, including the partner in co-op.
 * The game is 6 waves, each one preceded by a short build phase.
 */
public class DefenseScreen {
    /** Number of waves to survive. */
    public static final int TOTAL_WAVES = 6;
    /** Core energy at the start. */
    public static final int CORE_MAX = 20;

    private static final int START_BLOCKS = 10;
    private static final int START_DYNAMITE = 5;
    private static final int BLOCKS_PER_PREP = 6;
    private static final int DYNAMITE_PER_WAVE = 3;
    /** Co-op: extra resources, and 25% more automatons per wave. */
    private static final int COOP_EXTRA_BLOCKS = 4;
    private static final int COOP_EXTRA_PREP_BLOCKS = 2;
    /** Co-op shares one dynamite pool between two players, so it gets noticeably more. */
    private static final int COOP_EXTRA_DYNAMITE = 3;
    private static final int COOP_EXTRA_WAVE_DYNAMITE = 3;
    private static final double COOP_DROP_CHANCE = 0.4;
    private static final double COOP_ENEMY_FACTOR = 1.25;
    private static final int RUBBLE = 24;
    private static final double FIRST_PREP = 25;
    private static final double PREP_TIME = 15;
    private static final double INTRO_TIME = 3.0;
    private static final double END_DELAY = 2.8;
    private static final double EMERGENCY_DELAY = 5.0;
    private static final double DROP_CHANCE = 0.3;

    private static final Color SHADE = Color.rgb(12, 10, 20);
    private static final double MUTE_X = 804;
    private static final double MUTE_Y = 100;
    private static final double MUTE_W = 30;
    private static final double MUTE_H = 18;
    private static final double PAUSE_X = 768;
    /** Buttons of the pause menu: {x, y, w, h}. */
    private static final double[] RESUME_BTN = {244, 352, 168, 46};
    private static final double[] MENU_BTN = {428, 352, 168, 46};

    /** One wave: how many of each automaton, seconds between spawns, speed boost, second portal. */
    private record Wave(int grunts, int runners, int brutes, double interval, double speedBoost, boolean twoPortals) {
        int size() {
            return grunts + runners + brutes;
        }
    }

    private static final Wave[] WAVES = {
            new Wave(8, 0, 0, 2.0, 1.00, false),
            new Wave(10, 3, 0, 1.7, 1.00, false),
            new Wave(10, 5, 1, 1.5, 1.05, false),
            new Wave(10, 4, 2, 1.4, 1.05, true),
            new Wave(10, 8, 4, 1.25, 1.10, true),
            new Wave(14, 10, 6, 1.05, 1.15, true),
    };

    private enum Phase {
        INTRO, PREP, WAVE, WON, LOST
    }

    private final Canvas canvas;
    private final GraphicsContext gc;
    private final Random random = new Random();
    private final Board board;
    private final boolean coop;
    private final List<Avatar> avatars = new ArrayList<>();
    private final List<Bomb> charges = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Pickup> pickups = new ArrayList<>();
    private final List<FloatingText> texts = new ArrayList<>();
    private final Deque<EnemyType> spawnQueue = new ArrayDeque<>();
    private final Particles particles = new Particles();

    private Phase phase = Phase.INTRO;
    private int wave;
    private int core = CORE_MAX;
    private int blocks;
    private int dynamite;
    private final int[] kills = new int[2];
    private final int[] walls = new int[2];
    private final int[] knockouts = new int[2];
    private final double[] beamTimer = new double[2];
    private final double[] stepDust = new double[2];
    private final double[] messageCooldown = new double[2];
    private int nextPortal;
    private boolean startRequested;
    private double time;
    private double phaseTimer = INTRO_TIME;
    private double spawnTimer;
    private double endTimer;
    private double emergencyTimer;
    private double coreFlash;
    private double shake;
    private double dynamiteFlash;
    private double blockFlash;
    private double deniedTimer;
    private int deniedCol;
    private int deniedRow;
    private boolean paused;
    private boolean quitRequested;
    private String bannerTitle;
    private String bannerSub;
    private double bannerTimer;

    /**
     * Builds a new game with the number of players chosen on the title screen.
     *
     * @param canvas the canvas to draw on
     */
    public DefenseScreen(Canvas canvas) {
        this(canvas, GameSettings.getPlayers());
    }

    /**
     * Builds a new game.
     *
     * @param canvas  the canvas to draw on
     * @param players 1 for solo, 2 for local co-op
     */
    public DefenseScreen(Canvas canvas, int players) {
        this.canvas = canvas;
        this.gc = canvas.getGraphicsContext2D();
        this.coop = players == 2;
        this.board = new Board(RUBBLE, random);
        if (coop) {
            avatars.add(new Avatar(canvas, 0, Avatar.Controls.P1, Art.Outfit.BLUE, Board.START_COL, Board.START_ROW));
            avatars.add(new Avatar(canvas, 1, Avatar.Controls.P2, Art.Outfit.AMBER, Board.START2_COL, Board.START2_ROW));
        } else {
            avatars.add(new Avatar(canvas));
        }
        blocks = START_BLOCKS + (coop ? COOP_EXTRA_BLOCKS : 0);
        dynamite = START_DYNAMITE + (coop ? COOP_EXTRA_DYNAMITE : 0);
        banner("BOMBER-DEFENSA", coop ? "Modo cooperativo · Defiendan el Núcleo juntos"
                : "Defiende el Núcleo durante " + TOTAL_WAVES + " oleadas", INTRO_TIME);
    }

    // ------------------------------------------------------------------ rules

    /**
     * Runs the game rules for one frame.
     *
     * @param dt elapsed seconds since the previous frame
     */
    public void update(double dt) {
        if (paused) {
            return;
        }
        time += dt;
        board.update(dt);
        texts.forEach(t -> t.update(dt));
        texts.removeIf(FloatingText::isDone);
        coreFlash = Math.max(0, coreFlash - dt);
        shake = Math.max(0, shake - dt);
        dynamiteFlash = Math.max(0, dynamiteFlash - dt);
        blockFlash = Math.max(0, blockFlash - dt);
        deniedTimer = Math.max(0, deniedTimer - dt);
        bannerTimer = Math.max(0, bannerTimer - dt);
        for (int i = 0; i < 2; i++) {
            beamTimer[i] = Math.max(0, beamTimer[i] - dt);
            messageCooldown[i] = Math.max(0, messageCooldown[i] - dt);
        }
        particles.update(dt);

        if (phase == Phase.WON || phase == Phase.LOST) {
            endTimer += dt;
            enemies.forEach(e -> e.update(dt, board, charges));
            return;
        }
        if (phase == Phase.INTRO) {
            phaseTimer -= dt;
            for (Avatar a : avatars) {
                a.consumeBombRequest();
                a.consumeBuildRequest();
            }
            startRequested = false;
            if (phaseTimer <= 0) {
                startPrep();
            }
            return;
        }

        for (Avatar a : avatars) {
            a.update(dt, board, charges);
            if (a.consumeRespawned()) {
                onRespawn(a);
            }
            if (a.isAlive() && a.getState() == State.RUN) {
                stepDust[a.getIndex()] -= dt;
                if (stepDust[a.getIndex()] <= 0) {
                    particles.dust(a.getX(), a.getY() + 14, 1);
                    stepDust[a.getIndex()] = 0.16;
                }
            }
            if (a.isAlive()) {
                handleDynamite(a);
                handleBuilding(a);
            } else {
                a.consumeBombRequest();
                a.consumeBuildRequest();
            }
        }
        boolean startNow = startRequested;
        startRequested = false;
        updateCharges(dt);
        updateEnemies(dt);
        updatePickups(dt);
        updateEmergencyDynamite(dt);

        if (phase == Phase.PREP) {
            phaseTimer -= dt;
            if (phaseTimer <= 0 || startNow) {
                startWave();
            }
        } else if (phase == Phase.WAVE) {
            updateSpawning(dt);
            if (spawnQueue.isEmpty() && enemies.isEmpty()) {
                if (wave >= TOTAL_WAVES) {
                    finish(Phase.WON);
                } else {
                    startPrep();
                }
            }
        }
        if (core <= 0 && phase != Phase.LOST) {
            finish(Phase.LOST);
        }
    }

    private void startPrep() {
        phase = Phase.PREP;
        phaseTimer = wave == 0 ? FIRST_PREP : PREP_TIME;
        if (wave > 0) {
            int gift = BLOCKS_PER_PREP + (coop ? COOP_EXTRA_PREP_BLOCKS : 0);
            blocks += gift;
            blockFlash = 0.8;
            banner("¡OLEADA " + wave + " SUPERADA!", "+" + gift + " bloques · Refuercen el laberinto", 2.6);
        } else {
            banner("FASE DE CONSTRUCCIÓN", coop ? "J1 construye con E · J2 con M" : "Construye muros con E", 3.0);
        }
        if (wave < TOTAL_WAVES && WAVES[wave].twoPortals() && !board.isPortalOpen(1)) {
            texts.add(new FloatingText(Board.centerX(5), Board.centerY(Board.ROWS - 2) - 10,
                    "¡El portal de abajo se abrirá!", Color.rgb(230, 150, 255), 16));
        }
    }

    private int waveDynamite() {
        return DYNAMITE_PER_WAVE + (coop ? COOP_EXTRA_WAVE_DYNAMITE : 0);
    }

    private int scaled(int n) {
        return coop ? (int) Math.round(n * COOP_ENEMY_FACTOR) : n;
    }

    private void startWave() {
        Wave w = WAVES[wave];
        wave++;
        phase = Phase.WAVE;
        if (wave > 1) {
            int gift = waveDynamite();
            dynamite += gift;
            dynamiteFlash = 0.8;
        }
        List<EnemyType> list = new ArrayList<>();
        for (int i = 0; i < scaled(w.grunts()); i++) {
            list.add(EnemyType.GRUNT);
        }
        for (int i = 0; i < scaled(w.runners()); i++) {
            list.add(EnemyType.RUNNER);
        }
        for (int i = 0; i < scaled(w.brutes()); i++) {
            list.add(EnemyType.BRUTE);
        }
        Collections.shuffle(list, random);
        // The first automatons of a wave are plain ones, so every wave starts readable
        for (int i = 0; i < Math.min(3, list.size()); i++) {
            if (list.get(i) != EnemyType.GRUNT) {
                int g = list.indexOf(EnemyType.GRUNT);
                if (g > i) {
                    Collections.swap(list, i, g);
                }
            }
        }
        spawnQueue.clear();
        spawnQueue.addAll(list);
        spawnTimer = 0.5;
        String sub = list.size() + " autómatas";
        if (w.twoPortals() && !board.isPortalOpen(1)) {
            board.openPortal(1);
            sub += " · ¡Segundo portal abierto!";
        }
        if (wave > 1) {
            sub += " · +" + waveDynamite() + " dinamita";
        }
        SoundManager.play(Sfx.WAVE_START);
        banner(wave == TOTAL_WAVES ? "¡OLEADA FINAL!" : "OLEADA " + wave, sub, 2.4);
    }

    private void updateSpawning(double dt) {
        if (spawnQueue.isEmpty()) {
            return;
        }
        spawnTimer -= dt;
        if (spawnTimer > 0) {
            return;
        }
        Wave w = WAVES[wave - 1];
        // Co-op has 25% more automatons in the same time window: denser, not longer
        spawnTimer = w.interval() / (coop ? COOP_ENEMY_FACTOR : 1);
        int portal = 0;
        if (board.isPortalOpen(1)) {
            portal = nextPortal;
            nextPortal = 1 - nextPortal;
        }
        int[] spawn = Board.SPAWNS[portal];
        enemies.add(new Enemy(canvas, spawn[0], spawn[1], spawnQueue.poll(), w.speedBoost()));
    }

    private void finish(Phase result) {
        phase = result;
        endTimer = 0;
        GameResult.save(avatars.size(), wave, TOTAL_WAVES, core, CORE_MAX, time, kills, walls, knockouts);
        SoundManager.play(result == Phase.WON ? Sfx.VICTORY : Sfx.DEFEAT);
        if (result == Phase.WON) {
            banner("¡NÚCLEO A SALVO!", "Resistieron las " + TOTAL_WAVES + " oleadas", END_DELAY + 1);
        } else {
            banner("EL NÚCLEO CAYÓ", "Llegaron a la oleada " + wave, END_DELAY + 1);
        }
    }

    private Bomb activeChargeOf(Avatar a) {
        for (Bomb b : charges) {
            if (b.getOwner() == a.getIndex() && !b.isExploded()) {
                return b;
            }
        }
        return null;
    }

    private void handleDynamite(Avatar a) {
        if (!a.consumeBombRequest()) {
            return;
        }
        int c = a.getCol();
        int r = a.getRow();
        if (activeChargeOf(a) != null) {
            message(a, "¡Una dinamita a la vez!", Color.rgb(255, 200, 120));
        } else if (dynamite <= 0) {
            message(a, "¡Sin dinamita!", Color.rgb(255, 90, 90));
            SoundManager.play(Sfx.DENIED);
        } else if (!board.isWall(c, r) && !Board.isBombAt(charges, c, r)) {
            Bomb b = new Bomb(c, r, a.getIndex());
            charges.add(b);
            dynamite--;
            a.ignoreBomb(b);
            SoundManager.play(Sfx.BOMB_PLACE);
        }
    }

    // The tile in front of an engineer, where it builds a wall
    private static int[] buildTarget(Avatar a) {
        Direction d = a.getLastDir();
        return new int[]{a.getCol() + d.getDx(), a.getRow() + d.getDy()};
    }

    private List<int[]> pathSources() {
        List<int[]> sources = new ArrayList<>();
        Collections.addAll(sources, Board.SPAWNS);
        for (Enemy e : enemies) {
            if (e.isAlive()) {
                sources.add(e.getTile());
                sources.add(e.getTargetTile());
            }
        }
        return sources;
    }

    // Checks every rule for building on a tile; returns null if allowed, or the reason it isn't
    private String buildProblem(Avatar builder, int c, int r) {
        if (Board.isBombAt(charges, c, r)) {
            return "¡Hay dinamita ahí!";
        }
        for (Pickup p : pickups) {
            if (p.isVisible() && p.getCol() == c && p.getRow() == r) {
                return "¡Recoge el objeto primero!";
            }
        }
        for (Avatar other : avatars) {
            if (other != builder && other.overlaps(c, r)) {
                return "¡Tu compañero está ahí!";
            }
        }
        return switch (board.canBuild(c, r, pathSources())) {
            case OK -> null;
            case SOLID -> "Ahí no se puede construir";
            case RESERVED -> "Zona protegida";
            case OCCUPIED -> "¡Hay un autómata ahí!";
            case BLOCKS_PATH -> "¡Bloquearías el camino!";
        };
    }

    private void handleBuilding(Avatar a) {
        if (!a.consumeBuildRequest()) {
            return;
        }
        if (blocks <= 0) {
            message(a, "¡Sin bloques!", Color.rgb(255, 90, 90));
            SoundManager.play(Sfx.DENIED);
            return;
        }
        int[] t = buildTarget(a);
        String problem = buildProblem(a, t[0], t[1]);
        if (problem != null) {
            message(a, problem, Color.rgb(255, 110, 110));
            deniedCol = t[0];
            deniedRow = t[1];
            deniedTimer = 0.6;
            SoundManager.play(Sfx.DENIED);
            return;
        }
        if (a.overlaps(t[0], t[1])) {
            a.snapToTile();
        }
        board.build(t[0], t[1]);
        particles.dust(Board.centerX(t[0]), Board.centerY(t[1]) + 16, 6);
        SoundManager.play(Sfx.BUILD);
        blocks--;
        walls[a.getIndex()]++;
    }

    private void message(Avatar a, String text, Color color) {
        int i = a.getIndex();
        if (messageCooldown[i] <= 0) {
            texts.add(new FloatingText(clampX(a.getX()), a.getY() - 30, text, color, 13));
            messageCooldown[i] = 0.8;
        }
    }

    private void hurtCore(int amount, double x, double y, String why) {
        core = Math.max(0, core - amount);
        coreFlash = 0.5;
        shake = 0.3;
        texts.add(new FloatingText(x, y, why + " -" + amount, Color.rgb(255, 80, 80), 14));
        particles.sparks(Board.centerX(Board.CORE_COL) - 6, Board.centerY(Board.CORE_ROW),
                Art.coreColor((double) core / CORE_MAX), 14);
        SoundManager.play(Sfx.CORE_HIT);
    }

    // Knocked out: no permanent life is lost, but the core will pay for the respawn
    private void knockOut(Avatar a, boolean friendlyFire) {
        knockouts[a.getIndex()]++;
        texts.add(new FloatingText(clampX(a.getX()), a.getY() - 78, friendlyFire ? "¡Fuego amigo!" : "¡Aturdido!",
                Color.rgb(255, 210, 90), 18));
        particles.sparks(a.getX(), a.getY() - 10, Color.rgb(255, 240, 120), 10);
        SoundManager.play(Sfx.DENIED);
    }

    // The core rebuilds the engineer next to it and pays 1 energy for it
    private void onRespawn(Avatar a) {
        beamTimer[a.getIndex()] = 0.7;
        hurtCore(1, clampX(a.getX()), a.getY() - 34, (coop ? "J" + (a.getIndex() + 1) + " reaparece: Núcleo" : "Reaparición: Núcleo"));
        particles.sparks(a.getX(), a.getY(), Color.rgb(120, 220, 255), 16);
        SoundManager.play(Sfx.RESPAWN);
    }

    private void updateCharges(double dt) {
        Iterator<Bomb> it = charges.iterator();
        while (it.hasNext()) {
            Bomb b = it.next();
            b.update(dt);
            if (b.isReadyToExplode()) {
                List<int[]> brokenBlocks = b.explode(board);
                particles.explosion(b.getFlames());
                SoundManager.play(Sfx.EXPLOSION);
                shake = Math.max(shake, 0.18);
                for (int[] broken : brokenBlocks) {
                    particles.debris(broken[0], broken[1], broken[2] == TypeBox.BUILT.ordinal());
                    if (broken[2] == TypeBox.DESTROYABLE.ordinal()) {
                        pickups.add(new Pickup(broken[0], broken[1], Pickup.Kind.BLOCKS, 1, Box.DESTROY_TIME));
                    } else {
                        texts.add(new FloatingText(Board.centerX(broken[0]), Board.centerY(broken[1]) - 18,
                                "¡Tu muro!", Color.rgb(255, 170, 60), 12));
                    }
                }
            }
            if (b.isExploding()) {
                double h = Avatar.HALF - 4;
                for (Avatar a : avatars) {
                    if (a.isAlive() && b.hits(a.getX() - h, a.getY() - h, a.getX() + h, a.getY() + h) && a.hit()) {
                        knockOut(a, coop && b.getOwner() != a.getIndex());
                    }
                }
                for (Enemy enemy : enemies) {
                    double ex = enemy.getPosition().getX();
                    double ey = enemy.getPosition().getY();
                    if (enemy.canBeHit() && b.hits(ex - 8, ey - 8, ex + 8, ey + 8)) {
                        if (enemy.damage()) {
                            kills[b.getOwner()]++;
                            particles.splat(ex, ey, enemy.getType().getColor());
                            SoundManager.play(Sfx.ENEMY_DOWN);
                            texts.add(new FloatingText(ex, ey - 22, "¡Pum!", Color.rgb(255, 170, 40), 13));
                            if (random.nextDouble() < (coop ? COOP_DROP_CHANCE : DROP_CHANCE)) {
                                pickups.add(new Pickup(Board.colOf(ex), Board.rowOf(ey), Pickup.Kind.BOMB, 1, 0.3));
                            }
                        } else if (enemy.isAlive()) {
                            texts.add(new FloatingText(ex, ey - 26, "-1", Color.WHITE, 12));
                        }
                    }
                }
            }
            if (b.isFinished()) {
                it.remove();
            }
        }
    }

    private void updateEnemies(double dt) {
        for (Enemy enemy : enemies) {
            enemy.update(dt, board, charges);
            if (enemy.consumeOverloadStart()) {
                texts.add(new FloatingText(enemy.getPosition().getX(), enemy.getPosition().getY() - 30,
                        "¡Sobrecarga!", Color.rgb(255, 110, 80), 12));
            }
            if (enemy.hasReachedCore()) {
                hurtCore(enemy.getType().getCoreDamage(), Board.centerX(Board.CORE_COL) - 70,
                        Board.centerY(Board.CORE_ROW) - 24, "Núcleo");
                continue;
            }
            for (Avatar a : avatars) {
                if (enemy.touches(a) && a.hit()) {
                    knockOut(a, false);
                }
            }
        }
        enemies.removeIf(Enemy::isRemovable);
    }

    private void updatePickups(double dt) {
        Iterator<Pickup> it = pickups.iterator();
        while (it.hasNext()) {
            Pickup pickup = it.next();
            pickup.update(dt);
            if (pickup.isExpired()) {
                it.remove();
                continue;
            }
            double px = Board.centerX(pickup.getCol());
            double py = Board.centerY(pickup.getRow());
            for (Avatar a : avatars) {
                if (pickup.isVisible() && a.isAlive() && Math.hypot(px - a.getX(), py - a.getY()) < 24) {
                    if (pickup.getKind() == Pickup.Kind.BOMB) {
                        dynamite += pickup.getAmount();
                        dynamiteFlash = 0.6;
                        texts.add(new FloatingText(px, py - 20, "+" + pickup.getAmount() + " dinamita", Color.rgb(255, 230, 60), 14));
                    } else {
                        blocks += pickup.getAmount();
                        blockFlash = 0.6;
                        texts.add(new FloatingText(px, py - 20, "+" + pickup.getAmount() + " bloque", Color.rgb(140, 210, 255), 14));
                    }
                    particles.sparks(px, py, pickup.getKind() == Pickup.Kind.BOMB ? Color.rgb(255, 220, 80) : Color.rgb(120, 190, 255), 10);
                    SoundManager.play(Sfx.PICKUP);
                    it.remove();
                    break;
                }
            }
        }
    }

    private boolean dynamiteOnFloor() {
        for (Pickup p : pickups) {
            if (p.getKind() == Pickup.Kind.BOMB) {
                return true;
            }
        }
        return false;
    }

    // If the team runs out of dynamite during a wave with no way to get more, give one back after a while
    private void updateEmergencyDynamite(double dt) {
        if (phase == Phase.WAVE && dynamite == 0 && charges.isEmpty() && !dynamiteOnFloor()) {
            emergencyTimer += dt;
            if (emergencyTimer >= EMERGENCY_DELAY) {
                emergencyTimer = 0;
                dynamite++;
                dynamiteFlash = 0.6;
                Avatar a = avatars.get(0);
                texts.add(new FloatingText(clampX(a.getX()), a.getY() - 30, "Dinamita de emergencia +1", Color.rgb(255, 230, 60), 13));
                SoundManager.play(Sfx.PICKUP);
            }
        } else {
            emergencyTimer = 0;
        }
    }

    private void banner(String title, String sub, double seconds) {
        bannerTitle = title;
        bannerSub = sub;
        bannerTimer = seconds;
    }

    // Keeps centered labels (about 220 px wide) inside the screen
    private double clampX(double x) {
        return Math.max(120, Math.min(canvas.getWidth() - 120, x));
    }

    // ------------------------------------------------------------------ drawing

    private void px(String text, double x, double y, double scale, Color color, Align align) {
        PixelFont.draw(gc, text, x, y, scale, color, SHADE, align);
    }

    /**
     * Draws the whole game.
     */
    public void paint() {
        gc.setFill(Color.rgb(24, 26, 34));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.save();
        if (shake > 0) {
            gc.translate((random.nextDouble() - 0.5) * 8 * shake / 0.3, (random.nextDouble() - 0.5) * 8 * shake / 0.3);
        }
        board.paint(gc, time, (double) core / CORE_MAX);
        paintPaths();
        for (Pickup pickup : pickups) {
            pickup.paint(gc);
        }
        for (Bomb b : charges) {
            if (!b.isExploded()) {
                b.paint(gc);
            }
        }
        paintBuildPreviews();
        List<Enemy> sorted = new ArrayList<>(enemies);
        sorted.sort((a, b) -> Double.compare(a.getPosition().getY(), b.getPosition().getY()));
        for (Enemy enemy : sorted) {
            enemy.paint();
        }
        for (Avatar a : avatars) {
            a.paint();
            if (coop) {
                paintPlayerTag(a);
            }
        }
        for (Bomb b : charges) {
            if (b.isExploded()) {
                b.paint(gc);
            }
        }
        particles.paint(gc);
        for (Avatar a : avatars) {
            paintRespawnInfo(a);
        }
        gc.restore();
        if (coreFlash > 0) {
            gc.setFill(Color.rgb(255, 0, 0, 0.25 * coreFlash / 0.5));
            gc.fillRect(0, Board.OFFSET_Y, canvas.getWidth(), canvas.getHeight() - Board.OFFSET_Y);
        }
        drawHud();
        for (FloatingText text : texts) {
            text.paint(gc);
        }
        if (phase == Phase.WON || phase == Phase.LOST) {
            gc.setFill(Color.rgb(0, 0, 0, Math.min(0.6, endTimer / END_DELAY * 0.6)));
            gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        }
        if (bannerTimer > 0) {
            drawBanner();
        }
        if (paused) {
            drawPauseMenu();
        }
    }

    private void drawPauseMenu() {
        double w = canvas.getWidth();
        gc.setFill(Color.rgb(8, 8, 16, 0.72));
        gc.fillRect(0, 0, w, canvas.getHeight());
        gc.setFill(Color.rgb(60, 64, 96));
        gc.fillRoundRect(w / 2 - 222, 214, 444, 228, 10, 10);
        gc.setFill(Color.rgb(18, 19, 31));
        gc.fillRoundRect(w / 2 - 219, 217, 438, 222, 8, 8);
        PixelFont.draw(gc, "Pausa", w / 2, 240, 7, Color.rgb(255, 205, 40), Color.rgb(163, 69, 26), Align.CENTER);
        px("La partida está detenida", w / 2, 304, 1.8, Color.rgb(200, 205, 225), Align.CENTER);
        drawMenuButton(RESUME_BTN, "Continuar", Color.rgb(255, 205, 40), Color.rgb(163, 69, 26), Color.rgb(28, 30, 42));
        drawMenuButton(MENU_BTN, "Portada", Color.rgb(63, 99, 200), Color.rgb(32, 48, 94), Color.WHITE);
        px("P o ESC: continuar", w / 2, 414, 1.4, Color.rgb(150, 155, 180), Align.CENTER);
    }

    private void drawMenuButton(double[] r, String label, Color face, Color edge, Color text) {
        gc.setFill(edge);
        gc.fillRoundRect(r[0], r[1], r[2], r[3], 8, 8);
        gc.setFill(face);
        gc.fillRoundRect(r[0], r[1], r[2], r[3] - 5, 8, 8);
        PixelFont.draw(gc, label, r[0] + r[2] / 2, r[1] + (r[3] - 5) / 2 - 8, 2.4, text, null, Align.CENTER);
    }

    private static boolean inside(double[] r, double x, double y) {
        return x >= r[0] && x <= r[0] + r[2] && y >= r[1] && y <= r[1] + r[3];
    }

    private void setPaused(boolean value) {
        if (phase == Phase.WON || phase == Phase.LOST) {
            return;
        }
        paused = value;
        SoundManager.play(Sfx.CLICK);
    }

    private void paintPlayerTag(Avatar a) {
        String tag = "J" + (a.getIndex() + 1);
        double x = a.getX();
        double y = a.getY() - 46;
        gc.setFill(a.getOutfit().getVest().darker());
        gc.fillRoundRect(x - 11, y - 3, 22, 14, 6, 6);
        gc.setFill(a.getOutfit().getVest());
        gc.fillRoundRect(x - 11, y - 4, 22, 13, 6, 6);
        gc.fillPolygon(new double[]{x - 4, x + 4, x}, new double[]{y + 9, y + 9, y + 14}, 3);
        px(tag, x + 0.5, y, 1.3, Color.WHITE, Align.CENTER);
    }

    // Dotted, flowing line along the route each open portal's automatons will take
    private void paintPaths() {
        boolean building = phase == Phase.PREP || phase == Phase.INTRO;
        for (int i = 0; i < Board.SPAWNS.length; i++) {
            boolean open = board.isPortalOpen(i);
            boolean upcoming = !open && wave < TOTAL_WAVES && WAVES[wave].twoPortals();
            if (!open && !upcoming) {
                continue;
            }
            List<int[]> path = board.pathToGate(Board.SPAWNS[i][0], Board.SPAWNS[i][1]);
            double alpha = building ? 0.85 : 0.35;
            if (upcoming) {
                alpha *= 0.6;
            }
            gc.setFill(i == 0 ? Color.rgb(220, 140, 255, alpha) : Color.rgb(255, 140, 200, alpha));
            for (int k = 0; k < path.size(); k++) {
                double pulse = 0.5 + 0.5 * Math.sin(time * 6 - k * 0.6);
                double rad = 2.5 + 2 * pulse;
                gc.fillOval(Board.centerX(path.get(k)[0]) - rad, Board.centerY(path.get(k)[1]) - rad, rad * 2, rad * 2);
            }
        }
    }

    private void paintBuildPreviews() {
        boolean active = phase == Phase.PREP || phase == Phase.WAVE;
        if (active) {
            for (Avatar a : avatars) {
                if (!a.isAlive()) {
                    continue;
                }
                int[] t = buildTarget(a);
                if (!board.inside(t[0], t[1]) || board.isWall(t[0], t[1])) {
                    continue;
                }
                boolean ok = blocks > 0 && buildProblem(a, t[0], t[1]) == null;
                double x = Board.tileX(t[0]);
                double y = Board.tileY(t[1]);
                double pulse = 0.5 + 0.5 * Math.sin(time * 6);
                gc.setFill(ok ? Color.rgb(90, 220, 120, 0.18 + 0.12 * pulse) : Color.rgb(255, 80, 80, 0.15 + 0.1 * pulse));
                gc.fillRect(x + 2, y + 2, Board.TILE - 4, Board.TILE - 4);
                Color edge = ok ? Color.rgb(120, 255, 150, 0.9) : Color.rgb(255, 100, 100, 0.8);
                if (coop) {
                    edge = ok ? a.getOutfit().getVest().brighter() : edge;
                }
                gc.setStroke(edge);
                gc.setLineWidth(2);
                gc.setLineDashes(6, 4);
                gc.strokeRect(x + 2, y + 2, Board.TILE - 4, Board.TILE - 4);
                gc.setLineDashes();
            }
        }
        if (deniedTimer > 0) {
            double x = Board.tileX(deniedCol);
            double y = Board.tileY(deniedRow);
            gc.setStroke(Color.rgb(255, 60, 60, deniedTimer / 0.6));
            gc.setLineWidth(4);
            gc.strokeLine(x + 8, y + 8, x + Board.TILE - 8, y + Board.TILE - 8);
            gc.strokeLine(x + Board.TILE - 8, y + 8, x + 8, y + Board.TILE - 8);
        }
    }

    // Energy beam from the core to the engineer when it respawns, and a countdown while knocked out
    private void paintRespawnInfo(Avatar a) {
        double beam = beamTimer[a.getIndex()];
        if (beam > 0) {
            double k = beam / 0.7;
            double cx = Board.centerX(Board.CORE_COL);
            double cy = Board.centerY(Board.CORE_ROW);
            Color c = Art.coreColor((double) core / CORE_MAX);
            gc.setStroke(Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0.35 * k));
            gc.setLineWidth(10);
            gc.strokeLine(cx, cy, a.getX(), a.getY());
            gc.setStroke(Color.rgb(255, 255, 255, 0.9 * k));
            gc.setLineWidth(2.5);
            gc.strokeLine(cx, cy, a.getX(), a.getY());
        }
        if (a.isKnockedOut()) {
            double x = clampX(a.getX());
            double y = a.getY() - 62;
            gc.setFill(Color.rgb(0, 0, 0, 0.75));
            gc.fillRoundRect(x - 104, y - 6, 208, 38, 8, 8);
            String who = coop ? "J" + (a.getIndex() + 1) + " " : "";
            px(who + String.format("reaparece en %.1f s", a.getKnockoutLeft()).replace(',', '.'), x, y, 1.4, Color.WHITE, Align.CENTER);
            px("costará -1 energía del Núcleo", x, y + 16, 1.2, Color.rgb(255, 130, 130), Align.CENTER);
        }
    }

    private void drawHud() {
        double w = canvas.getWidth();
        gc.setFill(Color.rgb(28, 30, 42));
        gc.fillRect(0, 0, w, 98);
        gc.setFill(Color.rgb(17, 18, 27));
        gc.fillRect(0, 98, w, Board.OFFSET_Y - 98);
        gc.setFill(Color.rgb(60, 64, 84));
        gc.fillRect(0, 97, w, 2);
        gc.fillRect(0, Board.OFFSET_Y - 2, w, 2);

        // Left: wave, phase, route length, defeated
        String waveText = phase == Phase.INTRO || (phase == Phase.PREP && wave == 0)
                ? "OLEADA 1/" + TOTAL_WAVES
                : "OLEADA " + (phase == Phase.PREP ? wave + 1 : wave) + "/" + TOTAL_WAVES;
        px(waveText, 16, 12, 3, Color.WHITE, Align.LEFT);
        String phaseText;
        Color phaseColor;
        switch (phase) {
            case PREP -> {
                int s = (int) Math.ceil(phaseTimer);
                phaseText = "Construye: " + s + " s";
                phaseColor = s <= 5 && ((int) (phaseTimer * 4)) % 2 == 0 ? Color.rgb(255, 120, 80) : Color.rgb(120, 230, 150);
            }
            case WAVE -> {
                int left = spawnQueue.size();
                for (Enemy e : enemies) {
                    if (e.isAlive()) {
                        left++;
                    }
                }
                phaseText = "Autómatas: " + left;
                phaseColor = Color.rgb(255, 170, 170);
            }
            case WON -> {
                phaseText = "¡Victoria!";
                phaseColor = Color.rgb(255, 220, 80);
            }
            case LOST -> {
                phaseText = "Derrota";
                phaseColor = Color.rgb(255, 90, 90);
            }
            default -> {
                phaseText = "Prepárate...";
                phaseColor = Color.WHITE;
            }
        }
        px(phaseText, 16, 44, 1.8, phaseColor, Align.LEFT);
        int pathLen = board.pathLength(Board.SPAWNS[0][0], Board.SPAWNS[0][1]);
        px("Recorrido: " + pathLen + " casillas", 16, 66, 1.3, Color.rgb(210, 170, 255), Align.LEFT);
        px("Derrotados: " + (kills[0] + kills[1]), 16, 82, 1.3, Color.rgb(190, 195, 210), Align.LEFT);

        // Center: core energy (one segment per point), then shared resources
        double bx = 300;
        double bw = 240;
        double ratio = (double) core / CORE_MAX;
        Art.coreCrystal(gc, bx - 16, 30, 0.5, time, ratio);
        px("Energía del Núcleo " + core + "/" + CORE_MAX, bx + bw / 2, 8, 1.4, Color.rgb(150, 220, 255), Align.CENTER);
        gc.setFill(Color.rgb(15, 15, 22));
        gc.fillRoundRect(bx - 3, 21, bw + 6, 22, 8, 8);
        Color barColor = ratio > 0.5 ? Color.rgb(80, 200, 255) : (ratio > 0.25 ? Color.rgb(255, 190, 60) : Color.rgb(255, 70, 70));
        if (coreFlash > 0 && ((int) (coreFlash * 20)) % 2 == 0) {
            barColor = Color.WHITE;
        }
        double seg = bw / CORE_MAX;
        for (int i = 0; i < CORE_MAX; i++) {
            gc.setFill(i < core ? barColor : Color.rgb(45, 48, 62));
            gc.fillRoundRect(bx + i * seg + 1, 24, seg - 2, 16, 3, 3);
        }
        double pop = 1 + 0.35 * dynamiteFlash;
        Art.dynamite(gc, 318, 68, 10 * pop, time);
        px("×" + dynamite, 336, 56, 2.6, dynamiteFlash > 0 ? Color.rgb(255, 230, 60) : Color.WHITE, Align.LEFT);
        String dynLabel = "dinamita";
        Color dynColor = Color.rgb(170, 175, 195);
        if (dynamite == 0) {
            if (phase != Phase.WAVE) {
                dynLabel = "+" + waveDynamite() + " en la oleada";
            } else if (!charges.isEmpty()) {
                dynLabel = "espera la explosión";
            } else if (dynamiteOnFloor()) {
                dynLabel = "¡recoge la del suelo!";
            } else {
                dynLabel = "emergencia en " + (int) Math.ceil(EMERGENCY_DELAY - emergencyTimer) + " s";
            }
            dynColor = Color.rgb(255, 170, 90);
        }
        px(dynLabel, 336, 80, 1.1, dynColor, Align.LEFT);
        double s = 22 * (1 + 0.35 * blockFlash);
        gc.save();
        gc.translate(452 - s / 2, 68 - s / 2);
        gc.scale(s / 40, s / 40);
        Art.crate(gc, 0, 0, 40);
        gc.restore();
        px("×" + blocks, 470, 56, 2.6, blockFlash > 0 ? Color.rgb(140, 210, 255) : Color.WHITE, Align.LEFT);
        px("bloques", 470, 80, 1.1, blocks == 0 ? Color.rgb(255, 170, 90) : Color.rgb(170, 175, 195), Align.LEFT);

        // Right: respawn rule panel, well away from the controls strip
        drawRespawnPanel(574, 8, 256, 82);

        // Bottom strip: controls and the sound button
        String hint = coop
                ? "J1: WASD · E muro · Q dinamita    J2: flechas · M muro · espacio dinamita    ENTER adelantar · P pausa"
                : "WASD/flechas mover · E muro · espacio dinamita · ENTER adelantar · P pausa · N sonido";
        px(hint, 392, 105, 1.15, Color.rgb(165, 170, 192), Align.CENTER);
        drawMuteButton();
        drawPauseButton();
    }

    private void drawRespawnPanel(double x, double y, double w, double h) {
        boolean anyDown = false;
        for (Avatar a : avatars) {
            anyDown |= a.isKnockedOut();
        }
        gc.setFill(Color.rgb(48, 36, 42));
        gc.fillRoundRect(x, y, w, h, 10, 10);
        gc.setStroke(Color.rgb(255, 130, 110, anyDown ? 0.6 + 0.4 * Math.sin(time * 12) : 0.45));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(x, y, w, h, 10, 10);
        if (coop) {
            Art.builder(gc, x + 16, y + 40, 0.6, 1, time, false, avatars.get(0).isKnockedOut(), Art.Outfit.BLUE);
            Art.builder(gc, x + 30, y + 46, 0.6, -1, time + 1, false, avatars.get(1).isKnockedOut(), Art.Outfit.AMBER);
        } else {
            Art.builder(gc, x + 20, y + 44, 0.75, 1, time, false, avatars.get(0).isKnockedOut());
        }
        double tx = x + 54;
        px("Reapariciones: ∞", tx, y + 9, 1.5, Color.WHITE, Align.LEFT);
        px("cada aturdimiento =", tx, y + 28, 1.15, Color.rgb(255, 160, 140), Align.LEFT);
        px("-1 energía del Núcleo", tx, y + 41, 1.15, Color.rgb(255, 160, 140), Align.LEFT);
        if (coop) {
            px("J1: " + knockouts[0], tx, y + 62, 1.3, Art.Outfit.BLUE.getVest().brighter(), Align.LEFT);
            px("J2: " + knockouts[1], tx + 64, y + 62, 1.3, Art.Outfit.AMBER.getVest().brighter(), Align.LEFT);
            px("aturdidos", tx + 124, y + 63, 1.1, Color.rgb(170, 175, 195), Align.LEFT);
        } else {
            px("aturdido: " + knockouts[0] + (knockouts[0] == 1 ? " vez" : " veces"), tx, y + 62, 1.3,
                    Color.rgb(190, 195, 210), Align.LEFT);
        }
    }

    private void drawMuteButton() {
        boolean muted = SoundManager.isMuted();
        gc.setFill(muted ? Color.rgb(90, 50, 55) : Color.rgb(55, 60, 85));
        gc.fillRoundRect(MUTE_X, MUTE_Y, MUTE_W, MUTE_H, 6, 6);
        double x = MUTE_X + 7;
        double y = MUTE_Y + MUTE_H / 2;
        gc.setFill(Color.WHITE);
        gc.fillPolygon(new double[]{x, x + 4, x + 9, x + 9, x + 4, x},
                new double[]{y - 3, y - 3, y - 7, y + 7, y + 3, y + 3}, 6);
        gc.setStroke(muted ? Color.rgb(255, 110, 110) : Color.WHITE);
        gc.setLineWidth(1.8);
        if (muted) {
            gc.strokeLine(x + 12, y - 4, x + 18, y + 4);
            gc.strokeLine(x + 18, y - 4, x + 12, y + 4);
        } else {
            gc.strokeArc(x + 6, y - 5, 7, 10, -50, 100, javafx.scene.shape.ArcType.OPEN);
            gc.strokeArc(x + 6, y - 8, 12, 16, -50, 100, javafx.scene.shape.ArcType.OPEN);
        }
    }

    private void drawPauseButton() {
        gc.setFill(paused ? Color.rgb(160, 120, 40) : Color.rgb(55, 60, 85));
        gc.fillRoundRect(PAUSE_X, MUTE_Y, MUTE_W, MUTE_H, 6, 6);
        gc.setFill(Color.WHITE);
        gc.fillRect(PAUSE_X + 10, MUTE_Y + 4, 3.5, MUTE_H - 8);
        gc.fillRect(PAUSE_X + 17, MUTE_Y + 4, 3.5, MUTE_H - 8);
    }

    private void drawBanner() {
        double alpha = Math.min(1, bannerTimer / 0.4);
        gc.setGlobalAlpha(alpha);
        gc.setFill(Color.rgb(0, 0, 0, 0.7));
        gc.fillRect(0, 300, canvas.getWidth(), 110);
        double cx = canvas.getWidth() / 2;
        PixelFont.draw(gc, bannerTitle, cx, 322, 5, Color.rgb(255, 205, 40), Color.rgb(150, 70, 20), Align.CENTER);
        px(bannerSub, cx, 374, 2, Color.WHITE, Align.CENTER);
        gc.setGlobalAlpha(1);
    }

    // ------------------------------------------------------------------ input & state

    /** @param event the key event: wave start and sound keys, then each player's keys */
    public void onKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();
        if (code == KeyCode.P || code == KeyCode.ESCAPE) {
            setPaused(!paused);
            event.consume();
            return;
        }
        if (paused) {
            if (code == KeyCode.N) {
                SoundManager.toggleMuted();
            }
            event.consume();
            return;
        }
        if (code == KeyCode.ENTER || code == KeyCode.TAB) {
            startRequested = true;
            event.consume();
            return;
        }
        if (code == KeyCode.N || (!coop && code == KeyCode.M)) {
            SoundManager.toggleMuted();
            return;
        }
        for (Avatar a : avatars) {
            a.onKeyPressed(event);
        }
        if (code == KeyCode.SPACE || code == KeyCode.TAB || code.isArrowKey()) {
            event.consume();
        }
    }

    /** @param event the key event, forwarded to the players */
    public void onKeyReleased(KeyEvent event) {
        for (Avatar a : avatars) {
            a.onKeyReleased(event);
        }
    }

    /**
     * Handles a click on the canvas (the sound button in the HUD).
     *
     * @param x click x
     * @param y click y
     */
    public void onMouseClicked(double x, double y) {
        if (paused) {
            if (inside(RESUME_BTN, x, y)) {
                setPaused(false);
            } else if (inside(MENU_BTN, x, y)) {
                SoundManager.play(Sfx.CLICK);
                quitRequested = true;
            }
        }
        if (x >= PAUSE_X && x <= PAUSE_X + MUTE_W && y >= MUTE_Y && y <= MUTE_Y + MUTE_H) {
            setPaused(!paused);
            return;
        }
        if (x >= MUTE_X && x <= MUTE_X + MUTE_W && y >= MUTE_Y && y <= MUTE_Y + MUTE_H) {
            SoundManager.toggleMuted();
        }
    }

    /** @return true while the game is paused */
    public boolean isPaused() {
        return paused;
    }

    /** @return true if the players chose "Portada" in the pause menu */
    public boolean isQuitRequested() {
        return quitRequested;
    }

    /** @return true once the victory has been shown long enough */
    public boolean isWon() {
        return phase == Phase.WON && endTimer >= END_DELAY;
    }

    /** @return true once the defeat has been shown long enough */
    public boolean isLost() {
        return phase == Phase.LOST && endTimer >= END_DELAY;
    }

    public Board getBoard() {
        return board;
    }

    public List<Enemy> getEnemies() {
        return enemies;
    }

    public List<Avatar> getAvatars() {
        return avatars;
    }
}
