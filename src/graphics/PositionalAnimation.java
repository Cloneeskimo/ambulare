package graphics;

/*
 * PositionalAnimation.java
 * Ambulare
 * Jacob Oaks
 * 4/18/20
 */

/**
 * Contains tools necessary for an object to undergo a positional animation (one where its position changes). This class
 * stores more variables than necessary to avoid repetitive arithmetic because instantiations of this class shouldn't
 * actually exist for longer than their duration (usually) so the tradeoff of memory for these extraneous variables
 * versus the smoothness they may provide is worth it
 */
public class PositionalAnimation {

    /**
     * Members
     */
    float x0, y0, r0;                     // starting position and rotation of owning game object
    float xf, yf, rf;                     // ending position and rotation of owning game object
    float dx, dy, dr;                     // difference between end and start pos and rotation of owning game object
    float duration;                       // duration (in seconds) of the animation
    float animProg;                       // animation progress (0-1)
    float time;                           // current amount of time that animating has occurred (in seconds)
    boolean x = true, y = true, r = true; // flags representing which components are being animated

    /**
     * Constructs this animation
     *
     * @param xf       the target x - if null, will not change x
     * @param yf       the target y - if null, will not change y
     * @param rf       the target rotation in degrees - if null, will not change rotation. Rotation can be given in
     *                 numbers
     *                 much greater than 360 and it will calculate the amount of rotation needed to get through all
     *                 of the
     *                 given degrees, while maintaining an actual rotation value < 360f in the owning game object.
     * @param duration how long (in seconds) the animation should take
     */
    public PositionalAnimation(Float xf, Float yf, Float rf, float duration) {
        if (xf != null) this.xf = xf;
        else this.x = false; // if x is null, disable x animating
        if (yf != null) this.yf = yf;
        else this.y = false; // if y is null, disable y animating
        if (rf != null) this.rf = (float) Math.toRadians(rf);
        else this.r = false; // if r is null, disable rotation animating
        this.duration = duration;
    }

    /**
     * Starts the animation by beginning the timekeeping and calculating extra variables
     *
     * @param x0 the starting x
     * @param y0 the starting y
     * @param r0 the starting rotation
     */
    public void start(float x0, float y0, float r0) {
        this.x0 = x0; // save starting x
        this.y0 = y0; // save starting y
        this.r0 = r0; // save starting rotation
        if (x) this.dx = this.xf - this.x0; // calculate difference in final and starting x if enabled
        else xf = x0; // if disabled, target x is starting x
        if (y) this.dy = this.yf - this.y0; // calculate difference in final and starting y if enabled
        else yf = y0; // if disabled, target y is starting y
        if (r) this.dr = this.rf - this.r0; // calculate difference in final and starting rotation if enabled
        else rf = r0; // if disabled, target rotation is starting rotation
    }

    /**
     * Updates the animation by keeping track of time for accurate calculations in the getter methods. It's important
     * to call this or the animation will not be timed correctly
     *
     * @param interval the amount of time to account for
     */
    public void update(float interval) {
        this.time += interval; // account for time
        this.animProg = this.time / this.duration; // calculate how far along the animation with new time
    }

    /**
     * @return the position animation's total assigned duration
     */
    public float getDuration() {
        return this.duration;
    }

    /**
     * @return the correct x for the owning game object based on how much time has passed
     */
    public float getX() {
        return this.x0 + this.dx * this.animProg; // starting position plus how far along the difference animation is
    }

    /**
     * @return the correct y for the owning game object based on how much time has passed
     */
    public float getY() {
        return this.y0 + this.dy * this.animProg; // starting position plus how far along the difference animation is
    }

    /**
     * @return the correct rotation for the owning game object based on how much time has passed
     */
    public float getR() {
        // starting position plus how far along the total difference the animation is
        return (this.r0 + this.dr * this.animProg) % (float) (2 * Math.PI);
    }

    /**
     * @return the target x of the animation
     */
    public float getFinalX() {
        return this.xf;
    }

    /**
     * @return the target y of the animation
     */
    public float getFinalY() {
        return this.yf;
    }

    /**
     * @return the target rotation of the animation
     */
    public float getFinalR() {
        return this.rf % (float) (2 * Math.PI);
    }

    /**
     * @return whether the animation has finished
     */
    public boolean finished() {
        return (this.animProg >= 1.0f);
    }
}
