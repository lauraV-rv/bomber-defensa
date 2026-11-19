package com.example.bombermario.model;

/**
 * Kinds of blocks on the board.
 */
public enum TypeBox {
    /** Rubble that was on the map from the start. Bombs break it and it drops building material. */
    DESTROYABLE,
    /** A wall the player built. Bombs break it too, and nothing is given back. */
    BUILT,
    /** Border walls and fixed pillars. */
    NON_DESTROYABLE,
    /** The core the player defends (lives in the right border). */
    CORE,
    /** A portal the invaders come out of (lives in the left border). */
    PORTAL
}
