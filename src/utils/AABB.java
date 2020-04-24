package utils;

/**
 * Axis-aligned bounding boxes can be used for collision. The goal is to have them generally fit the object they are
 * trying to describe. They are defined by a center point and a half-width and half-height. They purpose of having this
 * extra level instead of collision detection just using the object's center-point and width/height is to allow game
 * objects to modify the width/height of their AABB, for example, if they know that their texture won't fit the whole
 * model
 */
public class AABB {

    /**
     * Members
     */
    private float cx, cy; // center point
    private float w2, h2; // half-width and half-height

    /**
     * Constructor
     * @param cx the center-point x
     * @param cy the center-point y
     * @param w the full width of the object
     * @param h the full height of the object
     */
    public AABB(float cx, float cy, float w, float h) {
        this.cx = cx;
        this.cy = cy;
        this.w2 = w / 2;
        this.h2 = h / 2;
    }

    /**
     * @return the center-point x of the axis-aligned bounding box
     */
    public float getCX() { return this.cx; }

    /**
     * @return the center-point y of the axis-aligned bounding box
     */
    public float getCY() { return this.cy; }

    /**
     * @return the half-widths of the axis-aligned bounding box
     */
    public float getW2() { return this.w2; }

    /**
     * @return the half-height of the axis-aligned bounding box
     */
    public float getH2() { return this.h2; }
}
