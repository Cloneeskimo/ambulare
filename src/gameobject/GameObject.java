package gameobject;

import graphics.Material;
import graphics.Model;
import graphics.PositionalAnimation;
import graphics.ShaderProgram;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.glActiveTexture;

/**
 * Represents a single game object. This is the basic abstraction away from directly dealing with GL commands
 * These objects have a model and a material both used for rendering. They also have position, velocity, scaling, and
 * visibility components that are used during rendering to convert this object's model coordinates into object
 * coordinates. They and are able to be given positional animations to undergo
 */
public class GameObject {

    /**
     * Data
     */
    private float x = 0f, y = 0f; // position
    private float sx = 1f, sy = 1f; // x scale and y scale
    private float rot = 0f; // rotation - in radians
    protected boolean visible = true; // visibility
    protected float vx = 0f, vy = 0f; // velocity
    protected Model model; // model to use when rendering
    protected Material material; // material to use when rendering
    protected PositionalAnimation posAnim; // positional animation which can be set to animate positional changes


    /**
     * Constructs the game object at the default starting position (see above)
     * @param model the model to render
     * @param material the material to use for rendering
     */
    public GameObject(Model model, Material material) {
        this.model = model;
        this.material = material;
    }

    /**
     * Constructs the game object with a starting position
     * @param x the x position to place the game object at
     * @param y the y position to place the game object at
     * @param model the model to render
     * @param material the material to use for rendering
     */
    public GameObject(float x, float y, Model model, Material material) {
        this(model, material); // call other constructor
        this.x = x; // set x
        this.y = y; // set y
    }

    /**
     * Updates this game object
     * @param interval the amount of time to account for
     */
    public void update(float interval) {
        if (this.posAnim == null) { // if not in the middle of a positional animation
            this.x += this.vx * interval; // update x position
            this.y += this.vy * interval; // update y position
            if (this.vx != 0f || this.vy != 0f) this.onMove(); // if actual movement, call onMove()
        } else { // if in the middle of a positional animation
            this.posAnim.update(interval); // update animation
            this.x = this.posAnim.getX(); // set x position
            this.y = this.posAnim.getY(); // set y position
            this.rot = this.posAnim.getR(); // set rotation
            this.onMove(); // call onMove()
            if (this.posAnim.finished()) { // if animation is over
                this.x = this.posAnim.getFinalX(); // make sure at the correct ending x
                this.y = this.posAnim.getFinalY(); // make sure at the correct ending y
                this.rot = this.posAnim.getFinalR(); // make sure at the correct ending rotation
                this.posAnim = null; // delete the animation
            }
        }
    }

    /**
     * This is called whenever x, y, rotation, or scale is changed. The point is for extending classes to be able to
     * override this in order to react to a changes in position/rotation/scale
     */
    protected void onMove() {}

    /**
     * Renders this game object using the given shader program
     * This function will assume the given shader program is already bound and has the correct set of uniforms to handle
     * rendering a game object (see method for specifics)
     * @param sp the shader program to use to render this game object
     */
    public void render(ShaderProgram sp) {
        if (!this.visible) return; // do not render if invisible
        if (this.material.isTextured()) { // if this object's material is textured
            sp.setUniform("isTextured", 1); // set textured flag to true
            glActiveTexture(GL_TEXTURE0); // set active texture to one in slot 0
            glBindTexture(GL_TEXTURE_2D, this.material.getTexture().getID()); // bind texture
        } else sp.setUniform("isTextured", 0); // set textured flag to false otherwise
        if (this.material.isColored()) { // if this object's material is colored
            float[] color = this.material.getColor(); // and get color instead
            sp.setUniform("color", color[0], color[1], color[2], color[3]); // set color uniform
        }
        Material.BLEND_MODE bm = this.material.getBlendMode(); // get blend mode of this object's Material
        sp.setUniform("blend", bm == Material.BLEND_MODE.NONE ? 0 : (bm == Material.BLEND_MODE.MULTIPLICATIVE ? 1
                : 2)); // set blend uniform
        sp.setUniform("x", this.x); // set x
        sp.setUniform("y", this.y); // set y
        sp.setUniform("scaleX", this.sx); // set x scaling
        sp.setUniform("scaleY", this.sy); // set y scaling
        sp.setUniform("rot", this.rot); // set rotation
        this.model.render(); // render model
    }

    /**
     * Gives this game object a positional animation to undergo immediately
     * See the PositionalAnimation class definition for more details about positional animations
     * @param pa the animation to undergo
     */
    public void givePosAnim(PositionalAnimation pa) {
        this.posAnim = pa; // save animation
        this.posAnim.start(this.getX(), this.getY(), this.getRot()); // start animation
    }

    /**
     * @return whether this game object is currently undergoing a positional animation
     */
    public boolean posAnimating() { return this.posAnim != null; }

    /**
     * @return this game object's x
     */
    public float getX() { return this.x; }

    /**
     * @return this game object's width
     */
    public float getWidth() {
        return this.sy * (float)(this.model.getWidth()  * Math.abs(Math.cos(rot)) +
                                 this.model.getHeight() * Math.abs(Math.sin(rot))); // take rotation into account
    }

    /**
     * @return this game object's width without taking rotation into consideration
     */
    public float getUnrotatedWidth() {
        return this.sx * this.model.getWidth();
    }

    /**
     * Update's this game object's x position
     * @param x the new x position
     */
    public void setX(float x) { this.x = x; this.onMove(); }

    /**
     * Updates this game object's horizontal velocity (x velocity)
     * @param vx the new horizontal velocity
     */
    public void setVX(float vx) { this.vx = vx; }

    /**
     * Updates this game object's horizontal velocity by adding the given incremental change
     * @param dvx the incremental change
     */
    public void incrementVX(float dvx) { this.vx += dvx; }

    /**
     * Updates this game object's horizontal scaling
     * @param sx the new horizontal scaling
     */
    public void setScaleX(float sx) { this.sx = sx; this.onMove(); }

    /**
     * @return this game object's y position
     */
    public float getY() { return this.y; }

    /**
     * @return this game object's height
     */
    public float getHeight() {
        return this.sy * (float)(this.model.getHeight() * Math.abs(Math.cos(rot)) +
                                 this.model.getWidth()  * Math.abs(Math.sin(rot))); // take rotation into account
    }

    /**
     * @return this game object's height without taking rotation into consideration
     */
    public float getUnrotatedHeight() {
        return this.sy * this.model.getHeight();
    }

    /**
     * Update's this game object's y position
     * @param y the new y position
     */
    public void setY(float y) { this.y = y; this.onMove(); }

    /**
     * Updates this game object's vertical velocity
     * @param vy the new vertical velocity
     */
    public void setVY(float vy) { this.vy = vy; }

    /**
     * Updates this game object's vertical velocity by adding the given incremental change
     * @param dvy the incremental change
     */
    public void incrementVY(float dvy) { this.vy += dvy; }

    /**
     * Updates this game object's vertical scaling
     * @param sy the new horizontal scaling
     */
    public void setScaleY(float sy) { this.sy = sy; this.onMove(); }

    /**
     * Updates this game object's horizontal and vertical scaling
     * @param s the new scaling
     */
    public void setScale(float s) { this.sx = this.sy = s; this.onMove(); }

    /**
     * Updates the position of this game object
     * @param x the new x
     * @param y the new y
     */
    public void setPos(float x, float y) { this.x = x; this.y = y; this.onMove(); }

    /**
     * @return this game object's rotation in radians
     */
    public float getRot() { return this.rot; }

    /**
     * Updates the rotation of this game object
     * @param rot the new rotation value in degrees
     */
    public void setRotDeg(float rot) { this.rot = (float)Math.toRadians(rot % 360); this.onMove(); }

    /**
     * Updates the rotation of this game object
     * @param rot the new rotation value in radians
     */
    public void setRotRad(float rot) { this.rot = rot % (float)(Math.PI * 2); this.onMove(); }

    /**
     * Sets the visibility flag of this game object
     * @param v the new value of the visibility flag
     */
    public void setVisibility(boolean v) { this.visible = v; }

    /**
     * Sets the game object's velocities to 0
     * @param stopPosAnim whether or not to stop any positional animation that may be happening
     */
    public void stop(boolean stopPosAnim) {
        this.vx = this.vy = 0; // reset velocities
        if (stopPosAnim) this.posAnim = null; // delete positional animation if flag set
    }

    /**
     * Cleans up the game object
     */
    public void cleanup() {
        this.model.cleanup(); // cleanup model
        this.material.cleanup(); // cleanup material
    }
}
