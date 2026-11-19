package com.example.bombermario.model;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import com.example.bombermario.ui.PixelFont;

/**
 * A short message that rises and fades out, used as feedback ("+1 dinamita", "¡Aturdido!"...).
 */
public class FloatingText {
    private static final double LIFE_TIME = 1.3;

    private final double x;
    private final double y;
    private final String text;
    private final Color color;
    private final double size;
    private double age;

    /**
     * Instantiates a new floating text.
     *
     * @param x     center x in pixels
     * @param y     baseline y in pixels
     * @param text  the message
     * @param color the fill color
     * @param size  the font size
     */
    public FloatingText(double x, double y, String text, Color color, double size) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.color = color;
        this.size = size;
    }

    /**
     * Ages the text.
     *
     * @param dt elapsed seconds
     */
    public void update(double dt) {
        age += dt;
    }

    /**
     * Whether the text has faded out.
     *
     * @return true when it can be discarded
     */
    public boolean isDone() {
        return age >= LIFE_TIME;
    }

    /**
     * Draws the text.
     *
     * @param gc the graphics context
     */
    public void paint(GraphicsContext gc) {
        double p = age / LIFE_TIME;
        double alpha = p < 0.6 ? 1 : 1 - (p - 0.6) / 0.4;
        double drawY = y - 28 * p;
        gc.setGlobalAlpha(Math.max(0, alpha));
        double scale = Math.max(1.4, size / 8.0);
        PixelFont.draw(gc, text, x, drawY - PixelFont.height(scale), scale, color, Color.rgb(15, 12, 22), PixelFont.Align.CENTER);
        gc.setGlobalAlpha(1);
    }
}
