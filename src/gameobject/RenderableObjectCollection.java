package gameobject;

import graphics.Camera;
import graphics.PositionalAnimation;
import graphics.ShaderProgram;
import utils.Pair;
import utils.Transformation;
import utils.Utils;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;

/**
 * Holds a collection of game objects and renders them all in one method call. This class divides the game objects it
 * contains into static objects, which are bound to certain positions in the window, and world objects, which react to
 * a camera. In other words, game objects in the collection are either HUD or world items. This class contains
 * powerful positioning tools to position static objects to create HUDs very easily
 */
public class RenderableObjectCollection {

    /**
     * Data
     */
    private List<StaticObject> staticObjects; /* a list of game objects that will not used the camera when rendered.
                                                 Instead, they are bound to positioning settings that determine where
                                                 in the window they should be rendered at all times. In other words,
                                                 these are HUD items */
    private List<GameObject> worldObjects;    /* a list of objects that will react to a camera. In other words, these
                                                 are world objects */
    private List<PhysicsObject> collidables;  // a list of PhysicsObjects for collision detection
    private Camera cam;                       // the camera to use to render world objects
    private MIHSB mihsb;                      // mouse interactable hover state bundle to abstract away mouse input
    private ShaderProgram spw, sps;           // the shader programs used to render
    private float ar;                         // the window's aspect ratio
    private boolean arAction;                 // aspect ratio action (see GameLogic.init)

    /**
     * Constructor
     * @param ar the window's aspect ratio
     * @param arAction aspect ratio action (see GameLogic.init)
     * @param windowHandle the window's GLFW handle
     */
    public RenderableObjectCollection(float ar, boolean arAction, long windowHandle) {
        this.ar = ar; // save aspect ratio as member
        this.arAction = arAction; // save aspect ratio action as member
        this.staticObjects = new ArrayList<>(); // create static object list
        this.worldObjects = new ArrayList<>(); // create world object list
        this.collidables = new ArrayList<>(); // create collidables list
        this.cam = new Camera(); // create camera
        this.mihsb = new MIHSB(); // create MIHSB
        this.createSPs(); // create and initialize shader programs
        glfwSetScrollCallback(windowHandle, (w, x, y) -> { // when the user scrolls
            this.cam.zoom(y > 0 ? 1.15f : 0.85f); // zoom on camera
        });
    }

    /**
     * Creates the two shader programs (one for static objects, one for world objects) and then initializes them
     */
    protected void createSPs() {
        this.sps = new ShaderProgram("/shaders/vertex.glsl",
                "/shaders/fragment.glsl"); // create static object shader program
        this.spw = new ShaderProgram("/shaders/vertex.glsl",
                "/shaders/fragment.glsl"); // create world object shader program
        this.initSP(sps, false); // initialize static object shader program with no camera uniforms
        this.initSP(spw, true); // initialize world object shader program with camera uniforms
    }

    /**
     * Initializes the given shader program by registering the appropriate uniforms
     * @param sp the shader program to initialize
     * @param camera whether the shader program will use a camera
     */
    protected void initSP(ShaderProgram sp, boolean camera) {
        sp.registerUniform("ar"); // register aspect ratio uniform
        sp.registerUniform("arAction"); // register aspect ratio action uniform
        sp.registerUniform("x"); // register object x uniform
        sp.registerUniform("y"); // register object y uniform
        sp.registerUniform("isTextured"); // register texture flag uniform
        sp.registerUniform("color"); // register color uniform
        sp.registerUniform("blend"); // register blend uniform
        sp.registerUniform("texSampler"); // register texture sampler uniform
        if (camera) { // only register camera uniforms if camera enabled for that shader program
            sp.registerUniform("camX"); // register camera x uniform
            sp.registerUniform("camY"); // register camera y uniform
            sp.registerUniform("camZoom"); // register camera zoom uniform
        }
    }

    /**
     * Delegates mouse input the MIHSB
     * @param x the normalized and de-aspected x position of the mouse if hover event, 0 otherwise
     * @param y the normalized and de-aspected y position of the mouse if hover event, 0 otherwise
     * @param action the nature of the mouse input (GLFW_PRESS, GLFW_RELEASE, or GLFW_HOVERED)
     * @return an array containing the mouse interactable IDs of all mouse interactable objects that were clicked
     */
    public int[] mouseInput(float x, float y, int action) {
        return this.mihsb.mouseInput(x, y, this.cam, action); // delegate to MIHSB
    }

    /**
     * Handles a resize of the window
     * @param ar the new aspect ratio
     * @param arAction the new aspect ratio action (see GameEngine.init)
     */
    public void resized(float ar, boolean arAction) {
        this.ar = ar; // save new aspect ratio
        this.arAction = arAction; // save new aspect ratio action (see GameEngine.init)
        this.ensureAllPlacements(); // make sure static objects are correctly positioned
    }

    /**
     * Updates the objects in the collection
     * @param interval the amount of time to account for
     */
    public void update(float interval) {
        for (StaticObject so : this.staticObjects) so.o.update(interval); // update static objects
        for (GameObject wo : this.worldObjects) wo.update(interval); // update world objects
        this.cam.update(); // update camera
    }

    /**
     * Renders the world objects (the world) and then the static objects (the HUD)
     */
    public void render() {
        this.renderWOs(); // render world objects first
        this.renderSOs(); // render static objects next
    }

    /**
     * Renders all the static objects
     */
    private void renderSOs() {
        this.sps.bind(); // bind shader program
        this.sps.setUniform("texSampler", 0); // set texture sampler uniform to use texture unit 0
        this.sps.setUniform("ar", this.ar); // set aspect ratio uniform
        this.sps.setUniform("arAction", this.arAction ? 1 : 0); // set aspect ratio action uniform
        for (StaticObject so : this.staticObjects) so.o.render(this.sps); // render static objects
        this.sps.unbind(); // unbind shader program
    }

    /**
     * Renders all the world objects
     */
    private void renderWOs() {
        this.spw.bind(); // bind shader program
        this.spw.setUniform("texSampler", 0); // set texture sampler uniform to use texture unit 0
        this.spw.setUniform("ar", this.ar); // set aspect ratio uniform
        this.spw.setUniform("arAction", this.arAction ? 1 : 0); // set aspect ratio action uniform
        this.spw.setUniform("camX", this.cam.getX()); // set camera x uniform
        this.spw.setUniform("camY", this.cam.getY()); // set camera y uniform
        this.spw.setUniform("camZoom", this.cam.getZoom()); // set camera zoom uniform
        for (GameObject wo : this.worldObjects) wo.render(this.spw); // render world objects
        this.spw.unbind(); // unbind shader program
    }

    /**
     * Ensures the positions of all static objects in the order that they were added
     */
    public void ensureAllPlacements() { for (StaticObject so : this.staticObjects ) so.ensurePosition(this.ar); }

    /**
     * Ensures the static object at the given index is positioned according to its position settings. This is good to
     * call after modifying the size of a static object's corresponding game object in any fashion (scale, width, new
     * Model, etc.)
     * @param i the index of the object whose placement should be ensured
     */
    public void ensurePlacement(int i) { getStaticObject(i).ensurePosition(this.ar); }

    /**
     * Changes the positioning settings of the static object at the given index
     * @param i the index to look for the static object whose settings are to be changed
     * @param settings the new settings
     * @param duration the duration of animation to undergo when changing (<= 0f will result in no animation)
     */
    public void moveStaticObject(int i, PositionSettings settings, float duration) {
        StaticObject so = getStaticObject(i); // attempt to get static object
        so.settings = settings; // update settings
        if (duration > 0f) { // if animated change
            Pair pos = so.settings.getCorrectPosition(so.o, this.ar); // get correct position
            so.o.givePosAnim(new PositionalAnimation(pos.x, pos.y, null, duration)); // start animation
        } else { // if not an animated change
            so.ensurePosition(this.ar); // just change position immediately
        }
    }

    /**
     * Adds the given game object to the collection as a world object
     * @param o the game object to add
     */
    public void addObject(GameObject o) {
        this.worldObjects.add(o); // add to game objects
        // if object is interactable with a mouse, add it to the MIHSB with the camera usage flag true (world object)
        if (o instanceof MIHSB.MouseInteractable) this.mihsb.add((MIHSB.MouseInteractable)o, true);
        if (o instanceof PhysicsObject) { // if object is a physics object
            this.collidables.add((PhysicsObject)o); // add it to collidables
            ((PhysicsObject)o).setCollidables(this.collidables); // and tell it to pay attention to ROC's collidables
        }
    }

    /**
     * Adds the given game object to the collection as a static object
     * @param o the game object to add
     * @param settings the settings to use to place the game objet
     */
    public void addObject(GameObject o, PositionSettings settings) {
        StaticObject so = new StaticObject(o, settings); // wrap object and settings into single object
        // if object is interactable with a mouse, add it to the MIHSB with the camera usage flag false (static object)
        if (o instanceof MIHSB.MouseInteractable) this.mihsb.add((MIHSB.MouseInteractable)o, false);
        so.ensurePosition(this.ar); // position the object according to its settings
        this.staticObjects.add(so); // add object to static objects list
    }

    /**
     * Attempts to acquire the static object at the given index, throwing an error if out of bounds
     * @param i the index to look for the static object
     */
    private StaticObject getStaticObject(int i) {
        try { // try to get item
            return this.staticObjects.get(i); // and return it
        } catch (Exception e) { // if exception
            Utils.handleException(e, "gameobjects.RenderableObjectCollection", "getStaticObject(i)", true); // handle
        }
        return null; // this is here to make the compiler be quiet
    }

    /**
     * Attempts to acquire the static game object at the given index
     * @param i the index to look at
     * @return the static game object at the corresponding index
     */
    public GameObject getStaticGameObject(int i) {
        return getStaticObject(i).o; // get static object's game object and return
    }

    /**
     * Attempts to acquire the world game object at the given index
     * @param i the index to look at
     * @return the world game object at the corresponding index
     */
    public GameObject getWorldGameObject(int i) {
        try { // try to get object
            return this.worldObjects.get(i); // and return it
        } catch (Exception e) { // if exception
            Utils.handleException(e, "gameobjects.RenderableObjectCollection", "getWorldGameObject(i)", true); // handle
        }
        return null; // this is here to make the compiler be quiet
    }

    /**
     * @return the camera
     */
    public Camera getCam() { return this.cam; }

    /**
     * Cleans up the HUD
     */
    public void cleanup() {
        if (this.sps != null) this.sps.cleanup(); // cleanup static object shader program
        if (this.spw != null) this.spw.cleanup(); // cleanup world object shader program
        for (StaticObject so : this.staticObjects) so.o.cleanup(); // cleanup static objects
        for (GameObject wo : this.worldObjects) wo.cleanup(); // cleanup world objects
    }

    /**
     * Settings which determine the position of a static object. Adding objects to an object collection using settings
     * like these allows for the easy creation of powerful HUDs
     */
    public static class PositionSettings {

        /**
         * Data
         */
        private final float ox, oy, padding;  // object x, object y, and padding value
        private final boolean accountForSize; // account for size flag
        private GameObject px, py;            // x-parent and y-parent

        /**
         * Creates the settings such that the corresponding game object will simply reside at the given object
         * coordinates (converted to projected coordinates)
         * @param ox the object x
         * @param oy the object y
         * @param accountForSize whether to account for the size of the corresponding game object when calculating the
         *                       position. For example, if x and y are -1f, only the top-right of the game object would
         *                       normally be visible because (0, 0) is at the center of the model. If this parameter is
         *                       set to true, the position will be modified so as to bump the object into view
         *                       perfectly. This essentially allows for very easy binding of game objects to the edge of
         *                       the window
         * @param padding the amount of padding to use if accounting for size
         */
        public PositionSettings(float ox, float oy, boolean accountForSize, float padding) {
            this.ox = ox; // save object x
            this.oy = oy; // save object y
            this.accountForSize = accountForSize; // save accounting for size flag
            this.padding = padding; // save padding
        }

        /**
         * Creates these settings such that the corresponding game objects's position may be dependent on one or two
         * other game objects' positions. px and py do not need to be the same game object. One or both of them may be
         * null. For a null parent, that specific axis position is determined independently as a projected coordinate
         * converted from ox or oy. For example, if px is null, the corresponding game object's x position will be
         * independently found by directly converting ox to projected coordinates.
         * @param px the parent game object whose x position determines these settings' corresponding game object's
         *           x position
         * @param py the parent game object whose y position determines these settings' corresponding game object's
         *           y position
         * @param ox if px is not null, how many widths away o should be from px (where the width is the average of both
         *           of their widths). If px is null, the object x position for this game object
         * @param oy if py is not null, how many widths away o should be from py (where the height is the average of
         *           both of their heights). If py is null, the object y position for this game object
         * @param padding the amount of padding to place between px/py and the corresponding game object for these
         *                settings
         */
        public PositionSettings(GameObject px, GameObject py, float ox, float oy, float padding) {
            this.px = px; // save px as member
            this.py = py; // save py as member
            this.ox = ox; // save ox as member
            this.oy = oy; // save oy as member
            this.accountForSize = true; // save account for size flag
            this.padding = padding; // save padding value
        }

        /**
         * Calculates the correct position of a given game object based on the settings
         * @param o the object whose position to calculate
         * @param ar the aspect ratio of the window
         * @return a coordinate containing the correct coordinates
         */
        public Pair getCorrectPosition(GameObject o, float ar) {

            // create Pair object and start with independent object coordinates
            Pair pos = new Pair(this.ox, this.oy); // create new Pair object with o-coordinates to start
            if (px == null || py == null)
                Transformation.deaspect(pos, ar); // if either x or y is independent, just de-aspect and be done

            // calculate x
            if (px == null) { // if x is independent
                if (accountForSize) { // and size needs to be accounted for
                    if (pos.x < 0f) pos.x += (o.getWidth() / 2 + padding); // if on left side, nudge to the right
                    else if (pos.x > 0f) pos.x -= (o.getWidth() / 2 + padding); // if on right side, nudge to the left
                } // if size does not need to be accounted for, we are done calculating x
            } else pos.x = px.getX() + this.ox * ((o.getWidth() / 2) + (px.getWidth() / 2)) +
                    Math.signum(this.ox) * padding; // otherwise calculate x relative to parent

            // calculate y
            if (py == null) { // if y is independent
                if (accountForSize) { // and size needs to be accounted for
                    if (pos.y < 0f) pos.y += (o.getHeight() / 2 + padding); // if on bottom side, nudge upwards
                    else if (pos.y > 0f) pos.y -= (o.getHeight() / 2 + padding); // if on top side, nudge downwards
                }
            } else pos.y = py.getY() + this.oy * ((o.getHeight() / 2) + (py.getHeight() / 2)) +
                    Math.signum(this.oy) * padding; // otherwise calculate y relative to parent

            // return calculate position
            return pos; // return
        }
    }

    /**
     * Binds a game object to a set of static positioning settings. This is useful for building HUDs
     */
    private static class StaticObject {

        /**
         * Data
         */
        GameObject o;              // the game object
        PositionSettings settings; // settings for positioning

        /**
         * Constructor
         * @param o the game object
         * @param settings the settings
         */
        public StaticObject(GameObject o, PositionSettings settings) {
            this.o = o; // save game object as member
            this.settings = settings; // save settings as member
        }

        /**
         * Ensures that the game object is correctly positioned according to its settings
         * @param ar the aspect ratio of the window
         */
        public void ensurePosition(float ar) {
            Pair correctPos = this.settings.getCorrectPosition(this.o, ar); // calculate correct position
            o.setX(correctPos.x); // set x
            o.setY(correctPos.y); // set y
        }
    }
}
