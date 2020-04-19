package utils;

import graphics.Camera;
import graphics.Model;

/**
 * This class provides methods to transform between various coordinate systems such as those listed and described below:
 * All coordinates list below, except for window and grid, are normalized such that, if vertices were rendered at that
 * coordinate, they would have to be within the range of [-1, 1] (for both x and y) to actually be rendered. Projected
 * coordinates are the result of many transformations and calculations (many of which are done in the GLSL shader
 * programs) that essentially assign such coordinates to objects based on many different things (each of which are
 * described below in their corresponding coordinate system sections). Any of the normalized coordinates can be
 * converted into window coordinates
 *
 * WINDOW COORDINATES: non-normalized coordinates corresponding to the pixels of the window
 * - can be directly converted into normalized coordinates
 *
 * MODEL: normalized coordinates corresponding to a model's vertices
 * - can be directly converted into window (non-normalized) coordinates
 * - can be directly converted into object coordinates (should be done by a class representing the object)
 * - can be directly converted into camera-view coordinates if there are no object properties to take into account
 * - can be directly converted into projected coordinates if there are no object properties or camera properties to
 *   take into account
 * -
 *
 * OBJECT COORDINATES: normalized coordinates corresponding to model coordinates with various object properties taken
 *   into account such as position, rotation and scale
 * - can be directly converted into model coordinates (should be done by a class representing the object)
 * - can be directly converted into camera-view coordinates
 * - can be directly converted into projected coordinates if there is no camera to take into account
 *
 * CAMERA-VIEW COORDINATES: object coordinates where a camera's position and zoom have been take into account
 * - can be directly converted into object coordinates
 * - can be directly converted into model coordinates if there are no object properties to take into account
 * - can be directly converted into projected coordinates
 * - can be directly converted into grid coordinates
 *
 * PROJECTED COORDINATES: coordinates where projection has been taken into account. For 2D graphics, this just means
 *   making sure the aspect ratio is taken into account
 * - can be directly converted into camera-view coordinates
 * - can be directly converted into object coordinates if there is no camera to take into account
 * - can be directly converted into model coordinates if there is no camera or object properties to take into account
 *
 * GRID COORDINATES: camera-view coordinates restricted to a grid. In Ambulare, this grid is defined by cells whose sizez
 *   match Model.STD_SQUARE_SIZE
 * - can be directly converted into camera-view coordinates
 *
 * The conversions made to render are: model -> object (done in GameObject) -> (if rendering with a camera) ->
 * camera-view -> projected
 */
public class Transformation {

    /**
     * Converts the given window coordinates to normalized coordinates
     * @param pos the coordinates to convert
     * @param w the width of the window
     * @param h the height of the window
     */
    public static void normalize(Coord pos, int w, int h) {
        pos.x = ((pos.x / (float)w) * 2) - 1; // normalize x
        pos.y = -(((pos.y / (float)h) * 2) - 1); // normalize y and take its inverse
    }

    /**
     * Converts the given normalized coordinates to window coordinates
     * @param pos the coordinates to convert
     * @param w the width of the window
     * @param h the height of the window
     */
    public static void denormalize(Coord pos, int w, int h) {
        pos.x = ((pos.x + 1) / 2) * w; // denormalize x
        pos.y = ((pos.y + 1) / 2) * h; // denormalize y
    }

    /**
     * Converts the given model, object, or camera-view coordinates to projected coordinates
     * @param pos the coordinates to convert
     * @param ar the aspect ratio of the window
     */
    public static void project(Coord pos, float ar) {
        if (ar > 1.0f) pos.x *= ar; // widen x if necessary
        else pos.y /= ar; // thin y if necessary
    }

    /**
     * Converts the given project coordinates to un-projected coordinates (either model, object, or camera-view
     * coordinates)
     * @param pos the coordinates to convert
     * @param ar the aspect ratio of the window
     */
    public static void unproject(Coord pos, float ar) {
        if (ar > 1.0f) pos.x /= ar; // slim x if necessary
        else pos.y *= ar; // widen y if necessary
    }

    /**
     * Converts the given model or object coordinates to camera-view coordinates
     * @param pos the coordinates to convert
     * @param cam the camera
     */
    public static void useCam(Coord pos, Camera cam) {
        pos.x = (pos.x / cam.getZoom()) + cam.getX(); // account for zoom and camera x
        pos.y = (pos.y / cam.getZoom()) + cam.getY(); // account for zoom and camera y
    }

    /**
     * Converts the given camera-view coordinates to non-camera related coordinates (either model or object coordinates)
     * @param pos the coordinates to convert
     * @param cam the camera
     */
    public static void removeCam(Coord pos, Camera cam) {
        pos.x = (pos.x - cam.getX()) * cam.getZoom(); // account for camera zoom and x
        pos.y = (pos.y - cam.getY()) * cam.getZoom(); // account for camera zoom and y
    }

    /**
     * Converts the given camera-view coordinates to grid coordinates
     * This assumes a grid in which each cell is the standard square size as defined by the Model class
     * @param pos the coordinates to convert
     */
    public static void alignToGrid(Coord pos) {
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
     * Converts the given grid coordinates to camera-view coordinates
     * This assumes a grid in which each cell is the standard square size as defined by the Model class
     * @param pos the coordinates to convert
     */
    public static void unalignFromGrid(Coord pos) {
        pos.x *= Model.STD_SQUARE_SIZE; // un-align where cell width is Model's standard square size
        pos.y *= Model.STD_SQUARE_SIZE; // un-align where cell height is Model's standard square size
        pos.x += pos.x < 0 ? (Model.STD_SQUARE_SIZE / 2) : (-Model.STD_SQUARE_SIZE / 2); /* move slightly over so that
                                                                                            the origin corresponds to
                                                                                            the middle of the model. See
                                                                                            alignToGrid() */
        pos.y += pos.y < 0 ? (Model.STD_SQUARE_SIZE / 2) : (-Model.STD_SQUARE_SIZE / 2); // see above comment
    }
}
