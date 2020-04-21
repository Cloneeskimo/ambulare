package utils;

import gameobject.GameObject;

/**
 * Uses the Separating Axis Theorem to detect collision
 * This only works with rectangular frames. For models that are not rectangular, rectangular bounding boxes are
 * provided anyways, so this should work for all general cases
 * Separating Axis Theorem: If you are able to draw a line to separate two polygons, then they do not collide
 */
public class CollisionDetector {

    /**
     * Calculates if the two given game objects are colliding
     * @param a the first game object to consider
     * @param b the second game object to consider
     * @return whether the two game objects are colliding
     */
    public static boolean colliding(GameObject a, GameObject b) {
        Frame fa = a.getFrame(), fb = b.getFrame(); // get their frames
        Pair[] axes1 = getAxes(fa.getR()); // get axes based on a's rotation
        Pair[] axes2 = getAxes(fb.getR()); // get axes based on b's rotation
        Pair[] axes = new Pair[4]; // normalize and combine axes into one array
        for (int i = 0; i < 4; i++) axes[i] = normalize(i > 1 ? axes2[i - 2] : axes1[i]); // normalize/combine
        Pair[] fac = fa.getCorners(); // get a's corner points
        Pair[] fbc = fb.getCorners(); // get b's corner points
        for (Pair axis : axes) { // for each axis
            Pair proja = project(fac, axis); // project a onto it
            Pair projb = project(fbc, axis); // then project b onto it
            if (proja.y < projb.x || projb.y < proja.x) return false; /* and if any of a's projections don't overlap
                                                                         b's, there is no collision */
        }
        return true; // if all of the projections overlapped, the objects are colliding
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
}
