package gameobject;

import graphics.*;
import utils.FittingBox;
import utils.Pair;

/**
 * Represents a single game object. This is the basic abstraction away from directly dealing with GL commands
 * These objects have a model and a material both used for rendering. They also have position and visibility
 * components that are used during rendering. They and are able to be given positional animations. Game object's
 * coordinates are considered to represent the center of the game object
 */
public class GameObject {

    /**
     * Other Members
     */
    private float x = 0f, y = 0f;          // position
    protected PositionalAnimation posAnim; // positional animation which can be set to animate positional changes
    protected Material material;           // material to use when rendering
    protected Model model;                 // model to use when rendering
    protected boolean visible = true;      // visibility

    /**
     * Constructs the game object at the default starting position
     *
     * @param model    the model to render
     * @param material the material to use for rendering
     */
    public GameObject(Model model, Material material) {
        this.material = material;
        this.model = model;
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
        this(model, material);
        this.x = x;
        this.y = y;
    }

    /**
     * Updates the game object
     *
     * @param interval the amount of time to account for
     */
    public void update(float interval) {
        if (this.posAnimating()) this.updatePosAnim(interval); // update positional animation
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
        this.onMove(); // call onMove() to signify that a move was made
        if (this.posAnim.finished()) { // if animation is over
            this.x = this.posAnim.getFinalX(); // make sure at the correct ending x
            this.y = this.posAnim.getFinalY(); // make sure at the correct ending y
            this.setRotRad(this.posAnim.getFinalR()); // make sure at the correct ending rotation
            this.onMove(); // call onMove() to signify that a move was made
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
        // update material's light's position if it is a light source material
        if (this.material instanceof LightSourceMaterial) ((LightSourceMaterial) this.material).setPos(this.x, this.y);
        this.material.setUniforms(sp); // set material uniforms
        Texture t = this.material.getTexture(); // get material's texture
        // if the texture is animated, tell the model with texture coordinates to use
        if (t instanceof AnimatedTexture) this.model.useTexCoordVBO(((AnimatedTexture) t).getTexCoordVBO(), false);
        this.model.render(); // render model
    }

    /**
     * This is called whenever there is a change in the game object's position, scale, or rotation. The point is for
     * extending classes to be able to override this in order to react to a changes in position, scale, or rotation
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
     * Sets the x scaling factor of the game object to the given x scaling factor by scaling the model. Note that if
     * other game objects share a model with this game object, they will be scaled as well
     *
     * @param x the x scaling factor to use
     */
    public void setXScale(float x) {
        this.model.setXScale(x); // scale model
        this.onMove(); // call on move
    }

    /**
     * Sets the y scaling factor of the game object to the given y scaling factor by scaling the model. Note that if
     * other game objects share a model with this game object, they will be scaled as well
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
     * Stops the game object from completing its current positional animation
     */
    public void stopPosAnimating() {
        this.posAnim = null;
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
     * @return the game object's (model's) width
     */
    public float getWidth() {
        return this.model.getWidth();
    }

    /**
     * @return the game object's (model's) width
     */
    public float getHeight() {
        return this.model.getHeight();
    }

    /**
     * @return the game object's (model's) width without taking rotation into consideration
     */
    public float getUnrotatedWidth() {
        return this.model.getUnrotatedWidth();
    }

    /**
     * @return the game object's (model's) height without taking rotation into consideration
     */
    public float getUnrotatedHeight() {
        return this.model.getUnrotatedHeight();
    }

    /**
     * @return the game object's (model's) rotation in radians
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
     * game object's position.
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
        this.material.cleanup(); // cleanup material
    }
}
