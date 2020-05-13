package utils;

/*
 * Timer.java
 * Ambulare
 * Jacob Oaks
 * 4/15/20
 */

/**
 * Keeps track of time and allows for easily getting elapsed time for things like game loops
 */
public class Timer {

    /**
     * Members
     */
    private double timestamp; // latest timestamp

    /**
     * Initializes the timer
     */
    public void init() {
        timestamp = getTimeSeconds(); // start timestamp at current time
    }

    /**
     * @return the current time in seconds
     */
    public static double getTimeSeconds() {
        return System.nanoTime() / 1000_000_000.0; // convert ns to s
    }

    /**
     * @return the current time in milliseconds
     */
    public static double getTimeMilliseconds() {
        return (double) System.nanoTime() / 1_000_000.0; // convert ns to ms
    }

    /**
     * @return the current timestamp of the timer
     */
    public double getTimestamp() {
        return this.timestamp;
    }

    /**
     * Calculates the elapsed time since the last timestamp (in seconds)
     *
     * @param mark whether to overwrite the timestamp with the current time
     * @return the amount of elapsed time since the timestamp in seconds
     */
    public float getElapsedTime(boolean mark) {
        double time = getTimeSeconds(); // get current time
        float et = (float) (time - this.timestamp); // get elapsed time
        if (mark) this.timestamp = time; // record new time if param set to true
        return et; // return calculated elapsed time
    }
}
