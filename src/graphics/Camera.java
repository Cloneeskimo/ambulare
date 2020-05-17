package graphics;

import gameobject.GameObject;
import utils.Global;
import utils.Pair;
import utils.PhysicsEngine;
import utils.Transformation;

/*
 * ROC.java
 * Ambulare
 * Jacob Oaks
 * 4/16/20
 */

/**
 * Stimulates a camera by maintaining a position, velocity, and zoom for the view
 */
public class Camera {

    /**
     * Static Data
     */
    public static final float ZOOM_SENSITIVITY = 0.02f; /* how sensitive aesthetic zooms are to their parameters. Any
        zoom acceleration given in the aestheticZoom() method will be multiplied by this factor */
    public static final float ZOOM_ACCELERATION_FRICTION = 0.5f; /* how much friction to apply to zoom acceleration. In
        other words, how long should zoom acceleration last? Zoom acceleration is multiplied by this every update */
    public static final float ZOOM_VELOCITY_FRICTION = 0.9f; /* how much friction to apply to zoom velocity. In other
        words, given a lack of acceleration, how long should zoom velocity last? Zoom velocity is multiplied by this
        every update */

    /**
     * Members
     */
    private float x, y, vx, vy;                      // position and velocity
    private float zoom, vz, az;                      // zoom, zoom velocity, zoom acceleration
    private GameObject following;                    // a game object to follow, if assigned
    public final static float DEFAULT_ZOOM = 0.3f;   // default zoom
    public final static float MIN_ZOOM = 0.15f;      // minimum zoom
    public final static float MAX_ZOOM = 0.6f;       // maximum zoom

    /**
     * Constructs the camera with a specified zoom
     *
     * @param x    the x
     * @param y    the y
     * @param zoom the zoom of this camera - will be bounded by MIN_ZOOM and MAX_ZOOM defined above
     */
    public Camera(float x, float y, float zoom) {
        this.x = x;
        this.y = y;
        this.zoom = Math.max(Camera.MIN_ZOOM, Math.min(Camera.MAX_ZOOM, zoom)); // set zoom bounded by min and max
    }

    /**
     * Constructs the camera with the default zoom
     *
     * @param x the x
     * @param y the y
     */
    public Camera(float x, float y) {
        this(x, y, Camera.DEFAULT_ZOOM);
    }

    /**
     * Constructs the camera at pos (0, 0) and with the default zoom
     */
    public Camera() {
        this(0f, 0f);
    }

    /**
     * Updates the camera
     *
     * @param interval the amount of time (in seconds) to account for
     */
    public void update(float interval) {
        if (this.following == null) { // if not following anything
            // update world position based on velocity and interval
            this.x += (this.vx * interval);
            this.y += (this.vy * interval);
        } else { // if following something
            // set position to the followed object's position
            this.x = this.following.getX();
            this.y = this.following.getY();
        }
        this.setZoom(this.zoom * (this.vz + 1)); // update zoom based on zoom velocity
        this.vz += this.az; // update velocity based on zoom acceleration
        this.az *= ZOOM_ACCELERATION_FRICTION; // apply constant friction to zoom acceleration
        this.vz *= ZOOM_VELOCITY_FRICTION; // apply constant friction to zoom velocity
    }

    /**
     * Assigns a game object for the camera to follow
     *
     * @param o the game object to follow. If null, won't follow anything
     */
    public void follow(GameObject o) {
        this.following = o; // save reference to followed object as member
    }

    /**
     * Sets the zoom to the given zoom, or the minimum/maximum bound that it surpasses
     *
     * @param z the value to set it to
     */
    public void setZoom(float z) {
        this.zoom = Math.min(Camera.MAX_ZOOM, Math.max(Camera.MIN_ZOOM, z));
        Global.debugInfo.setField("zoom", Float.toString(this.zoom));
    }

    /**
     * Change the zoom using an aesthetic zoom based on treating the given value as a zoom acceleration
     *
     * @param za the zoom acceleration
     */
    public void aestheticZoom(float za) {
        this.az = (za - 1) * ZOOM_SENSITIVITY;
    }

    /**
     * Updates the camera's position
     *
     * @param x the new x
     * @param y the new y
     */
    public void setPos(float x, float y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Update the camera's x (horizontal) velocity
     */
    public void setVX(float vx) {
        this.vx = vx;
    }

    /**
     * Update the camera's y (vertical) velocity
     */
    public void setVY(float vy) {
        this.vy = vy;
    }

    /**
     * @return an axis-aligned bounding box defining the camera's view
     */
    public PhysicsEngine.AABB getView() {
        // get the width/height of the bounding box based off zoom and aspect ratio
        Pair<Float> size = Transformation.deaspect(new Pair<>(2 / this.zoom, 2 / this.zoom), Global.ar);
        return new PhysicsEngine.AABB(this.x, this.y, size.x, size.y); // create and return bounding box
    }

    /**
     * Get the camera's zoom as a normalized linear function of a given zoom scroll factor
     *
     * @return the camera's zoom in the format described above
     */
    public float getLinearZoom(float factor) {
        float minz = (float) (Math.log(MIN_ZOOM) / Math.log(factor));
        float z = (float) (Math.log(this.zoom) / Math.log(factor)) - minz;
        float mz = (float) (Math.log(MAX_ZOOM) / Math.log(factor)) - minz;
        return z / mz;
    }

    /**
     * @return the camera's x
     */
    public float getX() {
        return this.x;
    }

    /**
     * @return the camera's y
     */
    public float getY() {
        return this.y;
    }

    /**
     * @return the camera's zoom
     */
    public float getZoom() {
        return this.zoom;
    }
}
