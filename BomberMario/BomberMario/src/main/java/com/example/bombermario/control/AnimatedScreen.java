package com.example.bombermario.control;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Base for the menu screens: an animated {@link MenuBackground} canvas, a centered
 * column of content built in code, and a sound button in the top-right corner.
 */
abstract class AnimatedScreen {
    @FXML
    protected StackPane root;
    @FXML
    protected Canvas background;

    protected final VBox content = new VBox(12);
    protected double time;
    private MenuBackground backdrop;
    private AnimationTimer timer;

    /** @return which backdrop to show */
    protected abstract MenuBackground.Mode mode();

    /** Fills {@link #content}. */
    protected abstract void build();

    /**
     * Called every frame after the backdrop is drawn, to animate drawings.
     *
     * @param dt elapsed seconds
     */
    protected void frame(double dt) {
    }

    @FXML
    protected void initialize() {
        backdrop = new MenuBackground(mode());
        content.setAlignment(Pos.TOP_CENTER);
        content.setPickOnBounds(false);
        content.setPadding(new Insets(28, 20, 20, 20));
        root.getChildren().add(content);
        Button sound = UiKit.soundButton();
        StackPane.setAlignment(sound, Pos.TOP_RIGHT);
        StackPane.setMargin(sound, new Insets(12, 14, 0, 0));
        root.getChildren().add(sound);
        build();
        timer = new AnimationTimer() {
            private long last = -1;

            @Override
            public void handle(long now) {
                double dt = last < 0 ? 0 : Math.min((now - last) / 1e9, 0.05);
                last = now;
                time += dt;
                backdrop.paint(background.getGraphicsContext2D(), background.getWidth(), background.getHeight(), dt);
                frame(dt);
            }
        };
        timer.start();
    }

    /**
     * Stops the animation and opens another screen.
     *
     * @param fxml the screen to open
     */
    protected void go(String fxml) {
        timer.stop();
        UiKit.open(fxml);
    }
}
