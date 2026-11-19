package com.example.bombermario.control;

import com.example.bombermario.model.Art;
import com.example.bombermario.model.EnemyType;
import com.example.bombermario.model.GameSettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Title screen: the game's name, an animated illustration and the choice between
 * one player and two-player co-op. Story and controls come on the next screens.
 */
public class MainController extends AnimatedScreen {
    private final Canvas hero = new Canvas(520, 176);

    @Override
    protected MenuBackground.Mode mode() {
        return MenuBackground.Mode.TITLE;
    }

    @Override
    protected void build() {
        content.setSpacing(8);
        content.getChildren().addAll(
                UiKit.text("Un mod del clásico de bombas y laberintos, convertido en defensa táctica", 1.5, "#9fa6c8"),
                UiKit.title("Bomber-Defensa", 7, UiKit.GOLD, UiKit.GOLD_SHADE),
                UiKit.title("Castillo en Obras", 3.2, "#ffffff", "#3c2a5a"),
                UiKit.text("Construye · Desvía · Detona · Defiende el Núcleo", 1.8, "#c9a8ff"),
                hero);
        VBox.setMargin(hero, new Insets(4, 0, 0, 0));

        Button solo = UiKit.button("1 jugador", 3, "#1c1e2a", "big-button", () -> start(1));
        Button coop = UiKit.button("2 jugadores (co-op)", 3, "#ffffff", "coop-button", () -> start(2));
        HBox buttons = new HBox(18, solo, coop);
        buttons.setAlignment(Pos.CENTER);
        VBox.setMargin(buttons, new Insets(6, 0, 0, 0));
        content.getChildren().addAll(buttons,
                UiKit.text("6 oleadas · 6 a 9 minutos · mismo teclado para 2", 1.4, "#9fa6c8"));
    }

    private void start(int players) {
        GameSettings.setPlayers(players);
        go("story.fxml");
    }

    // The two engineers guarding the core behind their wall, with dynamite ready
    @Override
    protected void frame(double dt) {
        GraphicsContext g = hero.getGraphicsContext2D();
        double w = hero.getWidth();
        g.clearRect(0, 0, w, hero.getHeight());
        g.save();
        g.translate(w * 0.06, 0);
        g.scale(0.88, 0.88);
        g.setFill(Color.rgb(0, 0, 0, 0.25));
        g.fillOval(w / 2 - 210, 168, 420, 26);
        Art.stone(g, 3, 3, w / 2 - 30, 110, 60);
        Art.coreCrystal(g, w / 2, 82, 2.6, time, 1);
        for (int i = 0; i < 5; i++) {
            Art.crate(g, w / 2 - 125 + i * 50, 140, 50);
        }
        Art.builder(g, w / 2 - 150, 128, 2.1, 1, time, false, false, Art.Outfit.BLUE);
        Art.builder(g, w / 2 + 150, 128, 2.1, -1, time + 1.3, false, false, Art.Outfit.AMBER);
        Art.dynamite(g, w / 2 + 205, 160, 13, time);
        double x = 30 + (time * 22) % 60;
        Art.invader(g, x, 150, 30, EnemyType.GRUNT.getColor(), 1, time, true, false);
        Art.invader(g, w - 20 - (time * 16) % 40, 150, 24, EnemyType.RUNNER.getColor(), -1, time + 0.5, true, false);
        g.restore();
    }
}
