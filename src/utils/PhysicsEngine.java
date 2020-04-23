package utils;

import gameobject.gameworld.PhysicsObject;

/**
 * Provides framework for performing physics. Namely, the physics engine can detect collisions using the Separating
 * Axis theorem. It can only do this for rectangular bounding boxes, but it can do it for any rotation, and most
 * shapes can be pretty closed fit with a rectangular bounding box. The physics engine also provides an inner class
 * that lays out the properties an object must take on and configure in order to properly exhibit physics. Finally,
 * this class provides a method to perform collision reactions, taking into account the aforementioned physics
 * properties of the two colliding objects.
 */
public class PhysicsEngine {

    /**
     * Calculates whether the two given bounding boxes are colliding
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
        }
        return true; // if all of the projections overlapped, the objects are colliding
    }

    /**
     * Calculates and performs a collision reaction between two objects affected by physics. Note that this method
     * assumes that collidability was checked beforehand. This also assumes that the collision in question is being
     * handled separately for both components (x and y) as it will only calculate a reaction for one component
     * @param poa the first object
     * @param pob the second object
     * @param y whether this is a y-component collision (false for x)
     */
    public static void performCollisionRxn(PhysicsObject poa, PhysicsObject pob, boolean y) {
        // get both object's physics settings
        PhysicsSettings psa = poa.getPhysicsSettings();
        PhysicsSettings psb = pob.getPhysicsSettings();
        if (psa.rigid && psb.rigid) return; // if both rigid, no reaction should occur
        else if (psa.rigid) { // if only a rigid, only b feels a reaction
            float vb = y ? pob.getVY() : pob.getVX(); // get b's velocity based on which component is being checked
            vb = -vb * psb.bounciness; // reverse velocity and apply bounciness
            // set the new velocity and apply friction to the opposite component
            if (y) {
                pob.setVY(vb);
                pob.setVX(pob.getVX() * psb.fricResis);
            } else {
                pob.setVX(vb);
                pob.setVY(pob.getVY() * psb.fricResis);
            }
        } else if (psb.rigid) { // if only b rigid, only a feels a reaction
            float va = y ? poa.getVY() : poa.getVX(); // get a's velocity based on which component is being checked
            va = -va * psa.bounciness; // reverse velocity and apply bounciness
            // set the new velocity and apply friction to the opposite component
            if (y) {
                poa.setVY(va);
                poa.setVX(poa.getVX() * psa.fricResis);
            } else {
                poa.setVX(va);
                poa.setVY(poa.getVY() * psa.fricResis);
            }
        } else { // if neither are rigid, both feel a reaction
            // calculate the momentum of both objects
            float ma = (y ? poa.getVY() : poa.getVX()) * psa.mass;
            float mb = (y ? pob.getVY() : pob.getVX()) * psb.mass;
            // for both objects, transfer the momentum and apply friction to the opposite component
            if (y) {
                poa.setVY(mb / psa.mass * (1 - psa.kbResis));
                poa.setVX(poa.getVX() * psa.fricResis);
                pob.setVY(ma / psb.mass * (1 - psb.kbResis));
                pob.setVX(pob.getVX() * psb.fricResis);
            } else {
                poa.setVX(mb / psa.mass * (1 - psa.kbResis));
                poa.setVY(poa.getVY() * psa.fricResis);
                pob.setVX(ma / psb.mass * (1 - psb.kbResis));
                pob.setVY(pob.getVY() * psb.fricResis);
            }
        }
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
                new Pair((float)Math.cos(r + (float)Math.PI / 2), (float)Math.sin(r + (float)Math.PI / 2))
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
     * Encapsulates settings necessary to exhibit physics. Specifically, any object must have these physics settings
     * in order to have a collision reaction calculated for them in this class. PhysicsObject is the extension of
     * GameObject where physics settings become included. Each setting is described in depth below
     */
    public static class PhysicsSettings {

        /**
         * Members
         */
        public float mass = 1f; /* how much mass the object has. The more mass an object has, the more momentum it
            will bring into collisions, and the less effected it will be by other objects colliding into it */
        public float bounciness = 0.5f; /* determines how much of original velocity, as a proportion, will be
            inverted upon collision within a rigid object */
        public float kbResis = 0.2f; /* if non-rigid, determines how much incoming momentum will be blocked during a
            collision with another non-rigid object */
        public float fricResis = 0.98f; /* when objects collide, friction resistance determines how much of the
            opposite component of velocity will remain. For example, during a y collision, the object's horizontal
            (x) velocity will be multiplied by its friction resistance, and vice-versa */
        public boolean rigid = false; /* if an object is rigid, it is unable to be affected by a collision. As such,
            none of the above settings applied to rigid objects. Rigid objects still cause collisions, but their own
            velocities and positions will be unaffected. They will, however, still be affected by gravity unless gravity
            is set to 0 */
        public boolean collidable = true; // this determines if an object is able to collide
        public float gravity = 19.6f; /* this determines how much the object is affected by gravity */

        /**
         * Constructs the physics settings with the given settings, assuming non-rigid and collidable
         */
        public PhysicsSettings(float mass, float bounciness, float fricResis, float kbResis, float gravity) {
            this.mass = mass;
            this.bounciness = bounciness;
            this.fricResis = fricResis;
            this.kbResis = kbResis;
            this.gravity = gravity;
        }

        /**
         * Constructs the physics settings at their default settings but with the given rigidity and collidability flags
         */
        public PhysicsSettings(boolean rigid, boolean collidable) {
            this.rigid = rigid;
            this.collidable = collidable;
        }

        /**
         * Constructs the physics settings at their defaults (see members above)
         */
        public PhysicsSettings() {}
    }
}
