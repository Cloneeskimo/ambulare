package utils;

/**
 * Used by GameEngine to keep track of timing for loop calculations
 */
public class Timer {

    /**
     * Data
     */
    private double lastLoop; // time of the last loop

    /**
     * Initializes the timer
     */
    public void init() { lastLoop = getTime(); } // set last loop time to current time

    /**
     * @return the current time in seconds
     */
    public static double getTime() { return System.nanoTime() / 1000_000_000.0; } // calculate seconds from nano seconds

    /**
     * @return the time of the last loop or the time that this Timer was initialized (whichever was later)
     */
    public double getLastLoop() { return this.lastLoop; }

    /**
     * @return the amount of elapsed time since lastLoop (either when this timer was initialized, or the last time
     * this function was called) in seconds
     */
    public float getElapsedTime() {
        double time = getTime(); // get current time
        float et = (float)(time - this.lastLoop); // get elapsed time
        this.lastLoop = time; // record new time
        return et; // return calculated elapsed time
    }
}
