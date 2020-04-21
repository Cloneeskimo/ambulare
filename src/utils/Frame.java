package utils;

/**
 * Defines a rectangle that should describe the object it corresponds to. Frames can be used for things such as checking
 * if the mouse is hovering over an object, or if the object is colliding with another object
 */
public class Frame {

    /**
     * Data
     */
    float[] corners; // contains the four corner vertices of the frame: top left, lower left, bottom right, upper right
    float r;         // how rotated (in radians) the frame should consider itself to be
    float cx, cy;    // the center point of the frame

    /**
     * Constructor
     * @param corners the four corners of the frame, x before y, in the following order: top left, lower left, bottom
     *                right, then upper right
     * @param r how rotated the frame should consider itself to be
     * @param cx the center point x
     * @param cy the center point y
     */
    public Frame(float[] corners, float r, float cx, float cy) {
        if (corners.length != 8) // if invalid amount of corners
            Utils.handleException(new Exception("Invalid corners given for Frame. Length should be 8, is actually " +
                    corners.length), "Frame", "Frame(float[]", true); // throw exception
        this.corners = corners; // save corners as member
        this.r = r; // save rotation as member
        this.cx = cx; // save center point x as member
        this.cy = cy; // save center point y as member
    }

    /**
     * Checks if the frame contains the given point
     * @param x the x of the point to check
     * @param y the y of the point to check
     * @return whether the frame contains the given point
     */
    public boolean contains(float x, float y) {
        Pair rp = Utils.rotatePoint(this.cx, this.cy, x, y, this.r); // rotate the point to align with the frame
        x = rp.x; y = rp.y; // reset x and y to rotated points
        return (corners[0] < x && corners[1] > y && // check top left
                corners[2] < x && corners[3] < y && // check bottom left
                corners[4] > x && corners[5] < y && // check bottom right
                corners[6] > x && corners[7] > y);  // check top right
    }

    /**
     * Translates this entire frame by the given amount. This is useful so that game objects can get their model's frame
     * and then simply translate it by the game object's world position
     * @param x the x to translate by
     * @param y the y to translate by
     * @return the frame after translation has occured
     */
    public Frame translate(float x, float y) {
        this.cx += x; // translate center point x
        this.cy += y; // translate center point y
        for (int i = 0; i < corners.length; i++) corners[i] += i % 2 == 0 ? x : y; // translate each corner
        return this; // return this, translated
    }
}
