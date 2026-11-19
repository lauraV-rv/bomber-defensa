package com.example.bombermario.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The game's own blocky pixel font (5x7 glyphs designed here, no font files). Text is
 * drawn in capitals with a solid "3D" shadow offset down-right, in the style of retro
 * arcade lettering. Sizes are given as the size of one font pixel ("scale").
 */
public final class PixelFont {
    /** Glyph rows (cap height). */
    public static final int ROWS = 7;
    /** Rows reserved above the caps for accents. */
    public static final int ACCENT_ROWS = 2;
    /** Total line height in font pixels, including accents and spacing. */
    public static final int LINE = ROWS + ACCENT_ROWS + 2;

    /** Horizontal alignment. */
    public enum Align {
        LEFT, CENTER, RIGHT
    }

    // Each glyph: rows of '#' (ink) and '.' (empty); rows are as wide as the glyph
    private static final Map<Character, String[]> GLYPHS = new HashMap<>();
    // Pre-merged horizontal runs per glyph: {row, startCol, length}
    private static final Map<Character, int[][]> RUNS = new HashMap<>();
    private static final Map<Character, Integer> WIDTH = new HashMap<>();

    private static final String[] ACUTE = {"..#", ".#."};
    private static final String[] TILDE = {".#.#", "#.#."};
    private static final String[] DIAERESIS = {"...", "#.#"};

    static {
        g('A', ".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#");
        g('B', "####.", "#...#", "#...#", "####.", "#...#", "#...#", "####.");
        g('C', ".###.", "#...#", "#....", "#....", "#....", "#...#", ".###.");
        g('D', "####.", "#...#", "#...#", "#...#", "#...#", "#...#", "####.");
        g('E', "#####", "#....", "#....", "####.", "#....", "#....", "#####");
        g('F', "#####", "#....", "#....", "####.", "#....", "#....", "#....");
        g('G', ".###.", "#...#", "#....", "#.###", "#...#", "#...#", ".####");
        g('H', "#...#", "#...#", "#...#", "#####", "#...#", "#...#", "#...#");
        g('I', "###", ".#.", ".#.", ".#.", ".#.", ".#.", "###");
        g('J', "..###", "...#.", "...#.", "...#.", "...#.", "#..#.", ".##..");
        g('K', "#...#", "#..#.", "#.#..", "##...", "#.#..", "#..#.", "#...#");
        g('L', "#....", "#....", "#....", "#....", "#....", "#....", "#####");
        g('M', "#...#", "##.##", "#.#.#", "#.#.#", "#...#", "#...#", "#...#");
        g('N', "#...#", "#...#", "##..#", "#.#.#", "#..##", "#...#", "#...#");
        g('O', ".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###.");
        g('P', "####.", "#...#", "#...#", "####.", "#....", "#....", "#....");
        g('Q', ".###.", "#...#", "#...#", "#...#", "#.#.#", "#..#.", ".##.#");
        g('R', "####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#");
        g('S', ".####", "#....", "#....", ".###.", "....#", "....#", "####.");
        g('T', "#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#..");
        g('U', "#...#", "#...#", "#...#", "#...#", "#...#", "#...#", ".###.");
        g('V', "#...#", "#...#", "#...#", "#...#", "#...#", ".#.#.", "..#..");
        g('W', "#...#", "#...#", "#...#", "#.#.#", "#.#.#", "#.#.#", ".#.#.");
        g('X', "#...#", "#...#", ".#.#.", "..#..", ".#.#.", "#...#", "#...#");
        g('Y', "#...#", "#...#", ".#.#.", "..#..", "..#..", "..#..", "..#..");
        g('Z', "#####", "....#", "...#.", "..#..", ".#...", "#....", "#####");
        g('0', ".###.", "#...#", "#..##", "#.#.#", "##..#", "#...#", ".###.");
        g('1', ".#.", "##.", ".#.", ".#.", ".#.", ".#.", "###");
        g('2', ".###.", "#...#", "....#", "...#.", "..#..", ".#...", "#####");
        g('3', "####.", "....#", "....#", ".###.", "....#", "....#", "####.");
        g('4', "...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#.");
        g('5', "#####", "#....", "####.", "....#", "....#", "#...#", ".###.");
        g('6', ".###.", "#....", "#....", "####.", "#...#", "#...#", ".###.");
        g('7', "#####", "....#", "...#.", "..#..", ".#...", ".#...", ".#...");
        g('8', ".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###.");
        g('9', ".###.", "#...#", "#...#", ".####", "....#", "....#", ".###.");
        g('.', ".", ".", ".", ".", ".", ".", "#");
        g(',', "..", "..", "..", "..", "..", ".#", "#.");
        g(':', ".", ".", "#", ".", ".", "#", ".");
        g(';', "..", "..", ".#", "..", "..", ".#", "#.");
        g('!', "#", "#", "#", "#", "#", ".", "#");
        g('¡', "#", ".", "#", "#", "#", "#", "#");
        g('?', ".###.", "#...#", "....#", "...#.", "..#..", ".....", "..#..");
        g('¿', "..#..", ".....", "..#..", ".#...", "#....", "#...#", ".###.");
        g('-', "....", "....", "....", "####", "....", "....", "....");
        g('+', ".....", "..#..", "..#..", "#####", "..#..", "..#..", ".....");
        g('=', "....", "....", "####", "....", "####", "....", "....");
        g('/', "....#", "...#.", "...#.", "..#..", ".#...", ".#...", "#....");
        g('×', ".....", "#...#", ".#.#.", "..#..", ".#.#.", "#...#", ".....");
        g('%', "##..#", "##..#", "...#.", "..#..", ".#...", "#..##", "#..##");
        g('(', "..#", ".#.", "#..", "#..", "#..", ".#.", "..#");
        g(')', "#..", ".#.", "..#", "..#", "..#", ".#.", "#..");
        g('\'', "#", "#", ".", ".", ".", ".", ".");
        g('"', "#.#", "#.#", "...", "...", "...", "...", "...");
        g('·', ".", ".", ".", "#", ".", ".", ".");
        g('∞', ".......", ".......", ".##.##.", "#..#..#", ".##.##.", ".......", ".......");
        g('▸', "#...", "##..", "###.", "####", "###.", "##..", "#...");
        g('◂', "...#", "..##", ".###", "####", ".###", "..##", "...#");
        g('↑', "..#..", ".###.", "#.#.#", "..#..", "..#..", "..#..", "..#..");
        g('↓', "..#..", "..#..", "..#..", "..#..", "#.#.#", ".###.", "..#..");
        g('←', ".......", "..#....", ".#.....", "#######", ".#.....", "..#....", ".......");
        g('→', ".......", "....#..", ".....#.", "#######", ".....#.", "....#..", ".......");
        g('<', "...#", "..#.", ".#..", "#...", ".#..", "..#.", "...#");
        g('>', "#...", ".#..", "..#.", "...#", "..#.", ".#..", "#...");
        g('#', ".#.#.", "#####", ".#.#.", ".#.#.", ".#.#.", "#####", ".#.#.");
        g('*', ".....", "#.#.#", ".###.", "#####", ".###.", "#.#.#", ".....");
    }

    private PixelFont() {
    }

    private static void g(char c, String... rows) {
        GLYPHS.put(c, rows);
        WIDTH.put(c, rows[0].length());
        List<int[]> runs = new ArrayList<>();
        for (int r = 0; r < rows.length; r++) {
            addRuns(runs, rows[r], r);
        }
        RUNS.put(c, runs.toArray(new int[0][]));
    }

    private static void addRuns(List<int[]> runs, String row, int r) {
        int start = -1;
        for (int i = 0; i <= row.length(); i++) {
            boolean ink = i < row.length() && row.charAt(i) == '#';
            if (ink && start < 0) {
                start = i;
            } else if (!ink && start >= 0) {
                runs.add(new int[]{r, start, i - start});
                start = -1;
            }
        }
    }

    // Splits accented letters into base letter + accent
    private static char base(char c) {
        return switch (c) {
            case 'Á' -> 'A';
            case 'É' -> 'E';
            case 'Í' -> 'I';
            case 'Ó' -> 'O';
            case 'Ú', 'Ü' -> 'U';
            case 'Ñ' -> 'N';
            default -> c;
        };
    }

    private static String[] accent(char c) {
        return switch (c) {
            case 'Á', 'É', 'Í', 'Ó', 'Ú' -> ACUTE;
            case 'Ñ' -> TILDE;
            case 'Ü' -> DIAERESIS;
            default -> null;
        };
    }

    private static String normalize(String text) {
        return text.toUpperCase().replace("…", "...").replace("—", "-").replace("–", "-");
    }

    private static int advance(char c) {
        if (c == ' ') {
            return 4;
        }
        Integer w = WIDTH.get(base(c));
        return (w == null ? 5 : w) + 1;
    }

    /**
     * Width of a line of text.
     *
     * @param text  the text (no line breaks)
     * @param scale size of one font pixel
     * @return width in canvas pixels
     */
    public static double width(String text, double scale) {
        String t = normalize(text);
        int units = 0;
        for (int i = 0; i < t.length(); i++) {
            units += advance(t.charAt(i));
        }
        return Math.max(0, units - 1) * scale;
    }

    /** @param scale size of one font pixel @return cap height in canvas pixels */
    public static double height(double scale) {
        return ROWS * scale;
    }

    /**
     * Draws one line of text. y is the top of the capital letters.
     *
     * @param gc    the graphics context
     * @param text  the text
     * @param x     anchor x (meaning depends on align)
     * @param y     top of the capitals
     * @param scale size of one font pixel
     * @param fill  letter color
     * @param shade 3D shadow color, or null for none
     * @param align horizontal alignment
     */
    public static void draw(GraphicsContext gc, String text, double x, double y, double scale,
                            Color fill, Color shade, Align align) {
        String t = normalize(text);
        double w = width(t, scale);
        double left = switch (align) {
            case LEFT -> x;
            case CENTER -> x - w / 2;
            case RIGHT -> x - w;
        };
        left = Math.round(left);
        y = Math.round(y);
        if (shade != null) {
            double off = Math.max(1, Math.round(scale * 0.8));
            gc.setFill(shade);
            ink(gc, t, left + off, y + off, scale);
        }
        gc.setFill(fill);
        ink(gc, t, left, y, scale);
    }

    private static void ink(GraphicsContext gc, String t, double x, double y, double s) {
        double cx = x;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            char b = base(c);
            int[][] runs = RUNS.get(b);
            if (runs == null && c != ' ') {
                runs = RUNS.get('?');
            }
            if (runs != null) {
                for (int[] run : runs) {
                    gc.fillRect(cx + run[1] * s, y + run[0] * s, run[2] * s + 0.5, s + 0.5);
                }
                String[] acc = accent(c);
                if (acc != null) {
                    int gw = WIDTH.get(b);
                    double ax = cx + Math.floor((gw - acc[1].length()) / 2.0) * s;
                    for (int r = 0; r < acc.length; r++) {
                        String row = acc[r];
                        for (int k = 0; k < row.length(); k++) {
                            if (row.charAt(k) == '#') {
                                gc.fillRect(ax + k * s, y - (ACCENT_ROWS - r) * s - s * 0.5, s + 0.5, s + 0.5);
                            }
                        }
                    }
                }
            }
            cx += advance(c) * s;
        }
    }

    /**
     * Splits text into lines that fit a width. "**" markers are kept so highlights survive.
     *
     * @param text     the text ("\n" forces a break)
     * @param scale    size of one font pixel
     * @param maxWidth available width in canvas pixels
     * @return the lines
     */
    public static List<String> wrap(String text, double scale, double maxWidth) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (line.length() > 0 && maxWidth > 0 && width(candidate.replace("**", ""), scale) > maxWidth) {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(candidate);
                }
            }
            lines.add(line.toString());
        }
        return lines;
    }

    /**
     * Draws a line that may contain "**highlighted**" words in a second color.
     *
     * @param gc        the graphics context
     * @param text      the text with markers
     * @param x         left x
     * @param y         top of the capitals
     * @param scale     size of one font pixel
     * @param fill      normal color
     * @param highlight highlight color
     * @param shade     shadow color or null
     * @param open      whether the line starts inside a highlight
     * @return whether the line ends inside a highlight
     */
    public static boolean drawRich(GraphicsContext gc, String text, double x, double y, double scale,
                                   Color fill, Color highlight, Color shade, boolean open) {
        String[] parts = text.split("\\*\\*", -1);
        double cx = x;
        boolean hl = open;
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                hl = !hl;
            }
            String p = parts[i];
            if (!p.isEmpty()) {
                draw(gc, p, cx, y, scale, hl ? highlight : fill, shade, Align.LEFT);
                // width() leaves out the gap after the last letter; add it back
                cx += width(p, scale) + scale;
            }
        }
        return hl;
    }
}
