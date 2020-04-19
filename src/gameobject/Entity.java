package gameobject;

import graphics.ShaderProgram;
import utils.Global;
import utils.Node;
import utils.Utils;

/**
 * Extends the Tile class by holding entity-related info such as health points and basic world AI wandering mechanics
 * Entities can be constructed using a node-file or with all default properties
 */
public class Entity extends Tile {

    /**
     * Wandering Parameters
     */
    private float minWaitTime = 0.2f; // minimum amount of time between wanders
    private float maxWaitTime = 4f; // maximum amount of time between wanders
    private float minWanderTime = 1f; // minimum amount of time per wander
    private float maxWanderTime = 4f; // maximum amount of time per wander
    protected boolean wanders = true; // a flag that determines if this Entity wanders

    /**
     * Wandering Data
     */
    private float nextWanderTime; // the next time this Entity will wander
    private float wanderLength; // the amount of time the next wander will take
    private float acc; // an accumulator for time
    private boolean wandering = true; // whether this Entity is currently wandering

    /**
     * Other Data
     */
    private float speed = 1f; // how fast the entity will move
    private int maxHP = 10, HP = 10; // how much health the entity has
    private TextObject nameplate; // nameplate to display about the entity

    /**
     * Constructs this entity using all default properties
     */
    public Entity() { super(); this.initNameplate(null); } // call super and initialize nameplate

    /**
     * Constructs this entity using data from a node-file
     * The node-file can contain the following children but does not need to contain any:
     *  - every child that a tile's node-file can contain (see Tile's constructor)
     *  - speed: how fast the entity should be - defaults is 1f
     *  - max_hp: maximum hp the entity should have - default is 10
     *  - hp: hit points to start with - default is 10
     *  - wanders: whether or not this entity will wander around
     *  - min_wait_time: minimum amount of time to wait between wanders if enabled
     *  - max_wait_time: maximum amount of time to wait between wanders if enabled
     *  - min_wander_time: minimum amount of time to spend per wander if enabled
     *  - max_wander_time: maximum amount of time to spend per wander if enabled
     *  - nameplate_color: the color to use for the nameplate text
     * @param resPath the resource-relative path to the node-file
     */
    public Entity(String resPath) {
        super(resPath); // configure tile data
        Node n = Node.resToNode(resPath); // load Node again for entity info
        float[] nameplateColor = null;
        try {
            for (Node c : n.getChildren()) {
                if (c.getName().equals("speed")) this.speed = Float.parseFloat(c.getValue()); // speed
                else if (c.getName().equals("max_hp")) this.maxHP = Integer.parseInt(c.getValue()); // max hp
                else if (c.getName().equals("hp")) this.HP = Integer.parseInt(c.getValue()); // starting hp
                else if (c.getName().equals("wanders")) this.wanders = Boolean.parseBoolean(c.getValue()); // wanders setting
                else if (c.getName().equals("min_wait_time"))
                    this.minWaitTime = Float.parseFloat(c.getValue()); // minimum wait time between wanders
                else if (c.getName().equals("max_wait_time"))
                    this.maxWaitTime = Float.parseFloat(c.getValue()); // maximum wait time between wanders
                else if (c.getName().equals("min_wander_time"))
                    this.minWanderTime = Float.parseFloat(c.getValue()); // minimum wait time between wanders
                else if (c.getName().equals("max_wander_time"))
                    this.maxWanderTime = Float.parseFloat(c.getValue()); // maximum wait time between wanders
                else if (c.getName().equals("nameplate_color")) { // color
                    nameplateColor = Utils.strToColor(c.getValue()); // convert to float array
                    if (nameplateColor == null) // if incorrectly formatted color
                        Utils.handleException(new Exception(Node.getNodeParseErrorMsg("Entity",
                                "incorrectly formatted color", resPath)), "gameobject.Entity", "Entity(String)",
                                true); // throw exception
                }
            }
        } catch (Exception e) { // catch general exceptions
            Utils.handleException(e, "Entity", "Entity(String)", true); // handle them
        }
        this.initNameplate(nameplateColor); // initialize nameplate
    }

    /**
     * Initializes the nameplate above the entity
     */
    private void initNameplate(float[] color) {
        this.nameplate = color == null ? new TextObject(Global.FONT, this.name) : // default color if color is null
            new TextObject(Global.FONT, this.name, color, 0f, 0f); // custom color if color provided
        this.nameplate.setScale(0.28f); // scale it appropriately
        this.positionNameplate(); // position kit
    }

    /**
     * Updates the entity
     * @param interval the amount of time to account for
     */
    @Override
    public void update(float interval) {
        super.update(interval); // call super's update
        if (this.posAnim == null && this.wanders) // if wandering is enabled and not in the of a positional animation
            this.updateWanderAI(interval); // update wandering
    }

    /**
     * Positions the nameplate above the entity
     */
    private void positionNameplate() {
        this.nameplate.setX(this.getX()); // x is same at this x
        this.nameplate.setY(this.getY() + 0.12f + (this.getUnrotatedHeight() / 2) +
                (this.nameplate.getHeight() / 2)); // y is just slightly above this entity
    }

    /**
     * Occurs when the entity moves, rotates, or scales
     */
    @Override
    protected void onMove() {
        if (this.nameplate != null) this.positionNameplate(); // reposition nameplate when such an action happens
    }

    /**
     * Updates wandering AI of the entity
     * @param interval the amount of time to account for
     */
    private void updateWanderAI(float interval) {
        this.acc += interval; // account for time
        if (this.wandering) { // if wandering
            if (this.acc > wanderLength) { // if wander is over
                this.vx = this.vy = 0; // reset velocity to 0
                this.nextWanderTime = Utils.genRandFloat(minWaitTime, maxWaitTime); // generate the next wander time
                this.acc = 0; // reset accumulator
                this.wandering = false; // set wandering flag to false
            }
        } else { // if waiting for the next wander
            if (this.acc > nextWanderTime) { // if wait is up
                float angle = Utils.genRandFloat(0, (float)Math.toRadians(360)); // generate an angle to wander at
                this.setRotRad(angle); // set rotation angle
                this.moveForward(); // set velocities accordingly
                this.wanderLength = Utils.genRandFloat(minWanderTime, maxWanderTime); // generate length of wander
                this.acc = 0; // reset accumulator
                this.wandering = true; // set wandering flag to true
            }
        }
    }

    @Override
    public void render(ShaderProgram sp) {
        super.render(sp); // render game object
        this.nameplate.render(sp); // render nameplate
    }

    /**
     * Sets the x and y velocity components according to rotation
     */
    public void moveForward() {
        this.vx = (float)Math.cos(this.getRot()) * this.speed; // get the x component of velocity
        this.vy = (float)Math.sin(this.getRot()) * this.speed; // get the y component of velocity
    }

    /**
     * Enables or disables wandering for this entity
     * @param wanders whether to enable (true) or disable (false) wandering
     */
    public void setWanders(boolean wanders) {
        if (!wanders && this.wanders && this.wandering) { // if disabling in the middle of a wander
            this.wandering = false; // stop the wander
            this.vx = this.vy = 0; // make this entity stationary
        } else if (wanders && !this.wanders) { // if enabling wandering
            this.acc = 0f; // reset accumulator
            this.nextWanderTime = Utils.genRandFloat(minWanderTime, maxWanderTime); // generate a next wander time
        }
        this.wanders = wanders; // save new flag
    }

    /**
     * Cleans up the entity
     */
    @Override
    public void cleanup() {
        super.cleanup(); // clean up game object stuff
        if (this.nameplate != null) this.nameplate.cleanup(); // cleanup nameplate
    }
}
