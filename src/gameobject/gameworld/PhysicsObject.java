package gameobject.gameworld;

import gameobject.GameObject;
import graphics.Material;
import graphics.Model;
import utils.BoundingBox;
import utils.PhysicsEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * Extends game objects to have physics properties. Namely, they are affected by gravity (customizable) and are able
 * to react to collision with other physics objects who also react to collision.
 */
public class PhysicsObject extends GameObject {

    /**
     * Static Data
     */
    private static final float TERMINAL_VELOCITY = -50f; // the minimum vertical velocity from gravity

    /**
     * Data
     */
    private List<PhysicsObject> collidables;  // a reference to the list of other objects to consider for collision
    private PhysicsEngine.PhysicsProperties pp; // the settings which describe how this object interacts with physics

    /**
     * Constructor
     * @param model the model to use
     * @param material the material to use
     */
    public PhysicsObject(Model model, Material material) {
        super(model, material);                        // call super
        this.collidables = new ArrayList<>();          // initialize collidables to empty list
        this.pp = new PhysicsEngine.PhysicsProperties(); // start physics settings at default
    }

    /**
     * Updates the physics object by updating its normal game object properties and also applying gravity
     * @param interval the amount of time to account for
     */
    @Override
    public void update(float interval) {
                                // apply gravity but do not go below terminal velocity
        this.vy = Math.max(this.vy - (this.pp.gravity * interval), TERMINAL_VELOCITY);
        super.update(interval); // call super
    }

    /**
     * React to movements by checking for velocity. This checks for collisions and reacts for each component (x and
     * y) separately
     * @param dx the x offset
     * @param dy the y offset
     * @return whether or not any movement actually occurred
     */
    @Override
    public boolean move(float dx, float dy) {
        if (this.pp.collidable) return PhysicsEngine.move(this, dx, dy);
        else return super.move(dx, dy); // if collision for this object is off, handle movement normally
    }

    /**
     * Calculate if there is an object under this one
     * @param precision how far down the look. The smaller the number, the more precisely timed the answer may be, but
     *                  the higher the chance of not returning true at all even when it is. This is because,
     *                  technically, there is always some amount of space between objects, even colliding
     * @return if there is an object under this object
     */
    public boolean somethingUnder(float precision) {
        this.setY(this.getY() - precision); // move y down based on precision
        BoundingBox f = null; // start at null so as to avoid calculating frame until absolutely necessary
        for (PhysicsObject po : this.collidables) { // for each collidable object
            if (po.pp.collidable && po != this) { // if the object has collision on and isn't this
                if (f == null) f = this.getBoundingBox(true); // calculate frame if not done yet
                if (PhysicsEngine.colliding(this.getBoundingBox(true), po.getBoundingBox(true))) { // if they
                    // collide
                    this.setY(this.getY() + precision); // return y to original position
                    return true; // there is something underneath
                }
            }
        }
        this.setY(this.getY() + precision); // return y to original position
        return false; // nothing underneath
    }

    /**
     * Sets the list of collidables to check for collisions
     * @param collidables the list of collidables
     */
    public void setCollidables(List<PhysicsObject> collidables) {
        this.collidables = collidables;
    }

    public List<PhysicsObject> getCollidables() { return this.collidables; }

    /**
     * @return the physics settings for the object
     */
    public PhysicsEngine.PhysicsProperties getPhysicsProperties() { return this.pp; }
}
