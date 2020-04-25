package utils;

import gameobject.gameworld.Block;
import gameobject.gameworld.WorldObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides framework for performing physics. Specifically, the physics engine performs moves for world objects. In
 * doing so, it checks for collisions with blocks in a block map, and collisions with other collidable world objects.
 * It also outlines a set of properties than each world object must have which are used to calculate reactions to
 * collisions.
 */
public class PhysicsEngine {

    /**
     * Static Data
     */
    public static final float TERMINAL_VELOCITY = -50f; // the minimum vertical velocity from gravity
    private static final double COLLISION_THRESHOLD = -1E-6; /* the minimum distance between world objects at which they
        are not considered to be colliding. Should be slightly less than zero to avoid precision issues */
    private static final float NEXT_TO_PRECISION = 0.0001f; /* the amount away from game objects to look to determine
        if there is something there. */
    private static Block[][] blockMap; /* the block map to use for collision detection with blocks */

    /**
     * Attempts to move the given world object by the given amount. If a collision occurs, will move the most it
     * possibly can without having its axis-aligned bounding box overlap any other world object's axis-aligned
     * bounding boxes or any blocks. After a collision, will calculate and perform an appropriate reaction based on
     * all involved parties' physics properties
     * @param o the world object to move
     * @param dx the x component to move the object by
     * @param dy the y component to move the object by
     * @return whether or not any movement actually occurred
     */
    public static boolean move(WorldObject o, float dx, float dy) {
        if (!o.getPhysicsProperties().collidable) { // if object isn't collidable in the first place
            // make the move regardless of collision
            o.setX(o.getX() + dx);
            o.setY(o.getY() + dy);
            return (dx != 0 && dy != 0); // and return whether dx or dy are non-zero values
        }
        // save original positions
        float ox = o.getX();
        float oy = o.getY();
        Object a = moveX(o, dx); // move x and save object collided with if there is one
        moveY(o, dy, a); // move y and tell it to exclude the object collided with in x
        return (o.getX() != ox || o.getY() != oy); // return whether either x or y actually changed
    }

    /**
     * Attempts to move the x-component of a given world object. Will perform all necessary collision detections and
     * reactions
     * @param o the object to move
     * @param dx the x component to move the object by
     * @return the object collided with (either a WorldObject or a Block), or null if no collision occurred
     */
    private static Object moveX(WorldObject o, float dx) {
        o.setX(o.getX() + dx); // make the move
        Block a = checkBlocks(o, null, false); // check for block collisions first
        if (a != null) { // if hit a block
            float pushback = calcPBFromBlock(o.getAABB(), a, false); // calculate the pushback from the block
            rectifyXCollision(o, a, pushback); // rectify the collision using pushback
            Pair rxn = performReaction(o.getVX(), o.getPhysicsProperties()); // perform the reaction
            o.setVX(rxn.x); // set the new velocity
            o.setVY(o.getVY() * rxn.y); // use the opposite component multiplier
            return a; // return the hit block
        } else { // otherwise, check other world objects
            AABB aabb = null; // don't get axis-aligned bounding box until absolutely necessary
            for (WorldObject wo : o.getCollidables()) { // for each world object
                if (wo.getPhysicsProperties().collidable && wo != o) { // if it is collidable and isn't the same object
                    if (aabb == null) aabb = o.getAABB(); // make sure have axis-aligned bounding box
                    Pair pb = AABBColliding(aabb, wo.getAABB()); // check if o and wo are colliding
                    if (pb != null) { // if the objects are colliding
                        float pushback = pb.x; // get pushback from return
                        rectifyXCollision(o, wo, pushback); // rectify the collision using pushback
                        Pair[] rxn = performReaction(o.getVX(), wo.getVX(), o.getPhysicsProperties(),
                                wo.getPhysicsProperties()); // get reaction from collision
                        o.setVX(rxn[0].x); // set o's new velocity
                        o.setVY(o.getVY() * rxn[0].y); // apply o's y velocity multiplier
                        wo.setVX(rxn[1].x); // set wo's new velocity
                        wo.setVY(wo.getVY() * rxn[1].y); // apply p's y velocity multiplier
                        return wo; // return the hit world object
                    }
                }
            }
        }
        return null; // if no x collisions, return null
    }

    /**
     * Attempts to move the y-component of a given world object. Will perform all necessary collision detections and
     * reactions
     * @param o the object to move
     * @param dy the y component to move the object by
     * @param exclude an object that should be excluded from collision detection. Usually, if a collision occurred in
     *               an x-component move, the excluded object is the collided object
     */
    private static void moveY(WorldObject o, float dy, Object exclude) {
        o.setY(o.getY() + dy); // make the move
        // if a block was previously hit, get it to pass along to the block checking method
        Block b = (exclude == null) ? null : ((exclude instanceof Block) ? ((Block)exclude) : null);
        Block a = checkBlocks(o, b, true); // check for block collisions first
        if (a != null) { // if hit a block
            float pushback = calcPBFromBlock(o.getAABB(), a, true); // calculate the pushback from the block
            rectifyYCollision(o, a, pushback); // rectify the collision using pushback
            Pair rxn = performReaction(o.getVY(), o.getPhysicsProperties()); // perform the reaction
            o.setVY(rxn.x); // set the new velocity
            o.setVX(o.getVX() * rxn.y); // use the opposite component multiplier
        } else { // otherwise, check other world objects
            AABB aabb = null; // don't get axis-aligned bounding box until absolutely necessary
            for (WorldObject wo : o.getCollidables()) { // for each world object
                // if it is collidable, isn't the same object, or isn't the excluded object
                if (wo.getPhysicsProperties().collidable && wo != o && wo != exclude) {
                    if (aabb == null) aabb = o.getAABB(); // make sure have axis-aligned bounding box
                    Pair pb = AABBColliding(aabb, wo.getAABB()); // check if o and wo are colliding
                    if (pb != null) { // if the objects are colliding
                        float pushback = pb.y; // get pushback from return
                        rectifyYCollision(o, wo, pushback); // rectify the collision using pushback
                        Pair[] rxn = performReaction(o.getVY(), wo.getVY(), o.getPhysicsProperties(),
                                wo.getPhysicsProperties()); // perform a reaction based on the collision
                        // the reaction returns two pairs corresponding to new velocity values/multipliers
                        o.setVY(rxn[0].x); // set o's new y velocity
                        o.setVX(o.getVX() * rxn[0].y); // use o's x velocity multiplier
                        wo.setVY(rxn[1].x); // set wo's new y velocity
                        wo.setVX(wo.getVX() * rxn[1].y); // use wo's x velocity multiplier
                        return; // don't check any other objects
                    }
                }
            }
        }
    }

    /**
     * Checks a given world object for collision with blocks in the block map
     * @param o the object to check
     * @param exclude any blocks to exclude from collision detection. Usually, if a collision occurred in
     *                an x-component move, the y-component is checked with the collided block from the x-component
     *                move as the excluded block
     * @param y whether this is a check based on a y-component move
     * @return the block that was collided, or null if no collision occurred
     */
    private static Block checkBlocks(WorldObject o, Block exclude, boolean y) {
        List<Pair> points = getPointsToTest(o.getAABB());
        for (Pair point : points) { // for each point to check
            Pair opoint = new Pair(point);
            Transformation.getGridCell(point); // get the grid cell it is in
            // if the corner is within the bounds of the block map
            if (point.x >= 0 && point.x < blockMap.length && point.y >= 0 && point.y < blockMap[0].length) {
                Block b = blockMap[(int)point.x][(int)point.y]; // try to get the block in the grid cell
                if (b != null) { // if a block is actually there
                    if (exclude == null || b != exclude) { // if it isn't the excluded block
                        /* before we can conclude that a collision has occurred, the pushback of an opposite component
                           collision must be calculated. If it is zero, then that means the object is snug against
                           the object in question in the other component. Technically, this would lead to a collision
                           and would cause the object to randomly move in the opposite direction of the snugness.
                           For example, without this check, objects will climb walls when they are snug against them */
                        float oppPB = calcPBFromBlock(o.getAABB(), b, !y); // calculate the opposite pushback
                        if (oppPB != 0) return b; // if the opposite pushback is not zero, then this is valid collision
                    }
                }
            }
        }
        return null; // if none of the corners were within used cells of the block map, no block collision occured
    }

    /**
     * Populates a list of points to check for block collision. This includes the corners as well as additional
     * points along the edges of objects whose width or height are greater than 1f
     * @param a the axis-aligned bounding box of the object for whom to populate a list of points
     * @return the populated list of points
     */
    private static List<Pair> getPointsToTest(AABB a) {
        List<Pair> points = new ArrayList<>();
        // start with corners
        Pair bottomLeft = new Pair(a.getCX() - a.getW2(), a.getCY() - a.getH2());
        Pair topLeft = new Pair(a.getCX() - a.getW2(), a.getCY() + a.getH2());
        Pair topRight = new Pair(a.getCX() + a.getW2(), a.getCY() + a.getH2());
        Pair bottomRight = new Pair(a.getCX() + a.getW2(), a.getCY() - a.getH2());
        /*
          If extra points on the edges are not added, blocks will be able to go through larger objects because only
          the corners are being checked.
         */
        // add any extra points on the bottom and top
        for (float x = bottomLeft.x + 1; x < bottomRight.x; x++) {
            points.add(new Pair(x, bottomLeft.y));
            points.add(new Pair(x, topLeft.y));
        }
        // add any extra points on the left and right
        for (float y =  bottomLeft.y + 1; y < topLeft.y; y++) {
            points.add(new Pair(bottomLeft.x, y));
            points.add(new Pair(bottomRight.x, y));
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
     * @param o the axis-aligned bounding box of the world object who collided with a block
     * @param b the block collided with
     * @param y whether the push back should be calculated for the y component
     * @return the appropriate push back value
     */
    private static float calcPBFromBlock(AABB o, Block b, boolean y) {
        // calculate overlap between block and axis-aligned bounding box and return that
        if (y) return Math.abs(o.getCY() - Transformation.getCenterOfCellComponent(b.getY())) - o.getH2() - 0.5f;
        else return Math.abs(o.getCX() - Transformation.getCenterOfCellComponent(b.getX())) - o.getW2() - 0.5f;
    }

    /**
     * Checks if two world objects are colliding by considering their axis-aligned bounding boxes
     * @param a the first object's AABB
     * @param b the second object's AABB
     * @return null if no collision, or a Pair containing the necessary x or y push-back to back out of the collision
     */
    private static Pair AABBColliding(AABB a, AABB b) {
        // get the x distance between them
        float dx = Math.abs(a.getCX() - b.getCX()) - a.getW2() - b.getW2();
        if (dx >= COLLISION_THRESHOLD) return null; // if it's greater than threshold, no collision
        // get the y distance between them
        float dy = Math.abs(a.getCY() - b.getCY()) - a.getH2() - b.getH2();
        if (dy >= COLLISION_THRESHOLD) return null; // if it's greater than threshold, no collision
        return new Pair(dx, dy); // if both were less than threshold, collision
    }

    /**
     * Rectifies an X collision by pushing the colliding object out of the collided object
     * @param o the colliding object
     * @param a the collided object (either a world object or a block)
     * @param pushback the pushback
     */
    private static void rectifyXCollision(WorldObject o, Object a, float pushback) {
        float pbDir = (a instanceof WorldObject) ? // calculate the direction of the pushback
                (((WorldObject) a).getX() > o.getX() ? 1f : -1f) :
                (Transformation.getCenterOfCellComponent(((Block) a).getX()) > o.getX() ? 1f : -1f);
        o.setX(o.getX() + pbDir * pushback); // push o back out of the collision
    }

    /**
     * Rectifies a Y collision by pushing the colliding object out of the collided object
     * @param o the colliding object
     * @param a the collided object (either a world object or a block)
     * @param pushback the pushback
     */
    private static void rectifyYCollision(WorldObject o, Object a, float pushback) {
        float pbDir = (a instanceof WorldObject) ? // calculate the direction of the pushback
                (((WorldObject)a).getY() > o.getY() ? 1f : -1f) :
                (Transformation.getCenterOfCellComponent(((Block)a).getY()) > o.getY() ? 1f : -1f);
        o.setY(o.getY() + pbDir * pushback); // push o back out of the collision
    }

    /**
     * Calculates a collision reaction between two world objects given their velocities and physics properties. This
     * will work for either x or y component
     * @param va the velocity of the first world object
     * @param vb the velocity of the second world object
     * @param ppa the physics properties of the first world object
     * @param ppb the physics properties of the second world object
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
     * Calculates a collision reaction between a world object and a block given the world object's velocity and
     * physics properties
     * @param v the velocity of the world object
     * @param pp the physics properties of the world object
     * @return a pair where the x value is the object's new velocity in the component in question and the y value is
     *         a multiplier for the object's velocity in the opposite component
     */
    private static Pair performReaction(float v, PhysicsProperties pp) {
        if (pp.rigid) return new Pair(0f, 1f);
        else return new Pair(-v * pp.bounciness, pp.fricResis);
    }

    /**
     * Calculate if there is a world object or block next the the given world object in the given direction. X-component
     * and y-component checks may be combined (for example, can use this to look if there is an object to the "bottom
     * left" of the given world object). This works for non-collidable objects as well, but will not count other
     * non-collidable objects as valid checks
     * @param wo the world object to look next to
     * @param x the x direction to look. If x < 0, will look to the left of the object. If x > 0, will look to the
     *          right of the object. If x == 0, will not look in any x-component direction.
     * @param y the y direction to look. If y < 0, will look beneath the object. If y > 0, will look above the object.
     *          If y == 0, will not look in any y-component direction.
     * @return if there is a world object or block in the given direction of the given object
     */
    public static boolean nextTo(WorldObject wo, float x, float y) {
        // calculate the necessary changes in position to perform the check
        float dx = NEXT_TO_PRECISION * (x < 0 ? -1 : (x > 0 ? 1 : 0));
        float dy = NEXT_TO_PRECISION * (y < 0 ? -1 : (y > 0 ? 1 : 0));
        // make the necessary changes to perform the check
        wo.setX(wo.getX() + dx);
        wo.setY(wo.getY() + dy);
        // check blocks first
        if (checkBlocks(wo, null, true) != null) { // if block collision
            // return to original position
            wo.setX(wo.getX() - dx);
            wo.setY(wo.getY() - dy);
            return true; // there is something in that direction
        } else {
            List<WorldObject> collidables = wo.getCollidables(); // get possible collisions
            for (WorldObject o : collidables) { // for each collidable object
                if (o.getPhysicsProperties().collidable && o != wo) { // if the object has collision on and isn't wo
                    if (AABBColliding(wo.getAABB(), o.getAABB()) != null) { // if they collide
                        // return to original position
                        wo.setX(wo.getX() - dx);
                        wo.setY(wo.getY() - dy);
                        return true; // there is something in that direction
                    }
                }
            }
        }
        // return to original position
        wo.setX(wo.getX() - dx);
        wo.setY(wo.getY() - dy);
        return false; // nothing in that direction
    }

    /**
     * Tells the physics engine what block map to use for detecting collisions with blocks
     * @param blockMap the block map to use
     */
    public static void giveBlockMap(Block[][] blockMap) {
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
        public float fricResis = 0.98f; /* when objects collide, friction resistance determines how much of the
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
        public PhysicsProperties() {}
    }
}
