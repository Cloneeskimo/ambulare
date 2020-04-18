package gameobject;

import graphics.PositionalAnimation;
import graphics.ShaderProgram;
import utils.Coord;
import utils.Transformation;
import utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates a collection of items that do not react to a Camera when rendered
 * Any items added to this HUD are considered to be in normalized space when received and will ultimately be
 * converted and maintained by this class in aspect coordinates
 */
public class HUD {

    /**
     * Data
     */
    private List<HUDItem> hudItems; // a List of HUDItems belonging to this HUD
    private ShaderProgram sp; // the ShaderProgram used to render the HUD
    private float ar; // the Window's aspect ratio
    private boolean arAction; // aspect ratio action (for projection)

    /**
     * Constructs this HUD
     * @param ar the Window's aspect ratio
     * @param arAction aspect ratio action (for projection)
     */
    public HUD(float ar, boolean arAction) {
        this.ar = ar; // save aspect ratio for rendering
        this.arAction = arAction; // save aspect ratio action for rendering
        this.hudItems = new ArrayList<>(); // create HUDItem list
        this.initSP(); // initialize ShaderProgram
    }

    /**
     * Initializes this HUD's ShaderProgram
     */
    private void initSP() {
        this.sp = new ShaderProgram("/shaders/hudV.glsl", "/shaders/worldF.glsl"); // create ShaderProgram
        this.sp.registerUniform("x"); // register aspect x uniform
        this.sp.registerUniform("y"); // register aspect y uniform
        this.sp.registerUniform("scaleX"); // register x scale uniform
        this.sp.registerUniform("scaleY"); // register y scale uniform
        this.sp.registerUniform("ar"); // register aspect ratio uniform
        this.sp.registerUniform("arAction"); // register aspect ratio action uniform
        this.sp.registerUniform("isTextured"); // register texture flag uniform
        this.sp.registerUniform("color"); // register color uniform
        this.sp.registerUniform("blend"); // register blend uniform
        this.sp.registerUniform("texSampler"); // register texture sampler uniform
    }

    /**
     * Updates this HUD
     * @param interval the amount of time to account for
     */
    public void update(float interval) {
        for (HUDItem i : this.hudItems) i.o.update(interval); // update GameObjects
    }

    /**
     * Renders this HUD
     */
    public void render() {
        this.sp.bind(); // bind this HUD's ShaderProgram
        this.sp.setUniform("texSampler", 0); // set texture sampler uniform to use texture unit 0
        this.sp.setUniform("ar", this.ar); // set aspect ratio uniform
        this.sp.setUniform("arAction", this.arAction ? 1 : 0); // set aspect ratio action uniform
        for (HUDItem i : this.hudItems) i.o.render(this.sp); // render each GameObject
        this.sp.unbind(); // unbind this HUD's ShaderProgram
    }

    /**
     * Handles a resize of the Window. It is vital that this is called whenever the Window in use is resized, or
     * position of HUD elements will be off
     * @param ar the new aspect ratio
     * @param arAction the new aspect ratio action
     */
    public void resized(float ar, boolean arAction) {
        this.ar = ar; // save new aspect ratio
        this.arAction = arAction; // save new aspect ratio action
        this.ensureAllPlacements(); // ensure all items are correctly paced
    }

    /**
     * Adds the given GameObject to this HUD, positioning it using the given settings
     * @param o the GameObject to add to this HUD
     * @param hps the HUDPositionSettings to use to maintain the given GameObject's position. For details on these
     *            settings, see the class definition of HUDPositionSettings below.
     */
    public void addObject(GameObject o, HUDPositionSettings hps) {
        HUDItem hi = new HUDItem(o, hps); // wrap object and settings into single object
        hi.ensurePosition(this.ar); // position the object according to its settings
        this.hudItems.add(hi); // add object to this HUD
    }

    /**
     * Changes the positioning settings of the GameObject at the given index to the new given HUDPositionSettings
     * @param i the index to look for the GameObject
     * @param hps the new HUDPositionSettings
     * @param duration the duration of animation (<= 0f will result in no animation)
     */
    public void moveObject(int i, HUDPositionSettings hps, float duration) {
        HUDItem hi = getHUDItem(i); // attempt to get HUDItem
        hi.hps = hps; // update HUDPositionSettings
        if (duration > 0f) { // if animated change
            Coord correctPos = hi.hps.getCorrectPosition(hi.o, this.ar); // get correct position
            hi.o.givePosAnim(new PositionalAnimation(correctPos.x, correctPos.y, duration)); // and start animation
        } else { // if not an animated change
            hi.ensurePosition(this.ar); // just change position immediately
        }
    }

    /**
     * Ensures the GameObject at the given index is positioned according to its original position settings. This is
     * good to call after modifying the size of a GameObject being rendered by this HUD in any fashion (scale, width,
     * new Model, etc.)
     * @param i the index of the GameObject whose placement should be ensured
     */
    public void ensurePlacement(int i) { getHUDItem(i).ensurePosition(this.ar); }

    /**
     * Attempts to acquire the HUDItem at the given index, throwing an error if out of bounds
     * @param i the index to look for the HUDItem
     */
    private HUDItem getHUDItem(int i) {
        try { // try to get item
            return this.hudItems.get(i); // and return it
        } catch (Exception e) { // if exception
            Utils.handleException(e, "HUD", "getHUDItem(i)", true); // handle exception
        }
        return null;
    }

    /**
     * Ensures the positions of all GameObject this HUD owns in the order that they were added
     */
    public void ensureAllPlacements() { for (HUDItem i : this.hudItems) i.ensurePosition(this.ar); }

    /**
     * Finds and returns the GameObject at the given index
     * @param i the index to find the GameObject at
     * @return the GameObject
     */
    public GameObject getObject(int i) { return this.getHUDItem(i).o; }

    /**
     * Cleans up this HUD
     */
    public void cleanup() {
        if (this.sp != null) this.sp.cleanup(); // cleanup ShaderProgram
        for (HUDItem i : this.hudItems) i.o.cleanup(); // cleanup GameObjects
    }

    /**
     * Settings which determine the position of a HUDItem. These are designed to be accessible but also highly
     * customizable
     */
    public static class HUDPositionSettings {

        /**
         * Data
         */
        private final float nx, ny, padding; // normalized x, normalized y, and padding values
        private final boolean accountForSize; // account for size flag
        private GameObject px, py; // x-parent and y-parent

        /**
         * Creates these settings such that the corresponding GameObject will simply reside at the given normalized
         * coordinates (converted to aspect coordinates)
         * @param nx the normalized x
         * @param ny the normalized y
         * @param accountForSize whether to account for the size of the corresponding GameObject when calculating the
         *                       position. For example, if x and y are -1f, only the top-right of the GameObject would
         *                       normally be visible because (0, 0) is at the center of the model. If this parameter is
         *                       set to true, the position will be modified so as to bump the object into view
         *                       perfectly. This essentially allows for very easy binding of GameObjects to the edge of
         *                       the Window in use
         * @param padding the amount of padding to use if accounting for size
         */
        public HUDPositionSettings(float nx, float ny, boolean accountForSize, float padding) {
            this.nx = nx; // save normalized x
            this.ny = ny; // save normalized y
            this.accountForSize = accountForSize; // save accounting for size flag
            this.padding = padding; // save padding
        }

        /**
         * Creates these settings such that the corresponding GameObject's position may be independent on one or two
         * other GameObject's position. px and py do not need to be the same GameObject. One or both of them may be
         * null. For a null parent, that specific axis position is determined independently as an aspect coordinate
         * converted from nx or ny. For example, if px is null, the corresponding GameObject's x position will be
         * independently found by converting nx to aspect coordinates.
         * @param px the parent GameObject whose x position determines these settings' corresponding GameObject's
         *           x position
         * @param py the parent GameObject whose y position determines these settings' corresponding GameObject's
         *           y position
         * @param nx if px is not null, how many widths away o should be from px (where the width is the average of both
         *           of their widths). If px is null, the normalized x position for this GameObject
         * @param ny if py is not null, how many widths away o should be from py (where the height is the average of
         *           both of their heights). If py is null, the normalized y position for this GameObject
         * @param padding the amount of padding to place between px/py and the corresponding GameObject for these
         *                settings
         */
        public HUDPositionSettings(GameObject px, GameObject py, float nx, float ny, float padding) {
            this.px = px; // save px
            this.py = py; // save py
            this.nx = nx; // save normalized x
            this.ny = ny; // save normalized y
            this.accountForSize = true; // save account for size
            this.padding = padding; // save padding value
        }

        /**
         * Calculates the correct position of a given GameObject based on these HUDPositionSettings
         * @param o the object whose position to calculate
         * @param ar the aspect ratio of the Window in use
         * @return a Coord object containing the correct coordinates
         */
        public Coord getCorrectPosition(GameObject o, float ar) {

            // create Coord object and start with normalized independent coordinates
            Coord pos = new Coord(this.nx, this.ny); // create new Coord object with norm coordinates to start
            if (px == null || py == null) Transformation.normToAspect(pos, ar); // if either x or y is independent, calculate as aspect coordinates

            // calculate x
            if (px == null) { // if x is independent
                if (accountForSize) { // and size needs to be accounted for
                    if (pos.x < 0f) pos.x += (o.getWidth() / 2 + padding); // if on the left side, nudge to the right
                    else if (pos.x > 0f) pos.x -= (o.getWidth() / 2 + padding); // if on the right side, nudge to the left
                } // if size does not need to be accounted for, we are done calculating x
            } else pos.x = px.getX() + this.nx * ((o.getWidth() / 2) + (px.getWidth() / 2)) + padding; // otherwise calculate x relative to parent

            // calculate y
            if (py == null) { // if y is independent
                if (accountForSize) { // and size needs to be accounted for
                    if (pos.y < 0f) pos.y += (o.getHeight() / 2 + padding); // if on bottom side, nudge upwards
                    else if (pos.y > 0f) pos.y -= (o.getHeight() / 2 + padding); // if on top side, nudge downwards
                }
            } else pos.y = py.getY() + this.ny * ((o.getHeight() / 2) + (py.getHeight() / 2)) + padding; // otherwise calculate y relative to parent

            // return calculate position
            return pos; // return
        }
    }

    /**
     * Binds a GameObject to HUDPositionSettings so that their settings can be remembered whenever positions need to be
     * ensured
     */
    private static class HUDItem {

        /**
         * Data
         */
        GameObject o; // reference to the GameObject
        HUDPositionSettings hps; // settings for positioning

        /**
         * Constructs this HUDItem
         * @param o the GameObject
         * @param hps the HUDPositionSettings to bind the to given GameObject
         */
        public HUDItem(GameObject o, HUDPositionSettings hps) {
            this.o = o; // save GameObject
            this.hps = hps; // save HudPositionSettings
        }

        /**
         * Ensures that this HUDItem's GameObject is correctly positioned
         * @param ar the aspect ratio of the Window in use
         */
        public void ensurePosition(float ar) {
            Coord correctPos = this.hps.getCorrectPosition(this.o, ar); // calculate correct position
            o.setX(correctPos.x); // set x
            o.setY(correctPos.y); // set y
        }
    }
}
