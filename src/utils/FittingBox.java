package utils;

/*
 * FittingBox.java
 * Ambulare
 * Jacob Oaks
 * 4/24/20
 */

/**
 * A fitting box is defined by four corners, a center point, and a rotation value. These can be used to fit objects
 * more precisely than an axis-aligned bounding box. They are not used for collision detection, but they can and should
 * be used to for checking if a specific point is within the object they describe. This is particularly useful for
 * mouse input detection
 */
public class FittingBox {

    /**
     * Members
     */
    private float[] corners; // contains the four corner vertices in clockwise order, starting from the bottom left
    private float r;         // how rotated (in radians) the fitting box is
    private float cx, cy;    // the center point of the fitting box (should be perfectly centered within the corners)

    /**
     * Constructor
     *
     * @param corners the four corners of the bounding box, x before y, in the following order: bottom left, top left,
     *                top right, then bottom right. These points should be rotated already. That is, passing a
     *                non-zero value to r will not cause the bounding box to rotate the points
     * @param r       how rotated the bounding box is
     * @param cx      the center point x
     * @param cy      the center point y
     */
    public FittingBox(float[] corners, float r, float cx, float cy) {
        if (corners.length != 8) // if invalid amount of corners
            Utils.handleException(new Exception("Invalid corners given for BoundingBox. Length should be 8, is " +
                    "actually " + corners.length), "BoundingBox", "BoundingBox(float[]", true); // throw exception
        this.corners = corners;
        this.r = r;
        this.cx = cx;
        this.cy = cy;
    }

    /**
     * Checks if the fitting box contains the given point
     *
     * @param x the x of the point to check
     * @param y the y of the point to check
     * @return whether the fitting box contains the given point
     */
    public boolean contains(float x, float y) {
        // create pair array
        Pair<Float>[] rps = new Pair[4];
        for (int i = 0; i < 4; i++) { // for each corner
            if (this.r != 0) rps[i] = Utils.rotatePoint(this.cx, this.cy, this.corners[i * 2], this.corners[i * 2 + 1],
                    -this.r); // un-rotate the corner
            else rps[i] = new Pair(this.corners[i * 2], this.corners[i * 2 + 1]); // don't do rot calc if not rotated
        }
        Pair<Float> rp = Utils.rotatePoint(this.cx, this.cy, x, y, -this.r); // and then rotate the point
        return (rps[0].x < rp.x && rps[0].y < rp.y && // check bottom left
                rps[1].x < rp.x && rps[1].y > rp.y && // check top left
                rps[2].x > rp.x && rps[2].y > rp.y && // check top right
                rps[3].x > rp.x && rps[3].y < rp.y);  // check bottom right
    }

    /**
     * Checks if the fitting box contains the given point
     *
     * @param pos the point to check for
     * @return whether the fitting box contains the given point
     */
    public boolean contains(Pair<Float> pos) {
        return this.contains(pos.x, pos.y); // call other method with pair deconstructed
    }

    /**
     * Translates the entire fitting box by the given amount.
     *
     * @param x the x to translate by
     * @param y the y to translate by
     * @return the fitting box after translation has occurred
     */
    public FittingBox translate(float x, float y) {
        // translate center point
        this.cx += x;
        this.cy += y;
        for (int i = 0; i < corners.length; i++) corners[i] += i % 2 == 0 ? x : y; // translate each corner
        return this;
    }
}
