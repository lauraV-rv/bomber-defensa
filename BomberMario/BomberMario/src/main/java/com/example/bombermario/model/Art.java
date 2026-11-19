package com.example.bombermario.model;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;

/**
 * All the game's art, drawn with code (no image files). The same functions draw the
 * board, the menu background and the story screen, so everything shares one look.
 */
public final class Art {
    private Art() {
    }

    /** Engineer outfits: the classic amber one, and the blue one for player 1 in co-op. */
    public enum Outfit {
        AMBER(Color.rgb(245, 130, 30), Color.rgb(220, 110, 25), Color.rgb(245, 140, 40),
                Color.rgb(255, 205, 40), Color.rgb(230, 170, 20), Color.rgb(255, 245, 190)),
        BLUE(Color.rgb(50, 130, 230), Color.rgb(35, 100, 200), Color.rgb(60, 145, 240),
                Color.rgb(235, 240, 250), Color.rgb(170, 190, 220), Color.rgb(255, 255, 255));

        final Color vest;
        final Color backArm;
        final Color frontArm;
        final Color hat;
        final Color hatDark;
        final Color hatShine;

        Outfit(Color vest, Color backArm, Color frontArm, Color hat, Color hatDark, Color hatShine) {
            this.vest = vest;
            this.backArm = backArm;
            this.frontArm = frontArm;
            this.hat = hat;
            this.hatDark = hatDark;
            this.hatShine = hatShine;
        }

        /** @return the main color, used for labels */
        public Color getVest() {
            return vest;
        }
    }

    /**
     * A stable pseudo-random number in [0, 1) for a tile, so decorations don't flicker.
     *
     * @param c column
     * @param r row
     * @param k which number for that tile
     * @return the number
     */
    public static double hash(int c, int r, int k) {
        double v = Math.sin(c * 12.9898 + r * 78.233 + k * 37.719) * 43758.5453;
        return v - Math.floor(v);
    }

    // ------------------------------------------------------------------ terrain

    /**
     * Grass ground tile with tufts and pebbles.
     *
     * @param gc the graphics context
     * @param c  column (for the pattern)
     * @param r  row (for the pattern)
     * @param x  left
     * @param y  top
     * @param s  size
     */
    public static void ground(GraphicsContext gc, int c, int r, double x, double y, double s) {
        gc.setFill((c + r) % 2 == 0 ? Color.rgb(74, 94, 70) : Color.rgb(68, 87, 64));
        gc.fillRect(x, y, s, s);
        if (hash(c, r, 1) < 0.55) {
            gc.setStroke(Color.rgb(98, 128, 86));
            gc.setLineWidth(1.4);
            double tx = x + 6 + hash(c, r, 2) * (s - 12);
            double ty = y + 10 + hash(c, r, 3) * (s - 16);
            gc.strokeLine(tx, ty, tx - 2, ty - 5);
            gc.strokeLine(tx + 2, ty, tx + 2, ty - 6);
            gc.strokeLine(tx + 4, ty, tx + 6, ty - 4);
        }
        if (hash(c, r, 4) < 0.35) {
            gc.setFill(Color.rgb(110, 116, 100));
            double px = x + 4 + hash(c, r, 5) * (s - 8);
            double py = y + 4 + hash(c, r, 6) * (s - 8);
            gc.fillOval(px, py, 4, 3);
        }
    }

    /**
     * Stone wall / pillar with bevel, cracks and a bit of moss.
     *
     * @param gc the graphics context
     * @param c  column (for the pattern)
     * @param r  row (for the pattern)
     * @param x  left
     * @param y  top
     * @param s  size
     */
    public static void stone(GraphicsContext gc, int c, int r, double x, double y, double s) {
        gc.setFill(Color.rgb(96, 100, 112));
        gc.fillRect(x, y, s, s);
        gc.setFill(Color.rgb(132, 137, 150));
        gc.fillRect(x, y, s, 4);
        gc.fillRect(x, y, 4, s);
        gc.setFill(Color.rgb(60, 63, 73));
        gc.fillRect(x, y + s - 4, s, 4);
        gc.fillRect(x + s - 4, y, 4, s);
        gc.setStroke(Color.rgb(76, 80, 90));
        gc.setLineWidth(1.5);
        gc.strokeLine(x + 4, y + s / 2, x + s - 4, y + s / 2);
        double split = (c + r) % 2 == 0 ? s / 2 : s / 3;
        gc.strokeLine(x + split, y + 4, x + split, y + s / 2);
        gc.strokeLine(x + s - split, y + s / 2, x + s - split, y + s - 4);
        if (hash(c, r, 7) < 0.3) {
            gc.setStroke(Color.rgb(70, 72, 80));
            gc.setLineWidth(1);
            double cx = x + 8 + hash(c, r, 8) * (s - 16);
            gc.strokeLine(cx, y + 6, cx + 4, y + 12);
            gc.strokeLine(cx + 4, y + 12, cx + 2, y + 17);
        }
        if (hash(c, r, 9) < 0.35) {
            gc.setFill(Color.rgb(90, 130, 80, 0.8));
            double mx = x + 4 + hash(c, r, 10) * (s - 14);
            gc.fillOval(mx, y + s - 9, 10, 5);
            gc.fillOval(mx + 5, y + s - 11, 6, 4);
        }
    }

    /**
     * A pile of rubble: shaded rocks with cracks and a shadow.
     *
     * @param gc the graphics context
     * @param c  column (for the pattern)
     * @param r  row (for the pattern)
     * @param x  left
     * @param y  top
     * @param s  size
     */
    public static void rubble(GraphicsContext gc, int c, int r, double x, double y, double s) {
        gc.setFill(Color.rgb(0, 0, 0, 0.3));
        gc.fillOval(x + 3, y + s - 11, s - 4, 10);
        double j = hash(c, r, 11) * 4 - 2;
        rock(gc, x + 3 + j, y + 13, 22, 23, Color.rgb(122, 98, 74));
        rock(gc, x + 15 - j, y + 5, 22, 26, Color.rgb(145, 118, 90));
        rock(gc, x + 8, y + 20, 25, 17, Color.rgb(110, 88, 66));
        gc.setStroke(Color.rgb(70, 54, 40));
        gc.setLineWidth(1.2);
        gc.strokeLine(x + 21, y + 11, x + 25, y + 18);
        gc.strokeLine(x + 25, y + 18, x + 23, y + 24);
        gc.setFill(Color.rgb(160, 134, 104));
        gc.fillOval(x + 2, y + s - 8, 5, 4);
        gc.fillOval(x + s - 8, y + s - 7, 4, 3);
    }

    private static void rock(GraphicsContext gc, double x, double y, double w, double h, Color base) {
        gc.setFill(base.darker());
        gc.fillOval(x, y + 2, w, h - 2);
        gc.setFill(base);
        gc.fillOval(x, y, w - 2, h - 3);
        gc.setFill(base.brighter());
        gc.fillOval(x + w * 0.2, y + h * 0.15, w * 0.35, h * 0.22);
    }

    /**
     * The player's wooden wall: planks, cross brace, nails, blue corner tabs and a shadow.
     *
     * @param gc the graphics context
     * @param x  left
     * @param y  top
     * @param s  size
     */
    public static void crate(GraphicsContext gc, double x, double y, double s) {
        gc.setFill(Color.rgb(0, 0, 0, 0.3));
        gc.fillRect(x + 5, y + s - 3, s - 5, 4);
        gc.fillRect(x + s - 2, y + 5, 3, s - 6);
        gc.setFill(Color.rgb(196, 140, 72));
        gc.fillRect(x + 2, y + 2, s - 4, s - 4);
        // Planks
        gc.setStroke(Color.rgb(160, 108, 50));
        gc.setLineWidth(1);
        for (int i = 1; i < 4; i++) {
            double py = y + 2 + i * (s - 4) / 4;
            gc.strokeLine(x + 3, py, x + s - 3, py);
        }
        gc.setFill(Color.rgb(222, 170, 100));
        gc.fillRect(x + 2, y + 2, s - 4, 3);
        gc.setStroke(Color.rgb(120, 78, 32));
        gc.setLineWidth(3);
        gc.strokeRect(x + 3.5, y + 3.5, s - 7, s - 7);
        gc.strokeLine(x + 5, y + 5, x + s - 5, y + s - 5);
        gc.setStroke(Color.rgb(140, 92, 40));
        gc.setLineWidth(2);
        gc.strokeLine(x + s - 5, y + 5, x + 5, y + s - 5);
        // Nails
        gc.setFill(Color.rgb(70, 60, 50));
        gc.fillOval(x + 6, y + s / 2 - 1, 2.5, 2.5);
        gc.fillOval(x + s - 9, y + s / 2 - 1, 2.5, 2.5);
        // Blue corner tabs mark it as the player's wall
        gc.setFill(Color.rgb(70, 160, 255));
        gc.fillRect(x + 1, y + 1, 7, 7);
        gc.fillRect(x + s - 8, y + 1, 7, 7);
        gc.fillRect(x + 1, y + s - 8, 7, 7);
        gc.fillRect(x + s - 8, y + s - 8, 7, 7);
        gc.setFill(Color.rgb(160, 210, 255));
        gc.fillRect(x + 2, y + 2, 3, 2);
        gc.fillRect(x + s - 7, y + 2, 3, 2);
    }

    // ------------------------------------------------------------------ core & portal

    /**
     * The color of the core's light for a given health.
     *
     * @param health 0 to 1
     * @return the color
     */
    public static Color coreColor(double health) {
        health = Math.max(0, Math.min(1, health));
        return Color.rgb((int) (60 + 195 * (1 - health)), (int) (200 * health + 40), (int) (255 * health + 60 * (1 - health)));
    }

    /**
     * The energy crystal with its pulsing aura, expanding rings and orbiting sparks.
     *
     * @param gc     the graphics context
     * @param cx     center x
     * @param cy     center y
     * @param scale  1 = board size
     * @param time   seconds, for the animation
     * @param health 0 to 1, changes color and pulse speed
     */
    public static void coreCrystal(GraphicsContext gc, double cx, double cy, double scale, double time, double health) {
        health = Math.max(0, Math.min(1, health));
        Color glow = coreColor(health);
        double speed = 3 + 6 * (1 - health);
        double pulse = 0.5 + 0.5 * Math.sin(time * speed);
        // Expanding rings
        for (int i = 0; i < 2; i++) {
            double p = ((time * 0.7 + i * 0.5) % 1);
            double rad = (14 + 30 * p) * scale;
            gc.setStroke(Color.color(glow.getRed(), glow.getGreen(), glow.getBlue(), 0.45 * (1 - p)));
            gc.setLineWidth(2 * scale);
            gc.strokeOval(cx - rad, cy - rad, rad * 2, rad * 2);
        }
        double aura = (22 + 4 * pulse) * scale;
        gc.setFill(Color.color(glow.getRed(), glow.getGreen(), glow.getBlue(), 0.22 + 0.25 * pulse));
        gc.fillOval(cx - aura, cy - aura, aura * 2, aura * 2);
        double h = (15 + 2 * pulse) * scale;
        double w = 10 * scale;
        gc.setFill(glow.darker());
        gc.fillPolygon(new double[]{cx, cx + w, cx, cx - w}, new double[]{cy - h, cy, cy + h, cy}, 4);
        gc.setFill(glow);
        gc.fillPolygon(new double[]{cx, cx + w * 0.75, cx, cx - w}, new double[]{cy - h, cy - 1, cy + h * 0.8, cy}, 4);
        double shimmer = (time * 1.5) % 1;
        gc.setFill(Color.rgb(255, 255, 255, 0.8));
        gc.fillPolygon(new double[]{cx, cx + 4 * scale, cx, cx - 4 * scale},
                new double[]{cy - h + 4 * scale, cy - 3 * scale, cy + 2 * scale, cy - 3 * scale}, 4);
        gc.setFill(Color.rgb(255, 255, 255, 0.5 * (1 - shimmer)));
        gc.fillOval(cx - 3 * scale, cy - h + shimmer * h * 1.6, 6 * scale, 3 * scale);
        // Orbiting sparks
        for (int i = 0; i < 3; i++) {
            double a = time * 2.2 + i * Math.PI * 2 / 3;
            double sx = cx + Math.cos(a) * 19 * scale;
            double sy = cy + Math.sin(a) * 8 * scale;
            double sr = 2.2 * scale;
            gc.setFill(Color.rgb(255, 255, 255, 0.9));
            gc.fillOval(sx - sr, sy - sr, sr * 2, sr * 2);
        }
    }

    /**
     * A swirling portal (or a sealed one).
     *
     * @param gc    the graphics context
     * @param cx    center x
     * @param cy    center y
     * @param scale 1 = board size
     * @param time  seconds, for the animation
     * @param open  whether it is open
     */
    public static void portal(GraphicsContext gc, double cx, double cy, double scale, double time, boolean open) {
        if (!open) {
            double flicker = 0.5 + 0.5 * Math.sin(time * 2.3);
            gc.setFill(Color.rgb(40, 30, 50));
            gc.fillOval(cx - 13 * scale, cy - 16 * scale, 26 * scale, 32 * scale);
            gc.setFill(Color.rgb(150, 60, 220, 0.12 + 0.12 * flicker));
            gc.fillOval(cx - 9 * scale, cy - 12 * scale, 18 * scale, 24 * scale);
            gc.setStroke(Color.rgb(110, 100, 120));
            gc.setLineWidth(2.5 * scale);
            gc.strokeLine(cx - 10 * scale, cy - 10 * scale, cx + 10 * scale, cy + 10 * scale);
            gc.strokeLine(cx + 10 * scale, cy - 10 * scale, cx - 10 * scale, cy + 10 * scale);
            return;
        }
        double pulse = 0.5 + 0.5 * Math.sin(time * 5);
        double aura = (22 + 5 * pulse) * scale;
        gc.setFill(Color.rgb(170, 60, 255, 0.18 + 0.2 * pulse));
        gc.fillOval(cx - aura, cy - aura * 1.1, aura * 2, aura * 2.2);
        for (int i = 0; i < 3; i++) {
            double k = 1 - i * 0.3;
            gc.setFill(i % 2 == 0 ? Color.rgb(110, 35, 190) : Color.rgb(200, 110, 255));
            gc.fillOval(cx - 14 * k * scale, cy - 17 * k * scale, 28 * k * scale, 34 * k * scale);
        }
        gc.setStroke(Color.rgb(255, 220, 255, 0.85));
        gc.setLineWidth(2 * scale);
        for (int i = 0; i < 2; i++) {
            double a = Math.toDegrees(time * 4) + i * 180;
            gc.strokeArc(cx - 10 * scale, cy - 12 * scale, 20 * scale, 24 * scale, a % 360, 100, ArcType.OPEN);
        }
        // Motes drawn into the vortex
        for (int i = 0; i < 4; i++) {
            double p = (time * 0.8 + i * 0.25) % 1;
            double a = i * 1.7 + time;
            double d = (24 - 20 * p) * scale;
            gc.setFill(Color.rgb(230, 180, 255, p));
            gc.fillOval(cx + Math.cos(a) * d - 1.5 * scale, cy + Math.sin(a) * d * 1.2 - 1.5 * scale, 3 * scale, 3 * scale);
        }
    }

    // ------------------------------------------------------------------ characters

    /**
     * The Master Engineer: hard hat, orange vest, swinging arms, blinking eyes.
     * (x, y) is the center of the body; the feet are at y + 16 * scale.
     *
     * @param gc      the graphics context
     * @param x       center x
     * @param y       center y
     * @param scale   1 = board size
     * @param look    -1 facing left, 1 facing right
     * @param anim    seconds, for the animation
     * @param running whether walking
     * @param down    whether knocked out
     */
    public static void builder(GraphicsContext gc, double x, double y, double scale, double look, double anim,
                               boolean running, boolean down) {
        builder(gc, x, y, scale, look, anim, running, down, Outfit.AMBER);
    }

    /**
     * The engineer in a given outfit. See {@link #builder(GraphicsContext, double, double, double, double, double, boolean, boolean)}.
     *
     * @param gc      the graphics context
     * @param x       center x
     * @param y       center y
     * @param scale   1 = board size
     * @param look    -1 facing left, 1 facing right
     * @param anim    seconds, for the animation
     * @param running whether walking
     * @param down    whether knocked out
     * @param o       the outfit
     */
    public static void builder(GraphicsContext gc, double x, double y, double scale, double look, double anim,
                               boolean running, boolean down, Outfit o) {
        gc.save();
        gc.translate(x, y);
        gc.scale(scale, scale);
        gc.setFill(Color.rgb(0, 0, 0, 0.25));
        gc.fillOval(-11, 11, 22, 7);
        if (down) {
            gc.translate(0, 8);
            gc.rotate(look * 80);
            gc.translate(0, -8);
        }
        double bob = running ? Math.abs(Math.sin(anim * 14)) * 2 : Math.sin(anim * 3) * 0.8;
        double top = -24 - bob;
        double stride = running ? Math.sin(anim * 14) * 3 : 0;
        // Legs and boots
        gc.setFill(Color.rgb(50, 70, 120));
        gc.fillRoundRect(-8 + stride, top + 30, 6, 9, 3, 3);
        gc.fillRoundRect(2 - stride, top + 30, 6, 9, 3, 3);
        gc.setFill(Color.rgb(70, 45, 30));
        gc.fillRoundRect(-9 + stride, top + 37, 8, 4, 2, 2);
        gc.fillRoundRect(1 - stride, top + 37, 8, 4, 2, 2);
        // Back arm
        gc.setFill(o.backArm);
        gc.fillRoundRect(-look * 11 - 2.5 - stride * 0.6 * look, top + 18, 5, 11, 4, 4);
        // Body: orange vest with reflective stripe and tool belt
        gc.setFill(o.vest);
        gc.fillRoundRect(-10, top + 16, 20, 17, 8, 8);
        gc.setFill(Color.rgb(250, 240, 120));
        gc.fillRect(-10, top + 23, 20, 3);
        gc.setFill(Color.rgb(90, 60, 35));
        gc.fillRect(-10, top + 29, 20, 3);
        gc.setFill(Color.rgb(200, 200, 210));
        gc.fillRect(look * 4 - 1, top + 28, 3, 6);
        // Front arm with glove
        gc.setFill(o.frontArm);
        gc.fillRoundRect(look * 9 - 2.5 + stride * 0.6 * look, top + 18, 5, 11, 4, 4);
        gc.setFill(Color.rgb(240, 200, 80));
        gc.fillOval(look * 9 - 3 + stride * 0.6 * look, top + 27, 6, 5);
        // Head
        gc.setFill(Color.rgb(240, 200, 160));
        gc.fillOval(-9, top + 3, 18, 16);
        gc.setFill(Color.rgb(230, 150, 130, 0.6));
        gc.fillOval(look * 5 - 2, top + 13, 4, 3);
        // Eyes (blink every few seconds)
        gc.setFill(Color.rgb(30, 30, 40));
        gc.setStroke(Color.rgb(30, 30, 40));
        if (down) {
            gc.setLineWidth(1.5);
            for (int i = -1; i <= 1; i += 2) {
                double ex = look * 2 + i * 4;
                gc.strokeLine(ex - 2, top + 9, ex + 2, top + 13);
                gc.strokeLine(ex + 2, top + 9, ex - 2, top + 13);
            }
        } else if ((anim % 3.1) < 0.13) {
            gc.setLineWidth(1.5);
            gc.strokeLine(look * 2 - 6, top + 11, look * 2 - 2, top + 11);
            gc.strokeLine(look * 2 + 1, top + 11, look * 2 + 5, top + 11);
        } else {
            gc.fillOval(look * 2 - 5, top + 9, 3, 4);
            gc.fillOval(look * 2 + 2, top + 9, 3, 4);
        }
        // Yellow hard hat with highlight
        gc.setFill(o.hat);
        gc.fillArc(-11, top - 2, 22, 16, 0, 180, ArcType.ROUND);
        gc.fillRoundRect(-13 + look * 2, top + 5, 26, 4, 3, 3);
        gc.setFill(o.hatDark);
        gc.fillRect(-1.5, top - 1, 3, 8);
        gc.setFill(o.hatShine);
        gc.fillOval(-7, top + 1, 5, 3);
        gc.restore();
        if (down) {
            for (int i = 0; i < 3; i++) {
                double a = anim * 5 + i * Math.PI * 2 / 3;
                double sx = x + Math.cos(a) * 12 * scale;
                double sy = y - 22 * scale + Math.sin(a) * 4 * scale;
                gc.setFill(Color.rgb(255, 240, 120));
                star(gc, sx, sy, 3.5 * scale);
            }
        }
    }

    private static void star(GraphicsContext gc, double cx, double cy, double r) {
        double[] xs = new double[10];
        double[] ys = new double[10];
        for (int i = 0; i < 10; i++) {
            double a = -Math.PI / 2 + i * Math.PI / 5;
            double rr = i % 2 == 0 ? r : r * 0.45;
            xs[i] = cx + Math.cos(a) * rr;
            ys[i] = cy + Math.sin(a) * rr;
        }
        gc.fillPolygon(xs, ys, 10);
    }

    /**
     * An automaton from the Rift: a hopping blob with horns, one blinking eye and a glowing gem.
     * (x, y) is the center; the feet are at y + 16.
     *
     * @param gc     the graphics context
     * @param x      center x
     * @param y      center y
     * @param size   body size in pixels
     * @param color  body color
     * @param look   -1 facing left, 1 facing right
     * @param anim   seconds, for the animation
     * @param moving whether walking
     * @param flash  whether to draw it white (just hit)
     */
    public static void invader(GraphicsContext gc, double x, double y, double size, Color color, double look,
                               double anim, boolean moving, boolean flash) {
        double hop = moving ? Math.abs(Math.sin(anim * 9)) : 0;
        double squash = moving ? 1 + 0.08 * Math.cos(anim * 18) : 1 + 0.03 * Math.sin(anim * 4);
        double w = size / squash;
        double h = size * 0.9 * squash;
        double left = x - w / 2;
        double top = y + 16 - h - hop * 3;

        gc.setFill(Color.rgb(0, 0, 0, 0.25 - 0.08 * hop));
        gc.fillOval(x - w / 2 * (1 - hop * 0.15), y + 11, w * (1 - hop * 0.15), 7);

        Color body = flash ? Color.WHITE : color;
        gc.setFill(body.darker());
        gc.fillPolygon(new double[]{left + w * 0.2, left + w * 0.3, left + w * 0.38},
                new double[]{top + h * 0.15, top - h * 0.14, top + h * 0.1}, 3);
        gc.fillPolygon(new double[]{left + w * 0.8, left + w * 0.7, left + w * 0.62},
                new double[]{top + h * 0.15, top - h * 0.14, top + h * 0.1}, 3);
        gc.fillRoundRect(left, top + 2, w, h, w * 0.8, h * 0.9);
        gc.setFill(body);
        gc.fillRoundRect(left, top, w, h - 2, w * 0.8, h * 0.9);
        gc.setFill(body.brighter());
        gc.fillOval(left + w * 0.16, top + h * 0.1, w * 0.25, h * 0.16);
        // Gem on the belly, pulsing
        double gem = 0.5 + 0.5 * Math.sin(anim * 6);
        gc.setFill(Color.rgb(255, 230, 120, 0.6 + 0.4 * gem));
        gc.fillOval(x - w * 0.08, top + h * 0.72, w * 0.16, h * 0.12);
        // One big eye looking where it walks; blinks now and then
        double eyeR = w * 0.22;
        double ex = x + look * w * 0.06;
        double ey = top + h * 0.42;
        double blinkCycle = (anim + size) % 2.7;
        if (blinkCycle < 0.12) {
            gc.setStroke(Color.rgb(25, 20, 35));
            gc.setLineWidth(2);
            gc.strokeLine(ex - eyeR, ey, ex + eyeR, ey);
        } else {
            gc.setFill(Color.WHITE);
            gc.fillOval(ex - eyeR, ey - eyeR, eyeR * 2, eyeR * 2);
            gc.setFill(Color.rgb(25, 20, 35));
            gc.fillOval(ex - eyeR * 0.45 + look * eyeR * 0.35, ey - eyeR * 0.45, eyeR * 0.9, eyeR * 0.9);
            gc.setFill(Color.WHITE);
            gc.fillOval(ex - eyeR * 0.1 + look * eyeR * 0.35, ey - eyeR * 0.35, eyeR * 0.3, eyeR * 0.3);
        }
        // Feet
        gc.setFill(body.darker());
        double step = moving ? Math.sin(anim * 12) * 2 : 0;
        gc.fillOval(left + w * 0.12, top + h - 4 + step, w * 0.3, 7);
        gc.fillOval(left + w * 0.58, top + h - 4 - step, w * 0.3, 7);
    }

    // ------------------------------------------------------------------ items

    /**
     * A bundle of three dynamite sticks with a tape band and a lit, sparkling fuse.
     * Kept under the name "bomb" because the game logic still calls them bombs.
     *
     * @param gc     the graphics context
     * @param cx     center x
     * @param cy     center y
     * @param radius half the bundle's height
     * @param time   seconds, animates the spark
     */
    public static void bomb(GraphicsContext gc, double cx, double cy, double radius, double time) {
        dynamite(gc, cx, cy, radius, time);
    }

    /**
     * A bundle of three dynamite sticks with a tape band and a lit, sparkling fuse.
     *
     * @param gc   the graphics context
     * @param cx   center x
     * @param cy   center y
     * @param size half the bundle's height
     * @param time seconds, animates the spark
     */
    public static void dynamite(GraphicsContext gc, double cx, double cy, double size, double time) {
        double h = size * 2;
        double w = size * 0.62;
        double top = cy - size;
        double[] offs = {-w, 0, w};
        for (int i = 0; i < 3; i++) {
            double sx = cx + offs[i] - w / 2;
            double lift = i == 1 ? -size * 0.12 : 0;
            gc.setFill(Color.rgb(150, 25, 25));
            gc.fillRoundRect(sx, top + lift, w, h, w * 0.6, w * 0.6);
            gc.setFill(Color.rgb(215, 45, 40));
            gc.fillRoundRect(sx, top + lift, w * 0.78, h - 1, w * 0.6, w * 0.6);
            gc.setFill(Color.rgb(255, 130, 110, 0.8));
            gc.fillRect(sx + w * 0.18, top + lift + h * 0.12, Math.max(1, w * 0.16), h * 0.6);
            gc.setFill(Color.rgb(235, 215, 175));
            gc.fillOval(sx + w * 0.1, top + lift - w * 0.18, w * 0.8, w * 0.45);
        }
        // Tape band
        gc.setFill(Color.rgb(60, 50, 45));
        gc.fillRect(cx - w * 1.55, cy + size * 0.15, w * 3.1, size * 0.36);
        gc.setFill(Color.rgb(110, 95, 85));
        gc.fillRect(cx - w * 1.55, cy + size * 0.15, w * 3.1, size * 0.1);
        // Fuse from the middle stick, with a spark
        double fx = cx + size * 0.55;
        double fy = top - size * 0.75;
        gc.setStroke(Color.rgb(90, 80, 70));
        gc.setLineWidth(Math.max(1.5, size * 0.14));
        gc.strokeLine(cx, top - size * 0.1, cx + size * 0.25, top - size * 0.55);
        gc.strokeLine(cx + size * 0.25, top - size * 0.55, fx, fy);
        double spark = size * (0.32 + 0.14 * Math.sin(time * 25));
        gc.setFill(Color.rgb(255, 210, 60));
        gc.fillOval(fx - spark, fy - spark, spark * 2, spark * 2);
        gc.setFill(Color.rgb(255, 90, 30));
        gc.fillOval(fx - spark / 2, fy - spark / 2, spark, spark);
        gc.setStroke(Color.rgb(255, 240, 160));
        gc.setLineWidth(1);
        double a = time * 30;
        gc.strokeLine(fx, fy, fx + Math.cos(a) * spark * 1.8, fy + Math.sin(a) * spark * 1.8);
    }
}
