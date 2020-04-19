package gameobject;

import graphics.PositionalAnimation;
import graphics.ShaderProgram;
import utils.Coord;
import utils.Transformation;
import utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates a collection of items that do not react to a camera when rendered
 * Any items added to this HUD are considered to be in normalized space when received and will ultimately be
 * converted and maintained by this class in aspect coordinates
 */
public class HUD {

    /**
     * Data
     */
    private List<HUDItem> hudItems; // a list of HUD items belonging to this HUD
    private ShaderProgram sp; // the shader program used to render the HUD
    private float ar; // the window's  aspect ratio
    private boolean arAction; // aspect ratio action (see GameEngine.init)

    /**
     * Constructor
     * @param ar the window's aspect ratio
     * @param arAction aspect ratio action (see GameEngine.init)
     */
    public HUD(float ar, boolean arAction) {
        this.ar = ar; // save aspect ratio for rendering
        this.arAction = arAction; // save aspect ratio action for rendering
        this.hudItems = new ArrayList<>(); // create HUD item list
        this.initSP(); // initialize shader program
    }

    /**
     * Initializes the HUD's shader program
     */
    private void initSP() {
        this.sp = new ShaderProgram("/shaders/hudV.glsl", "/shaders/worldF.glsl"); // create SP
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
     * Updates the HUD
     * @param interval the amount of time to account for
     */
    public void update(float interval) {
        for (HUDItem i : this.hudItems) i.o.update(interval); // update game objects
    }

    /**
     * Renders the HUD
     */
    public void render() {
        this.sp.bind(); // bind this HUD's shader program
        this.sp.setUniform("texSampler", 0); // set texture sampler uniform to use texture unit 0
        this.sp.setUniform("ar", this.ar); // set aspect ratio uniform
        this.sp.setUniform("arAction", this.arAction ? 1 : 0); // set aspect ratio action uniform
        for (HUDItem i : this.hudItems) i.o.render(this.sp); // render each game object
        this.sp.unbind(); // unbind the HUD's shader program
    }

    /**
     * Handles a resize of the window. It is vital that this is called whenever the window is resized, or position of
     * HUD elements will be off
     * @param ar the new aspect ratio
     * @param arAction the new aspect ratio action (see GameEngine.init)
     */
    public void resized(float ar, boolean arAction) {
        this.ar = ar; // save new aspect ratio
        this.arAction = arAction; // save new aspect ratio action
        this.ensureAllPlacements(); // ensure all items are correctly paced
    }

    /**
     * Adds the given game object to this HUD, positioning it using the given settings
     * @param o the game object to add to this HUD
     * @param hps the position settings to use to maintain the game object's position. For details on these settings,
     *            see the class definition of HUDPositionSettings below.
     */
    public void addObject(GameObject o, HUDPositionSettings hps) {
        HUDItem hi = new HUDItem(o, hps); // wrap object and settings into single object
        hi.ensurePosition(this.ar); // position the object according to its settings
        this.hudItems.add(hi); // add object to the HUD
    }

    /**
     * Changes the positioning settings of the game object at the given index
     * @param i the index to look for the game object whose settings are to be changed
     * @param hps the new settings
     * @param duration the duration of animation to undergo when changing (<= 0f will result in no animation)
     */
    public void moveObject(int i, HUDPositionSettings hps, float duration) {
        HUDItem hi = getHUDItem(i); // attempt to get HUD item
        hi.hps = hps; // update settings
        if (duration > 0f) { // if animated change
            Coord correctPos = hi.hps.getCorrectPosition(hi.o, this.ar); // get correct position
            hi.o.givePosAnim(new PositionalAnimation(correctPos.x, correctPos.y, duration)); // and start animation
        } else { // if not an animated change
            hi.ensurePosition(this.ar); // just change position immediately
        }
    }

    /**
     * Ensures the game object at the given index is positioned according to its position settings. This is good to call
     * after modifying the size of a game object being rendered by this HUD in any fashion (scale, width, new Model,
     * etc.)
     * @param i the index of the GameObject whose placement should be ensured
     */
    public void ensurePlacement(int i) { getHUDItem(i).ensurePosition(this.ar); }

    /**
     * Attempts to acquire the HUD item at the given index, throwing an error if out of bounds
     * @param i the index to look for the HUD item
     */
    private HUDItem getHUDItem(int i) {
        try { // try to get item
            return this.hudItems.get(i); // and return it
        } catch (Exception e) { // if exception
            Utils.handleException(e, "gameobject.HUD", "getHUDItem(i)", true); // handle exception
        }
        return null;
    }

    /**
     * Ensures the positions of all game objects in this HUD in the order that they were added
     */
    public void ensureAllPlacements() { for (HUDItem i : this.hudItems) i.ensurePosition(this.ar); }

    /**
     * Finds and returns the game object at the given index
     * @param i the index to find the game object at
     * @return the game object
     */
    public GameObject getObject(int i) { return this.getHUDItem(i).o; }

    /**
     * Cleans up the HUD
     */
    public void cleanup() {
        if (this.sp != null) this.sp.cleanup(); // cleanup shader program
        for (HUDItem i : this.hudItems) i.o.cleanup(); // cleanup game objects
    }

    /**
     * Settings which determine the position of a HUDItem. These are designed to be accessible but also highly
     * customizable. See the constructors for more details
     */
    public static class HUDPositionSettings {

        /**
         * Data
         */
        private final float nx, ny, padding; // normalized x, normalized y, and padding values
        private final boolean accountForSize; // account for size flag
        private GameObject px, py; // x-parent and y-parent

        /**
         * Creates these settings such that the corresponding game object will simply reside at the given normalized
         * coordinates (converted to aspect coordinates)
         * @param nx the normalized x
         * @param ny the normalized y
         * @param accountForSize whether to account for the size of the corresponding game object when calculating the
         *                       position. For example, if x and y are -1f, only the top-right of the game object would
         *                       normally be visible because (0, 0) is at the center of the model. If this parameter is
         *                       set to true, the position will be modified so as to bump the object into view
         *                       perfectly. This essentially allows for very easy binding of game objects to the edge of
         *                       the window
         * @param padding the amount of padding to use if accounting for size
         */
        public HUDPositionSettings(float nx, float ny, boolean accountForSize, float padding) {
            this.nx = nx; // save normalized x
            this.ny = ny; // save normalized y
            this.accountForSize = accountForSize; // save accounting for size flag
            this.padding = padding; // save padding
        }

        /**
         * Creates these settings such that the corresponding game objects's position may be dependent on one or two
         * other game objects' positions. px and py do not need to be the same game object. One or both of them may be
         * null. For a null parent, that specific axis position is determined independently as an aspect coordinate
         * converted from nx or ny. For example, if px is null, the corresponding game object's x position will be
         * independently found by converting nx to aspect coordinates.
         * @param px the parent game object whose x position determines these settings' corresponding game object's
         *           x position
         * @param py the parent game object whose y position determines these settings' corresponding game object's
         *           y position
         * @param nx if px is not null, how many widths away o should be from px (where the width is the average of both
         *           of their widths). If px is null, the normalized x position for this game object
         * @param ny if py is not null, how many widths away o should be from py (where the height is the average of
         *           both of their heights). If py is null, the normalized y position for this game object
         * @param padding the amount of padding to place between px/py and the corresponding game object for these
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
         * Calculates the correct position of a given game object based on these settings
         * @param o the object whose position to calculate
         * @param ar the aspect ratio of the window
         * @return a coordinate containing the correct coordinates
         */
        public Coord getCorrectPosition(GameObject o, float ar) {

            // create Coord object and start with normalized independent coordinates
            Coord pos = new Coord(this.nx, this.ny); // create new Coord object with norm coordinates to start
            if (px == null || py == null)
                Transformation.normToAspect(pos, ar); // if either x or y is independent, calculate as aspect coords

            // calculate x
            if (px == null) { // if x is independent
                if (accountForSize) { // and size needs to be accounted for
                    if (pos.x < 0f) pos.x += (o.getWidth() / 2 + padding); // if on left side, nudge to the right
                    else if (pos.x > 0f) pos.x -= (o.getWidth() / 2 + padding); // if on right side, nudge to the left
                } // if size does not need to be accounted for, we are done calculating x
            } else pos.x = px.getX() + this.nx * ((o.getWidth() / 2) + (px.getWidth() / 2)) +
                    padding; // otherwise calculate x relative to parent

            // calculate y
            if (py == null) { // if y is independent
                if (accountForSize) { // and size needs to be accounted for
                    if (pos.y < 0f) pos.y += (o.getHeight() / 2 + padding); // if on bottom side, nudge upwards
                    else if (pos.y > 0f) pos.y -= (o.getHeight() / 2 + padding); // if on top side, nudge downwards
                }
            } else pos.y = py.getY() + this.ny * ((o.getHeight() / 2) + (py.getHeight() / 2)) +
                    padding; // otherwise calculate y relative to parent

            // return calculate position
            return pos; // return
        }
    }

    /**
     * Binds a game object to a set of positioning settings so that their settings can be retrieved whenever positions
     * need to be ensured
     */
    private static class HUDItem {

        /**
         * Data
         */
        GameObject o; // reference to the game object
        HUDPositionSettings hps; // settings for positioning

        /**
         * Constructor
         * @param o the game object
         * @param hps the settings
         */
        public HUDItem(GameObject o, HUDPositionSettings hps) {
            this.o = o; // save game object
            this.hps = hps; // save settings
        }

        /**
         * Ensures that the game object is correctly positioned according to its setting
         * @param ar the aspect ratio of the window
         */
        public void ensurePosition(float ar) {
            Coord correctPos = this.hps.getCorrectPosition(this.o, ar); // calculate correct position
            o.setX(correctPos.x); // set x
            o.setY(correctPos.y); // set y
        }
    }
}
