package graphics;

import utils.Coord;
import utils.Timer;

/**
 * Contains tools necessary for a GameObject to undergo a positionl animation (one where its position changes)
 * This class stores more variables than necessary to avoid repetitive arithmetic because instantiations of this class
 * won't actually exist for longer than their duration (usually) so the tradeoff of memory for these extraneous variables
 * versus the smoothness they may provide is worth it
 */
public class PositionalAnimation {

    /**
     * Data
     */
    float x0, y0; // starting position of owning GameObject
    float xf, yf; // ending position of owning GameObject
    float dx, dy; // difference between ending and starting position of owning GameObject
    float duration; // duration (in seconds) of the animation
    double time0, timef; // starting and ending time of the animation (in second)

    /**
     * Constructs this PositionalAnimation
     * @param xf the target x
     * @param yf the target y
     * @param duration how long (in seconds) the animation should take
     */
    public PositionalAnimation(float xf, float yf, float duration) {
        this.xf = xf; // save target x
        this.yf = yf; // save target y
        this.duration = duration; // save duration
    }

    /**
     * Starts this PositionalAnimation by beginning the timekeeping and calculating extra variables
     * @param x0 the starting x
     * @param y0 the starting y
     */
    public void start(float x0, float y0) {
        this.x0 = x0; // save starting x
        this.y0 = y0; // save starting y
        this.dx = this.xf - this.x0; // calculate difference in final and starting x
        this.dy = this.yf - this.y0; // calculate difference in final and starting y
        this.time0 = Timer.getTime(); // save starting time (current time)
        this.timef = this.time0 + duration; // calculate end time
    }

    /**
     * Called by the owning GameObject to get an exact position based on how much time has passed since the start of
     * the animation
     * @return a Coord object containing the appropriate x and y values
     */
    public Coord getCurrentPos() {
        float timeFrac = (float)((Timer.getTime() - this.time0) / this.duration); // calculate how far along the animation we are
        return new Coord(this.x0 + this.dx * timeFrac, this.y0 + this.dy * timeFrac); // apply that to the target difference in x and y
    }

    /**
     * @return the target x of this PositionalAnimation
     */
    public float getFinalX() { return this.xf; }

    /**
     * @return the target y of this PositionalAnimation
     */
    public float getFinalY() { return this.yf; }

    /**
     * @return whether this PositionalAnimation is finished
     */
    public boolean finished() { return (Timer.getTime() > this.timef); }
}
