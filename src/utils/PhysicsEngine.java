package utils;

import gameobject.gameworld.PhysicsObject;

import java.util.List;

/**
 * Provides framework for performing physics. Specifically, the physics engine performs moves for physics objects,
 * checks for collision in doing so, and performs reactions upon collision based on a set of defined physics
 * characteristics unique to each physics object.
 */
public class PhysicsEngine {

    /**
     * Moves the given physics object by the given x and y components, checking for and reacting to collisions in the
     * process. This method will only use un-rotated bounding boxes for all objects in consideration
     * @param o the object to move
     * @param dx the x factor to move the object by
     * @param dy the y factor to move the object by
     * @return whether or not an actual move occurred
     */
    public static boolean move(PhysicsObject o, float dx, float dy) {
        List<PhysicsObject> collidables = o.getCollidables(); // get the collidables to check for
        float ox = o.getX(); float oy = o.getY(); // save original position for checking against final position
        BoundingBox bb = null; // don't get the bounding box until absolutely need to
        PhysicsObject l = null; /* this is to keep track of an object collided with while checking the x component.
            this is so as to avoid checking for collisions for the same object when checking the y component, as that
            can produce strange behavior */
        if (dx != 0) { // if there is actually a movement in x
            o.setX(o.getX() + dx); // perform the move
            for (PhysicsObject po : collidables) { // then look through all collidable objects
                if (po.getPhysicsProperties().collidable && po != o) { // if curr is collidable and not the given object
                    if (bb == null) bb = o.getBoundingBox(false); // make sure we have o's bounding box
                    if (colliding(bb, po.getBoundingBox(false))) { // if the objects are colliding
                        float horizontalDistance = Math.abs(po.getX() - o.getX()); // find the distance between them
                        // calculate the minimum amount necessary to push o back to reverse the collision
                        float horizontalPushback = horizontalDistance - po.getWidth() / 2 - o.getWidth() / 2;
                        o.setX(o.getX() + (po.getX() > o.getX() ? 1 : -1) * horizontalPushback); // push o back
                        Pair[] reaction = performReaction(o.getVX(), po.getVX(), o.getPhysicsProperties(),
                                po.getPhysicsProperties()); // perform a reaction based on the collision
                        // the reaction returns two pairs corresponding to new velocity values/multipliers
                        o.setVX(reaction[0].x); // set o's new velocity
                        o.setVY(o.getVY() * reaction[0].y); // apply o's y velocity multiplier
                        po.setVX(reaction[1].x); // set po's new velocity
                        po.setVY(po.getVY() * reaction[1].y); // apply p's y velocity multiplier
                        l = po; // save the object we collided with so we don't check again for y component
                    }
                }
            }
        }
        if (dy != 0) { // if there is an actual change in y
            o.setY(o.getY() + dy); // perform the move
            bb = null; // reset bounding box to null - it may have changed by now
            for (PhysicsObject po : collidables) { // go through possible colliding objects
                if (po.getPhysicsProperties().collidable && po != o && po != l) { // if curr is collidable and different
                    if (bb == null) bb = o.getBoundingBox(false); // make sure we have the bounding box
                    if (colliding(bb, po.getBoundingBox(false))) { // if colliding
                        float verticalDistance = Math.abs(po.getY() - o.getY()); // calculate the distance between them
                        // calculate the minimum amount necessary to push o back to reverse the collision
                        float verticalPushback = Math.abs(verticalDistance - po.getHeight() / 2 - o.getHeight() / 2);
                        // push o back out of the collision
                        o.setY(o.getY() + (po.getY() > o.getY() ? -1 : 1) * verticalPushback);
                        Pair[] reaction = performReaction(o.getVY(), po.getVY(), o.getPhysicsProperties(),
                                po.getPhysicsProperties()); // perform a reaction based on the collision
                        // the reaction returns two pairs corresponding to new velocity values/multipliers
                        o.setVY(reaction[0].x); // set o's new y velocity
                        o.setVX(o.getVX() * reaction[0].y); // use o's x velocity multiplier
                        po.setVY(reaction[1].x); // set po's new y velocity
                        po.setVX(po.getVX() * reaction[1].y); // use po's x velocity multiplier
                    }
                }
            }
        }
        return o.getX() != ox || o.getY() != oy; // return whether either of the positions actually changed
    }

    /**
     * Calculates whether the two given bounding boxes are colliding. This method is based on the separating axis
     * theorem and will work for rotated bounding boxes, but the move function does not support them
     * @param a the first bounding box to consider
     * @param b the second bounding box to consider
     * @return whether the two bounding boxes are colliding
     */
    public static boolean colliding(BoundingBox a, BoundingBox b) {
        // get axes based on the rotations of both bounding boxes
        Pair[] axes1 = getAxes(a.getR());
        Pair[] axes2 = getAxes(b.getR());
        Pair[] axes = new Pair[4]; // create an array to hold all four normalized axes
        for (int i = 0; i < 4; i++) axes[i] = normalize(i > 1 ? axes2[i - 2] : axes1[i]);
        // get the corner points of the two bounding boxes
        Pair[] ca = a.getCorners();
        Pair[] cb = b.getCorners();
        for (Pair axis : axes) { // for each axis
            // project the bounding boxes (defined by their corners) onto the axis
            Pair proja = project(ca, axis);
            Pair projb = project(cb, axis);
            // if any of a's projections don't overlap with b's, then there is decidedly not a collision
            if (proja.y < projb.x || projb.y < proja.x) return false;
            else if (Utils.XOR(proja.y <= projb.x, projb.y <= proja.x)) return false;
        }
        return true;
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
     * Projects a shape onto an vector/axis
     * @param shape the set of points defining the shape to project
     * @param axis the vector/axis to project onto
     * @return the projection, defined by a minimum (x) and a maximum (y)
     */
    private static Pair project(Pair[] shape, Pair axis) {
        float min = dot(shape[0], axis); // get the first point's dot product to start as the min
        float max = min; // and the max
        for (int i = 0; i < shape.length; i++) { // then go through the rest of the points
            float p = dot(shape[i], axis); // find their dot product
            if (p < min) min = p; // find the min of the dot products
            else if (p > max) max = p; // and the max of the dot products
        }
        return new Pair(min, max);
    }

    /**
     * Calculates the appropriate axes to check for a SAT collision test based on a bounding box's rotation
     * @param r the rotation of the bounding box (in radians)
     * @return two corresponding axes to check
     */
    private static Pair[] getAxes(double r) {
        return new Pair[] { // create new pair object and calculate the first and second normals based on rotation
                new Pair((float)Math.cos(r), (float)Math.sin(r)),
                new Pair((float)Math.cos(r + Math.PI / 2), (float)Math.sin(r + Math.PI / 2))
        };
    }

    /**
     * Calculates the dot product of two vectors
     * @param v1 the first vector
     * @param v2 the second vector
     * @return the dot product
     */
    private static float dot(Pair v1, Pair v2) {
        return (v1.x * v2.x) + (v1.y * v2.y);
    }

    /**
     * Normalizes a given vector
     * @param v the vector to normalize
     * @return a new normalized vector - or the same vector if its length is 0
     */
    private static Pair normalize(Pair v) {
        float length = (float)Math.sqrt(v.x * v.x + v.y * v.y); // calculate total length of the vector
        if (length != 0) return new Pair(v.x / length, v.y / length); // calculate and return normalize vector
        return v; // return old vector if length was zero
    }

    /**
     * Encapsulates properties necessary to exhibit physics. Specifically, any object must have these physics settings
     * in order to have a collision reaction calculated for them in this class. PhysicsObject is the extension of
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
