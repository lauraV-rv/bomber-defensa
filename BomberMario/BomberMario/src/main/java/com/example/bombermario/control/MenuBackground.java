package com.example.bombermario.control;

import com.example.bombermario.model.Art;
import com.example.bombermario.model.EnemyType;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Animated backdrop for the title, story, controls and end screens: floating energy motes, the
 * Citadel's silhouette, and (on the title screen) automatons marching from the Rift
 * toward the Core while the engineer stands guard.
 */
public class MenuBackground {
    /** Which screen the backdrop is for. */
    public enum Mode {
        TITLE, STORY, WIN, LOSE
    }

    private static final double GROUND_Y = 580;

    private final Mode mode;
    private final boolean march;
    private final List<double[]> fireworks = new ArrayList<>();
    private double fireworkTimer;
    private final Random random = new Random();
    private final List<double[]> motes = new ArrayList<>();
    private final List<double[]> marchers = new ArrayList<>();
    private final List<double[]> pops = new ArrayList<>();
    private double time;
    private double spawnTimer = 0.5;

    /**
     * @param mode the screen it decorates
     */
    public MenuBackground(Mode mode) {
        this.mode = mode;
        this.march = mode == Mode.TITLE;
        for (int i = 0; i < 46; i++) {
            motes.add(newMote(random.nextDouble() * 640));
        }
    }

    // {x, y, speed, sway phase, size, color index}
    private double[] newMote(double y) {
        return new double[]{random.nextDouble() * 840, y, 10 + random.nextDouble() * 25,
                random.nextDouble() * 6.28, 1.5 + random.nextDouble() * 2.5, random.nextInt(3)};
    }

    /**
     * Advances and draws one frame.
     *
     * @param gc the graphics context
     * @param w  width
     * @param h  height
     * @param dt elapsed seconds
     */
    public void paint(GraphicsContext gc, double w, double h, double dt) {
        time += dt;
        gc.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(20, 22, 36)), new Stop(0.6, Color.rgb(40, 30, 62)), new Stop(1, Color.rgb(58, 34, 70))));
        gc.fillRect(0, 0, w, h);

        // Rift glow on the left, core glow on the right
        double pulse = 0.5 + 0.5 * Math.sin(time * 1.5);
        gc.setFill(Color.rgb(160, 60, 255, 0.08 + 0.05 * pulse));
        gc.fillOval(-180, 200, 420, 460);
        gc.setFill(Color.rgb(80, 200, 255, 0.07 + 0.05 * (1 - pulse)));
        gc.fillOval(600, 220, 420, 440);

        paintCitadel(gc);
        paintMotes(gc, w, h, dt);

        // Ground strip
        for (int c = 0; c < (int) (w / 40) + 1; c++) {
            for (int r = 0; r < 2; r++) {
                Art.ground(gc, c, r, c * 40, GROUND_Y + r * 40, 40);
            }
        }
        gc.setFill(Color.rgb(0, 0, 0, 0.25));
        gc.fillRect(0, GROUND_Y, w, 6);

        // Portal and core on stone pedestals
        Art.stone(gc, 0, 0, 20, GROUND_Y - 20, 40);
        Art.portal(gc, 40, GROUND_Y - 30, 1.5, time, true);
        Art.stone(gc, 1, 0, w - 70, GROUND_Y - 20, 40);
        Art.coreCrystal(gc, w - 50, GROUND_Y - 44, 1.7, time, mode == Mode.LOSE ? 0 : 1);

        if (march) {
            paintMarch(gc, w, dt);
        }
        if (mode == Mode.WIN) {
            paintFireworks(gc, w, dt);
        }
        if (mode == Mode.LOSE) {
            for (int i = 0; i < 4; i++) {
                EnemyType t = EnemyType.values()[i % 3];
                Art.invader(gc, w - 230 + i * 38, GROUND_Y - 4, t.getSize(), t.getColor(), 1, time + i, i % 2 == 0, false);
            }
            gc.setFill(Color.rgb(120, 0, 20, 0.18 + 0.06 * Math.sin(time * 3)));
            gc.fillRect(0, 0, w, h);
        }
    }

    // Bursts of colored sparks over the citadel for the victory screen
    private void paintFireworks(GraphicsContext gc, double w, double dt) {
        fireworkTimer -= dt;
        if (fireworkTimer <= 0) {
            fireworkTimer = 0.5 + random.nextDouble() * 0.6;
            fireworks.add(new double[]{80 + random.nextDouble() * (w - 160), 70 + random.nextDouble() * 220, 0,
                    random.nextInt(4)});
        }
        Color[] colors = {Color.rgb(255, 205, 40), Color.rgb(120, 220, 255), Color.rgb(240, 120, 200), Color.rgb(140, 240, 140)};
        Iterator<double[]> it = fireworks.iterator();
        while (it.hasNext()) {
            double[] f = it.next();
            f[2] += dt;
            if (f[2] > 1.3) {
                it.remove();
                continue;
            }
            double k = f[2] / 1.3;
            Color c = colors[(int) f[3]];
            gc.setFill(Color.color(c.getRed(), c.getGreen(), c.getBlue(), 1 - k));
            for (int i = 0; i < 14; i++) {
                double a = i * Math.PI * 2 / 14;
                double r = 70 * Math.sqrt(k);
                double x = f[0] + Math.cos(a) * r;
                double y = f[1] + Math.sin(a) * r + 30 * k * k;
                gc.fillRect(x - 2, y - 2, 4, 4);
            }
        }
    }

    private void paintCitadel(GraphicsContext gc) {
        gc.setFill(Color.rgb(30, 26, 48));
        double base = GROUND_Y;
        double[][] towers = {{560, 150, 50}, {620, 210, 40}, {670, 120, 60}, {740, 190, 46}, {790, 140, 50}};
        for (double[] t : towers) {
            gc.fillRect(t[0], base - t[1], t[2], t[1]);
            for (int i = 0; i < t[2] / 12; i++) {
                gc.fillRect(t[0] + i * 12, base - t[1] - 8, 7, 8);
            }
        }
        gc.fillRect(540, base - 80, 300, 80);
        // Lit windows
        gc.setFill(Color.rgb(255, 210, 120, 0.55 + 0.2 * Math.sin(time * 2)));
        gc.fillRect(690, base - 95, 6, 10);
        gc.fillRect(633, base - 170, 5, 9);
        gc.fillRect(805, base - 110, 6, 10);
        gc.fillRect(578, base - 120, 5, 9);
    }

    private void paintMotes(GraphicsContext gc, double w, double h, double dt) {
        Color[] colors = {Color.rgb(120, 220, 255), Color.rgb(210, 150, 255), Color.rgb(255, 215, 110)};
        for (double[] m : motes) {
            m[1] -= m[2] * dt;
            if (m[1] < -10) {
                double[] fresh = newMote(h + 10);
                System.arraycopy(fresh, 0, m, 0, m.length);
            }
            double x = m[0] + Math.sin(time * 0.8 + m[3]) * 14;
            double twinkle = 0.4 + 0.6 * Math.abs(Math.sin(time * 1.7 + m[3]));
            Color c = colors[(int) m[5]];
            gc.setFill(Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0.25 * twinkle));
            gc.fillOval(x - m[4] * 2, m[1] - m[4] * 2, m[4] * 4, m[4] * 4);
            gc.setFill(Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0.8 * twinkle));
            gc.fillOval(x - m[4] / 2, m[1] - m[4] / 2, m[4], m[4]);
        }
    }

    // {x, type ordinal, anim}
    private void paintMarch(GraphicsContext gc, double w, double dt) {
        spawnTimer -= dt;
        if (spawnTimer <= 0) {
            spawnTimer = 1.6 + random.nextDouble() * 1.8;
            marchers.add(new double[]{70, random.nextInt(EnemyType.values().length), random.nextDouble()});
        }
        double guardX = w - 150;
        Iterator<double[]> it = marchers.iterator();
        while (it.hasNext()) {
            double[] m = it.next();
            EnemyType type = EnemyType.values()[(int) m[1]];
            m[0] += type.getSpeed() * 0.9 * dt;
            m[2] += dt;
            if (m[0] > guardX - 40) {
                pops.add(new double[]{m[0], 0});
                it.remove();
                continue;
            }
            Art.invader(gc, m[0], GROUND_Y - 4, type.getSize(), type.getColor(), 1, m[2], true, false);
        }
        // Little blasts where the engineer stops them
        Iterator<double[]> pi = pops.iterator();
        while (pi.hasNext()) {
            double[] p = pi.next();
            p[1] += dt;
            if (p[1] > 0.5) {
                pi.remove();
                continue;
            }
            double k = p[1] / 0.5;
            gc.setFill(Color.rgb(255, 180, 40, 0.8 * (1 - k)));
            gc.fillOval(p[0] - 30 * k, GROUND_Y - 10 - 30 * k, 60 * k, 60 * k);
            gc.setFill(Color.rgb(255, 245, 200, 0.9 * (1 - k)));
            gc.fillOval(p[0] - 14 * k, GROUND_Y - 10 - 14 * k, 28 * k, 28 * k);
        }
        Art.builder(gc, guardX, GROUND_Y - 4, 1.3, -1, time, false, false);
    }
}
