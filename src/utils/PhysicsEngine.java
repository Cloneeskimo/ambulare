package utils;

import gameobject.gameworld.WorldObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/*
 * PhysicsEngine.java
 * Ambulare
 * Jacob Oaks
 * 4/22/20
 */

/**
 * Provides framework for performing physics. Specifically, the physics engine performs moves for world objects. In
 * doing so, it checks for collisions with blocks using a block map, and collisions with other collidable world objects.
 * Note that the physics engine rounds all velocities and positions it is given and it produces to the amount of digits
 * corresponding to ROUNDED_FORMAT in order to avoid precision errors. However, errors will still occur when dealing
 * with very large floating point numbers. ROUNDED_FORMAT can be modified to deal with this by rounding to fewer decimal
 * points at the cost of more precise movements. The physics engine also outlines a set of properties than each world
 * object must have which are used to calculate reactions to collisions
 */
public class PhysicsEngine {

    /**
     * Static Data
     */
    private static final DecimalFormat ROUNDED_FORMAT = new DecimalFormat("0.000"); /* formats numbers so that
        they are appropriately rounded for physics calculations. Without rounding, precision errors occur and correctly
        calculating push-back vector during collision resolution becomes infinitely more difficult and prone to bugs.
        Thus, rounding all numbers in the form allows for much more stability. The physics engine handless this rounding
        automatically */
    private static boolean[][] blockMap; // the block map to use for collision detection with blocks
    public static final float UNIT_AND_HALF = 0.0015f; /* the minimum unit (as specified by the rounding performed by
        the physics engine by ROUNDED_FORMAT) to be added to a push-back vector in order to resolve a collision */
    public static final float TERMINAL_VELOCITY = -50f; /* the minimum vertical velocity from gravity. Note that the
        the physics engine does not apply gravity. It is up to the object to apply it, hence public access */

    /**
     * Attempts to moves the given world object by the given change in x and y. This method will round the values as
     * specifies by the physics engine's ROUNDED_FORMAT to avoid precision issues. It will also check for collisions
     * with blocks in the physics engine's block map and with other world objects as specified by the given world
     * object's collidables list. In the case of collision, this method will perform collision resolution and reactions
     *
     * @param o  the world object to move
     * @param dx the amount on the x axis to move the world object by
     * @param dy the amount on the y axis to move the world object by
     * @return whether or not any movement actually occurred
     */
    public static boolean move(WorldObject o, float dx, float dy) {
        // properly round the values
        dx = round(dx);
        dy = round(dy);
        // save the original position to check if any changed occurred and to make later calculations easier
        float ox = o.getX();
        float oy = o.getY();
        if (!o.getPhysicsProperties().collidable) { // if the given object isn't even collidable
            // just perform the move without considering collision
            o.setX(ox + dx);
            o.setY(oy + dy);
        } else { // if the given object is indeed collidable

            /*
             * check x axis
             */
            if (dx != 0) { // if an x movement is desired
                o.setX(round(ox + dx)); // make the move and round the new position
                AABB aabb = o.getAABB(); // get the axis-aligned bounding box after movement

                /*
                 * check blocks on x axis
                 */
                Pair<Integer> cell = checkBlocks(aabb); // check for collisions with blocks
                if (cell != null) { // if a block collision occurred
                    float pb = calcPBFromBlock(aabb, cell, false); // calculate the push-back for resolution
                    if ((float) cell.x + 0.5f < o.getX()) pb *= -1; // make sure the sign is correct
                    o.setX(round(o.getX() + pb)); // perform collision resolution
                    Pair<Float> rxn = performReaction(o.getVX(), o.getPhysicsProperties()); // calculate a reaction
                    // apply the reaction to the object's velocity
                    o.setVX(rxn.x);
                    o.setVY(rxn.y * o.getVY());
                }

                /*
                 * check world objects on x axis
                 */
                else { // only check for world object collision if no block collision has occurred
                    for (WorldObject other : o.getCollidables()) { // loop through every other object
                        if (other != o && other.getPhysicsProperties().collidable) { // if collidable and different
                            AABB aabb2 = other.getAABB(); // get the other object's axis-aligned bounding box
                            Pair<Float> pb = AABBColliding(aabb, other.getAABB()); // check for collision
                            if (pb != null) { // if there is a push-back (and thus a collision)
                                if (aabb2.getCX() < aabb.getCX()) pb.x *= -1; // make sure the sign is correct
                                o.setX(round(o.getX() + pb.x)); // perform collisino resolution
                                Pair<Float>[] rxn = performReaction(o.getVX(), other.getVX(), o.getPhysicsProperties(),
                                        other.getPhysicsProperties()); // calculate a reaction
                                // apply the reaction to the object's velocity
                                o.setVX(rxn[0].x);
                                o.setVY(rxn[0].y * o.getVY());
                                // apply the reaction to the other object's velocity
                                other.setVX(rxn[1].x);
                                other.setVY(rxn[1].y * other.getVY());
                                break; // break - don't continue checking other objects
                            }
                        }
                    }
                }
            }

            /*
             * check y axis
             */
            if (dy != 0) { // if a y movement is desired
                o.setY(round(oy + dy)); // make the move and round the new position
                AABB aabb = o.getAABB(); // get the axis-aligned bounding box after movement

                /*
                 * check blocks on y axis
                 */
                Pair<Integer> cell = checkBlocks(aabb); // check for collisions with blocks
                if (cell != null) { // if a block collision occurred
                    float pb = calcPBFromBlock(aabb, cell, true); // calculate the push-back for resolution
                    if ((float) cell.y + 0.5f < o.getY()) pb *= -1; // make sure the sign is correct
                    o.setY(round(o.getY() + pb)); // perform collision resolution
                    Pair<Float> rxn = performReaction(o.getVY(), o.getPhysicsProperties()); // calculate a reaction
                    // apply the reaction to the object's velocity
                    o.setVY(rxn.x);
                    o.setVX(rxn.y * o.getVX());

                }

                /*
                 * check world objects on y axis
                 */
                else { // only check for world object collision if no block collision has occurred
                    for (WorldObject other : o.getCollidables()) { // loop through every other object
                        if (other != o && other.getPhysicsProperties().collidable) { // if collidable and different
                            AABB aabb2 = other.getAABB(); // get the other object's axis-aligned bounding box
                            Pair<Float> pb = AABBColliding(aabb, aabb2); // check for collision
                            if (pb != null) { // if there is a push-back (and thus a collision)
                                if (aabb2.getCY() < aabb.getCY()) pb.y *= -1; // make sure the sign is correct
                                o.setY(round(o.getY() + pb.y)); // perform collision resolution
                                Pair<Float>[] rxn = performReaction(o.getVY(), other.getVY(), o.getPhysicsProperties(),
                                        other.getPhysicsProperties()); // calculate a reaction
                                // apply the reaction to the object's velocity
                                o.setVY(rxn[0].x);
                                o.setVX(rxn[0].y * o.getVX());
                                // apply the reaction to the other object's velocity
                                other.setVY(rxn[1].x);
                                other.setVX(rxn[1].y * other.getVX());
                                break;
                            }
                        }
                    }

                }
            }
        }
        return oy != o.getY() || ox != o.getX(); // if either x or y has changed, return that a change has occurred
    }

    /**
     * Rounds the given number to the proper format to be used by the physics engine
     *
     * @param x the number to round
     * @return the rounded number
     */
    private static float round(float x) {
        return Float.parseFloat(ROUNDED_FORMAT.format(x));
    }

    /**
     * Checks an axis-aligned bounding box for collision with blocks in the block map
     *
     * @param aabb the axis-aligned bounding box to check for collision with blocks
     * @return the grid cell collided with if a collision occurs
     */
    private static Pair<Integer> checkBlocks(AABB aabb) {
        List<Pair<Float>> points = getPointsToTest(aabb); // calculate the points to apply on the grid for testing
        for (Pair<Float> point : points) { // for each point
            Pair<Integer> cell = Transformation.getGridCell(point); // get the corresponding grid cell
            // if the grid cell is not out of bounds
            if (cell.x >= 0 && cell.x < blockMap.length && cell.y >= 0 && cell.y < blockMap[0].length) {
                if (blockMap[cell.x][cell.y]) return cell; // return the cell if it is occupied by a block
            }
        }
        return null; // if no points were within a cell, return that no collision has occurred
    }

    /**
     * Populates a list of points to check for block collision. This includes the corners as well as additional
     * points along the edges of axis-aligned bounding boxes whose width or height are greater than 1f
     *
     * @param a the axis-aligned bounding box for whom to populate a list of points to test
     * @return the populated list of points
     */
    private static List<Pair<Float>> getPointsToTest(AABB a) {
        List<Pair<Float>> points = new ArrayList<>();
        // start with corners
        Pair<Float> bottomLeft = new Pair<>(a.getCX() - a.getW2(), a.getCY() - a.getH2());
        Pair<Float> topLeft = new Pair<>(a.getCX() - a.getW2(), a.getCY() + a.getH2());
        Pair<Float> topRight = new Pair<>(a.getCX() + a.getW2(), a.getCY() + a.getH2());
        Pair<Float> bottomRight = new Pair<>(a.getCX() + a.getW2(), a.getCY() - a.getH2());
        /*
          If extra points on the edges are not added, blocks will be able to go through larger objects because only
          the corners are being checked.
         */
        // add any extra points on the bottom and top
        for (float x = bottomLeft.x + 1; x < bottomRight.x; x++) {
            points.add(new Pair<>(x, bottomLeft.y));
            points.add(new Pair<>(x, topLeft.y));
        }
        // add any extra points on the left and right
        for (float y = bottomLeft.y + 1; y < topLeft.y; y++) {
            points.add(new Pair<>(bottomLeft.x, y));
            points.add(new Pair<>(bottomRight.x, y));
        }
        // add corners
        points.add(bottomLeft);
        points.add(topLeft);
        points.add(topRight);
        points.add(bottomRight);
        return points;
    }

    /**
     * Calculates the push back from a collision between a world object and a block
     *
     * @param o the axis-aligned bounding box of the world object who collided with a block
     * @param b the block collided with
     * @param y whether the push back should be calculated for the y component
     * @return the appropriate push back value
     */
    private static float calcPBFromBlock(AABB o, Pair<Integer> b, boolean y) {
        // calculate overlap between block and axis-aligned bounding box and return that plus a single unit
        if (y) return Math.abs(o.getCY() - Transformation.getCenterOfCellComponent(b.y)) - o.getH2() - 0.5f
                - UNIT_AND_HALF;
        else return Math.abs(o.getCX() - Transformation.getCenterOfCellComponent(b.x)) - o.getW2() - 0.5f
                - UNIT_AND_HALF;
    }

    /**
     * Checks if two axis-aligned bounding boxes are colliding
     *
     * @param a the first AABB
     * @param b the second AABB
     * @return null if no collision, or a pair of floats containing the necessary x/y push-back to resolve the collision
     */
    private static Pair<Float> AABBColliding(AABB a, AABB b) {
        // get the x distance between them
        float dx = Math.abs(a.getCX() - b.getCX()) - a.getW2() - b.getW2();
        if (dx > 0) return null; // if it's greater than 0, return no collision
        // get the y distance between them
        float dy = Math.abs(a.getCY() - b.getCY()) - a.getH2() - b.getH2();
        if (dy > 0) return null; // if it's greater than 0, return no collision
        // if both components had negative distances (positive overlaps), return the overlap plus a unit as push-backs
        return new Pair<>(round(dx - UNIT_AND_HALF), round(dy - UNIT_AND_HALF));
    }

    /**
     * Calculates a collision reaction between two objects given their velocities and physics properties. This will
     * work for either x or y component
     *
     * @param va  the velocity of the first world object
     * @param vb  the velocity of the second world object
     * @param ppa the physics properties of the first world object
     * @param ppb the physics properties of the second world object
     * @return an array containing two pairs formatted as follows:
     * [{a's new velocity in the component in question, a multiplier for a's velocity in the opposite component},
     * {b's new velocity in the component in question, a multiplier for b's velocity in the opposite component}]
     */
    private static Pair<Float>[] performReaction(float va, float vb, PhysicsProperties ppa, PhysicsProperties ppb) {
        // if both are rigid, both should just stop moving in that direction
        Pair<Float>[] v = new Pair[]{new Pair<>(0f, 1f), new Pair<>(0f, 1f)};
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
     * Calculates a collision reaction between an object and a block given the world object's velocity and physics
     * properties
     *
     * @param v  the velocity of the world object
     * @param pp the physics properties of the world object
     * @return a pair where the x value is the object's new velocity in the component in question and the y value is
     * a multiplier for the object's velocity in the opposite component
     */
    private static Pair<Float> performReaction(float v, PhysicsProperties pp) {
        if (pp.rigid) return new Pair<>(0f, 1f);
        else return new Pair<>(-v * pp.bounciness, pp.fricResis);
    }

    /**
     * Calculate if there is an object or block next the given world object in the given direction. X-component and
     * y-component checks may be combined (for example, can use this to look if there is an object to the "bottom
     * left" of the given world object). This works for non-collidable objects as well, but will not count other
     * non-collidable objects as valid checks
     *
     * @param wo the world object to look next to
     * @param x  the x direction to look. If x < 0, will look to the left of the object. If x > 0, will look to the
     *           right of the object. If x == 0, will not look in any x-component direction.
     * @param y  the y direction to look. If y < 0, will look beneath the object. If y > 0, will look above the object.
     *           If y == 0, will not look in any y-component direction.
     * @return if there is an object or block in the given direction of the given object
     */
    public static boolean nextTo(WorldObject wo, float x, float y) {
        // calculate necessary movement to check if next to
        float dx = (x < 0f) ? -0.005f : (x > 0f) ? 0.005f : 0f;
        float dy = (y < 0f) ? -0.005f : (y > 0f) ? 0.005f : 0f;
        // save original position to reset later
        float ox = wo.getX();
        float oy = wo.getY();
        // move slightly next to according to parameters
        wo.setX(round(wo.getX() + dx));
        wo.setY(round(wo.getY() + dy));
        // check if new position collides with blocks
        boolean nextTo = checkBlocks(wo.getAABB()) != null;
        if (!nextTo) { // if no block collision was found
            AABB aabb = wo.getAABB(); // get the object's axis-aligned bounding box
            for (WorldObject o : wo.getCollidables()) { // and for each collidable object
                if (o != wo) { // don't check the object against itseslf
                    if (AABBColliding(aabb, o.getAABB()) != null) { // if they are colliding
                        nextTo = true; // the object is next to something
                        break; // break from the loop
                    }
                }
            }
        }
        // reset object to original position
        wo.setX(ox);
        wo.setY(oy);
        return nextTo; // return whether the object was determined to be next to anothers
    }

    /**
     * Tells the physics engine what block map to use for detecting collisions with blocks
     *
     * @param blockMap the block map to use
     */
    public static void giveBlockMap(boolean[][] blockMap) {
        PhysicsEngine.blockMap = blockMap;
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
        public float fricResis = 0.95f; /* when objects collide, friction resistance determines how much of the
            opposite component of velocity will remain. For example, during a y collision, the object's horizontal
            (x) velocity will be multiplied by its friction resistance, and vice-versa */
        public float gravity = 19.6f; /* this determines how much the object is affected by gravity */
        public boolean rigid = false; /* if an object is rigid, its velocity is unable to be affected by collisions
            with non-rigid objects. Rigid objects can still cause collisions. If they collide with other rigid objects,
            both of the colliding rigid objects will have their velocities reduced to zero in the component in
            question. Rigid objects are still affected by gravity unless the gravity property is changed to 0. Rigid
            objects produce collision reactions similar to blocks */
        public boolean collidable = true; /* this determines if an object is able to collide. If false, the object
            will be excluded from all collision detection and reaction calculations. */

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
        public PhysicsProperties() {
        }
    }

    /**
     * Axis-aligned bounding boxes that are used for collision detection. The goal is to have them generally fit the
     * object they are trying to describe. They are defined by a center point and a half-width and half-height. They
     * purpose of having this extra level instead of collision detection just using the object's center-point and
     * width/height is to allow game objects to modify the width/height of their AABB, for example, if they know that
     * their texture won't fit the whole model
     */
    public static class AABB {

        /**
         * Members
         */
        private float cx, cy; // center point
        private float w2, h2; // half-width and half-height

        /**
         * Constructor
         *
         * @param cx the center-point x
         * @param cy the center-point y
         * @param w  the full width of the object
         * @param h  the full height of the object
         */
        public AABB(float cx, float cy, float w, float h) {
            this.cx = PhysicsEngine.round(cx);
            this.cy = PhysicsEngine.round(cy);
            this.w2 = PhysicsEngine.round(w / 2);
            this.h2 = PhysicsEngine.round(h / 2);
        }

        /**
         * Calculates if the given point is within the axis-aligned bounding box
         * @param x the x of the point to check
         * @param y the y of the point to check
         * @return whether the given point is within the axis-aligned bounding box
         */
        public boolean contains(float x, float y) {
            return (x >= this.cx - this.w2 && x <= this.cx + this.w2) &&
                    (y >= this.cy - this.h2 && y <= this.cy + this.h2);
        }

        /**
         * Multiplies the width and height of the axis-aligned bounding box by the given factor, essentially shrinking
         * or expanding it
         * @param factor the factor to scale by
         */
        public void scale(float factor) {
            this.w2 *= factor; // scale width
            this.h2 *= factor; // scale height
        }

        /**
         * @return the center-point x of the axis-aligned bounding box
         */
        public float getCX() {
            return this.cx;
        }

        /**
         * @return the center-point y of the axis-aligned bounding box
         */
        public float getCY() {
            return this.cy;
        }

        /**
         * @return the half-width of the axis-aligned bounding box
         */
        public float getW2() {
            return this.w2;
        }

        /**
         * @return the half-height of the axis-aligned bounding box
         */
        public float getH2() {
            return this.h2;
        }
    }

}
