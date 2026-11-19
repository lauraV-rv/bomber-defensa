package com.example.bombermario.model;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Small, short-lived particles for juice: fire and smoke from explosions, debris from
 * broken blocks, goo from defeated automatons, sparks from the core, dust from steps.
 */
public class Particles {
    private static final int MAX = 900;

    private static final class P {
        double x;
        double y;
        double vx;
        double vy;
        double life;
        double maxLife;
        double size;
        double grow;
        double gravity;
        double drag;
        Color color;
        boolean square;
        double spin;
    }

    private final List<P> list = new ArrayList<>();
    private final Random random = new Random();

    private P add(double x, double y, double angle, double speed, double life, double size, Color color) {
        P p = new P();
        p.x = x;
        p.y = y;
        p.vx = Math.cos(angle) * speed;
        p.vy = Math.sin(angle) * speed;
        p.life = life;
        p.maxLife = life;
        p.size = size;
        p.color = color;
        p.drag = 2;
        if (list.size() < MAX) {
            list.add(p);
        }
        return p;
    }

    private double rnd(double a, double b) {
        return a + random.nextDouble() * (b - a);
    }

    /**
     * Fire, embers and smoke over every flame tile of a bomb.
     *
     * @param flames the flame tiles {col, row, kind}
     */
    public void explosion(List<int[]> flames) {
        for (int[] f : flames) {
            double cx = Board.centerX(f[0]);
            double cy = Board.centerY(f[1]);
            boolean center = f[2] == 0;
            int n = center ? 22 : 9;
            for (int i = 0; i < n; i++) {
                Color c = random.nextBoolean() ? Color.rgb(255, 200, 60) : Color.rgb(255, 110, 20);
                P p = add(cx + rnd(-10, 10), cy + rnd(-10, 10), rnd(0, Math.PI * 2), rnd(30, center ? 160 : 90),
                        rnd(0.25, 0.6), rnd(3, 6), c);
                p.gravity = -60;
                p.grow = -4;
            }
            for (int i = 0; i < (center ? 6 : 2); i++) {
                P p = add(cx + rnd(-8, 8), cy + rnd(-8, 8), -Math.PI / 2 + rnd(-0.5, 0.5), rnd(15, 40),
                        rnd(0.6, 1.1), rnd(6, 10), Color.rgb(90, 90, 100));
                p.grow = 14;
                p.drag = 1;
            }
            if (center) {
                for (int i = 0; i < 10; i++) {
                    P p = add(cx, cy, rnd(0, Math.PI * 2), rnd(150, 260), rnd(0.3, 0.6), 2.2, Color.rgb(255, 245, 200));
                    p.gravity = 200;
                }
            }
        }
    }

    /**
     * Chunks flying out of a broken block.
     *
     * @param c     column
     * @param r     row
     * @param built true for a player wall (wood), false for rubble (stone)
     */
    public void debris(int c, int r, boolean built) {
        double cx = Board.centerX(c);
        double cy = Board.centerY(r);
        for (int i = 0; i < 14; i++) {
            Color col = built
                    ? (random.nextBoolean() ? Color.rgb(196, 140, 72) : Color.rgb(120, 78, 32))
                    : (random.nextBoolean() ? Color.rgb(145, 118, 90) : Color.rgb(100, 80, 60));
            P p = add(cx + rnd(-12, 12), cy + rnd(-12, 12), -Math.PI / 2 + rnd(-1.4, 1.4), rnd(60, 170),
                    rnd(0.5, 0.9), rnd(3, 6), col);
            p.gravity = 420;
            p.drag = 0.5;
            p.square = true;
            p.spin = rnd(-10, 10);
        }
        if (built) {
            for (int i = 0; i < 4; i++) {
                P p = add(cx, cy, rnd(0, Math.PI * 2), rnd(40, 90), 0.5, 3, Color.rgb(70, 160, 255));
                p.square = true;
                p.gravity = 300;
            }
        }
    }

    /**
     * Goo splash when an automaton is defeated.
     *
     * @param x     center x
     * @param y     center y
     * @param color body color
     */
    public void splat(double x, double y, Color color) {
        for (int i = 0; i < 12; i++) {
            P p = add(x, y, rnd(0, Math.PI * 2), rnd(50, 140), rnd(0.35, 0.7), rnd(3, 5.5), color);
            p.gravity = 300;
            p.grow = -3;
        }
        for (int i = 0; i < 5; i++) {
            P p = add(x, y, rnd(0, Math.PI * 2), rnd(20, 60), 0.5, 2.5, Color.rgb(255, 230, 120));
            p.gravity = -30;
        }
    }

    /**
     * Sparks bursting from a point (core hit, wall built, pickups).
     *
     * @param x     center x
     * @param y     center y
     * @param color spark color
     * @param count how many
     */
    public void sparks(double x, double y, Color color, int count) {
        for (int i = 0; i < count; i++) {
            P p = add(x, y, rnd(0, Math.PI * 2), rnd(60, 180), rnd(0.3, 0.7), rnd(2, 3.5), color);
            p.gravity = 120;
        }
    }

    /**
     * A small puff of dust (steps, a wall appearing).
     *
     * @param x     center x
     * @param y     ground y
     * @param count how many puffs
     */
    public void dust(double x, double y, int count) {
        for (int i = 0; i < count; i++) {
            P p = add(x + rnd(-6, 6), y, -Math.PI / 2 + rnd(-1.2, 1.2), rnd(10, 35), rnd(0.3, 0.55), rnd(2.5, 4),
                    Color.rgb(200, 195, 170));
            p.grow = 6;
        }
    }

    /**
     * Advances every particle.
     *
     * @param dt elapsed seconds
     */
    public void update(double dt) {
        Iterator<P> it = list.iterator();
        while (it.hasNext()) {
            P p = it.next();
            p.life -= dt;
            if (p.life <= 0) {
                it.remove();
                continue;
            }
            double k = Math.max(0, 1 - p.drag * dt);
            p.vx *= k;
            p.vy = p.vy * k + p.gravity * dt;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.size = Math.max(0.5, p.size + p.grow * dt);
        }
    }

    /**
     * Draws every particle, fading out with age.
     *
     * @param gc the graphics context
     */
    public void paint(GraphicsContext gc) {
        for (P p : list) {
            double a = Math.min(1, p.life / p.maxLife * 1.5);
            gc.setGlobalAlpha(a);
            gc.setFill(p.color);
            if (p.square) {
                gc.save();
                gc.translate(p.x, p.y);
                gc.rotate(Math.toDegrees(p.spin * (p.maxLife - p.life)));
                gc.fillRect(-p.size / 2, -p.size / 2, p.size, p.size);
                gc.restore();
            } else {
                gc.fillOval(p.x - p.size / 2, p.y - p.size / 2, p.size, p.size);
            }
        }
        gc.setGlobalAlpha(1);
    }

    /** @return how many particles are alive */
    public int size() {
        return list.size();
    }
}
