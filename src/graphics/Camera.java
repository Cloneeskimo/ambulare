package graphics;

import gameobject.GameObject;

/**
 * Stimulates a camera by maintaining a position, velocity, and zoom for the view
 */
public class Camera {

    /**
     * Data
     */
    private final static float MIN_ZOOM = 0.05f;    // minimum zoom
    private final static float MAX_ZOOM = 1.8f;     // maximum zoom
    private final static float DEFAULT_ZOOM = 0.7f; // default zoom
    private float x, y, vx, vy;                     // position and velocity
    private float zoom;                             // zoom
    private GameObject following;                   // a game object to follow, if assigned

    /**
     * Constructs the camera with a specified zoom
     * @param x the x
     * @param y the y
     * @param zoom the zoom of this camera - will be bounded by MIN_ZOOM and MAX_ZOOM defined above
     */
    public Camera(float x, float y, float zoom) {
        this.x = x; // set x
        this.y = y; // set y
        this.zoom = Math.max(Camera.MIN_ZOOM, Math.min(Camera.MAX_ZOOM, zoom)); // set zoom bounded by min and max
    }

    /**
     * Constructs the camera with the default zoom
     * @param x the x
     * @param y the y
     */
    public Camera(float x, float y) { this(x, y, Camera.DEFAULT_ZOOM); } // call other constructor

    /**
     * Constructs the camera at pos (0, 0) and with the default zoom
     */
    public Camera() { this(0f, 0f); } // call other constructor

    /**
     * Updates the camera
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
     * Assigns a game object for the camera to follow
     * @param o the game object to follow. If null, won't follow anything
     */
    public void follow(GameObject o) { this.following = o; }

    /**
     * Sets the zoom to the given zoom, or the minimum/maximum bound that it surpasses
     * @param z the value to set it to
     */
    public void setZoom(float z) { this.zoom = Math.min(Camera.MAX_ZOOM, Math.max(Camera.MIN_ZOOM, z)); }

    /**
     * Change the zoom by the given magnitude in a multiplicative manner
     */
    public void zoom(float dz) { this.setZoom(this.zoom * dz); }

    /**
     * @return the camera's x
     */
    public float getX() { return this.x; }

    /**
     * @return the camera's y
     */
    public float getY() { return this.y; }

    /**
     * @return the camera's zoom
     */
    public float getZoom() { return this.zoom; }
}
