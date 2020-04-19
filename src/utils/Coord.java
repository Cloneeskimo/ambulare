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
     * Constructs the coordinate with the given x and y values
     * @param x the x of this coordinate
     * @param y the y of this coordinate
     */
    public Coord(float x, float y) {
        this.x = x; // save x
        this.y = y; // save y
    }

    /**
     * Constructs the coordinate at the origin
     */
    public Coord() {
        this(0f, 0f); // call other constructor
    };

    /**
     * Constructs the coordinate using values from another coordinate
     * @param c the other coordinate
     */
    public Coord(Coord c) { this(c.x, c.y); }

    /**
     * Converts the coordinate to a string
     */
    @Override
    public String toString() {
        return "(" + this.x + ", " + this.y + ")"; // return as an ordered pair
    }

    /**
     * Checks if the given coordinate is equal in x and y value to this one
     * @param other the other coordinate to check
     */
    public boolean equals(Coord other) {
        return (other.x == this.x && other.y == this.y); // return true if x and y match
    }
}
