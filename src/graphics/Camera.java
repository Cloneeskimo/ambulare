package graphics;

import gameobject.GameObject;

/**
 * Stimulates a camera by maintaining a position, velocity, and zoom for the view
 */
public class Camera {

    /**
     * Members
     */
    private float x, y, vx, vy;                      // position and velocity
    private float zoom;                              // zoom
    private GameObject following;                    // a game object to follow, if assigned
    public final static float DEFAULT_ZOOM = 0.2f;   // default zoom
    public final static float MIN_ZOOM = 0.02f;      // minimum zoom
    public final static float MAX_ZOOM = 1.2f;       // maximum zoom

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
     */
    public void update() {
        if (this.following == null) { // if not following anything
            // update world position based on velocity
            this.x += this.vx;
            this.y += this.vy;
        } else { // if following something
            // set position to the followed object's position
            this.x = this.following.getX();
            this.y = this.following.getY();
        }
    }

    /**
     * Assigns a game object for the camera to follow
     *
     * @param o the game object to follow. If null, won't follow anything
     */
    public void follow(GameObject o) {
        this.following = o;
    }

    /**
     * Sets the zoom to the given zoom, or the minimum/maximum bound that it surpasses
     *
     * @param z the value to set it to
     */
    public void setZoom(float z) {
        this.zoom = Math.min(Camera.MAX_ZOOM, Math.max(Camera.MIN_ZOOM, z));
    }

    /**
     * Change the zoom by the given magnitude in a multiplicative manner
     */
    public void zoom(float dz) {
        this.setZoom(this.zoom * dz);
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
     * Get the camera's zoom as a normalized linear function of a given zoom scroll factor
     * @return the camera's zoom in the format described above
     */
    public float getLinearZoom(float factor) {
        float minz = (float)(Math.log(MIN_ZOOM) / Math.log(factor));
        float z = (float)(Math.log(this.zoom) / Math.log(factor)) - minz;
        float mz = (float)(Math.log(MAX_ZOOM) / Math.log(factor)) - minz;
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
