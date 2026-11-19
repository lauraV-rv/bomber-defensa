package com.example.bombermario.model;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import com.example.bombermario.ui.PixelFont;

/**
 * Something the builder can pick up: extra dynamite (dropped by automatons) or building
 * material (dropped by rubble). It pops out, floats and glows until collected, and
 * disappears after a while if nobody takes it.
 */
public class Pickup {
    /** What the pickup gives. */
    public enum Kind {
        BOMB, BLOCKS
    }

    private static final double POP_TIME = 0.3;
    private static final double LIFE_TIME = 18;

    private final int col;
    private final int row;
    private final Kind kind;
    private final int amount;
    private double delay;
    private double time;

    /**
     * Instantiates a new pickup.
     *
     * @param col    the board column
     * @param row    the board row
     * @param kind   bombs or building material
     * @param amount how many it gives
     * @param delay  seconds before it appears
     */
    public Pickup(int col, int row, Kind kind, int amount, double delay) {
        this.col = col;
        this.row = row;
        this.kind = kind;
        this.amount = amount;
        this.delay = delay;
    }

    /**
     * Advances the appear delay and the animation.
     *
     * @param dt elapsed seconds
     */
    public void update(double dt) {
        if (delay > 0) {
            delay -= dt;
        } else {
            time += dt;
        }
    }

    /** @return true once it has appeared */
    public boolean isVisible() {
        return delay <= 0;
    }

    /** @return true when it has been on the floor too long */
    public boolean isExpired() {
        return time >= LIFE_TIME;
    }

    /**
     * Draws the pickup.
     *
     * @param gc the graphics context
     */
    public void paint(GraphicsContext gc) {
        if (!isVisible()) {
            return;
        }
        // Blink during the last 3 seconds
        if (time > LIFE_TIME - 3 && ((int) (time * 8)) % 2 == 0) {
            return;
        }
        double cx = Board.centerX(col);
        double cy = Board.centerY(row);
        double pop;
        if (time < POP_TIME) {
            pop = 1.25 * time / POP_TIME;
        } else if (time < POP_TIME * 1.6) {
            pop = 1.25 - 0.25 * (time - POP_TIME) / (POP_TIME * 0.6);
        } else {
            pop = 1;
        }
        double bob = time < POP_TIME ? 0 : Math.sin(time * 4) * 3;
        double pulse = 0.5 + 0.5 * Math.sin(time * 5);

        double glow = 16 * pop;
        Color glowColor = kind == Kind.BOMB ? Color.rgb(255, 215, 50, 0.35 + 0.3 * pulse) : Color.rgb(80, 170, 255, 0.35 + 0.3 * pulse);
        gc.setFill(glowColor);
        gc.fillOval(cx - glow, cy - glow, glow * 2, glow * 2);
        gc.setStroke(Color.rgb(255, 255, 255, 0.8));
        gc.setLineWidth(2);
        gc.strokeOval(cx - glow, cy - glow, glow * 2, glow * 2);

        if (kind == Kind.BOMB) {
            Bomb.drawBomb(gc, cx, cy + 2 + bob, 8 * pop, time);
        } else {
            double s = 16 * pop;
            gc.setFill(Color.rgb(196, 140, 72));
            gc.fillRect(cx - s / 2, cy - s / 2 + bob, s, s);
            gc.setStroke(Color.rgb(120, 78, 32));
            gc.setLineWidth(2);
            gc.strokeRect(cx - s / 2, cy - s / 2 + bob, s, s);
            gc.strokeLine(cx - s / 2, cy - s / 2 + bob, cx + s / 2, cy + s / 2 + bob);
        }

        PixelFont.draw(gc, "+" + amount, cx + 11, cy + 9, 1.5,
                kind == Kind.BOMB ? Color.rgb(255, 240, 80) : Color.rgb(140, 210, 255), Color.rgb(10, 10, 15), PixelFont.Align.CENTER);
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public Kind getKind() {
        return kind;
    }

    public int getAmount() {
        return amount;
    }
}
