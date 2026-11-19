package com.example.bombermario.control;

/**
 * Defeat screen: the core fell. Shows the stats and lets the players try again.
 */
public class GameOverController extends EndController {
    @Override
    protected boolean won() {
        return false;
    }
}
