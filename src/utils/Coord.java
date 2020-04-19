package utils;

/**
 * Represents a two-dimensional coordinate
 * Note that these coordinates are not bound to any singular system. They are just sometimes useful for returning both
 * an x and a y from a method, or handling both at the same time for any other arbitrary purpose.
 */
public class Coord {

    /**
     * Data
     */
    public float x, y; // x and y

    /**
     * Constructor
     * @param x the x of this coordinate
     * @param y the y of this coordinate
     */
    public Coord(float x, float y) {
        this.x = x; // save x
        this.y = y; // save y
    }
}
