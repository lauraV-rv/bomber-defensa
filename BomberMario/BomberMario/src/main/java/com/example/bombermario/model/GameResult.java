package com.example.bombermario.model;

/**
 * Stats of the last finished game, shown on the victory / defeat screens.
 */
public final class GameResult {
    private static int players = 1;
    private static int waveReached;
    private static int totalWaves;
    private static int coreLeft;
    private static int coreMax;
    private static double seconds;
    private static int[] kills = new int[2];
    private static int[] walls = new int[2];
    private static int[] knockouts = new int[2];

    private GameResult() {
    }

    /**
     * Stores the stats of a finished game.
     *
     * @param players     1 or 2
     * @param waveReached last wave played
     * @param totalWaves  number of waves in the game
     * @param coreLeft    core energy left
     * @param coreMax     core energy at the start
     * @param seconds     length of the game
     * @param kills       automatons defeated, per player
     * @param walls       walls built, per player
     * @param knockouts   times knocked out, per player
     */
    public static void save(int players, int waveReached, int totalWaves, int coreLeft, int coreMax, double seconds,
                            int[] kills, int[] walls, int[] knockouts) {
        GameResult.players = players;
        GameResult.waveReached = waveReached;
        GameResult.totalWaves = totalWaves;
        GameResult.coreLeft = coreLeft;
        GameResult.coreMax = coreMax;
        GameResult.seconds = seconds;
        GameResult.kills = kills.clone();
        GameResult.walls = walls.clone();
        GameResult.knockouts = knockouts.clone();
    }

    public static int getPlayers() {
        return players;
    }

    public static int getWaveReached() {
        return waveReached;
    }

    public static int getTotalWaves() {
        return totalWaves;
    }

    public static int getCoreLeft() {
        return coreLeft;
    }

    public static int getCoreMax() {
        return coreMax;
    }

    /** @return the game length as m:ss */
    public static String getTime() {
        int total = (int) Math.round(seconds);
        return (total / 60) + ":" + String.format("%02d", total % 60);
    }

    /** @param p player index @return automatons that player defeated */
    public static int getKills(int p) {
        return kills[p];
    }

    /** @param p player index @return walls that player built */
    public static int getWalls(int p) {
        return walls[p];
    }

    /** @param p player index @return times that player was knocked out */
    public static int getKnockouts(int p) {
        return knockouts[p];
    }

    /** @return automatons defeated by the whole team */
    public static int getTotalKills() {
        return kills[0] + kills[1];
    }

    /** @return walls built by the whole team */
    public static int getTotalWalls() {
        return walls[0] + walls[1];
    }

    /** @return knock-outs of the whole team */
    public static int getTotalKnockouts() {
        return knockouts[0] + knockouts[1];
    }
}
