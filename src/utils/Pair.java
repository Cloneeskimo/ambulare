package utils;

/**
 * Contains a pair of floats. Can represent a coordinate, vector, etc.
 */
public class Pair {

    /**
     * Data
     */
    public float x, y; // the two values

    /**
     * Constructs the pair with the given values
     * @param x the first in this pair
     * @param y the second in this pair
     */
    public Pair(float x, float y) {
        this.x = x; // save first value
        this.y = y; // save second value
    }

    /**
     * Constructs this pair with values (0f, 0f)
     */
    public Pair() {
        this(0f, 0f); // call other constructor
    };

    /**
     * Constructs the pair using values from another pair
     * @param p the other coordinate
     */
    public Pair(Pair p) { this(p.x, p.y); }

    /**
     * Converts the pair into a string
     */
    @Override
    public String toString() {
        return "(" + this.x + ", " + this.y + ")"; // return as an ordered pair
    }

    /**
     * Checks if the given pair has equal values to another pair
     * @param p the other pair to check
     */
    public boolean equals(Pair p) {
        return (p.x == this.x && p.y == this.y); // return true if x and y match
    }
}
