package utils;

import graphics.Camera;

/*
 * Transformation.java
 * Ambulare
 * Jacob Oaks
 * 4/17/20
 */

/**
 * This class provides methods to transform between various coordinate systems such as those listed and described below:
 * All coordinates listed below, except for window and grid, are normalized such that, if vertices were rendered at that
 * coordinate, they would have to be within the range of [-1, 1] (for both x and y) to actually be rendered. Aspect
 * coordinates used for rendering are the result of many transformations and calculations. Any of the normalized
 * coordinates can be converted into window coordinates
 * <p>
 * WINDOW COORDINATES: non-normalized coordinates corresponding to window position
 * - can be converted into normalized coordinates
 * <p>
 * MODEL: normalized coordinates corresponding to a model's vertices. In this code, models are directly manipulated
 * for scaling and rotating. That is, rotated and scaled model coordinates are still model coordinates, just of a
 * different model than their unrotated and unscaled counterparts
 * - can be directly converted into world coordinates
 * - can be directly converted into camera-view coordinates if there is no position to take into account
 * - can be directly converted into aspect coordinates if there is not position or camera to take into account
 * <p>
 * WORLD COORDINATES: normalized coordinates corresponding to model coordinates with position taken into account
 * - can be directly converted into model coordinates
 * - can be directly converted into camera-view coordinates
 * - can be directly converted into aspect coordinates if there is no camera to take into account
 * - can be directly converted into grid coordinates
 * <p>
 * CAMERA-VIEW COORDINATES: world coordinates where a camera's position and zoom have been take into account
 * - can be directly converted into world coordinates
 * - can be directly converted into model coordinates if there is no position to take into account
 * - can be directly converted into aspect coordinates
 * <p>
 * ASPECT COORDINATES: coordinates where the window's aspect ratio has been taken into account
 * - can be directly converted into camera-view coordinates
 * - can be directly converted into world coordinates if there is no camera to take into account
 * - can be directly converted into model coordinates if there is no camera or position to take into account
 * <p>
 * GRID COORDINATES: world coordinates restricted to a grid. In Ambulare, this grid is such that all cells' widths
 * and heights are 1.0f (in world coordinates)
 * - can be directly converted into world coordinates
 * <p>
 * The conversions made to render are: model -> world (done in GameObject) -> (if rendering with a camera) camera-view
 * -> aspect
 */
public abstract class Transformation {

    /**
     * Converts the given window coordinates to normalized coordinates
     *
     * @param pos the coordinates to convert
     * @param w   the width of the window
     * @param h   the height of the window
     */
    public static Pair<Float> normalize(Pair<Float> pos, int w, int h) {
        pos.x = ((pos.x / (float) w) * 2) - 1; // normalize x
        pos.y = -(((pos.y / (float) h) * 2) - 1); // normalize y and take its inverse
        return pos;
    }

    /**
     * Converts the given normalized coordinates to window coordinates
     *
     * @param pos the coordinates to convert
     * @param w   the width of the window
     * @param h   the height of the window
     */
    public static Pair<Float> denormalize(Pair<Float> pos, int w, int h) {
        pos.x = ((pos.x + 1) / 2) * w; // denormalize x
        pos.y = -((pos.y + 1) / 2) * h; // denormalize y and take its inverse
        return pos;
    }

    /**
     * Converts the given model, world, or camera-view coordinates to aspect coordinates
     *
     * @param pos the coordinates to convert
     * @param ar  the aspect ratio of the window
     */
    public static Pair<Float> aspect(Pair<Float> pos, float ar) {
        if (ar > 1.0f) pos.x /= ar; // slim x if necessary
        else pos.y *= ar; // widen y if necessary
        return pos;
    }

    /**
     * Converts the given aspect coordinates to non-aspect coordinates (either model, world, or camera-view
     * coordinates)
     *
     * @param pos the coordinates to convert
     * @param ar  the aspect ratio of the window
     */
    public static Pair<Float> deaspect(Pair<Float> pos, float ar) {
        if (ar > 1.0f) pos.x *= ar; // widen x if necessary
        else pos.y /= ar; // thin y if necessary
        return pos;
    }

    /**
     * Converts the given model or world coordinates to camera-view coordinates
     *
     * @param pos the coordinates to convert
     * @param cam the camera
     */
    public static Pair<Float> useCam(Pair<Float> pos, Camera cam) {
        pos.x = (pos.x / cam.getZoom()) + cam.getX(); // account for zoom and camera x
        pos.y = (pos.y / cam.getZoom()) + cam.getY(); // account for zoom and camera y
        return pos;
    }

    /**
     * Converts the given camera-view coordinates to non-camera related coordinates (either model or world coordinates)
     *
     * @param pos the coordinates to convert
     * @param cam the camera
     */
    public static Pair<Float> removeCam(Pair<Float> pos, Camera cam) {
        pos.x = (pos.x - cam.getX()) * cam.getZoom(); // account for camera zoom and x
        pos.y = (pos.y - cam.getY()) * cam.getZoom(); // account for camera zoom and y
        return pos;
    }

    /**
     * Calculates the center position of the given grid cell. For example, for grid cell [-2, 4], the center position
     * (between cells [-3, 4], [-1, 4], [-2, 3], [-2, 5]) is (-1.5, 4.5)
     *
     * @param pos the cell to calculate the center position for
     */
    public static Pair<Float> getCenterOfCell(Pair<Integer> pos) {
        // get the centers of each component
        return new Pair<>(getCenterOfCellComponent(pos.x), getCenterOfCellComponent(pos.y));
    }

    /**
     * Calculates what grid cell the given position lies in
     *
     * @param pos the position to consider
     */
    public static Pair<Integer> getGridCell(Pair<Float> pos) {
        // get grid cell components of each component
        return new Pair<>(getGridCellComponent(pos.x), getGridCellComponent(pos.y));
    }

    /**
     * Calculates the center position of the given grid cell component. For example, for grid cell 5, the center
     * position (between cell components 5 and 6) is 5.5f
     *
     * @param x the cell to calculate the center position for
     * @return the center position
     */
    public static float getCenterOfCellComponent(int x) {
        return (float) x + 0.5f; // add 0.5f and return as float
    }

    /**
     * Calculates what grid cell component the given position component lies in
     *
     * @param x the position component
     * @return the grid cell the given position component lies in
     */
    public static int getGridCellComponent(float x) {
        if (x < 0) x--; // for negatives, subtract one (or there would be two zero components)
        return (int) x; // truncate and return
    }
}
