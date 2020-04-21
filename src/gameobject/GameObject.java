package gameobject;

import graphics.Material;
import graphics.Model;
import graphics.PositionalAnimation;
import graphics.ShaderProgram;
import utils.Frame;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.glActiveTexture;

/**
 * Represents a single game object. This is the basic abstraction away from directly dealing with GL commands
 * These objects have a model and a material both used for rendering. They also have position, velocity, visibility
 * components that are used during rendering to convert this object's model coordinates into object
 * coordinates. They and are able to be given positional animations to undergo. Their coordinates are considered to be
 * world coordinates
 */
public class GameObject {

    /**
     * Data
     */
    private PositionalAnimation posAnim; // positional animation which can be set to animate positional changes
    private float x = 0f, y = 0f;        // position
    protected Material material;         // material to use when rendering
    protected Model model;               // model to use when rendering
    protected float vx = 0f, vy = 0f;    // velocity
    protected boolean visible = true;    // visibility

    /**
     * Constructs the game object at the default starting position
     * @param model the model to render
     * @param material the material to use for rendering
     */
    public GameObject(Model model, Material material) {
        this.material = material; // save material as member
        this.model = model; // save model as member
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
        this.x = x; // save x as member
        this.y = y; // save y as member
    }

    /**
     * Updates the game object
     * @param interval the amount of time to account for
     */
    public void update(float interval) {
        if (this.posAnim == null) { // if not in the middle of a positional animation
            this.x += this.vx * interval; // update x position based on velocity
            this.y += this.vy * interval; // update y position based on velocity
            if (this.vx != 0f || this.vy != 0f) this.onMove(); // if actual movement, call onMove()
        } else { // if in the middle of a positional animation
            this.posAnim.update(interval); // update animation
            this.x = this.posAnim.getX(); // set x position
            this.y = this.posAnim.getY(); // set y position
            this.setRotRad(this.posAnim.getR()); // set rotation
            this.onMove(); // call onMove()
            if (this.posAnim.finished()) { // if animation is over
                this.x = this.posAnim.getFinalX(); // make sure at the correct ending x
                this.y = this.posAnim.getFinalY(); // make sure at the correct ending y
                this.setRotRad(this.posAnim.getFinalR()); // make sure at the correct ending rotation
                this.posAnim = null; // delete the animation
            }
        }
    }

    /**
     * Renders the game object using the given shader program
     * This function will assume the given shader program is already bound and has the correct set of uniforms to handle
     * rendering a game object (see method for specifics)
     * @param sp the shader program to use to render the game object
     */
    public void render(ShaderProgram sp) {
        if (!this.visible) return; // do not render if invisible
        if (this.material.isTextured()) { // if the object's material is textured
            sp.setUniform("isTextured", 1); // set textured flag to true
            glActiveTexture(GL_TEXTURE0); // set active texture to one in slot 0
            glBindTexture(GL_TEXTURE_2D, this.material.getTexture().getID()); // bind texture
        } else sp.setUniform("isTextured", 0); // set textured flag to false otherwise
        if (this.material.isColored()) { // if the object's material is colored
            float[] color = this.material.getColor(); // and get color instead
            sp.setUniform("color", color[0], color[1], color[2], color[3]); // set color uniform
        }
        Material.BLEND_MODE bm = this.material.getBlendMode(); // get blend mode of the object's material
        sp.setUniform("blend", bm == Material.BLEND_MODE.NONE ? 0 : (bm == Material.BLEND_MODE.MULTIPLICATIVE ? 1
                : 2)); // set blend uniform
        sp.setUniform("x", this.x); // set x
        sp.setUniform("y", this.y); // set y
        this.model.render(); // render model
    }

    /**
     * Sets the game object's velocities to 0
     * @param stopPosAnim whether or not to stop any positional animation that may be happening
     */
    public void stop(boolean stopPosAnim) {
        this.vx = this.vy = 0; // reset velocities
        if (stopPosAnim) this.posAnim = null; // delete positional animation if flag set
    }

    /**
     * This is called whenever there is a change in the game object's position. The point is for extending classes to be
     * able to override this in order to react to a changes in position
     */
    protected void onMove() {}

    /**
     * Gives the game object a positional animation to undergo immediately
     * See the PositionalAnimation class definition for more details about positional animations
     * @param pa the animation to undergo
     */
    public void givePosAnim(PositionalAnimation pa) {
        this.posAnim = pa; // save animation
        this.posAnim.start(this.getX(), this.getY(), this.getRotationRad()); // start animation
    }

    /**
     * Update's the game object's x position
     * @param x the new x position
     */
    public void setX(float x) { this.x = x; this.onMove(); }

    /**
     * Update's the game object's y position
     * @param y the new y position
     */
    public void setY(float y) { this.y = y; this.onMove(); }

    /**
     * Updates the game object's horizontal velocity (x velocity)
     * @param vx the new horizontal velocity
     */
    public void setVX(float vx) { this.vx = vx; }

    /**
     * Updates the game object's vertical velocity
     * @param vy the new vertical velocity
     */
    public void setVY(float vy) { this.vy = vy; }

    /**
     * Updates the game object's horizontal velocity by adding the given incremental change
     * @param dvx the incremental change
     */
    public void incrementVX(float dvx) { this.vx += dvx; }

    /**
     * Updates the game object's vertical velocity by adding the given incremental change
     * @param dvy the incremental change
     */
    public void incrementVY(float dvy) { this.vy += dvy; }

    /**
     * Sets the x scaling factor of the game object to the given x scaling factor
     * @param x the x scaling factor to use
     */
    public void setXScale(float x) {
        this.model.setXScale(x); // scale model
        this.onMove(); // call on move
    }

    /**
     * Sets the y scaling factor of the game object to the given y scaling factor
     * @param y the y scaling factor to use
     */
    public void setYScale(float y) {
        this.model.setYScale(y); // scale model
        this.onMove(); // call on move
    }

    /**
     * Updates the position of the game object
     * @param x the new x
     * @param y the new y
     */
    public void setPos(float x, float y) {
        this.x = x; // save x
        this.y = y; // save y
        this.onMove(); // call on move
    }

    /**
     * Sets the scaling factors of the game object to the given scaling factors
     * @param x the x scaling factor to use
     * @param y the y scaling factor to use
     */
    public void setScale(float x, float y) {
        this.model.setScale(x, y); // tell model to scale
        this.onMove(); // call on move
    }

    /**
     * Updates the rotation of the game object
     * @param r the new rotation value in degrees
     */
    public void setRotDeg(float r) {
        this.setRotRad((float)Math.toRadians(r)); // convert to radians and call other method
    }

    /**
     * Updates the rotation of the game object
     * @param r the new rotation value in radians
     */
    public void setRotRad(float r) {
        this.model.setRotationRad(r); // rotate model
        this.onMove(); // call on-move
    }

    /**
     * Sets the visibility flag of the game object
     * @param v the new value of the visibility flag
     */
    public void setVisibility(boolean v) { this.visible = v; }

    /**
     * @return whether the game object is currently undergoing a positional animation
     */
    public boolean posAnimating() { return this.posAnim != null; }

    /**
     * @return the game object's x position
     */
    public float getX() { return this.x; }

    /**
     * @return the game object's y position
     */
    public float getY() { return this.y; }

    /**
     * @return the game object's width
     */
    public float getWidth() {
        return this.model.getWidth(); // return model width
    }

    /**
     * @return the game object's height
     */
    public float getHeight() {
        return this.model.getHeight(); // return model's height
    }

    /**
     * @return the game object's width without taking rotation into consideration
     */
    public float getUnrotatedWidth() {
        return this.model.getUnrotatedWidth(); // return unrotated model width
    }

    /**
     * @return the game object's height without taking rotation into consideration
     */
    public float getUnrotatedHeight() {
        return this.model.getUnrotatedHeight(); // return model's unrotated height
    }

    /**
     * @return the game object's rotation in radians
     */
    public float getRotationRad() { return this.model.getRotationRad(); }

    /**
     * Calculates the frame for the game object by getting the model's frame and translating it to the game object's
     * position
     * @return the frame corresponding to the game object
     */
    public Frame getFrame() {
        return model.getFrame().translate(this.x, this.y); // get model's frame and translate
    }

    /**
     * Cleans up the game object
     */
    public void cleanup() {
        this.model.cleanup(); // cleanup model
        this.material.cleanup(); // cleanup material
    }
}
