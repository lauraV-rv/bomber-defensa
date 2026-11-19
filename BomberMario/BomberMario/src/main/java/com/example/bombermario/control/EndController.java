package com.example.bombermario.control;

import com.example.bombermario.model.Art;
import com.example.bombermario.model.EnemyType;
import com.example.bombermario.model.GameResult;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Shared victory / defeat screen: an illustrated header, the game's stats as cards
 * (per player in co-op) and buttons to play again or go back to the cover.
 */
abstract class EndController extends AnimatedScreen {
    private final Canvas hero = new Canvas(420, 112);

    /** @return true for the victory screen */
    protected abstract boolean won();

    @Override
    protected MenuBackground.Mode mode() {
        return won() ? MenuBackground.Mode.WIN : MenuBackground.Mode.LOSE;
    }

    @Override
    protected void build() {
        boolean coop = GameResult.getPlayers() == 2;
        content.setSpacing(9);
        content.setPadding(new Insets(22, 20, 16, 20));
        content.getChildren().addAll(
                UiKit.text(coop ? "Bomber-Defensa · fin de la partida cooperativa" : "Bomber-Defensa · fin de la partida", 1.6, "#9fa6c8"),
                won() ? UiKit.title("¡Núcleo a salvo!", 5.5, UiKit.GOLD, UiKit.GOLD_SHADE)
                        : UiKit.title("El Núcleo cayó", 5.5, "#ff6a5a", "#5a1418"),
                UiKit.text(won() ? (coop ? "Resistieron todas las oleadas. La Ciudadela sigue en pie."
                                : "Resististe todas las oleadas. La Ciudadela sigue en pie.")
                        : "Los Autómatas sobrecargaron el corazón de la Ciudadela.", 1.8, "#c9a8ff"),
                hero);

        HBox stats = new HBox(12,
                stat("Oleada", GameResult.getWaveReached() + "/" + GameResult.getTotalWaves(), "#ffffff"),
                stat("Derrotados", String.valueOf(GameResult.getTotalKills()), "#ffcd28"),
                stat("Energía", GameResult.getCoreLeft() + "/" + GameResult.getCoreMax(),
                        GameResult.getCoreLeft() > 0 ? "#78d8ff" : "#ff6a5a"),
                stat("Tiempo", GameResult.getTime(), "#ffffff"));
        stats.setAlignment(Pos.CENTER);
        content.getChildren().add(stats);

        HBox players = new HBox(12);
        players.setAlignment(Pos.CENTER);
        if (coop) {
            players.getChildren().addAll(playerCard(0, "J1 · azul", "#5aa2ff"), playerCard(1, "J2 · ámbar", "#ffa040"));
        } else {
            players.getChildren().addAll(
                    stat("Muros", String.valueOf(GameResult.getWalls(0)), "#ffffff"),
                    stat("Veces aturdido (-" + GameResult.getKnockouts(0) + " energía)", String.valueOf(GameResult.getKnockouts(0)), "#ff9c86"));
        }
        content.getChildren().add(players);

        Button again = UiKit.button(won() ? "Jugar de nuevo" : "Reintentar", 3, "#1c1e2a", "big-button",
                () -> go("game-scenary.fxml"));
        again.setDefaultButton(true);
        Button menu = UiKit.button("Portada", 2, "#ffffff", "coop-button", () -> go("main-menu.fxml"));
        HBox buttons = new HBox(18, menu, again);
        buttons.setAlignment(Pos.CENTER);
        VBox.setMargin(buttons, new Insets(6, 0, 0, 0));
        content.getChildren().add(buttons);
    }

    private static Node stat(String label, String value, String color) {
        VBox card = UiKit.card(label.length() > 14 ? 260 : 170, UiKit.text(value, 3, color), UiKit.text(label, 1.5, "#b8bdd6"));
        card.setMinHeight(0);
        card.getStyleClass().add("stat-card");
        return card;
    }

    private static Node playerCard(int p, String title, String color) {
        VBox card = UiKit.card(300, UiKit.text(title, 1.8, color),
                UiKit.text("Derrotados: " + GameResult.getKills(p), 1.5, "#e3e6f2"),
                UiKit.text("Muros: " + GameResult.getWalls(p), 1.5, "#e3e6f2"),
                UiKit.text("Aturdido: " + GameResult.getKnockouts(p) + " (-" + GameResult.getKnockouts(p) + " energía)",
                        1.5, "#ff9c86"));
        card.getStyleClass().add("stat-card");
        card.setStyle("-fx-border-color: " + color + ";");
        return card;
    }

    @Override
    protected void frame(double dt) {
        GraphicsContext g = hero.getGraphicsContext2D();
        double w = hero.getWidth();
        g.clearRect(0, 0, w, hero.getHeight());
        boolean coop = GameResult.getPlayers() == 2;
        Art.stone(g, 2, 2, w / 2 - 22, 68, 44);
        if (won()) {
            Art.coreCrystal(g, w / 2, 48, 1.8, time, 1);
            double jump = Math.abs(Math.sin(time * 5)) * 10;
            Art.builder(g, w / 2 - 110, 76 - jump, 1.7, 1, time, false, false, coop ? Art.Outfit.BLUE : Art.Outfit.AMBER);
            if (coop) {
                double jump2 = Math.abs(Math.sin(time * 5 + 1.2)) * 10;
                Art.builder(g, w / 2 + 110, 76 - jump2, 1.7, -1, time + 1, false, false, Art.Outfit.AMBER);
            }
        } else {
            Art.coreCrystal(g, w / 2, 48, 1.8, time, 0);
            g.setStroke(Color.rgb(40, 10, 10));
            g.setLineWidth(2.5);
            g.strokeLine(w / 2 - 6, 30, w / 2 + 4, 46);
            g.strokeLine(w / 2 + 4, 46, w / 2 - 3, 62);
            Art.builder(g, w / 2 - 110, 76, 1.7, 1, time, false, true, coop ? Art.Outfit.BLUE : Art.Outfit.AMBER);
            if (coop) {
                Art.builder(g, w / 2 + 110, 76, 1.7, -1, time, false, true, Art.Outfit.AMBER);
            } else {
                Art.invader(g, w / 2 + 100, 82, 34, EnemyType.BRUTE.getColor(), -1, time, false, false);
            }
            Art.invader(g, w / 2 + 50, 84, 28, EnemyType.GRUNT.getColor(), -1, time + 0.6, false, false);
        }
    }
}
