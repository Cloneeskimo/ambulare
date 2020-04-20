package utils;

/**
 * A class used to check for intersection of two areas, or an area and a point. Instantiations of this class also hold
 * a rotation member so that their checks for intersection can take rotation into account
 */
public class Bounds {

    /**
     * Data
     */
    private float[] limits = new float[4]; /* the limits of these bounds defined as follows:
                                              [0] - lower x, [1] - lower y, [2] - upper x, [3] - upper y */
    private float cx, cy; // center point
    private float rot; // rotation in radians

    /**
     * Constructor
     * @param cx the center point x value
     * @param cy the center point y value
     * @param w the width to make the bounds
     * @param h the height to make the bounds
     * @param rot the rotation to take into account (in radians)
     */
    public Bounds(float cx, float cy, float w, float h, float rot) {
        float w2 = w / 2, h2 = h / 2; // divide width and height in half for calculation of corners
        this.limits[0] = cx - w2; // calculate lower x bound
        this.limits[1] = cy - h2; // calculate lower y bound
        this.limits[2] = cx + w2; // calculate upper x bound
        this.limits[3] = cy + h2; // calculate upper y bound
        this.cx = cx; // save center point x
        this.cy = cy; // save center point y
        this.rot = rot; // save rotation
    }

    /**
     * Checks if a given point is within these bounds
     * @param x the x of the point to check
     * @param y the y of the point to check
     * @return whether or not the given point is within these bounds
     */
    public boolean contains(float x, float y) {
        Coord r = Utils.rotatePoint(cx, cy, x, y, -rot); /* if the object these bounds are rotated A degrees, the given
                                                            point must be rotated -A degrees relative to the center
                                                            point of these bounds to take rotation into account */
        return (r.x > this.limits[0] && r.x < this.limits[2] &&
                r.y > this.limits[1] && r.y < this.limits[3]); // if rotated point is within bounds, return true
    }

    /**
     * Checks if a given bounds overlaps these bounds
     * @param other the bounds to check
     * @return whether the given bounds overlaps these bounds
     */
    public boolean overlaps(Bounds other) {
        return this.hasCornerIn(other) || other.hasCornerIn(this); /* if either bounds have a corner in the other,
                                                                            then they overlap */
    }

    /**
     * Checks if these bounds have a point that is contained by the given bounds
     * @param other the other bounds to consider
     * @return whether these bounds have a point that the other bounds contain
     */
    public boolean hasCornerIn(Bounds other) {
        Coord[] rps = new Coord[4]; // create array to hold rotated points
        rps[0] = Utils.rotatePoint(cx, cy, limits[0], limits[1], rot); // rotate lower left corner
        rps[1] = Utils.rotatePoint(cx, cy, limits[0], limits[3], rot); // rotate upper left corner
        rps[2] = Utils.rotatePoint(cx, cy, limits[2], limits[1], rot); // rotate lower right corner
        rps[3] = Utils.rotatePoint(cx, cy, limits[2], limits[3], rot); // rotate upper right corner
        for (Coord c : rps) if (other.contains(c.x, c.y)) return true; // check if any rotated corner is on other bounds
        return false; // if not, return false
    }
}
