package utils;

/**
 * Defines a rectangle that should describe the object it corresponds to. Frames are used to check if a point is within
 * the object it represents and to detect
 */
public class Frame {

    /**
     * Data
     */
    private float[] corners; // contains the four corner vertices in clockwise order, starting from the top-left
    private float r;         // how rotated (in radians) the frame should consider itself to be
    private float cx, cy;    // the center point of the frame

    /**
     * Constructor
     * @param corners the four corners of the frame, x before y, in the following order: top left, lower left, bottom
     *                right, then upper right. These points should be rotated already. That is, passing a non-zero
     *                value to r will not cause this frame to rotate those points. It only uses r to rotate the a point
     *                it is given in the contains method to match up rotations and give accurate detection for rotated
     *                frames
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
        Pair[] rps = new Pair[4]; // create pair array
        for (int i = 0; i < 4; i++) { // for each corner
            rps[i] = Utils.rotatePoint(this.cx, this.cy, this.corners[i * 2], this.corners[i * 2 + 1],
                    -this.r); // un-rotate the corner
        }
        Pair rp = Utils.rotatePoint(this.cx, this.cy, x, y, -this.r); // and then rotate the point
        return (rps[0].x < rp.x && rps[0].y < rp.y && // check top left
                rps[1].x < rp.x && rps[1].y > rp.y && // check bottom left
                rps[2].x > rp.x && rps[2].y > rp.y && // check bottom right
                rps[3].x > rp.x && rps[3].y < rp.y);  // check top right
    }

    /**
     * Translates this entire frame by the given amount. This is useful so that game objects can get their model's frame
     * and then simply translate it by the game object's world position
     * @param x the x to translate by
     * @param y the y to translate by
     * @return the frame after translation has occurred
     */
    public Frame translate(float x, float y) {
        this.cx += x; // translate center point x
        this.cy += y; // translate center point y
        for (int i = 0; i < corners.length; i++) corners[i] += i % 2 == 0 ? x : y; // translate each corner
        return this; // return this, translated
    }

    /**
     * @return the rotation of the Frame
     */
    public float getR() { return this.r; }

    /**
     * @return an array containg the four corners of this frame in clockwise order starting with the bottom-left
     */
    public Pair[] getCorners() {
        Pair[] cs = new Pair[4];
        cs[0] = new Pair(this.corners[0], this.corners[1]);
        cs[1] = new Pair(this.corners[2], this.corners[3]);
        cs[2] = new Pair(this.corners[4], this.corners[5]);
        cs[3] = new Pair(this.corners[6], this.corners[7]);
        return cs;
    }
}
