package utils;

/**
 * Defines a rectangle that should describe the object it corresponds to. Bounding boxes can have rotation or be
 * axis-aligned. These are used for collision and mouse input
 */
public class BoundingBox {

    /**
     * Members
     */
    private float[] corners; // contains the four corner vertices in clockwise order, starting from the bottom left
    private float r;         // how rotated (in radians) the bounding box is
    private float cx, cy;    // the center point of the box (should be perfectly centered within the corners)

    /**
     * Constructor
     * @param corners the four corners of the bounding box, x before y, in the following order: bottom left, top left,
     *                top right, then bottom right. These points should be rotated already. That is, passing a
     *                non-zero value to r will not cause the bounding box to rotate the points
     * @param r how rotated the bounding box is
     * @param cx the center point x
     * @param cy the center point y
     */
    public BoundingBox(float[] corners, float r, float cx, float cy) {
        if (corners.length != 8) // if invalid amount of corners
            Utils.handleException(new Exception("Invalid corners given for BoundingBox. Length should be 8, is " +
                    "actually " + corners.length), "BoundingBox", "BoundingBox(float[]", true); // throw exception
        this.corners = corners;
        this.r = r;
        this.cx = cx;
        this.cy = cy;
    }

    /**
     * Checks if the bounding box contains the given point
     * @param x the x of the point to check
     * @param y the y of the point to check
     * @return whether the bounding box contains the given point
     */
    public boolean contains(float x, float y) {
        Pair[] rps = new Pair[4]; // create pair array
        for (int i = 0; i < 4; i++) { // for each corner
            if (this.r != 0) rps[i] = Utils.rotatePoint(this.cx, this.cy, this.corners[i * 2], this.corners[i * 2 + 1],
                    -this.r); // un-rotate the corner
            else rps[i] = new Pair(this.corners[i * 2], this.corners[i * 2 + 1]); // don't do rot calc if not rotated
        }
        Pair rp = Utils.rotatePoint(this.cx, this.cy, x, y, -this.r); // and then rotate the point
        return (rps[0].x < rp.x && rps[0].y < rp.y && // check bottom left
                rps[1].x < rp.x && rps[1].y > rp.y && // check top left
                rps[2].x > rp.x && rps[2].y > rp.y && // check top right
                rps[3].x > rp.x && rps[3].y < rp.y);  // check bottom right
    }

    /**
     * Checks if the bounding box contains the given point
     * @param pos the point to check for
     * @return whether the bounding box contains the given point
     */
    public boolean contains(Pair pos) {
        return this.contains(pos.x, pos.y); // call other method with pair deconstructed
    }

    /**
     * Translates this entire bounding box by the given amount.
     * @param x the x to translate by
     * @param y the y to translate by
     * @return the bounding box after translation has occurred
     */
    public BoundingBox translate(float x, float y) {
        // translate center point
        this.cx += x;
        this.cy += y;
        for (int i = 0; i < corners.length; i++) corners[i] += i % 2 == 0 ? x : y; // translate each corner
        return this;
    }

    /**
     * @return an array containing the four corners of the bounding box in the order in which they are stored: clockwise
     * starting from the bottom-left
     */
    public Pair[] getCorners() {
        Pair[] cs = new Pair[4];
        cs[0] = new Pair(this.corners[0], this.corners[1]);
        cs[1] = new Pair(this.corners[2], this.corners[3]);
        cs[2] = new Pair(this.corners[4], this.corners[5]);
        cs[3] = new Pair(this.corners[6], this.corners[7]);
        return cs;
    }

    /**
     * @return the rotation of the bounding box
     */
    public float getR() { return this.r; }
}
