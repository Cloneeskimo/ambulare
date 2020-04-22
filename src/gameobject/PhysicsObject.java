package gameobject;

import graphics.Material;
import graphics.Model;
import utils.CollisionDetector;

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
    private List<PhysicsObject> collidables; // a reference to the list of other objects to consider for collision
    float gravity = 9.8f;                    // how affected this physics object is by gravity
    float bounciness = 0.5f;                 /* how much reverse velocity will be applied when colliding */
    float mass = 1.0f;                       /* used in momentum calculations when two non-rigid objects collide.
                                                more mass means more transference of momentum to the other object */
    boolean rigid = false;                   /* whether to disallow this object to move in any reactionary way to
                                                collision */
    boolean collision = true;                // whether to enable collision checking at all for this object

    /**
     * Constructor
     * @param model the model to use
     * @param material the material to use
     */
    public PhysicsObject(Model model, Material material) {
        super(model, material); // call super
        this.collidables = new ArrayList<>(); // initialize collidables to empty list
    }

    /**
     * Updates the physics object by updating its normal game object properties and also applying gravity
     * @param interval the amount of time to account for
     */
    @Override
    public void update(float interval) {
        // apply gravity but do not go below terminal velocity
        this.vy = Math.max(this.vy - (this.gravity * interval), TERMINAL_VELOCITY);
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
        if (this.collision) { // if this physics object checks for collision
            boolean xResult = super.move(dx, 0); // move just x
            if (xResult) { // if an actual movement
                for (PhysicsObject po : this.collidables) { // look through collidable objects
                    if (po.collision && this != po) { // if the other has collision on and isn't this
                        if (CollisionDetector.colliding(this, po)) { // check for collision. if colliding,
                            if (this.rigid && !po.rigid) { // if other is rigid but this is not
                                po.setVX(-po.vx * po.bounciness); // reverse just this object's velocity by bounciness
                            } else if (po.rigid && !this.rigid) { // if this is rigid but other is not
                                this.setVX(-this.vx * this.bounciness); // reverse just other's velocity by bounciness
                            } else if (!this.rigid) { // if neither are rigid
                                float tMom = this.vx * this.mass; // calculate this object's momentum
                                float oMom = po.vx * po.mass; // calculate other object's momentum
                                po.setVX(po.bounciness * (tMom / po.mass)); // apply this momentum to other object
                                this.setVX(this.bounciness * (oMom / this.mass)); // apply other's momentum to this
                            }
                            this.setX(this.getX() - dx); // move back to original x
                            xResult = false; // set xResult to false because, effectively, no x position change occurred
                            break; // break from collision checking loop
                        }
                    }
                }
            }
            boolean yResult = super.move(0, dy); // then move just y
            if (yResult) { // if an actual movement
                for (PhysicsObject po : this.collidables) { // look through collidable objects
                    if (po.collision && this != po) { // if the other has collision on and isn't this
                        if (CollisionDetector.colliding(this, po)) { // check for collision. if colliding,
                            if (this.rigid && !po.rigid) { // if other is rigid but this is not
                                po.setVY(-po.vy * po.bounciness); // reverse just this object's velocity by bounciness
                            } else if (po.rigid && !this.rigid) { // if other is rigid but this is not
                                this.setVY(-this.vy * this.bounciness); // reverse just other's velocity by bounciness
                            } else if (!this.rigid) { // if neither are rigid
                                float tMom = this.vy * this.mass; // calculate this object's momentum
                                float oMom = po.vy * po.mass; // calculate other object's momentum
                                po.setVY(po.bounciness * (tMom / po.mass)); // apply this momentum to other object
                                this.setVY(this.bounciness * (oMom / this.mass)); // apply other momentum to this
                            }
                            this.setY(this.getY() - dy); // move back to original y
                            xResult = false; // set yResult to false because, effectively, no y position change occurred
                            break; // break from collision checking loop
                        }
                    }
                }
            }
            return xResult || yResult; // if either y movement or x movement actually occurred, return true
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
        for (PhysicsObject po : this.collidables) { // for each collidable object
            if (po.collision && po != this) { // if the object has collision on and isn't this
                if (CollisionDetector.colliding(this, po)) { // if they collide
                    this.setY(this.getY() + precision); // return y to original position
                    return true; // there is something underneath
                }
            }
        }
        this.setY(this.getY() + precision); // return y to original position
        return false; // nothing underneath
    }

    /**
     * Sets the gravity value of the physics object. This, multiplied by interval, will be negatively applied to the
     * object's vertical velocity each update
     * @param gravity the gravity value to apply
     */
    public void setGravity(float gravity) { this.gravity = gravity; }

    /**
     * Sets the bounciness value of the physics object. See members above for a description of bounciness
     * @param bounciness the bounciness value
     */
    public void setBounciness(float bounciness) { this.bounciness = bounciness; }

    /**
     * Sets the rigidity flag status of the physics object. See members above for a description of rigidity
     * @param rigid the rigidity flag
     */
    public void setRigid(boolean rigid) { this.rigid = rigid; }

    /**
     * Sets the mass of the physics object. See members above for a description of mass
     * @param mass the mass
     */
    public void setMass(float mass) { this.mass = mass; }

    /**
     * Sets the list of collidables to check for collisions
     * @param collidables the list of collidables
     */
    public void setCollidables(List<PhysicsObject> collidables) {
        this.collidables = collidables;
    }
}
