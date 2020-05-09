package gameobject.gameworld;

import gameobject.ui.TextObject;
import graphics.*;
import utils.Global;
import utils.MouseInputEngine;
import utils.PhysicsEngine;

/*
 * Entity.java
 * Ambulare
 * Jacob Oaks
 * 4/27/2020
 */

/**
 * Extends world objects by directly interacting with a multi-state animated texture in order to allow different
 * animations for different actions an entity might do. It is not imperative that an entity have an MSAT, however.
 * Entities also have a name and a nameplate display their name above them
 */
public class Entity extends WorldObject implements MouseInputEngine.MouseInteractive {

    /**
     * Static Data
     */
    public static final float NAMEPLATE_PADDING = 0.1f; // padding between an entity and its nameplate

    /**
     * Members
     */
    private MouseInputEngine.MouseCallback[] mcs = new MouseInputEngine.MouseCallback[4]; /* array of mouse callbacks as
        specified by the mouse interaction interface */
    private String name;          // name of the entity
    private TextObject nameplate; // nameplate to display above entity
    private boolean right;        // whether the entity is facing to the right
    private boolean airborne;     // whether the entity is airborne
    private boolean moving;       // whether the entity is moving

    /**
     * Constructor
     *
     * @param name     the name of the entity
     * @param model    the model to use
     * @param material the material to use. This model's texture should be an MSAT with six states representing the
     *                 following: (0) entity non-airborne and facing to the left, (1) entity non-airborne and facing to
     *                 the right, (2) entity airborne and facing to the left, (3) entity airborne and facing to the
     *                 right, (4) entity non-airborne and moving to the left, (5) entity non-airborne and moving to the
     *                 right
     */
    public Entity(String name, Model model, Material material) {
        super(model, material); // call world object constructor
        this.name = name; // save name as member
        this.nameplate = new TextObject(Global.FONT, this.name); // create nameplate
        this.nameplate.setScale(2.5f, 2.5f); // make nameplate larger
        this.positionNameplate(); // position the nameplate above the entity
    }

    /**
     * Correctly positions the nameplate of the entity
     */
    private void positionNameplate() {
        this.nameplate.setPos(this.getX(), this.getY() + this.getHeight() / 2 + this.nameplate.getHeight() / 2 +
                NAMEPLATE_PADDING); // put above entity
    }

    /**
     * Tells the entity which way to face
     *
     * @param right whether the entity should face right (false makes the entity face left)
     */
    public void setFacing(boolean right) {
        if (right != this.right) {
            this.right = right; // update flag
            this.updateTextureState(); // update MSAT state
        }
    }

    /**
     * Updates the entity's MSAT based on what actions the entity is taking
     */
    private void updateTextureState() {
        if (this.material.getTexture() instanceof MSAT) { // if the material's texture is even an MSAT
            MSAT t = (MSAT) this.material.getTexture(); // get an MSAT reference to it
            t.setState(airborne ? 1 : this.moving ? 2 : 0); // set state based on airborne and moving flags
        }
    }

    /**
     * Updates the entity's moving flag which will update the animation
     *
     * @param moving the new moving flag
     */
    public void setIsMoving(boolean moving) {
        if (moving != this.moving) { // if moving changed
            this.moving = moving; // update moving flag
            this.updateTextureState(); // and update state in MSAT
        }
    }

    /**
     * Updates the entity
     *
     * @param interval the amount of time to account for
     */
    @Override
    public void update(float interval) {
        super.update(interval);
        boolean airborne = !PhysicsEngine.nextTo(this, 0f, -1f); // check if airborne
        if (airborne != this.airborne) { // if airborne value is different
            this.airborne = airborne; // update flag
            this.updateTextureState(); // and update state in MSAT
        }
        // update facing flag based on velocity
//        if (this.vx > 0f) this.right = true;
//        else if (this.vx < 0f) this.right = false;
    }

    /**
     * Reacts to the entity moving by repositioning the nameplate above the entity
     */
    @Override
    protected void onMove() {
        this.positionNameplate(); // reposition the nameplate
    }

    /**
     * Renders the entity and the nameplate of the entity
     *
     * @param sp the shader program to use to render the game object
     */
    @Override
    public void render(ShaderProgram sp) {
        super.render(sp); // render the entity
        this.nameplate.render(sp); // render the nameplate
    }

    /**
     * Tells the model with VBO to use based on the animated texture's current frame and whether the entity is facing
     * left or right
     *
     * @param at   the animated texture to consider
     * @param flip whether to flip the texture or not (not actually used in this overridden method)
     */
    @Override
    protected void updateAnimatedTexture(AnimatedTexture at, boolean flip) {
        super.updateAnimatedTexture(at, this.right);
    }

    /**
     * Saves the given callback according to the mouse interaction interface
     *
     * @param type the mouse input type to give a callback for
     * @param mc   the callback
     */
    @Override
    public void giveCallback(MouseInputEngine.MouseInputType type, MouseInputEngine.MouseCallback mc) {
        MouseInputEngine.MouseInteractive.saveCallback(type, mc, this.mcs);
    }

    /**
     * Responds to mouse input by scaling the nameplate and invoking any necessary callbacks
     *
     * @param type the type of mouse input that occurred
     * @param x    the x position of the mouse in world coordinate or camera-view coordinates, depending on the mouse
     *             input engine's camera usage flag for this particular implementing object
     * @param y    the y position of the mouse in world coordinate or camera-view coordinates, depending on the mouse
     */
    @Override
    public void mouseInteraction(MouseInputEngine.MouseInputType type, float x, float y) {
        if (type == MouseInputEngine.MouseInputType.HOVER) { // if entity is being hovered
            this.nameplate.setScale(3f, 3f); // scale up slightly
            this.positionNameplate(); // reposition nameplate
        } else if (type == MouseInputEngine.MouseInputType.DONE_HOVERING) { // if entity is done being hovered
            this.nameplate.setScale(2.5f, 2.5f); // scale back to normal
            this.positionNameplate(); // reposition nameplate
        }
        MouseInputEngine.MouseInteractive.invokeCallback(type, this.mcs, x, y); // invoke necessary callbacks
    }
}
