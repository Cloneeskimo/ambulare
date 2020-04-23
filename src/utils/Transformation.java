package utils;

import graphics.Camera;

/**
 * This class provides methods to transform between various coordinate systems such as those listed and described below:
 * All coordinates listed below, except for window and grid, are normalized such that, if vertices were rendered at that
 * coordinate, they would have to be within the range of [-1, 1] (for both x and y) to actually be rendered. Aspect
 * coordinates used for rendering are the result of many transformations and calculations. Any of the normalized
 * coordinates can be converted into window coordinates
 *
 * WINDOW COORDINATES: non-normalized coordinates corresponding to window position
 * - can be converted into normalized coordinates
 *
 * MODEL: normalized coordinates corresponding to a model's vertices. In this code, models are directly manipulated
 *   for scaling and rotating. That is, rotated and scaled model coordinates are still model coordinates, just of a
 *   different model than their unrotated and unscaled counterparts
 * - can be directly converted into world coordinates
 * - can be directly converted into camera-view coordinates if there is no position to take into account
 * - can be directly converted into aspect coordinates if there is not position or camera to take into account
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
 * - can be directly converted into model coordinates if there is no camera or position to take into account
 *
 * GRID COORDINATES: world coordinates restricted to a grid. In Ambulare, this grid is such that all cells' widths
 * and heights are 1.0f (in world coordinates)
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
        // nude the positions over by 0.5f to make truncations correct (all objects have their position as their center)
        pos.x += pos.x < 0 ? -0.5f : 0.5f;
        pos.y += pos.y < 0 ? -0.5f : 0.5f;
        pos.x = (int)(pos.x);
        pos.y = (int)(pos.y);
    }
}
