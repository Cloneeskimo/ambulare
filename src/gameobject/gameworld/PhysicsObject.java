package gameobject.gameworld;

import gameobject.GameObject;
import graphics.Material;
import graphics.Model;
import utils.Frame;
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
    private PhysicsEngine.PhysicsSettings ps; // the settings which describe how this object interacts with physics

    /**
     * Constructor
     * @param model the model to use
     * @param material the material to use
     */
    public PhysicsObject(Model model, Material material) {
        super(model, material);                        // call super
        this.collidables = new ArrayList<>();          // initialize collidables to empty list
        this.ps = new PhysicsEngine.PhysicsSettings(); // start physics settings at default
    }

    /**
     * Updates the physics object by updating its normal game object properties and also applying gravity
     * @param interval the amount of time to account for
     */
    @Override
    public void update(float interval) {
                                // apply gravity but do not go below terminal velocity
        this.vy = Math.max(this.vy - (this.ps.gravity * interval), TERMINAL_VELOCITY);
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
        if (this.ps.collidable) { // if this physics object is collidable
            boolean xMoved = super.move(dx, 0); // move just x
            Frame f = null; // null so that we can avoid calculating Frame until absolutely necessary
            if (xMoved) { // if an actual movement occurred
                for (PhysicsObject po : this.collidables) { // look through collidable objects
                    if (po.ps.collidable && this != po) { // if the other is collidable and isn't this
                        if (f == null) f = this.getFrame(); // get frame if don't have it yet
                        if (PhysicsEngine.colliding(f, po.getFrame())) { // check for collision. if colliding,
                            PhysicsEngine.performCollisionRxn(this, po, false); // react to the collision
                            this.setX(this.getX() - dx); // move back to original x
                            xMoved = false; // set x move flag to false because we denied the movement
                            break; // break from collision checking loop
                        }
                    }
                }
            }
            f = null; // reset frame to null because we will want to recalculate it for y collisions
            boolean yMoved = super.move(0, dy); // then move just y
            if (yMoved) { // if an actual movement occurred
                for (PhysicsObject po : this.collidables) { // look through collidable objects
                    if (po.ps.collidable && this != po) { // if the other is collidable and isn't this
                        if (f == null) f = this.getFrame(); // get frame if don't have it yet
                        if (PhysicsEngine.colliding(f, po.getFrame())) { // check for collision. if colliding,
                            PhysicsEngine.performCollisionRxn(this, po, true); // react to the collision
                            this.setY(this.getY() - dy); // move back to original y
                            yMoved = false; // set y move flag to false because we denied the movement
                            break; // break from collision checking loop
                        }
                    }
                }
            }
            return xMoved || yMoved; // if either y movement or x movement actually occurred, return true
        }
        return super.move(dx, dy); // if collision for this object is off, handle movement normally
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
        Frame f = null; // start at null so as to avoid calculating frame until absolutely necessary
        for (PhysicsObject po : this.collidables) { // for each collidable object
            if (po.ps.collidable && po != this) { // if the object has collision on and isn't this
                if (f == null) f = this.getFrame(); // calculate frame if not done yet
                if (PhysicsEngine.colliding(this.getFrame(), po.getFrame())) { // if they collide
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

    /**
     * @return the physics settings for the object
     */
    public PhysicsEngine.PhysicsSettings getPhysicsSettings() { return this.ps; }
}
