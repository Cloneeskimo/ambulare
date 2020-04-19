package utils;

import graphics.Camera;
import graphics.Model;

/**
 * Provides methods to transform between the various coordinate systems listed and described below
 * - window coordinates: coordinates of the window's pixels
 *     /\ +y
 *  0 <  > +x
 *     \/ 0
 * - normalized coordinates: coordinates of the screen from -1 to 1
 *     /\ +1
 * -1 <  > +1
 *     \/ -1
 * - aspect coordinates: normalized coordinates adjusted for aspect ratio such that a square is always a square
 *     /\ +y
 * -x <  > +x
 *     \/ -y
 * - world coordinates: coordinates of a world where a camera's position and zoom are taken into account
 *     /\ -y
 * -x <  > +x
 *     \/ -y
 * - grid coordinates: world coordinates where items are locked into an grid with discrete cells
 *     /\ -y
 * -x <  > +x
 *     \/ -y
 */
public class Transformation {

    /**
     * Converts the given window coordinates to normalized coordinates
     * @param pos the coordinates to convert
     * @param w the width of the window
     * @param h the height of the window
     */
    public static void windowToNorm(Coord pos, int w, int h) {
        pos.x = ((pos.x / (float)w) * 2) - 1; // normalize x
        pos.y = ((pos.y / (float)h) * 2) - 1; // normalize y
    }

    /**
     * Converts the given normalized coordinates to aspect coordinates
     * @param pos the coordinates to convert
     * @param ar the aspect ratio of the window
     */
    public static void normToAspect(Coord pos, float ar) {
        if (ar > 1.0f) pos.x *= ar; // widen x if necessary
        else pos.y /= ar; // thin y if necessary
    }

    /**
     * Converts the given aspect coordinates to world coordinates
     * @param pos the coordinates to convert
     * @param cam the camera
     */
    public static void aspectToWorld(Coord pos, Camera cam) {
        pos.x = (pos.x / cam.getZoom()) + cam.getX(); // account for zoom and camera x
        pos.y = (pos.y / cam.getZoom()) + cam.getY(); // account for zoom and camera y
    }

    /**
     * Converts the given window coordinates to world coordinates
     * @param pos the coordinates to convert
     * @param w the width of the window
     * @param h the height of the window
     * @param ar the aspect ratio of the window
     * @param cam the camera
     */
    public static void windowToWorld(Coord pos, int w, int h, float ar, Camera cam) {
        windowToNorm(pos, w, h); // convert window to normalized
        normToAspect(pos, ar); // then convert normalized to aspect
        aspectToWorld(pos, cam); // then convert aspect to world
    }

    /**
     * Converts the given world coordinates to grid coordinates
     * This assumes a grid in which each cell is the standard square size as defined by the Model class
     * @param pos the coordinates to convert
     */
    public static void worldToGrid(Coord pos) {
        pos.x += pos.x < 0 ? (-Model.STD_SQUARE_SIZE / 2) : (Model.STD_SQUARE_SIZE / 2); /* the positions of all of the
                                                                                            objects in this game have
                                                                                            (0, 0) at the center of
                                                                                            their models. This basically
                                                                                            nudges them over slightly so
                                                                                            as to make the truncation
                                                                                            correct*/
        pos.y += pos.y < 0 ? (-Model.STD_SQUARE_SIZE / 2) : (Model.STD_SQUARE_SIZE / 2); // see above comment
        pos.x = (int)(pos.x / Model.STD_SQUARE_SIZE); // align to grid where cell width is Model's standard square size
        pos.y = (int)(pos.y / Model.STD_SQUARE_SIZE); // align to grid where cell height is Model's standard square size
    }

    /**
     * Converts the given window coordinates to grid coordinates
     * This assumes a grid in which each cell is the standard square size as defined by the Model class
     * @param pos the coordinates to convert
     * @param w the width of the window
     * @param h the height of the window
     * @param ar the aspect ratio of the window
     * @param cam the camera
     */
    public static void windowToGrid(Coord pos, int w, int h, float ar, Camera cam) {
        windowToWorld(pos, w, h, ar, cam); // convert window to world
        worldToGrid(pos); // convert world to grid
    }

    /**
     * Converts the given grid coordinates to world coordinates
     * This assumes a grid in which each cell is the standard square size as defined by the Model class
     * @param pos the coordinates to convert
     */
    public static void gridToWorld(Coord pos) {
        pos.x *= Model.STD_SQUARE_SIZE; // un-align where cell width is Model's standard square size
        pos.y *= Model.STD_SQUARE_SIZE; // un-align where cell height is Model's standard square size
        pos.x += pos.x < 0 ? (Model.STD_SQUARE_SIZE / 2) : (-Model.STD_SQUARE_SIZE / 2); /* move slightly over so that
                                                                                            the origin corresponds to
                                                                                            the middle of the model. See
                                                                                            worldToGrid() */
        pos.y += pos.y < 0 ? (Model.STD_SQUARE_SIZE / 2) : (-Model.STD_SQUARE_SIZE / 2); // see above comment
    }

    /**
     * Converts the given world coordinates to aspect coordinates
     * @param pos the coordinates to convert
     * @param cam the camera
     */
    public static void worldToAspect(Coord pos, Camera cam) {
        pos.x = (pos.x - cam.getX()) * cam.getZoom(); // account for camera zoom and x
        pos.y = (pos.y - cam.getY()) * cam.getZoom(); // account for camera zoom and y
    }

    /**
     * Converts the given aspect coordinates to normalized coordinates
     * @param pos the coordinates to convert
     * @param ar the aspect ratio of the window
     */
    public static void aspectToNorm(Coord pos, float ar) {
        if (ar > 1.0f) pos.x /= ar; // slim x if necessary
        else pos.y *= ar; // widen y if necessary
    }

    /**
     * Converts the given normalized coordinates to window coordinates
     * @param pos the coordinates to convert
     * @param w the width of the window
     * @param h the height of the window
     */
    public static void normToWindow(Coord pos, int w, int h) {
        pos.x = ((pos.x + 1) / 2) * w; // un-normalize x
        pos.y = ((pos.y + 1) / 2) * h; // un-normalize y
    }

    /**
     * Converts the given world coordinates to window coordinates
     * @param pos the coordinates to convert
     * @param cam the camera
     * @param ar the aspect ratio of the window
     * @param w the width of the window
     * @param h the height of the window
     */
    public static void worldToWindow(Coord pos, Camera cam, float ar, int w, int h) {
        worldToAspect(pos, cam); // convert world to aspect
        aspectToNorm(pos, ar); // convert aspect to normalized
        normToWindow(pos, w, h); // convert normalized to window
    }

    /**
     * Converts the given grid coordinates to window coordinates
     * @param pos the coordinates to convert
     * @param cam the camera
     * @param ar the aspect ratio of the window
     * @param w the width of the window
     * @param h the height of the window
     */
    public static void gridToWindow(Coord pos, Camera cam, float ar, int w, int h) {
        gridToWorld(pos); // convert grid to world
        worldToWindow(pos, cam, ar, w, h); // convert world to window
    }
}
