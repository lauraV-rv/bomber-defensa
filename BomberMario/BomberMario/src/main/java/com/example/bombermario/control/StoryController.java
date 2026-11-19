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
 * The illustrated lore, shown after picking a mode: four lines appear one by one,
 * each with a small animated drawing. The text adapts to co-op. Next come the controls.
 */
public class StoryController extends AnimatedScreen {
    private static final double LINE_DELAY = 1.5;

    private static final String[] SOLO = {
            "Eres el **Ingeniero Maestro** de la Ciudadela, guardián del **Núcleo de Energía** que mantiene viva la ciudad.",
            "Desde la Grieta salen los **Autómatas**: siempre buscan el camino más corto para sobrecargar el Núcleo.",
            "Moldea el terreno con **muros** para alargar su marcha. La Ciudadela nunca te dejará cerrar el paso del todo.",
            "Tu **dinamita** los detiene, pero también derriba tus propios muros. Si caes aturdido, el Núcleo gasta "
                    + "**1 de energía** en reconstruirte.",
    };
    private static final String[] COOP = {
            "Son los dos **Ingenieros Maestros** de la Ciudadela: **J1 azul** y **J2 ámbar**, guardianes del Núcleo de Energía.",
            "Desde la Grieta salen los **Autómatas**, ahora en mayor número: buscan el camino más corto hacia el Núcleo.",
            "Compartan **muros** y **dinamita**: uno construye el laberinto mientras el otro vigila la marcha.",
            "¡Cuidado con el **fuego amigo**! La dinamita también aturde a tu compañero, y cada aturdido cuesta "
                    + "**1 de energía** del Núcleo.",
    };

    private final Canvas[] icons = new Canvas[4];
    private final Node[] rows = new Node[4];
    private Button next;

    @Override
    protected MenuBackground.Mode mode() {
        return MenuBackground.Mode.STORY;
    }

    @Override
    protected void build() {
        String[] lines = GameSettings.isCoop() ? COOP : SOLO;
        content.setSpacing(10);
        content.getChildren().addAll(
                UiKit.text("Crónicas de la Ciudadela", 1.6, "#9fa6c8"),
                UiKit.title("La Grieta se ha abierto", 4.5, "#c79cff", "#4a2470"));
        VBox panel = new VBox(14);
        panel.getStyleClass().add("panel");
        panel.setMaxWidth(720);
        VBox.setMargin(panel, new Insets(8, 0, 0, 0));
        for (int i = 0; i < lines.length; i++) {
            icons[i] = new Canvas(80, 76);
            HBox row = new HBox(16, icons[i], UiKit.paragraph(lines[i], 2, "#e3e6f2", 580));
            row.setAlignment(Pos.CENTER_LEFT);
            row.setOpacity(0);
            rows[i] = row;
            panel.getChildren().add(row);
        }
        content.getChildren().add(panel);
        next = UiKit.button("Siguiente ▸", 3, "#1c1e2a", "big-button", () -> go("controls.fxml"));
        next.setDefaultButton(true);
        next.setOpacity(0.4);
        Button skip = UiKit.button("Saltar historia ▸", 1.6, "#ffffff", "sound-button", () -> go("controls.fxml"));
        skip.setCancelButton(true);
        HBox buttons = new HBox(16, skip, next);
        buttons.setAlignment(Pos.CENTER);
        VBox.setMargin(buttons, new Insets(6, 0, 0, 0));
        content.getChildren().add(buttons);
    }

    @Override
    protected void frame(double dt) {
        for (int i = 0; i < rows.length; i++) {
            double k = Math.max(0, Math.min(1, (time - 0.3 - i * LINE_DELAY) / 0.6));
            rows[i].setOpacity(k);
            rows[i].setTranslateX(30 * (1 - k));
        }
        next.setOpacity(Math.max(0.4, Math.min(1, (time - 0.3 - rows.length * LINE_DELAY) / 0.5)));
        boolean coop = GameSettings.isCoop();

        GraphicsContext g = clear(icons[0]);
        if (coop) {
            Art.builder(g, 26, 46, 1.2, 1, time, false, false, Art.Outfit.BLUE);
            Art.builder(g, 56, 48, 1.2, -1, time + 1, false, false, Art.Outfit.AMBER);
        } else {
            Art.builder(g, 40, 46, 1.35, 1, time, false, false);
        }

        g = clear(icons[1]);
        Art.portal(g, 26, 40, 1.0, time, true);
        Art.invader(g, 40 + (time * 18) % 30, 42, 24, EnemyType.GRUNT.getColor(), 1, time, true, false);

        g = clear(icons[2]);
        for (int i = 0; i < 4; i++) {
            double pulse = 0.5 + 0.5 * Math.sin(time * 6 - i * 0.8);
            g.setFill(Color.rgb(220, 140, 255, 0.9));
            double r = 2 + 1.5 * pulse;
            g.fillOval(10 + i * 8 - r, 66 - r, r * 2, r * 2);
        }
        Art.crate(g, 20, 14, 40);

        g = clear(icons[3]);
        double cycle = time % 2.6;
        Art.crate(g, 38, 22, 36);
        if (cycle < 1.9) {
            Art.dynamite(g, 20, 50, 13, time);
        } else {
            double k = (cycle - 1.9) / 0.7;
            g.setFill(Color.rgb(255, 160, 40, 1 - k));
            g.fillOval(20 - 34 * k - 6, 46 - 34 * k - 6, 68 * k + 12, 68 * k + 12);
        }
    }

    private static GraphicsContext clear(Canvas c) {
        GraphicsContext g = c.getGraphicsContext2D();
        g.clearRect(0, 0, c.getWidth(), c.getHeight());
        return g;
    }
}
