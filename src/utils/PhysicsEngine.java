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
 * doing so, it checks for collisions with blocks using a block map, collisions with slopes using a slope map, and
 * collisions with other collidable world objects. Note that the physics engine rounds all velocities and positions it
 * is given and it produces to the amount of digits corresponding to ROUNDED_FORMAT in order to avoid precision errors.
 * However, errors will still occur when dealing with very large floating point numbers. ROUNDED_FORMAT can be modified
 * to deal with this by rounding to fewer decimal points at the cost of more precise movements. The physics engine also
 * outlines a set of properties than each world object must have which are used to calculate reactions to collisions
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
    private static boolean[][] blockMap;   // the block map to use for collision detection with blocks
    private static SlopeType[][] slopeMap; // the slope map to use for collision detection with slopes
    public static final float UNIT_AND_HALF = 0.0015f; /* the minimum unit (as specified by the rounding performed by
        the physics engine by ROUNDED_FORMAT) to be added to a push-back vector in order to resolve a collision */
    public static final float TERMINAL_VELOCITY = -50f; /* the minimum vertical velocity from gravity. Note that the
        the physics engine does not apply gravity. It is up to the object to apply it, hence public access */
    public static final float NEXT_TO_PRECISION = 0.005f; // how close objects must be to be next to each other

    /**
     * Flags describing how to respond to a block/slope collision. For more information, see checkBlocksAndSlopes()
     */
    private static final int NO_COLLISION = -1;         // no collision occurred
    private static final int RESPOND_AS_BLOCK = 0;      // respond to the collision normally as a block collision
    private static final int RESPOND_AS_BLOCK_IN_Y = 1; // respond to the collision as a block collision in y component
    private static final int RESPOND_AS_SLOPE = 2;      // respond to the collision as a slope collision

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
             * Check x axis
             */
            if (dx != 0) { // if an x movement is desired
                o.setX(round(ox + dx)); // make the move and round the new position
                AABB aabb = o.getAABB(); // get the axis-aligned bounding box after movement

                /*
                 * Check blocks and slopes on x axis
                 */
                Pair<Integer> cell = new Pair<>(); // create a pair to store collided cell
                int collision = checkBlocksAndSlopes(aabb, cell, 0); // check for collisions with blocks/slopes
                boolean bottom = collision != NO_COLLISION && (slopeMap[cell.x][cell.y] == SlopeType.NegativeBottom ||
                        slopeMap[cell.x][cell.y] == SlopeType.PositiveBottom);
                if (collision == RESPOND_AS_BLOCK_IN_Y) {

                    /*
                     * Respond as vertical block collision
                     */
                    float pb = calcPBFromBlock(aabb, cell, true); // calculate the push-back for resolution
                    if ((float) cell.y + 0.5f < o.getY()) pb *= -1; // make sure the sign is correct
                    o.setY(round(o.getY() + pb)); // perform collision resolution
                    Pair<Float> rxn = performReaction(o.getVY(), o.getPhysicsProperties()); // calculate a reaction
                    // apply the reaction to the object's velocity
                    o.setVY(bottom ? Math.max(o.getVY(), rxn.x) : rxn.x);
                    o.setVX(rxn.y * o.getVX());
                    dy = Math.max(0, dy); // do not check for y collisions this loop
                } else if (collision == RESPOND_AS_SLOPE) {

                    /*
                     * Respond as slope collision
                     */
                    // move up to respond to horizontal movement
                    o.setY(round(o.getY() + calcPBFromSlope(aabb, cell, true)));
                    aabb = o.getAABB(); // calculate new AABB
                    Pair<Integer> newCell = new Pair<>(); // create a pair to hold result of another check
                    // check if move up causes a new collision
                    int newCollision = checkBlocksAndSlopes(aabb, newCell, 0);
                    if (newCollision != NO_COLLISION) { // if there was a new collision
                        // if the new collision was with another slope, just reset to the original position
                        if (newCollision == RESPOND_AS_SLOPE) o.setPos(ox, oy);
                        else { // if the new collision was not with another slope
                            // move object back down to resolve with object above
                            o.setY(round(o.getY() + calcPBFromBlock(aabb, newCell, true)));
                            // then push back horizontally enough to resolve collision with the slope underneath
                            o.setX(round(o.getX() + calcPBFromSlope(o.getAABB(), cell, false)));
                        }
                        // perform a horizontal reaction
                        Pair<Float> rxn = performReaction(o.getVX(), o.getPhysicsProperties());
                        o.setVX(rxn.x);
                        o.setVY(rxn.y * o.getVY());
                    } else { // otherwise, perform a vertical reaction
                        Pair<Float> rxn = performReaction(o.getVY(), o.getPhysicsProperties()); // calculate a reaction
                        // apply the reaction to the object's velocity
                        o.setVY(bottom ? Math.max(o.getVY(), rxn.x) : rxn.x);
                        o.setVX(rxn.y * o.getVX());
                        dy = Math.max(0, dy); // do not check for y collisions this loop
                    }
                } else if (collision == RESPOND_AS_BLOCK) {

                    /*
                     * Respond as horizontal block collision
                     */
                    float pb = calcPBFromBlock(aabb, cell, false); // calculate the push-back for resolution
                    if ((float) cell.x + 0.5f < o.getX()) pb *= -1; // make sure the sign is correct
                    o.setX(round(o.getX() + pb)); // perform collision resolution
                    Pair<Float> rxn = performReaction(o.getVX(), o.getPhysicsProperties()); // calculate a reaction
                    // apply the reaction to the object's velocity
                    o.setVX(rxn.x);
                    o.setVY(rxn.y * o.getVY());
                }

                /*
                 * Check world objects on x axis
                 */
                if (collision == NO_COLLISION) { // only check world object collision if no block collision has occurred
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
             * Update change in y if object is sticky, based on horizontal movement
             */
            // if the object sticks to slopes and is on a slope
            if (o.getPhysicsProperties().sticky && o.getPhysicsProperties().onSlope != null) {
                float adx = o.getX() - ox; // calculate the horizontal change that occurred
                // apply the change to the vertical component as well to simulate sticking to the slop
                if (adx < 0 && o.getPhysicsProperties().onSlope == SlopeType.PositiveBottom) dy += adx;
                else if (adx > 0 && o.getPhysicsProperties().onSlope == SlopeType.NegativeBottom) dy -= adx;
            }

            /*
             * Check y axis
             */
            if (dy != 0) { // if a y movement is desired
                o.setY(round(oy + dy)); // make the move and round the new position
                AABB aabb = o.getAABB(); // get the axis-aligned bounding box after movement

                /*
                 * Check blocks and slopes on y axis
                 */
                Pair<Integer> cell = new Pair<>(); // create a pair to store collided cell
                int collision = checkBlocksAndSlopes(aabb, cell, dy); // check for collisions with blocks/slopes
                // preemptively calculate whether slope is a bottom slope in the collided cell
                boolean bottom = collision != NO_COLLISION && (slopeMap[cell.x][cell.y] == SlopeType.NegativeBottom ||
                        slopeMap[cell.x][cell.y] == SlopeType.PositiveBottom);
                if (collision == RESPOND_AS_BLOCK_IN_Y || collision == RESPOND_AS_BLOCK) {

                    /*
                     * Respond as vertical block collision
                     */
                    float pb = calcPBFromBlock(aabb, cell, true); // calculate the push-back for resolution
                    if ((float) cell.y + 0.5f < o.getY()) pb *= -1; // make sure the sign is correct
                    o.setY(round(o.getY() + pb)); // perform collision resolution
                    Pair<Float> rxn = performReaction(o.getVY(), o.getPhysicsProperties()); // calculate a reaction
                    // apply the reaction to the object's velocity
                    o.setVY((collision == RESPOND_AS_BLOCK || !bottom) ? rxn.x : Math.max(o.getVY(), rxn.x));
                    o.setVX(rxn.y * o.getVX());
                } else if (collision == RESPOND_AS_SLOPE) {

                    /*
                     * Respond as slope collision
                     */
                    o.setY(round(o.getY() + calcPBFromSlope(aabb, cell, true))); // calc and apply PB
                    Pair<Float> rxn = performReaction(o.getVY(), o.getPhysicsProperties()); // calculate a reaction
                    // apply the reaction to the object's velocity
                    o.setVY(bottom ? Math.max(o.getVY(), rxn.x) : rxn.x);
                    o.setVX(rxn.y * o.getVX());
                }


                /*
                 * check world objects on y axis
                 */
                if (collision == NO_COLLISION) { // only check world object collision if no block collision has occurred
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

        /*
         * Update sticky object onSlope value
         */
        if (o.getPhysicsProperties().sticky) { // if the object is sticky
            float oyb = o.getY(); // save object's original y
            o.setY(round(oyb - NEXT_TO_PRECISION)); // move it down according to the next to precision
            List<Pair<Float>> points = getPointsToTest(o.getAABB()); // get the points to test
            // save an x/y of the cell whose slope is most appropriate
            int x = -1;
            int y = -1;
            float closerX = Float.POSITIVE_INFINITY; // want to consider closest horizontal cell to be one object is on
            for (Pair<Float> point : points) { // for each point
                Pair<Integer> cell = Transformation.getGridCell(point); // get the corresponding grid cell
                if (cell.x >= 0 && cell.x < blockMap.length && cell.y >= 0 && cell.y < blockMap[0].length) {
                    if (slopeMap[cell.x][cell.y] != null && (
                        slopeMap[cell.x][cell.y] == SlopeType.NegativeBottom ||
                        slopeMap[cell.x][cell.y] == SlopeType.PositiveBottom)) { // if there is a bottom slope there
                        float d = Math.abs(((float)cell.x + 0.5f) - o.getX()); // get the horizontal distance to object
                        if (d < closerX) { // if closer
                            closerX = d; // save as new closest
                            // update correct cell position
                            x = cell.x;
                            y = cell.y;
                        }
                    }
                }
            }
            // if a slope wass found, update the object's onSlope value
            if (x != -1) o.getPhysicsProperties().onSlope = slopeMap[x][y];
            else o.getPhysicsProperties().onSlope = null; // otherwise set to null
            o.setY(oyb); // return the object to its original y position
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
     * Checks an axis-aligned bounding box for collision with blocks in the block map and slopes in the slope map
     *s
     * @param aabb the axis-aligned bounding box to check for collision with blocks and slopes
     * @param collision the pair to populate with the collided cell if a collision occurs
     * @param dy the change in the vertical positional component that was made before this test
     * @return the result of the check as an integer specifying how to respond where the possible returns are load out
     * at the top of this file
     */
    private static int checkBlocksAndSlopes(AABB aabb, Pair<Integer> collision, float dy) {
        List<Pair<Float>> points = getPointsToTest(aabb); // calculate the points to apply on the grid for testing
        for (Pair<Float> point : points) { // for each point
            Pair<Integer> cell = Transformation.getGridCell(point); // get the corresponding grid cell
            // if the grid cell is not out of bounds
            if (cell.x >= 0 && cell.x < blockMap.length && cell.y >= 0 && cell.y < blockMap[0].length) {
                if (slopeMap[cell.x][cell.y] != null) { // if there is a slope there
                    // calculate the appropriate response to being in the slope's grid cell
                    int idx = respondToSlopePresence(aabb, points, cell.x, cell.y, dy);
                    if (idx != NO_COLLISION) { // if a response is necessary
                        // set the collision position
                        collision.x = cell.x;
                        collision.y = cell.y;
                        return idx; // return the type of response necessary
                    }
                }
                else if (blockMap[cell.x][cell.y]) {
                    // set the collision position
                    collision.x = cell.x;
                    collision.y = cell.y;
                    return RESPOND_AS_BLOCK; // return that a normal block collision response is necessary
                }
            }
        }
        return NO_COLLISION; // if no collision occurs, return -1
    }

    /**
     * If an axis-aligned bounding box is within a slope's cell, this method will interpret and calculate howw to
     * respond to it if resolution is required
     * @param aabb the axis-aligned bounding box
     * @param points the points used to check for collision with the slope's grid cell
     * @param x the slope's x grid coordinate
     * @param y the slopy's y grid coordinate
     * @param dy the change in the vertical positional component that was made before this test
     * @return the result of the check as an integer specifying how to respond where the possible returns are load out
     * at the top of this file
     */
    private static int respondToSlopePresence(AABB aabb, List<Pair<Float>> points, int x, int y, float dy) {
        SlopeType type = slopeMap[x][y]; // get the type of slope at the given grid cell
        int pAboveBottomEdge = 0; // a counter for how many points of the AABB are above the bottom edge of slope
        int pBelowTopEdge = 0; // a counter for how many points of the AABB are below the top edge of the slope
        int pLeftOfRightEdge = 0; // a counter for how many points of the AABB are to the left of the right edge
        int pRightOfLeftEdge = 0; // a counter for how many points of the AABB are to the right of the left edge
        for (Pair<Float> p : points) { // for each point
            if (p.x >= x) pRightOfLeftEdge++; // count points to right of left edge
            if (p.x <= (x + 1)) pLeftOfRightEdge++; // count points to left of right edge
            if (p.y - dy >= y) pAboveBottomEdge++; // count points above bottom edge
            if (p.y - dy <= (y + 1)) pBelowTopEdge++; // count points below top edge
        }

        switch (type) { // switch on the type of slope
            case PositiveBottom: // if positive bottom slope
                // if all points are above the bottom edge and to the left of the right edge
                if (pLeftOfRightEdge == points.size() && pAboveBottomEdge == points.size()) {
                    // calculate the minimum y value that the bottom right point can have (relative to the grid cell)
                    float minY = round((aabb.getCX() + aabb.getW2()) % 1);
                    float ay = (aabb.getCY() - aabb.getH2() - y) ; // calculate the actual bottom right y (relative)
                    if (ay < minY) return RESPOND_AS_SLOPE; // if the actual is below the minimum slope collision occurs
                    else return NO_COLLISION; // otherwise, no collision occurs
                } else if (pLeftOfRightEdge < points.size()) { // if some points are to the ridge of the slope
                    // if AABB below top edge of the slope and center to the right of slope, respond as if block
                    if (aabb.getCY() - aabb.getH2() < y + 1 && aabb.getCX() > x + 1) return RESPOND_AS_BLOCK;
                    return RESPOND_AS_BLOCK_IN_Y; // otherwise respond vertically as a block
                }
                break;
            case NegativeBottom: // if negative bottom slope
                // if all points are above the bottom edge and to the right of the left edge
                if (pRightOfLeftEdge == points.size() && pAboveBottomEdge == points.size()) {
                    // calculate the minimum y value that the bottom left point can have (relative to the grid cell)
                    float minY = 1 - round((aabb.getCX() - aabb.getW2()) % 1);
                    float ay = (aabb.getCY() - aabb.getH2() - y); // calculate the actual bottom right y (relative)
                    if (ay < minY) return RESPOND_AS_SLOPE; // if the actual is below the minimum slope collision occurs
                    else return NO_COLLISION; // otherwise, no collision occurs
                } else if (pRightOfLeftEdge < points.size()) { // if some points are to the left of the slope
                    // if AABB below top edge of the slope and center to the left of slope, respond as if block
                    if (aabb.getCY() - aabb.getH2() < y + 1 && aabb.getCX() < x) return RESPOND_AS_BLOCK;
                    return RESPOND_AS_BLOCK_IN_Y; // otherwise respond vertically as a block
                }
                break;
            case PositiveTop: // if positive top slope
                // if all points are below the top edge and to the right of the left edge
                if (pRightOfLeftEdge == points.size() && pBelowTopEdge == points.size()) {
                    // calculate the maximum y value that the top left point can have (relative to the grid cell)
                    float maxY = round((aabb.getCX() - aabb.getW2()) % 1);
                    float ay = (aabb.getCY() + aabb.getH2() - y); // calculate the actual top left y (relative)
                    if (ay > maxY) return RESPOND_AS_SLOPE; // if the actual is above the maximum slope collision occurs
                    else return NO_COLLISION; // otherwise, no collision occurs
                } else if (pRightOfLeftEdge < points.size()) { // if some points are to the left of the slope
                    // if AABB above bottom edge of the slope and center to the left of slope, respond as if block
                    if (aabb.getCY() + aabb.getH2() > y && aabb.getCX() < x) return RESPOND_AS_BLOCK;
                    return RESPOND_AS_BLOCK_IN_Y; // otherwise respond vertically as a block
                }
                break;
            case NegativeTop: // if negative top slope
                // if all points are below the top edge and to the left of the right edge
                if (pLeftOfRightEdge == points.size() && pBelowTopEdge == points.size()) {
                    // calculate the maximum y value that the top right point can have (relative to the grid cell)
                    float maxY = 1 - round((aabb.getCX() + aabb.getW2()) % 1);
                    float ay = (aabb.getCY() + aabb.getH2() - y); // calculate the actual top right y (relative)
                    if (ay > maxY) return RESPOND_AS_SLOPE; // if the actual is above the maximum slope collision occurs
                    else return NO_COLLISION; // otherwise, no collision occurs
                } else if (pLeftOfRightEdge < points.size()) { // if some points are to the right of the slope
                    // if AABB above bottom edge of the slope and center to the right of slope, respond as if block
                    if (aabb.getCY() + aabb.getH2() > y && aabb.getCX() > x + 1) return RESPOND_AS_BLOCK;
                    return RESPOND_AS_BLOCK_IN_Y; // otherwise respond vertically as a block
                }
        }
        return RESPOND_AS_BLOCK; // if all else fails, respond as a block
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
        float v;
        if (y) v = Math.abs(o.getCY() - Transformation.getCenterOfCellComponent(b.y)) - o.getH2() - 0.5f
                - UNIT_AND_HALF;
        else v = Math.abs(o.getCX() - Transformation.getCenterOfCellComponent(b.x)) - o.getW2() - 0.5f
                - UNIT_AND_HALF;
        if (v > 0.2f) // if abnormally large pushback calculated
            Utils.log("Abnormally large block push-back value calculated: " + v + " in " + (y ? "y" : "x") +
                    "component", PhysicsEngine.class, "calcPBFromBlock", false); // log
        return v; // return value
    }

    /**
     * Calculates the push back from a collision between a world object and a slope
     * @param o the axis-aligned AABB of the object who has collided
     * @param p the slope's grid cell location
     * @param y whether to calculate the push back in the vertical component
     * @return the push back necessary to resolve collision
     */
    private static float calcPBFromSlope(AABB o, Pair<Integer> p, boolean y) {
        SlopeType type = slopeMap[p.x][p.y]; // get the type of slope
        boolean positive = type == SlopeType.PositiveBottom || type == SlopeType.PositiveTop; // is slope positive?
        boolean top = type == SlopeType.PositiveTop || type == SlopeType.NegativeTop; // is slope on top?
        boolean left = type == SlopeType.NegativeBottom || type == SlopeType.PositiveTop; // is slope on left?
        float v; // push back value
        if (y) { // if y push back is desired
            float marginY; // need to calculate max/min y
            if (left) marginY = round((o.getCX() - o.getW2()) % 1); // if left slope, calc as left point's x
            else marginY = round((o.getCX() + o.getW2()) % 1); // if right slope, calc as right points's x
            if (!positive) marginY = 1 - marginY; // if negative, invert
            float ay = (o.getCY() + (top ? 1f : -1f) * o.getH2()) - p.y; // calculate actual y
            float dif = Math.abs(marginY - ay); // calculate difference in actual and minimum
            v = (top ? -1f : 1f) * (dif + UNIT_AND_HALF); // use correct sign and add additional unit
        } else { // if an x push back is desired
            float marginX; // need to calculate max/min x
            if (top) marginX = round((o.getCY() + o.getH2()) % 1);
            else marginX = round((o.getCY() - o.getH2()) % 1);
            if (!positive) marginX = 1 - marginX;
            float ax = (o.getCX() + (left ? -1 : 1) * o.getW2()) - p.x; // calculate actual x
            float dif = Math.abs(marginX - ax); // calculate difference in actual and min/ax
            v = (left ? 1f : -1f) * (dif + UNIT_AND_HALF); // make negative and add necessary unti
        }
        if (Math.abs(v) > 0.2f) // if abnormally large push back calculated
            Utils.log("Abnormally large slope push-back value calculated: " + v + " in " + (y ? "y" : "x") +
                    "component", PhysicsEngine.class, "calcPBFromSlope", false); // log
        return v; // return the push back
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
     * Calculate if there is an object, block, or slope next the given world object in the given direction. X-component
     * and y-component checks may be combined (for example, can use this to look if there is an object to the "bottom
     * left" of the given world object). This works for non-collidable objects as well, but will not count other
     * non-collidable objects as valid checks. Looks are performed to a precision defined by the constant defined above
     * as NEXT_TO_PRECISION
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
        float dx = (x < 0f) ? -NEXT_TO_PRECISION : (x > 0f) ? NEXT_TO_PRECISION : 0f;
        float dy = (y < 0f) ? -NEXT_TO_PRECISION : (y > 0f) ? NEXT_TO_PRECISION : 0f;
        // save original position to reset later
        float ox = wo.getX();
        float oy = wo.getY();
        // move slightly next to according to parameters
        wo.setX(round(wo.getX() + dx));
        wo.setY(round(wo.getY() + dy));
        AABB aabb = wo.getAABB(); // get the object's axis-aligned bounding box
        // check if new position collides with blocks or slopes
        boolean nextTo = checkBlocksAndSlopes(aabb, new Pair<>(), dy) != NO_COLLISION;
        if (!nextTo) { // if no block or slope collision was found
            for (WorldObject o : wo.getCollidables()) { // and for each collidable object
                if (o != wo) { // don't check the object against itself
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
        PhysicsEngine.blockMap = blockMap; // save block map as member
        Utils.log("Received block map", PhysicsEngine.class, "giveBlockMap", false); // log
    }

    /**
     * Tells the physics engine what slope map to use for detecting collisions with slopes
     *
     * @param slopeMap the slope map to use,
     */
    public static void giveSlopeMap(SlopeType[][] slopeMap) {
        PhysicsEngine.slopeMap = slopeMap; // save slope map as member
        Utils.log("Received slope map", PhysicsEngine.class, "giveSlopeMap", false); // log
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
        public boolean sticky = false; // if true, the object will stick to slopes when descending them
        public boolean rigid = false; /* if an object is rigid, its velocity is unable to be affected by collisions
            with non-rigid objects. Rigid objects can still cause collisions. If they collide with other rigid objects,
            both of the colliding rigid objects will have their velocities reduced to zero in the component in
            question. Rigid objects are still affected by gravity unless the gravity property is changed to 0. Rigid
            objects produce collision reactions similar to blocks */
        public boolean collidable = true; /* this determines if an object is able to collide. If false, the object
            will be excluded from all collision detection and reaction calculations. */
        private SlopeType onSlope = null; // flag signifying what kind of slope the signifying object is on

        /**
         * Constructs the physics properties with the given properties, assuming non-rigid and collidable
         */
        public PhysicsProperties(float mass, float bounciness, float fricResis, float kbResis, float gravity,
                                 boolean sticky) {
            this.mass = mass;
            this.bounciness = bounciness;
            this.fricResis = fricResis;
            this.kbResis = kbResis;
            this.gravity = gravity;
            this.sticky = sticky;
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

        /**
         * @return the kind of slope the corresponding object is on, or null if none
         */
        public SlopeType onSlope() {
            return this.onSlope;
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
         * Constructs the axis-aligned bounding box using the given center bounding and width/height
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
         * Constructs the axis-aligned bounding box by copying the given other axis-aligned bounding box
         * @param other the other AABB whose properties to copy
         */
        public AABB(AABB other) {
            this.cx = other.cx;
            this.cy = other.cy;
            this.w2 = other.w2;
            this.h2 = other.h2;
        }

        /**
         * Calculates if the given point is within the axis-aligned bounding box
         *
         * @param x the x of the point to check
         * @param y the y of the point to check
         * @return whether the given point is within the axis-aligned bounding box
         */
        public boolean contains(float x, float y) {
            return (x >= this.cx - this.w2 && x <= this.cx + this.w2) &&
                    (y >= this.cy - this.h2 && y <= this.cy + this.h2);
        }

        /**
         * Multiplies the width and height of the axis-aligned bounding box by the given factor
         *
         * @param factor the factor to scale by
         */
        public void scale(float factor) {
            this.w2 *= factor; // scale width
            this.h2 *= factor; // scale height
        }

        /**
         * Adds the given value to the width/height of the axis-aligned bounding box
         * @param amount the amount to add
         */
        public void add(float amount) {
            this.w2 += amount / 2; // add amount to width
            this.h2 += amount / 2; // add amount to height
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

    /**
     * Define the four types of slopes
     */
    public enum SlopeType {
        PositiveBottom, NegativeBottom, PositiveTop, NegativeTop
    }
}
