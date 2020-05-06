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
        timestamp = getTime(); // start timestamp at current time
    }

    /**
     * @return the current time in seconds
     */
    public static double getTime() {
        return System.nanoTime() / 1000_000_000.0; // convert ns to s
    }

    /**
     * @return the current timestamp of the timer
     */
    public double getTimestamp() {
        return this.timestamp;
    }

    /**
     * Calculates the elapsed time since the last timestamp
     *
     * @param mark whether to overwrite the timestamp with the current time
     * @return the amount of elapsed time since the timestamp in seconds
     */
    public float getElapsedTime(boolean mark) {
        double time = getTime(); // get current time
        float et = (float) (time - this.timestamp); // get elapsed time
        if (mark) this.timestamp = time; // record new time if param set to true
        return et; // return calculated elapsed time
    }
}
