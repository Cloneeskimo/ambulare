package gameobject;

import graphics.Material;
import graphics.Model;
import graphics.PositionalAnimation;
import graphics.ShaderProgram;
import utils.FittingBox;
import utils.Pair;

/**
 * Represents a single game object. This is the basic abstraction away from directly dealing with GL commands
 * These objects have a model and a material both used for rendering. They also have position, velocity, visibility
 * components that are used during rendering to convert this object's model coordinates into object
 * coordinates. They and are able to be given positional animations as well as texture animations, if the underlying
 * model is a model that can have multiple sets of texture coordinates. Game object's coordinates are considered to be
 * world coordinates that represent the center of the game object
 */
public class GameObject {

    /**
     * Animation Members
     */
    protected PositionalAnimation posAnim; // positional animation which can be set to animate positional changes
    protected float frameTime = 0;         // the amount of time (s) an animated texture frame should last
    protected float frameTimeLeft = 0;     // the amount of time (s) left for the current animated texture frame
    protected int frameCount = -1;         // amount of animated texture frames
    protected int frame = 0;               // the current animated texture frame

    /**
     * Other Members
     */
    private float x = 0f, y = 0f;          // position
    protected Material material;           // material to use when rendering
    protected Model model;                 // model to use when rendering
    protected float vx = 0f, vy = 0f;      // velocity
    protected boolean visible = true;      // visibility

    /**
     * Constructs the game object at the default starting position
     *
     * @param model    the model to render
     * @param material the material to use for rendering
     */
    public GameObject(Model model, Material material) {
        this.material = material; // save material as member
        this.model = model; // save model as member
    }

    /**
     * Constructs the game object with a starting position
     *
     * @param x        the x position to place the game object at
     * @param y        the y position to place the game object at
     * @param model    the model to render
     * @param material the material to use for rendering
     */
    public GameObject(float x, float y, Model model, Material material) {
        this(model, material); // call other constructor
        this.x = x; // save x as member
        this.y = y; // save y as member
    }

    /**
     * Updates the game object
     *
     * @param interval the amount of time to account for
     */
    public void update(float interval) {
        if (this.posAnim == null) { // if not in the middle of a positional animation
            if (this.move(this.vx * interval, this.vy * interval)) // move according to velocity and interval
                this.onMove(); // call onMove if an actual move occurred
        } else this.updatePosAnim(interval); // otherwise, update positional animation
    }

    /**
     * Updates the positional animation fo the game object if there is one and sets the position accordingly
     *
     * @param interval the amount of time, in seconds, to account for
     */
    protected void updatePosAnim(float interval) {
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

    /**
     * Renders the game object using the given shader program
     * This function will assume the given shader program is already bound and has the correct set of uniforms to handle
     * rendering a game object (see method for specifics)
     *
     * @param sp the shader program to use to render the game object
     */
    public void render(ShaderProgram sp) {
        if (!this.visible) return; // do not render if invisible
        sp.setUniform("x", this.x); // set x
        sp.setUniform("y", this.y); // set y
        this.material.setUniforms(sp); // set material uniforms
        int texCoordVBO = this.material.getTexCoordVBO(); // get the correct texture coordinate VBO
        this.model.useTexCoordVBO(texCoordVBO); // give the VBO to the model
        this.model.render(); // render model
    }

    /**
     * Moves the game object by the given offset
     *
     * @param dx the x offset
     * @param dy the y offset
     * @return whether or not an actual move occurred
     */
    public boolean move(float dx, float dy) {
        this.x += dx; // update x
        this.y += dy; // update y
        return (dx != 0 || dy != 0); // call onMove() if an actual move occurred
    }

    /**
     * Sets the game object's velocities to 0
     *
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
    protected void onMove() {
    }

    /**
     * Gives the game object a positional animation to undergo immediately
     * See the PositionalAnimation class definition for more details about positional animations
     *
     * @param pa the animation to undergo
     */
    public void givePosAnim(PositionalAnimation pa) {
        this.posAnim = pa; // save animation
        this.posAnim.start(this.getX(), this.getY(), this.getRotationRad()); // start animation
    }

    /**
     * Update's the game object's x position
     *
     * @param x the new x position
     */
    public void setX(float x) {
        this.x = x;
        this.onMove();
    }

    /**
     * Update's the game object's y position
     *
     * @param y the new y position
     */
    public void setY(float y) {
        this.y = y;
        this.onMove();
    }

    /**
     * Updates the game object's horizontal velocity (x velocity)
     *
     * @param vx the new horizontal velocity
     */
    public void setVX(float vx) {
        this.vx = vx;
    }

    /**
     * Updates the game object's vertical velocity
     *
     * @param vy the new vertical velocity
     */
    public void setVY(float vy) {
        this.vy = vy;
    }

    /**
     * Updates the game object's horizontal velocity by adding the given incremental change
     *
     * @param dvx the incremental change
     */
    public void incrementVX(float dvx) {
        this.vx += dvx;
    }

    /**
     * Updates the game object's vertical velocity by adding the given incremental change
     *
     * @param dvy the incremental change
     */
    public void incrementVY(float dvy) {
        this.vy += dvy;
    }

    /**
     * Sets the x scaling factor of the game object to the given x scaling factor
     *
     * @param x the x scaling factor to use
     */
    public void setXScale(float x) {
        this.model.setXScale(x); // scale model
        this.onMove(); // call on move
    }

    /**
     * Sets the y scaling factor of the game object to the given y scaling factor
     *
     * @param y the y scaling factor to use
     */
    public void setYScale(float y) {
        this.model.setYScale(y); // scale model
        this.onMove(); // call on move
    }

    /**
     * Updates the position of the game object
     *
     * @param x the new x
     * @param y the new y
     */
    public void setPos(float x, float y) {
        this.x = x; // save x
        this.y = y; // save y
        this.onMove(); // call on move
    }

    /**
     * Updates the position of the game object
     *
     * @param pos the new position
     */
    public void setPos(Pair<Float> pos) {
        this.setPos(pos.x, pos.y);
    }

    /**
     * Sets the scaling factors of the game object to the given scaling factors
     *
     * @param x the x scaling factor to use
     * @param y the y scaling factor to use
     */
    public void setScale(float x, float y) {
        this.model.setScale(x, y); // tell model to scale
        this.onMove(); // call on move
    }

    /**
     * Updates the rotation of the game object
     *
     * @param r the new rotation value in degrees
     */
    public void setRotDeg(float r) {
        this.setRotRad((float) Math.toRadians(r)); // convert to radians and call other method
    }

    /**
     * Updates the rotation of the game object
     *
     * @param r the new rotation value in radians
     */
    public void setRotRad(float r) {
        this.model.setRotationRad(r); // rotate model
        this.onMove(); // call on-move
    }

    /**
     * Sets the visibility flag of the game object
     *
     * @param v the new value of the visibility flag
     */
    public void setVisibility(boolean v) {
        this.visible = v;
    }

    /**
     * Updates the game object's material
     *
     * @param m the new material to use
     */
    public void setMaterial(Material m) {
        this.material = m;
    }

    /**
     * @return whether the game object is currently undergoing a positional animation
     */
    public boolean posAnimating() {
        return this.posAnim != null;
    }

    /**
     * @return the game object's x position
     */
    public float getX() {
        return this.x;
    }

    /**
     * @return the game object's y position
     */
    public float getY() {
        return this.y;
    }

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
     * @return the game object's horizontal velocity
     */
    public float getVX() {
        return this.vx;
    }

    /**
     * @return the game object's vertical velocity
     */
    public float getVY() {
        return this.vy;
    }

    /**
     * @return the game object's rotation in radians
     */
    public float getRotationRad() {
        return this.model.getRotationRad();
    }

    /**
     * @return the material used to render the game object
     */
    public Material getMaterial() {
        return this.material;
    }

    /**
     * Calculates the fitting box for the game object by getting the model's fitting box and translating it to the
     * game object's position. If the game object's model is rectangular, it will create a perfectly-sized and rotated
     * fitting box. Otherwise, it will create the smallest possible fitting box that can house all vertices
     *
     * @return the fitting box describe above
     */
    public FittingBox getFittingBox() {
        return model.getFittingBox().translate(this.x, this.y); // get model's fitting box and translate
    }

    /**
     * Cleans up the game object
     */
    public void cleanup() {
        this.model.cleanup(); // cleanup model
    }
}
