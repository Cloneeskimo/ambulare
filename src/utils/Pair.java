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
     * Converts a given string to an integer pair. The string should be two floats separated by a space. For example,
     * "5 6" is a valid input while "5f 10A" is not
     *
     * @param s the string to convert to a float pair
     * @return the converted pair or null if the string was formatted incorrecetly
     */
    public static Pair<Integer> strToIntegerPair(String s) {
        String[] components = s.split(" "); // split by spaces
        if (components.length != 2) return null; // if incorrect length, return null
        try { // try to create a float pair from tokens
            return new Pair<>(Integer.parseInt(components[0]), Integer.parseInt(components[1]));
        } catch (Exception e) { // if exception occurs
            Utils.handleException(e, Utils.class, "strToIntegerPair", false); // log exception but don't crash
            return null; // and return null
        }
    }

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
     * @return the pair turned into a sstring
     */
    @Override
    public String toString() {
        return "(" + this.x + ", " + this.y + ")";
    }

    /**
     * Converts the pair into a string with the given digits to display after the decimal place
     * @param digits the amount of digits to display after the decimal place
     * @return the pair turned into a string displaying the given amount of decimal places
     */
    public String toString(int digits) {
        String s = Integer.toString(digits); // convert decimal place count into string
        // format and return final product
        return "(" + String.format("%." + s + "f", this.x) + ", " + String.format("%." + s + "f", this.y) + ")";
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
