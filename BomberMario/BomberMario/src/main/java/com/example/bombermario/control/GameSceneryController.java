package com.example.bombermario.control;

import com.example.bombermario.HelloApplication;
import com.example.bombermario.Screens.DefenseScreen;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Runs the game: a 60 fps loop that updates and draws the defense and moves on to the
 * victory or defeat screen when it ends.
 */
public class GameSceneryController implements Initializable {
    /** Longest frame step allowed, so a hiccup never makes things jump through walls. */
    private static final double MAX_STEP = 1.0 / 20;

    @FXML
    private Canvas canvas;
    private DefenseScreen game;
    private AnimationTimer loop;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        game = new DefenseScreen(canvas);
        canvas.setOnKeyPressed(event -> game.onKeyPressed(event));
        canvas.setOnKeyReleased(event -> game.onKeyReleased(event));
        canvas.setOnMouseClicked(event -> {
            game.onMouseClicked(event.getX(), event.getY());
            canvas.requestFocus();
        });
        loop = new AnimationTimer() {
            private long last = -1;

            @Override
            public void handle(long now) {
                double dt = last < 0 ? 0 : Math.min((now - last) / 1e9, MAX_STEP);
                last = now;
                step(dt);
            }
        };
        loop.start();
        Platform.runLater(canvas::requestFocus);
    }

    private void step(double dt) {
        game.update(dt);
        game.paint();
        if (game.isQuitRequested()) {
            finish("main-menu.fxml");
        } else if (game.isLost()) {
            finish("game-over.fxml");
        } else if (game.isWon()) {
            finish("goal-view.fxml");
        }
    }

    private void finish(String fxml) {
        loop.stop();
        try {
            HelloApplication.openScene(fxml);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
