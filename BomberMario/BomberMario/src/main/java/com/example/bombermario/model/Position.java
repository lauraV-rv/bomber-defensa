package com.example.bombermario.model;

/**
 * The type Position.
 */
public class Position {

    private double x, y;

    /**
     * Instantiates a new Position.
     *
     * @param x the x
     * @param y the y
     */
    public Position(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Gets x.
     *
     * @return the x
     */
    public double getX() {
        return x;
    }

    /**
     * Sets x.
     *
     * @param x the x
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * Gets y.
     *
     * @return the y
     */
    public double getY() {
        return y;
    }

    /**
     * Sets y.
     *
     * @param y the y
     */
    public void setY(double y) {
        this.y = y;
    }

    /**
     * Add x.
     *
     * @param x the x
     */
    public void addX(int x){
        this.x+=x;
    }

    /**
     * Add y.
     *
     * @param y the y
     */
    public void addY(int y){
        this.y+=y;
    }

    /**
     * Distance double.
     *
     * @param other the other
     * @return the double
     */
    public double distance(Position other){
        return Math.sqrt(Math.pow(this.x - other.getX(), 2) + Math.pow(this.y - other.getY(), 2));
    }

    /**
     * Normalize.
     */
    public void normalize() {
        double length = Math.sqrt(x * x + y * y);
        if (length != 0) {
            x /= length;
            y /= length;
        }
    }

    /**
     * Set speed.
     *
     * @param speed the speed
     */
    public void setSpeed(int speed){
        x *= speed;
        y *= speed;
    }
}
