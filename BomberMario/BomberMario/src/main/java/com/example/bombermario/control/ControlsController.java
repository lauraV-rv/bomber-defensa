package com.example.bombermario.control;

import com.example.bombermario.model.Art;
import com.example.bombermario.model.EnemyType;
import com.example.bombermario.model.GameSettings;
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
 * Controls and key rules, shown after the story and right before playing. Solo shows
 * four cards; co-op shows one card per player plus the shared keys.
 */
public class ControlsController extends AnimatedScreen {
    private final Canvas[] icons = {new Canvas(56, 56), new Canvas(56, 56), new Canvas(56, 56), new Canvas(56, 56)};

    @Override
    protected MenuBackground.Mode mode() {
        return MenuBackground.Mode.STORY;
    }

    @Override
    protected void build() {
        boolean coop = GameSettings.isCoop();
        content.setSpacing(12);
        content.getChildren().addAll(
                UiKit.text(coop ? "Modo cooperativo · mismo teclado" : "Modo 1 jugador", 1.6, "#9fa6c8"),
                UiKit.title("Controles", 5, UiKit.GOLD, UiKit.GOLD_SHADE));
        HBox cards = coop ? coopCards() : soloCards();
        cards.setAlignment(Pos.CENTER);
        VBox.setMargin(cards, new Insets(6, 0, 0, 0));
        content.getChildren().add(cards);

        VBox rules = new VBox(7);
        rules.setAlignment(Pos.CENTER);
        rules.getChildren().add(rule("Reapariciones infinitas, pero cada aturdimiento cuesta **-1 energía** del Núcleo."));
        if (coop) {
            rules.getChildren().add(rule("Comparten bloques y dinamita. **¡Fuego amigo!** La dinamita aturde al compañero."));
        } else {
            rules.getChildren().add(rule("¿Sin dinamita? A los 5 s recibes **1 de emergencia**, y los autómatas sueltan más."));
        }
        rules.getChildren().add(rule("Un autómata que camina más de 30 s entra en **sobrecarga** y acelera."));
        VBox.setMargin(rules, new Insets(4, 0, 0, 0));
        content.getChildren().add(rules);

        Button play = UiKit.button("¡A jugar!", 3.4, "#1c1e2a", "big-button", () -> go("game-scenary.fxml"));
        play.setDefaultButton(true);
        Button back = UiKit.button("◂ Portada", 1.6, "#ffffff", "sound-button", () -> go("main-menu.fxml"));
        HBox buttons = new HBox(18, back, play);
        buttons.setAlignment(Pos.CENTER);
        VBox.setMargin(buttons, new Insets(6, 0, 0, 0));
        content.getChildren().add(buttons);
    }

    private static Node rule(String text) {
        Node l = UiKit.centered(text, 1.5, "#ffc2b0", 640);
        VBox box = new VBox(l);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("rule-strip");
        box.setMaxWidth(700);
        return box;
    }

    private HBox soloCards() {
        return new HBox(14,
                UiKit.card(170, icons[0], UiKit.keys("W", "A", "S", "D"), UiKit.text("Moverte", 2, "#ffffff"),
                        UiKit.centered("o con las flechas", 1.3, "#b8bdd6", 150)),
                UiKit.card(170, icons[1], UiKit.keys("E"), UiKit.text("Construir", 2, "#ffffff"),
                        UiKit.centered("alarga el camino; nunca lo cierra", 1.3, "#b8bdd6", 150)),
                UiKit.card(170, icons[2], UiKit.keys("ESPACIO"), UiKit.text("Dinamita", 2, "#ffffff"),
                        UiKit.centered("¡también rompe tus muros!", 1.3, "#b8bdd6", 150)),
                UiKit.card(170, icons[3], UiKit.keys("ENTER"), UiKit.text("Adelantar", 2, "#ffffff"),
                        UiKit.centered("empieza la oleada · P pausa · N sonido", 1.3, "#b8bdd6", 150)));
    }

    private HBox coopCards() {
        VBox p1 = playerCard("J1 · Ingeniero azul", "#5aa2ff", icons[0],
                new String[]{"W", "A", "S", "D"}, new String[]{"E"}, new String[]{"Q", "F"});
        VBox p2 = playerCard("J2 · Ingeniero ámbar", "#ffa040", icons[1],
                new String[]{"↑", "↓", "←", "→"}, new String[]{"M", "SHIFT"}, new String[]{"ESPACIO", "/"});
        VBox shared = UiKit.card(150, icons[3], UiKit.text("Los dos", 2, "#ffffff"),
                UiKit.keys("ENTER"), UiKit.centered("o TAB: adelantar la oleada", 1.3, "#b8bdd6", 130),
                UiKit.keys("P", "N"), UiKit.centered("pausa · sonido", 1.3, "#b8bdd6", 130));
        return new HBox(12, p1, p2, shared);
    }

    private static VBox playerCard(String title, String color, Canvas icon, String[] move, String[] build, String[] bomb) {
        VBox card = UiKit.card(270, icon, UiKit.text(title, 1.8, color),
                line("Mover", UiKit.keys(move)), line("Muro", UiKit.keys(build)), line("Dinamita", UiKit.keys(bomb)));
        card.setStyle("-fx-border-color: " + color + ";");
        return card;
    }

    private static HBox line(String label, Node keys) {
        Node l = UiKit.text(label, 1.6, "#d8dcef");
        VBox lbox = new VBox(l);
        lbox.setPrefWidth(96);
        lbox.setAlignment(Pos.CENTER_LEFT);
        HBox row = new HBox(8, lbox, keys);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    @Override
    protected void frame(double dt) {
        boolean coop = GameSettings.isCoop();
        GraphicsContext g = clear(icons[0]);
        Art.builder(g, 28, 31, 0.95, Math.sin(time * 0.8) > 0 ? 1 : -1, time, true, false,
                coop ? Art.Outfit.BLUE : Art.Outfit.AMBER);
        if (coop) {
            g = clear(icons[1]);
            Art.builder(g, 28, 31, 0.95, Math.sin(time * 0.8 + 1) > 0 ? 1 : -1, time + 0.7, true, false, Art.Outfit.AMBER);
        } else {
            g = clear(icons[1]);
            double pop = Math.min(1, (time % 2.0) / 0.25);
            g.save();
            g.translate(28 - 20 * pop, 28 - 20 * pop);
            g.scale(pop, pop);
            Art.crate(g, 0, 0, 40);
            g.restore();
            g = clear(icons[2]);
            double cycle = time % 2.4;
            if (cycle < 1.8) {
                Art.dynamite(g, 28, 34, 13, time);
            } else {
                double k = (cycle - 1.8) / 0.6;
                g.setFill(Color.rgb(255, 160, 40, 1 - k));
                g.fillOval(28 - 26 * k - 6, 30 - 26 * k - 6, 52 * k + 12, 52 * k + 12);
            }
        }
        g = clear(icons[3]);
        Art.portal(g, 16, 28, 0.9, time, true);
        Art.invader(g, 40, 30, 22, EnemyType.GRUNT.getColor(), 1, time, true, false);
    }

    private static GraphicsContext clear(Canvas c) {
        GraphicsContext g = c.getGraphicsContext2D();
        g.clearRect(0, 0, c.getWidth(), c.getHeight());
        return g;
    }
}
