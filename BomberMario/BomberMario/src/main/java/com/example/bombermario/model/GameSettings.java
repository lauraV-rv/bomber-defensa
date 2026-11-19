package com.example.bombermario.model;

/**
 * Choices made on the title screen that the rest of the game reads.
 */
public final class GameSettings {
    private static int players = 1;

    private GameSettings() {
    }

    /** @return 1 for solo, 2 for local co-op */
    public static int getPlayers() {
        return players;
    }

    /** @param value 1 for solo, 2 for local co-op */
    public static void setPlayers(int value) {
        players = value == 2 ? 2 : 1;
    }

    /** @return true in local co-op */
    public static boolean isCoop() {
        return players == 2;
    }
}
