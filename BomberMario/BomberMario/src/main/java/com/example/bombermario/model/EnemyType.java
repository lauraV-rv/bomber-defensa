package com.example.bombermario.model;

import javafx.scene.paint.Color;

/**
 * The kinds of automatons from the Rift (about 28% slower than the first version).
 */
public enum EnemyType {
    /** Plain automaton: average speed, one hit. */
    GRUNT("Raso", 36, 1, 1, 30, Color.rgb(150, 80, 210)),
    /** Fast and fragile. */
    RUNNER("Veloz", 60, 1, 1, 24, Color.rgb(40, 200, 170)),
    /** Slow, takes three hits and hurts the core a lot. */
    BRUTE("Bruto", 25, 3, 3, 38, Color.rgb(210, 60, 60));

    private final String displayName;
    private final double speed;
    private final int health;
    private final int coreDamage;
    private final double size;
    private final Color color;

    EnemyType(String displayName, double speed, int health, int coreDamage, double size, Color color) {
        this.displayName = displayName;
        this.speed = speed;
        this.health = health;
        this.coreDamage = coreDamage;
        this.size = size;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** @return walking speed in pixels per second */
    public double getSpeed() {
        return speed;
    }

    /** @return bomb hits needed to defeat it */
    public int getHealth() {
        return health;
    }

    /** @return core energy it removes when it reaches the core */
    public int getCoreDamage() {
        return coreDamage;
    }

    /** @return body size in pixels */
    public double getSize() {
        return size;
    }

    /** @return body color */
    public Color getColor() {
        return color;
    }
}
