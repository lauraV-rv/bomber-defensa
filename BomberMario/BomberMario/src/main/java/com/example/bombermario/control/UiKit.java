package com.example.bombermario.control;

import com.example.bombermario.HelloApplication;
import com.example.bombermario.audio.SoundManager;
import com.example.bombermario.ui.PixelLabel;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

/**
 * Small helpers to build menu pieces with the game's pixel font.
 */
final class UiKit {
    /** Title yellow and its 3D shade, like arcade lettering. */
    static final String GOLD = "#ffcd28";
    static final String GOLD_SHADE = "#a3451a";
    static final String SHADE = "#14121e";

    private UiKit() {
    }

    static PixelLabel text(String text, double scale, String color) {
        PixelLabel l = new PixelLabel();
        l.setShade(SHADE);
        l.setScale(scale);
        l.setColor(color);
        l.setText(text);
        return l;
    }

    static PixelLabel title(String text, double scale, String color, String shade) {
        PixelLabel l = text(text, scale, color);
        l.setShade(shade);
        return l;
    }

    static PixelLabel paragraph(String text, double scale, String color, double wrap) {
        PixelLabel l = text(text, scale, color);
        l.setWrap(wrap);
        return l;
    }

    static PixelLabel centered(String text, double scale, String color, double wrap) {
        PixelLabel l = paragraph(text, scale, color, wrap);
        l.setAlign("CENTER");
        return l;
    }

    /** A pressable key drawn like a keyboard keycap. */
    static Node keycap(String key) {
        PixelLabel l = text(key, 1.6, "#1c1e2a");
        l.setShade("none");
        StackPane cap = new StackPane(l);
        cap.getStyleClass().add("keycap-box");
        return cap;
    }

    static HBox keys(String... keys) {
        HBox row = new HBox(4);
        row.setAlignment(Pos.CENTER);
        for (String k : keys) {
            row.getChildren().add(keycap(k));
        }
        return row;
    }

    /** A button whose label is drawn with the pixel font. */
    static Button button(String text, double scale, String textColor, String styleClass, Runnable action) {
        PixelLabel l = text(text, scale, textColor);
        l.setShade("none");
        Button b = new Button();
        b.setGraphic(l);
        b.getStyleClass().add(styleClass);
        b.setMnemonicParsing(false);
        b.setOnAction(e -> {
            SoundManager.play(SoundManager.Sfx.CLICK);
            action.run();
        });
        return b;
    }

    /** The small sound on/off button for the top-right corner. */
    static Button soundButton() {
        Button b = new Button();
        Runnable refresh = () -> {
            PixelLabel l = text(SoundManager.isMuted() ? "Sonido: no" : "Sonido: sí", 1.4, "#ffffff");
            l.setShade("none");
            b.setGraphic(l);
        };
        refresh.run();
        b.getStyleClass().add("sound-button");
        b.setFocusTraversable(false);
        b.setOnAction(e -> {
            SoundManager.toggleMuted();
            refresh.run();
        });
        return b;
    }

    static VBox card(double width, Node... children) {
        VBox v = new VBox(6, children);
        v.setAlignment(Pos.TOP_CENTER);
        v.getStyleClass().add("card");
        v.setPrefWidth(width);
        v.setMaxWidth(width);
        return v;
    }

    static void open(String fxml) {
        try {
            HelloApplication.openScene(fxml);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
