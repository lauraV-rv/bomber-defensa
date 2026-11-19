package com.example.bombermario.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * A text node for the menus that uses the game's {@link PixelFont}. Usable from FXML:
 * {@code <PixelLabel text="JUGAR" scale="4" color="#ffcd28" shade="#a8501a"/>}.
 * Text between "**" is drawn in the highlight color. With a wrap width it breaks lines.
 */
public class PixelLabel extends Canvas {
    private String text = "";
    private double scale = 3;
    private Color color = Color.WHITE;
    private Color shade = Color.rgb(20, 18, 30);
    private Color highlight = Color.rgb(255, 211, 77);
    private double wrap;
    private PixelFont.Align align = PixelFont.Align.LEFT;

    /** Creates an empty label. */
    public PixelLabel() {
        redraw();
    }

    /**
     * Creates a label.
     *
     * @param text  the text
     * @param scale size of one font pixel
     * @param color text color
     */
    public PixelLabel(String text, double scale, Color color) {
        this.text = text;
        this.scale = scale;
        this.color = color;
        redraw();
    }

    private void redraw() {
        List<String> lines = PixelFont.wrap(text == null ? "" : text, scale, wrap);
        double lineH = PixelFont.LINE * scale;
        double maxW = 0;
        for (String l : lines) {
            maxW = Math.max(maxW, PixelFont.width(l.replace("**", ""), scale));
        }
        double w = Math.ceil((wrap > 0 && align != PixelFont.Align.LEFT ? Math.max(wrap, maxW) : maxW) + scale * 2);
        double h = Math.ceil(lines.size() * lineH - 2 * scale + scale);
        setWidth(Math.max(1, w));
        setHeight(Math.max(1, h));
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());
        boolean open = false;
        double y = PixelFont.ACCENT_ROWS * scale;
        for (String l : lines) {
            double lw = PixelFont.width(l.replace("**", ""), scale);
            double x = switch (align) {
                case LEFT -> 0;
                case CENTER -> (w - scale * 2 - lw) / 2;
                case RIGHT -> w - scale * 2 - lw;
            };
            open = PixelFont.drawRich(gc, l, x, y, scale, color, highlight, shade, open);
            y += lineH;
        }
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        redraw();
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
        redraw();
    }

    public String getColor() {
        return color.toString();
    }

    public void setColor(String color) {
        this.color = Color.web(color);
        redraw();
    }

    public String getShade() {
        return shade == null ? "none" : shade.toString();
    }

    /** @param shade shadow color, or "none" */
    public void setShade(String shade) {
        this.shade = "none".equals(shade) ? null : Color.web(shade);
        redraw();
    }

    public String getHighlight() {
        return highlight.toString();
    }

    public void setHighlight(String highlight) {
        this.highlight = Color.web(highlight);
        redraw();
    }

    public double getWrap() {
        return wrap;
    }

    /** @param wrap maximum line width in pixels (0 = no wrapping) */
    public void setWrap(double wrap) {
        this.wrap = wrap;
        redraw();
    }

    public String getAlign() {
        return align.name();
    }

    /** @param align LEFT, CENTER or RIGHT */
    public void setAlign(String align) {
        this.align = PixelFont.Align.valueOf(align.toUpperCase());
        redraw();
    }
}
