package utils;

/*
 * Pair.java
 * Ambulare
 * Jacob Oaks
 * 4/21/20
 */

/**
 * Contains a pair of objects
 */
public class Pair<T> {

    /**
     * Members
     */
    public T x, y; // the two values

    /**
     * Constructs the pair with the given values
     *
     * @param x the first in this pair
     * @param y the second in this pair
     */
    public Pair(T x, T y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Constructs this pair with values (0f, 0f)
     */
    public Pair() {
        this(null, null);
    }

    /**
     * Constructs the pair using values from another pair
     *
     * @param p the other coordinate
     */
    public Pair(Pair<T> p) {
        this(p.x, p.y);
    }

    /**
     * Converts the pair into a string
     */
    @Override
    public String toString() {
        return "(" + this.x + ", " + this.y + ")";
    }

    /**
     * Checks if the given pair has equal values to another pair
     *
     * @param p the other pair to check
     */
    public boolean equals(Pair<T> p) {
        return (p.x == this.x && p.y == this.y);
    }
}
