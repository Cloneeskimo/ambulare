package utils;

import graphics.Camera;
import graphics.Model;

/**
 * Provides methods to transform between the various coordinate systems listed below
 * - window coordinates -> coordinates of the window's pixels
 *     /\ +y
 *  0 <  > +x
 *     \/ 0
 * - normalized coordinates -> coordinates of the screen from -1 to 1
 *     /\ +1
 * -1 <  > +1
 *     \/ -1
 * - aspect coordinates -> normalized coordinates adjusted for aspect ratio such that a square is always a square
 *     /\ +y
 * -x <  > +x
 *     \/ -y
 * - world coordinates -> coordinates of a world where a camera's position and zoom are taken into account
 *     /\ -y
 * -x <  > +x
 *     \/ -y
 * - grid coordinates -> world coordinates where items are locked into an grid with discrete (integer-sized) cells
 *     /\ -y
 * -x <  > +x
 *     \/ -y
 */
public class Transformation {

    /**
     * Converts the given window coordinates to normalized coordinates
     * @param pos the coordinates to convert
     * @param w the width of the Window in use
     * @param h the height of the Window in use
     */
    public static void windowToNorm(Coord pos, int w, int h) {
        pos.x = ((pos.x / (float)w) * 2) - 1;
        pos.y = ((pos.y / (float)h) * 2) - 1;
    }

    /**
     * Converts the given normalized coordinates to aspect coordinates
     * @param pos the coordinates to convert
     * @param ar the aspect ratio of the Window in use
     */
    public static void normToAspect(Coord pos, float ar) {
        if (ar > 1.0f) pos.x *= ar;
        else pos.y /= ar;
    }

    /**
     * Converts the given aspect coordinates to world coordinates
     * @param pos the coordinates to convert
     * @param cam the Camera in use
     */
    public static void aspectToWorld(Coord pos, Camera cam) {
        pos.x = (pos.x / cam.getZoom()) + cam.getX();
        pos.y = (pos.y / cam.getZoom()) + cam.getY();
    }

    /**
     * Converts the given window coordinates to world coordinates
     * @param pos the coordinates to convert
     * @param w the width of the Window in use
     * @param h the height of the Window in use
     * @param ar the aspect ratio of the Window in use
     * @param cam the Camera in use
     */
    public static void windowToWorld(Coord pos, int w, int h, float ar, Camera cam) {
        windowToNorm(pos, w, h); // window -> normalized
        normToAspect(pos, ar); // normalized -> aspect
        aspectToWorld(pos, cam); // aspect -> world
    }

    /**
     * Converts the given world coordinates to grid coordinates
     * This assumes a grid in which each cell is the standard square size as defined by the Model class
     * This will also result in a loss of accuracy that cannot be gained when converting the opposite direction by
     * rounding to integer values
     * @param pos the coordinates to convert
     */
    public static void worldToGrid(Coord pos) {
        pos.x += pos.x < 0 ? (-Model.STD_SQUARE_SIZE / 2) : (Model.STD_SQUARE_SIZE / 2);
        pos.y += pos.y < 0 ? (-Model.STD_SQUARE_SIZE / 2) : (Model.STD_SQUARE_SIZE / 2);
        pos.x = (int)(pos.x / Model.STD_SQUARE_SIZE);
        pos.y = (int)(pos.y / Model.STD_SQUARE_SIZE);
    }

    /**
     * Converts the given window coordinates to grid coordinates
     * This assumes a grid in which each cell is the standard square size as defined by the Model class
     * This will also result in a loss of accuracy that cannot be gained when converting the opposite direction by
     * rounding to integer values
     * @param pos the coordinates to convert
     * @param w the width of the Window in use
     * @param h the height of the Window in use
     * @param ar the aspect ratio of the Window in use
     * @param cam the Camera in use
     */
    public static void windowToGrid(Coord pos, int w, int h, float ar, Camera cam) {
        windowToWorld(pos, w, h, ar, cam); // window -> world
        worldToGrid(pos); // world -> grid
    }

    /**
     * Converts the given grid coordinates to world coordinates
     * This assumes a grid in which each cell is the standard square size as defined by the Model class
     * @param pos the coordinates to convert
     */
    public static void gridToWorld(Coord pos) {
        pos.x *= Model.STD_SQUARE_SIZE;
        pos.y *= Model.STD_SQUARE_SIZE;
        pos.x += pos.x < 0 ? (Model.STD_SQUARE_SIZE / 2) : (-Model.STD_SQUARE_SIZE / 2);
        pos.y += pos.y < 0 ? (Model.STD_SQUARE_SIZE / 2) : (-Model.STD_SQUARE_SIZE / 2);
    }

    /**
     * Converts the given world coordinates to aspect coordinates
     * @param pos the coordinates to convert
     * @param cam the Camera in use
     */
    public static void worldToAspect(Coord pos, Camera cam) {
        pos.x = (pos.x - cam.getX()) * cam.getZoom();
        pos.y = (pos.y - cam.getY()) * cam.getZoom();
    }

    /**
     * Converts the given aspect coordinates to normalized coordinates
     * @param pos the coordinates to convert
     * @param ar the aspect ratio of the Window in use
     */
    public static void aspectToNorm(Coord pos, float ar) {
        if (ar > 1.0f) pos.x /= ar;
        else pos.y *= ar;
    }

    /**
     * Converts the given normalized coordinates to window coordinates
     * @param pos the coordinates to convert
     * @param w the width of the Window in use
     * @param h the height of the Window in use
     */
    public static void normToWindow(Coord pos, int w, int h) {
        pos.x = ((pos.x + 1) / 2) * w;
        pos.y = ((pos.y + 1) / 2) * h;
    }

    /**
     * Converts the given world coordinates to window coordinates
     * @param pos the coordinates to convert
     * @param cam the Camera in use
     * @param ar the aspect ratio of the Window in use
     * @param w the width of the Window in use
     * @param h the height of the Window in use
     */
    public static void worldToWindow(Coord pos, Camera cam, float ar, int w, int h) {
        worldToAspect(pos, cam);
        aspectToNorm(pos, ar);
        normToWindow(pos, w, h);
    }

    /**
     * Converts the given grid coordinates to window coordinates
     * @param pos the coordinates to convert
     * @param cam the Camera in use
     * @param ar the aspect ratio of the Window in use
     * @param w the width of the Window in use
     * @param h the height of the Window in use
     */
    public static void gridToWindow(Coord pos, Camera cam, float ar, int w, int h) {
        gridToWorld(pos);
        worldToWindow(pos, cam, ar, w, h);
    }
}
