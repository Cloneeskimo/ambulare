package utils;

import gameobject.gameworld.PhysicsObject;

/**
 * Uses the Separating Axis Theorem to detect collision
 * This only works with rectangular frames. For models that are not rectangular, rectangular bounding boxes are
 * provided anyways, so this should work for all general cases
 * Separating Axis Theorem: If you are able to draw a line to separate two polygons, then they do not collide
 */
public class PhysicsEngine {

    /**
     * Calculates whether the two given frames are colliding
     * @param a the first frame to consider
     * @param b the second frame to consider
     * @return whether the two game objects are colliding
     */
    public static boolean colliding(Frame a, Frame b) {
        Pair[] axes1 = getAxes(a.getR()); // get axes based on a's rotation
        Pair[] axes2 = getAxes(b.getR()); // get axes based on b's rotation
        Pair[] axes = new Pair[4]; // normalize and combine axes into one array
        for (int i = 0; i < 4; i++) axes[i] = normalize(i > 1 ? axes2[i - 2] : axes1[i]); // normalize/combine
        Pair[] fac = a.getCorners(); // get a's corner points
        Pair[] fbc = b.getCorners(); // get b's corner points
        for (Pair axis : axes) { // for each axis
            Pair proja = project(fac, axis); // project a onto it
            Pair projb = project(fbc, axis); // then project b onto it
            if (proja.y < projb.x || projb.y < proja.x) return false; /* and if any of a's projections don't overlap
                                                                         b's, there is no collision */
        }
        return true; // if all of the projections overlapped, the objects are colliding
    }

    /**
     * Calculates and performs a collision reaction between two objects affected by physics. Note that this method
     * assumes that collidability was checked beforehand
     * @param poa the first object
     * @param pob the second object
     * @param y whether this is a y-component collision (false for x)
     */
    public static void performCollisionRxn(PhysicsObject poa, PhysicsObject pob, boolean y) {
        PhysicsSettings psa = poa.getPhysicsSettings();            // get first object's physics settings
        PhysicsSettings psb = pob.getPhysicsSettings();            // get second object's physics settings
        if (psa.rigid && psb.rigid) return;                        // if both rigid, no reaction should occur

        else if (psa.rigid) {                                      // if only a rigid, only b feels a reaction
            float vb = y ? pob.getVY() : pob.getVX();              // get b's velocity
            vb = -vb * psb.bounciness;                             // reverse velocity and apply bounciness
            if (y) {                                               // if this is a y-component reaction
                pob.setVY(vb);                                     // apply the new velocity as a y velocity
                pob.setVX(pob.getVX() * psb.frictionResistance);   // apply friction to x
            } else {                                               // if this is an x-component reaction
                pob.setVX(vb);                                     // apply the new velocity as an x velocity
                pob.setVY(pob.getVY() * psb.frictionResistance);   // apply friction to y
            }
        } else if (psb.rigid) {                                    // if only b rigid, only a feels a reaction
            float va = y ? poa.getVY() : poa.getVX();              // get a's velocity
            va = -va * psa.bounciness;                             // reverse velocity and apply bounciness
            if (y) {                                               // if this is a y-component reaction
                poa.setVY(va);                                     // apply the new velocity as a y velocity
                poa.setVX(poa.getVX() * psa.frictionResistance);   // apply friction to x
            } else {                                               // if this is an x-component reaction
                poa.setVX(va);                                     // apply the new velocity as an x velocity
                poa.setVY(poa.getVY() * psa.frictionResistance);   // apply friction to y
            }
        } else {                                                   // if neither are rigid, both feel a reaction
            float ma = (y ? poa.getVY() : poa.getVX()) * psa.mass; // calculate a's momentum
            float mb = (y ? pob.getVY() : pob.getVX()) * psb.mass; // calculate b's momentum
            if (y) {                                               // if this is a y-component collision
                poa.setVY(mb / psa.mass);                          // set a's velocity based on a's mass and b's mom.
                poa.setVX(poa.getVX() * psa.frictionResistance);   // apply friction to x
                pob.setVY(ma / psb.mass);                          // set b's velocity based on b's mass and a's mom.
                pob.setVX(pob.getVX() * psb.frictionResistance);   // apply friction to x
            } else {                                               // if this is a x-component collision
                poa.setVX(mb / psa.mass);                          // set a's velocity based on a's mass and b's mom.
                poa.setVY(poa.getVY() * psa.frictionResistance);   // apply friction to y
                pob.setVX(ma / psb.mass);                          // set b's velocity based on b's mass and a's mom.
                pob.setVY(pob.getVY() * psb.frictionResistance);   // apply friction to y
            }
        }
    }

    /**
     * Projects a shape onto an vector/axis
     * @param shape the shape to project, defined by a set of points
     * @param axis the vector to project onto
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
        return new Pair(min, max); // return the projection as a pair defined as (min, max)
    }

    /**
     * Calculates the appropriate axes to check for a SAT collision test based on the rectangle's rotation
     * @param r the rotation of the frame (in radians)
     * @return two corresponding axes to check
     */
    private static Pair[] getAxes(double r) {
        return new Pair[] { // create new pair object
                new Pair((float)Math.cos(r), (float)Math.sin(r)), // first normal
                new Pair((float)Math.cos(r + (float)Math.PI / 2), (float)Math.sin(r + (float)Math.PI / 2)) // second
        };
    }

    /**
     * Calculates the dot product of two vectors
     * @param v1 the first vector
     * @param v2 the second vector
     * @return the dot product
     */
    private static float dot(Pair v1, Pair v2) {
        return (v1.x * v2.x) + (v1.y * v2.y); // calculate dot product and return
    }

    /**
     * Normalizes a given vector
     * @param v the vector to normalize
     * @return a new normalized vector - or the same vector if its length is 0
     */
    private static Pair normalize(Pair v) {
        float length = (float)Math.sqrt(v.x * v.x + v.y * v.y); // find the length
        if (length != 0) return new Pair(v.x / length, v.y / length); // use division to calculation norm
        return v; // return old vector if length was zero
    }

    /**
     * Encapsulates various settings used to calculate collision reactions
     */
    public static class PhysicsSettings {

        /**
         * Members
         */
        public float mass = 1f;                  /* how much mass the object has. The more mass an object has, the more
                                                    momentum it will bring into collisions, and the less effected it
                                                    will be by other objects colliding into it */
        public float bounciness = 0.5f;          /* determines how much of original velocity, as a proportion, will be
                                                    inverted upon collision */
        public float frictionResistance = 0.98f; /* when objects collide, friction resistance determines how much of
                                                    the opposite component of velocity will remain. For example, during
                                                    a y collision, the object's horizontal (x) velocity will be
                                                    multiplied by its friction resistance, and vice-versa */
        public float gravity = 19.6f;            /* this determines how much the object is affected by gravity */
        public boolean rigid = false;            /* if an object is rigid, it is unable to be affected by a collision.
                                                    It is still able to collide and will cause collisions, but its own
                                                    velocity and position will be unaffected. It will, however, still be
                                                    affected by gravity unless gravity is set to 0 */
        public boolean collidable = true;        // this determines if an object is able to collide

        /**
         * Constructs the physics settings with the given settings, assuming rigidity is false
         * @param mass the mass as described in members above
         * @param bounciness the bounciness as described in members above
         * @param frictionResistance the friction resistance as described in members above
         * @param gravity gravity as described in members above
         * @param collidable collidability as described in members above
         */
        public PhysicsSettings(float mass, float bounciness, float frictionResistance, float gravity,
                               boolean collidable) {
            this.mass = mass;                             // save mass as member
            this.bounciness = bounciness;                 // save bounciness as member
            this.frictionResistance = frictionResistance; // save friction resistance as member
            this.gravity = gravity;                       // save gravity as member
            this.collidable = collidable;                 // save collidability as member
        }

        /**
         * Constructs the physics settings at their default and with the given rigidity flag.
         * @param rigid the rigidity as described in members above
         */
        public PhysicsSettings(boolean rigid) {
            this.rigid = rigid; // save rigidity as member
        }

        /**
         * Constructs the physics settings at their defaults (see members above)
         */
        public PhysicsSettings() {}
    }
}
