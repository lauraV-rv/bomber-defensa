package com.example.bombermario.control;

/**
 * Victory screen: all waves survived. Shows the stats and lets the players try again.
 */
public class GoalController extends EndController {
    @Override
    protected boolean won() {
        return true;
    }
}
