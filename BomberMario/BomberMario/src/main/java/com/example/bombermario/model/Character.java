package com.example.bombermario.model;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

/**
 * Base class for everything that walks on the board. The position is the
 * character's center in canvas pixels.
 */
public abstract class Character {
    /** The center of the character, in pixels. */
    public Position position;
    /** The canvas the character is drawn on. */
    public Canvas canvas;
    /** The graphics context of the canvas. */
    public GraphicsContext graphicsContext;

    /**
     * Instantiates a new Character.
     *
     * @param canvas the canvas
     */
    public Character(Canvas canvas) {
        this.canvas = canvas;
        this.graphicsContext = canvas.getGraphicsContext2D();
    }

    /**
     * Draws the character.
     */
    public abstract void paint();

    /**
     * Gets the board column the character's center is in.
     *
     * @return the column
     */
    public int getCol() {
        return Board.colOf(position.getX());
    }

    /**
     * Gets the board row the character's center is in.
     *
     * @return the row
     */
    public int getRow() {
        return Board.rowOf(position.getY());
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public void setCanvas(Canvas canvas) {
        this.canvas = canvas;
    }

    public GraphicsContext getGraphicsContext() {
        return graphicsContext;
    }

    public void setGraphicsContext(GraphicsContext graphicsContext) {
        this.graphicsContext = graphicsContext;
    }
}
