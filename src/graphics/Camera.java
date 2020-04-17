package graphics;

import gameobject.GameObject;

/**
 * Stimulates a camera by maintaining a position, velocity, and zoom for the view
 */
public class Camera {

    /**
     * Data
     */
    private final static float MIN_ZOOM = 0.05f; // minimum zoom
    private final static float MAX_ZOOM = 1.8f; // maximum zoom
    private final static float DEFAULT_ZOOM = 0.7f; // default zoom
    private float x, y, vx, vy; // position and velocity
    private float zoom; // zoom
    private GameObject following; // a GameObject to follow

    /**
     * Constructs this Camera
     * @param x the world x of this Camera
     * @param y the world y of this Camera
     * @param zoom the zoom of this Camera - will be bounded by MIN_ZOOM and MAX_ZOOM defined above
     */
    public Camera(float x, float y, float zoom) {
        this.x = x; // set x
        this.y = y; // set y
        this.zoom = Math.max(Camera.MIN_ZOOM, Math.min(Camera.MAX_ZOOM, zoom)); // set zoom bounded by min and max
    }

    /**
     * Constructs this Camera with the default zoom
     * @param x the world x of this Camera
     * @param y the world y of this Camera
     */
    public Camera(float x, float y) { this(x, y, Camera.DEFAULT_ZOOM); } // call other constructor

    /**
     * Constructs this Camera at world pos (0, 0) and with the default zoom
     */
    public Camera() { this(0f, 0f); } // call other constructor

    /**
     * Updates this Camera
     */
    public void update() {
        if (this.following == null) { // if not following anything
            this.x += this.vx; // update x world pos based on velocity
            this.y += this.vy; // update y world pos based on velocity
        } else { // if following something
            this.x = this.following.getX(); // set x to followed object's x
            this.y = this.following.getY(); // set y to followed object's y
        }
    }

    /**
     * Assigns a GameObject for this Camera to follow
     * @param o the GameObject to follow. If null, won't follow anything
     */
    public void follow(GameObject o) { this.following = o; }

    /**
     * @return this Camera's world x
     */
    public float getX() { return this.x; }

    /**
     * @return this Camera's world y
     */
    public float getY() { return this.y; }

    /**
     * @return this Camera's world zoom
     */
    public float getZoom() { return this.zoom; }

    /**
     * Sets this Camera's zoom to the given zoom, or the minimum or maximum bound that it surpasses
     * @param z the value to attempt to set this Camera's zoom too
     */
    public void setZoom(float z) { this.zoom = Math.min(Camera.MAX_ZOOM, Math.max(Camera.MIN_ZOOM, z)); }

    /**
     * Change's this Camera's soon by the given magnitude in a multiplicative manner
     */
    public void zoom(float dz) { this.setZoom(this.zoom * dz); }
}
