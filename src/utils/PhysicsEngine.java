package utils;

import gameobject.GameObject;
import gameobject.gameworld.WorldObject;

import java.util.List;

/**
 * Provides framework for performing physics. Specifically, the physics engine performs moves for world objects,
 * checks for collision in doing so, and performs reactions upon collision based on a set of defined physics
 * characteristics unique to each world object.
 */
public class PhysicsEngine {

    /**
     * Static Data
     */
    public static final float TERMINAL_VELOCITY = -50f; // the minimum vertical velocity from gravity
    private static final double COLLISION_THRESHOLD = -1E-6; /* the minimum distance between the objects at which they
        are not considered to be colliding. Should be slightly less than zero to avoid precision issues */
    private static final float NEXT_TO_PRECISION = 0.0001f; /* the amount away from game objects to look to determine
        if there is something there. For example, in somethingUnder(), the object is moved this far down and checked
        for collisions to determine if there is another object beneath it */

    /**
     * Moves the given world object by the given x and y components, checking for and reacting to collisions in the
     * process. This method will only use axis-aligned bounding boxes for all objects in consideration
     * @param o the object to move
     * @param dx the x factor to move the object by
     * @param dy the y factor to move the object by
     * @return whether or not an actual move occurred
     */
    public static boolean move(WorldObject o, float dx, float dy) {
        List<WorldObject> collidables = o.getCollidables(); // get the collidables to check for
        WorldObject l = null; // if an x collision occurs, save the object of collision to l to avoid checking for y too
        float ox = o.getX(); float oy = o.getY(); // save original position for checking against final position
        if (dx != 0) { // if there is actually a movement in x
            o.setX(o.getX() + dx); // perform the move
            for (WorldObject wo : collidables) { // then look through all collidable objects
                if (wo.getPhysicsProperties().collidable && wo != o) { // if curr is collidable and not the given object
                    Pair pb = AABBColliding(o.getAABB(), wo.getAABB()); // check if o and wo are colliding
                    if (pb != null) { // if the objects are colliding
                        o.setX(o.getX() + (wo.getX() > o.getX() ? 1 : -1) * pb.x); // push o back out of the collision
                        Pair[] reaction = performReaction(o.getVX(), wo.getVX(), o.getPhysicsProperties(),
                                wo.getPhysicsProperties()); // perform a reaction based on the collision
                        // the reaction returns two pairs corresponding to new velocity values/multipliers
                        o.setVX(reaction[0].x); // set o's new velocity
                        o.setVY(o.getVY() * reaction[0].y); // apply o's y velocity multiplier
                        wo.setVX(reaction[1].x); // set wo's new velocity
                        wo.setVY(wo.getVY() * reaction[1].y); // apply p's y velocity multiplier
                        l = wo; // save to l to avoid checking for y too
                    }
                }
            }
        }
        if (dy != 0) { // if there is an actual change in y
            o.setY(o.getY() + dy); // perform the move
            for (WorldObject wo : collidables) { // go through possible colliding objects
                if (wo.getPhysicsProperties().collidable && wo != o && wo != l) { // if curr is collidable and different
                    Pair pb = AABBColliding(o.getAABB(), wo.getAABB()); // check if o and wo are colliding
                    if (pb != null) { // if colliding
                        o.setY(o.getY() + (wo.getY() > o.getY() ? 1 : -1) * pb.y); // push o back out of the collision
                        Pair[] reaction = performReaction(o.getVY(), wo.getVY(), o.getPhysicsProperties(),
                                wo.getPhysicsProperties()); // perform a reaction based on the collision
                        // the reaction returns two pairs corresponding to new velocity values/multipliers
                        o.setVY(reaction[0].x); // set o's new y velocity
                        o.setVX(o.getVX() * reaction[0].y); // use o's x velocity multiplier
                        wo.setVY(reaction[1].x); // set wo's new y velocity
                        wo.setVX(wo.getVX() * reaction[1].y); // use wo's x velocity multiplier
                    }
                }
            }
        }
        return o.getX() != ox || o.getY() != oy; // return whether either of the positions actually changed
    }

    /**
     * Checks if two objects are colliding by considering their axis-aligned bounding boxes
     * @param a the first object's AABB
     * @param b the second object's AABB
     * @return null if no collision, or a Pair containing the necessary x or y push-back to back out of the collision
     */
    public static Pair AABBColliding(AABB a, AABB b) {
        // get the x distance between them
        float dx = Math.abs(a.getCX() - b.getCX()) - a.getW2() - b.getW2();
        if (dx >= COLLISION_THRESHOLD) return null; // if it's greater than threshold, no collision
        // get the y distance between them
        float dy = Math.abs(a.getCY() - b.getCY()) - a.getH2() - b.getH2();
        if (dy >= COLLISION_THRESHOLD) return null; // if it's greater than threshold, no collision
        return new Pair(dx, dy); // if both were less than threshold, collision
    }

    /**
     * Calculate if there is an object under the given world object
     * @param wo the world object to look under
     * @return if there is an object under the given world object
     */
    public static boolean somethingUnder(WorldObject wo) {
        wo.setY(wo.getY() - NEXT_TO_PRECISION); // move y down based on precision
        List<WorldObject> collidables = wo.getCollidables(); // get possible collisions
        for (WorldObject o : collidables) { // for each collidable object
            if (o.getPhysicsProperties().collidable && o != wo) { // if the object has collision on and isn't wo
                if (AABBColliding(wo.getAABB(), o.getAABB()) != null) { // if they collide
                    wo.setY(wo.getY() + NEXT_TO_PRECISION); // return y to original position
                    return true; // there is something underneath
                }
            }
        }
        wo.setY(wo.getY() + NEXT_TO_PRECISION); // return y to original position
        return false; // nothing underneath
    }

    /**
     * Calculates a collision reaction between two objects given their velocities and physics properties. This will
     * work for either x or y component
     * @param va the velocity of the first object
     * @param vb the velocity of the second object
     * @param ppa the physics properties of the first object
     * @param ppb the physics properties of the second object
     * @return an array containing two pairs formatted as follows:
     * [{a's new velocity in the component in question, a multiplier for a's velocity in the opposite component},
     *  {b's new velocity in the component in question, a multiplier for b's velocity in the opposite component}]
     */
    private static Pair[] performReaction(float va, float vb, PhysicsProperties ppa, PhysicsProperties ppb) {
        // if both are rigid, both should just stop moving in that direction
        Pair[] v = new Pair[]{new Pair(0, 1), new Pair(0, 1)};
        if (ppa.rigid && ppb.rigid) return v;
        if (ppa.rigid) { // if only a is rigid, then only b will feel a reaction
            if (va == 0) { // if a is not moving
                v[1].x = -vb * ppb.bounciness; // then b will just bound back (taking into account bounciness)
            } else { // if a is moving
                float amom = va * ppa.mass; // calculate a's momentum
                v[1].x = (amom / ppb.mass) * (1 - ppb.kbResis); // and apply it to b (taking into account kb resistance)
            }
            v[1].y = ppb.fricResis; // apply friction to other component for b
         } else if (ppb.rigid) { // if only b is rigid, then only a will feel a reaction
            if (vb == 0) { // if b is not moving
                v[0].x = -va * ppa.bounciness; // then a will just bound back (taking into account bounciness)
            } else { // if b is moving
                float bmom = vb * ppb.mass; // calculate b's momentum
                v[0].x = (bmom / ppa.mass) * (1 - ppa.kbResis); // and apply it to a (taking into account kb resistance)
            }
            v[0].y = ppa.fricResis; // apply friction to other component for b
        } else { // if neither are rigid, a momentum-based reaction will occur
            float amom = va * ppa.mass; // calculate a's momentum
            float bmom = vb * ppb.mass; // calculate b's momentum
            v[0].x = (bmom / ppa.mass) * (1 - ppa.kbResis); // and apply it to a (taking into account kb resistance)
            v[1].x = (amom / ppb.mass) * (1 - ppb.kbResis); // and apply it to b (taking into account kb resistance)
            v[1].y = ppb.fricResis; // apply friction to other component for b
            v[0].y = ppa.fricResis; // apply friction to other component for b
        }
        return v;
    }

    /**
     * Encapsulates properties necessary to exhibit physics. Specifically, any object must have these physics settings
     * in order to have a collision reaction calculated for them in this class. WorldObject is the extension of
     * GameObject where physics properties become included. Each setting is described in depth below
     */
    public static class PhysicsProperties {

        /**
         * Members
         */
        public float mass = 1f; /* how much mass the object has. The more mass an object has, the more momentum it
            will bring into collisions */
        public float bounciness = 0.5f; /* determines how much of original velocity, as a proportion, will be
            inverted upon collision within a rigid object */
        public float kbResis = 0.0f; /* determines how much incoming momentum will be blocked during a collision with
            another non-rigid object */
        public float fricResis = 0.98f; /* when objects collide, friction resistance determines how much of the
            opposite component of velocity will remain. For example, during a y collision, the object's horizontal
            (x) velocity will be multiplied by its friction resistance, and vice-versa */
        public boolean rigid = false; /* if an object is rigid, it is unable to be affected by a collision. As such,
            none of the above settings applied to rigid objects. Rigid objects still cause collisions, but their own
            velocities and positions will be unaffected - unless they collide with other rigid objects, in which case
            they will both stop. Rigid objects will still be affected by gravity unless gravity is set to 0 */
        public boolean collidable = true; // this determines if an object is able to collide
        public float gravity = 19.6f; /* this determines how much the object is affected by gravity */

        /**
         * Constructs the physics properties with the given properties, assuming non-rigid and collidable
         */
        public PhysicsProperties(float mass, float bounciness, float fricResis, float kbResis, float gravity) {
            this.mass = mass;
            this.bounciness = bounciness;
            this.fricResis = fricResis;
            this.kbResis = kbResis;
            this.gravity = gravity;
        }

        /**
         * Constructs the physics properties at their default settings but with the given rigidity and collidability
         * flags
         */
        public PhysicsProperties(boolean rigid, boolean collidable) {
            this.rigid = rigid;
            this.collidable = collidable;
        }

        /**
         * Constructs the physics properties at their defaults (see members above)
         */
        public PhysicsProperties() {}
    }
}
