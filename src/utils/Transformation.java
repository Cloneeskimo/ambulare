package utils;

import graphics.Camera;

/**
 * This class provides methods to transform between various coordinate systems such as those listed and described below:
 * All coordinates list below, except for window and grid, are normalized such that, if vertices were rendered at that
 * coordinate, they would have to be within the range of [-1, 1] (for both x and y) to actually be rendered. Aspect
 * coordinates used for rendering are the result of many transformations and calculations. Any of the normalized
 * coordinates can be converted into window coordinates
 *
 * WINDOW COORDINATES: non-normalized coordinates corresponding to the pixels of the window
 * - can be converted into normalized coordinates
 *
 * MODEL: normalized coordinates corresponding to a model's vertices. In this code, models are directly manipulated
 *   for scaling and rotating. That is, rotated and scaled model coordinates are still model coordinates, just of a
 *   different model
 * - can be directly converted into world coordinates
 * - can be directly converted into camera-view coordinates if there are no object properties to take into account
 * - can be directly converted into aspect coordinates if there are no object properties or camera properties to take
 *   into account
 *
 * WORLD COORDINATES: normalized coordinates corresponding to model coordinates with position taken into account
 * - can be directly converted into model coordinates
 * - can be directly converted into camera-view coordinates
 * - can be directly converted into aspect coordinates if there is no camera to take into account
 * - can be directly converted into grid coordinates
 *
 * CAMERA-VIEW COORDINATES: world coordinates where a camera's position and zoom have been take into account
 * - can be directly converted into world coordinates
 * - can be directly converted into model coordinates if there is no position to take into account
 * - can be directly converted into aspect coordinates
 *
 * ASPECT COORDINATES: coordinates where the window's aspect ratio has been taken into account
 * - can be directly converted into camera-view coordinates
 * - can be directly converted into world coordinates if there is no camera to take into account
 * - can be directly converted into model coordinates if there is no camera or object properties to take into account
 *
 * GRID COORDINATES: camera-view coordinates restricted to a grid. In Ambulare, this grid is defined by cells whose size
 *   match Global.GRID_CELL_SIZE
 * - can be directly converted into world coordinates
 *
 * The conversions made to render are: model -> world (done in GameObject) -> (if rendering with a camera) camera-view
 * -> aspect
 */
public class Transformation {

    /**
     * Converts the given window coordinates to normalized coordinates
     * @param pos the coordinates to convert
     * @param w the width of the window
     * @param h the height of the window
     */
    public static void normalize(Pair pos, int w, int h) {
        pos.x = ((pos.x / (float)w) * 2) - 1; // normalize x
        pos.y = -(((pos.y / (float)h) * 2) - 1); // normalize y and take its inverse
    }

    /**
     * Converts the given normalized coordinates to window coordinates
     * @param pos the coordinates to convert
     * @param w the width of the window
     * @param h the height of the window
     */
    public static void denormalize(Pair pos, int w, int h) {
        pos.x = ((pos.x + 1) / 2) * w; // denormalize x
        pos.y = -((pos.y + 1) / 2) * h; // denormalize y and take its inverse
    }

    /**
     * Converts the given model, world, or camera-view coordinates to aspect coordinates
     * @param pos the coordinates to convert
     * @param ar the aspect ratio of the window
     */
    public static void aspect(Pair pos, float ar) {
        if (ar > 1.0f) pos.x /= ar; // slim x if necessary
        else pos.y *= ar; // widen y if necessary
    }

    /**
     * Converts the given aspect coordinates to non-aspect coordinates (either model, world, or camera-view
     * coordinates)
     * @param pos the coordinates to convert
     * @param ar the aspect ratio of the window
     */
    public static void deaspect(Pair pos, float ar) {
        if (ar > 1.0f) pos.x *= ar; // widen x if necessary
        else pos.y /= ar; // thin y if necessary
    }

    /**
     * Converts the given model or world coordinates to camera-view coordinates
     * @param pos the coordinates to convert
     * @param cam the camera
     */
    public static void useCam(Pair pos, Camera cam) {
        pos.x = (pos.x / cam.getZoom()) + cam.getX(); // account for zoom and camera x
        pos.y = (pos.y / cam.getZoom()) + cam.getY(); // account for zoom and camera y
    }

    /**
     * Converts the given camera-view coordinates to non-camera related coordinates (either model or world coordinates)
     * @param pos the coordinates to convert
     * @param cam the camera
     */
    public static void removeCam(Pair pos, Camera cam) {
        pos.x = (pos.x - cam.getX()) * cam.getZoom(); // account for camera zoom and x
        pos.y = (pos.y - cam.getY()) * cam.getZoom(); // account for camera zoom and y
    }

    /**
     * Converts the given world coordinates to grid coordinates
     * This assumes a grid in which each cell is size defined by Global.GRID_CELL_SIZE
     * @param pos the coordinates to convert
     */
    public static void alignToGrid(Pair pos) {
        pos.x += pos.x < 0 ? (-Global.GRID_CELL_SIZE / 2) : (Global.GRID_CELL_SIZE / 2); /* the positions of all of the
                                                         objects in this game have (0, 0) at the center of their models.
                                                         This basically nudges them over slightly so  as to make the
                                                         truncation correct*/
        pos.y += pos.y < 0 ? (-Global.GRID_CELL_SIZE / 2) : (Global.GRID_CELL_SIZE / 2); // see above comment
        pos.x = (int)(pos.x / Global.GRID_CELL_SIZE); // align to grid where cell width is Model's standard square size
        pos.y = (int)(pos.y / Global.GRID_CELL_SIZE); // align to grid where cell height is Model's standard square size
    }

    /**
     * Converts the given grid coordinates ito world coordinates coordinates
     * This assumes a grid in which each cell is size defined by Global.GRID_CELL_SIZE
     * @param pos the coordinates to convert
     */
    public static void unalignFromGrid(Pair pos) {
        pos.x *= Global.GRID_CELL_SIZE; // un-align where cell width is Model's standard square size
        pos.y *= Global.GRID_CELL_SIZE; // un-align where cell height is Model's standard square size
        pos.x += pos.x < 0 ? (Global.GRID_CELL_SIZE / 2) : (-Global.GRID_CELL_SIZE / 2); /* move slightly over so that
                                                        the origin corresponds to the middle of the model. See
                                                        alignToGrid() */
        pos.y += pos.y < 0 ? (Global.GRID_CELL_SIZE / 2) : (-Global.GRID_CELL_SIZE / 2); // see above comment
    }
}
