package gameobject;

import gameobject.gameworld.Area;
import gameobject.gameworld.Entity;
import gameobject.gameworld.GameWorld;
import gameobject.gameworld.WorldObject;
import gameobject.ui.ListObject;
import graphics.*;
import utils.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static utils.Settings.SCROLL;

/*
 * ROC.java
 * Ambulare
 * Jacob Oaks
 * 4/22/20
 */

/**
 * Holds a collection of game objects and renders them all in one method call. This class divides the game objects it
 * contains into:
 * (1) StaticObjects: bound to certain positions in the window and have extensive positioning settings and customization
 * (useful for HUD creation). These are indexed by integer tags and it is a good idea to keep static final integer
 * variables maintaining tags for every element added to the ROC for easier retrieval
 * (2) WorldObjects: objects with physics that are directly given to a game world to manage
 * Note that, by default, ROCs do not instantiate their game world to save on resources for settings where a game world
 * is not necessary. useGameWorld() must be called for the ROC to instantiate its game world
 * ROCs also provide extensive support for mouse interaction through having a mouse input engine
 */
public class ROC {

    /**
     * Members
     */
    private final Map<Integer, StaticObject> staticObjects; /* a mapping from tags to game objects that will not used
                                                the camera when rendered. Instead, they are bound to positioning
                                                settings that determine where in the window they should be rendered at
                                                all times. In other words, these are HUD items. See ROC.PositionSettings
                                                for more info */
    private final List<ListObject> lists;    // a list of list objects to notify about scrolling
    private final List<AnimatedTexture> ats; // list of animated textures to update
    private final MouseInputEngine mip;      // mouse input engine to handle mouse input
    private GameWorld gameWorld;             // the game world to render underneath the static objects
    private GameObject fadeBox;              // fade box used for fading the entire screen for smooth transitions
    private ShaderProgram sp;                // the shader program to use to render static objects
    private float fadeTime;                  // amount of time the fade box fade should take
    private float fadeTimeLeft;              // amount of time left for the fade box fade if one is occurring

    /**
     * Constructor
     */
    public ROC() {
        this.staticObjects = new HashMap<>(); // initialize static objects to an empty hash map
        this.lists = new ArrayList<>(); // initialize list object list to an empty list
        this.ats = new ArrayList<>(); // initialize animated textures to an empty list
        this.mip = new MouseInputEngine(); // initialize mouse input engine
        this.initSP(); // create and initialize shader program
    }

    /**
     * Instantiates the ROC's game world using the given starting area
     *
     * @param startingArea the area to give the game world to start with
     * @param player the player to give to the game wworld
     */
    public void useGameWorld(Area startingArea, Entity player) {
        this.gameWorld = new GameWorld(this.mip, player, startingArea, this); // create world
        this.mip.useCam(this.gameWorld.getCam()); // tell the mouse input engine to use game world cam for calculations
        if (player != null) { // if the player isn't null
            this.mip.add(player, true); // add player to mouse input engine
            this.ats.add((AnimatedTexture)player.getMaterial().getTexture()); // add animated texture
        }
    }

    /**
     * Initializes the static object shader program by creating it and then registering the appropriate uniforms
     */
    protected void initSP() {
        // create the static object shader program using the HUD shaders
        this.sp = new ShaderProgram(new Utils.Path("/shaders/hud_vertex.glsl", true),
                new Utils.Path("/shaders/hud_fragment.glsl", true));
        sp.registerUniform("ar"); // register aspect ratio uniform
        sp.registerUniform("arAction"); // register aspect ratio action uniform
        sp.registerUniform("x"); // register object x uniform
        sp.registerUniform("y"); // register object y uniform
        sp.registerUniform("isTextured"); // register texture flag uniform
        sp.registerUniform("color"); // register color uniform
        sp.registerUniform("blend"); // register blend uniform
        sp.registerUniform("texSampler"); // register texture sampler uniform
        sp.registerUniform("minY"); // register minimum y uniform
        sp.registerUniform("maxY"); // register maximum y uniform
        sp.registerUniform("boundY"); // register y bounding flag uniform
    }

    /**
     * Delegates mouse input to the mouse input engine
     *
     * @param x      the normalized and de-aspected x position of the mouse if hover event, the horizontal scroll factor
     *               if scrolling event, or 0 otherwise
     * @param y      the normalized and de-aspected y position of the mouse if hover event, the vertical scroll factor
     *               if scrolling event, or 0 otherwise
     * @param action the nature of the mouse input (GLFW_PRESS, GLFW_RELEASE, GLFW_HOVERED, or SCROLL)
     */
    public void mouseInput(float x, float y, int action) {
        this.mip.mouseInput(x, y, action); // delegate to mouse input engine
        if (action == SCROLL) { // if the event was a scroll event
            if (this.gameWorld != null) this.gameWorld.scrollInput(x, y); // tell game world if it exists
            for (ListObject lo : this.lists) lo.scrollInput(x, y); // tell list objectss
        }
    }

    /**
     * Handles a resize of the window by ensuring the positions of all static objects and updating the fade box size
     */
    public void resized() {
        this.ensureAllPlacements(); // make sure static objects are correctly positioned
        if (this.fadeBox != null) // if there is a fade box,
            this.fadeBox.setScale(2f * (Global.ar > 1f ? Global.ar : 1),
                    2f / (Global.ar < 1f ? Global.ar : 1)); // update it to fit the new window size
        if (this.gameWorld != null) this.gameWorld.getArea().resized(); // tell the game world's area about the resize
    }

    /**
     * Updates everything held within the ROC: animated textures, the game world, static objects, and the fade box
     *
     * @param interval the amount of time to account for
     */
    public void update(float interval) {
        for (AnimatedTexture at : this.ats) at.update(interval); // update animated textures
        if (this.gameWorld != null) this.gameWorld.update(interval); // update the game world if it's not null
        for (StaticObject so : this.staticObjects.values()) so.o.update(interval); // update static objects
        if (this.fadeBox != null) { // if there is a fade box, need to update it
            if (this.fadeTimeLeft > 0f) { // if fading in
                fadeTimeLeft -= interval; // account for the time
                this.fadeBox.getMaterial().getColor()[3] = (fadeTimeLeft / fadeTime); // update the alpha of the color
                if (fadeTimeLeft <= 0f) this.fadeBox = null; // if time is up, delete fade box
            } else { // if fading out
                fadeTimeLeft += interval; // account for the time
                this.fadeBox.getMaterial().getColor()[3] = 1f - (fadeTimeLeft / fadeTime); // update the alpha of color
                /* wait an extra second after fade is done to delete the box in case another render or two occurs during
                   a transition */
                if (fadeTimeLeft >= 1f) this.fadeBox = null; // delete after an additional second of waiting
            }
        }
        Global.debugInfo.setField("ROC animated textures", Integer.toString(this.ats.size()));
    }

    /**
     * Renders everything held within the ROC: the game world, the static objects, and the fade box
     */
    public void render() {
        if (this.gameWorld != null) this.gameWorld.render(); // render the world first, underneath the static objects
        this.sp.bind(); // bind static object shader program
        this.sp.setUniform("texSampler", 0); // set texture sampler uniform to use texture unit 0
        this.sp.setUniform("ar", Global.ar); // set aspect ratio uniform
        this.sp.setUniform("arAction", Global.arAction ? 1 : 0); // set aspect ratio action uniform
        for (StaticObject so : this.staticObjects.values()) so.o.render(this.sp); // render static objects
        if (this.fadeBox != null) fadeBox.render(this.sp); // render fade box if enabled
        this.sp.unbind(); // unbind shader program
    }

    /**
     * Begins a fade-in of the window. Note that this only fades objects rendered by the ROC. Other objects may not fade
     * correctly
     *
     * @param color the color to use for the fade
     * @param time  how long (in seconds) the fade should take
     */
    public void fadeIn(float[] color, float time) {
        this.fadeBox = new GameObject(Model.getStdGridRect(1, 1), new Material(color)); // create fade box
        // scale the fade box based on the window size and aspect ratio
        this.fadeBox.setScale(2f * (Global.ar > 1f ? Global.ar : 1), 2f / (Global.ar < 1f ? Global.ar : 1));
        this.fadeTime = this.fadeTimeLeft = time; // start timer
    }

    /**
     * Begins a fade-out of the window. Note that this only fades objects rendered by the ROC. Other objects may not
     * fade correctly
     *
     * @param color the color to use for the fade
     * @param time  how long (in seconds) the fade should take
     */
    public void fadeOut(float[] color, float time) {
        this.fadeBox = new GameObject(Model.getStdGridRect(1, 1), new Material(color)); // create fade box
        // scale the fade box based on the window size and aspect ratio
        this.fadeBox.setScale(2f * (Global.ar > 1f ? Global.ar : 1), 2f / (Global.ar < 1f ? Global.ar : 1));
        this.fadeTime = this.fadeTimeLeft = -time; // start timer
    }

    /**
     * Ensures the positions of all static objects
     */
    public void ensureAllPlacements() {
        for (StaticObject so : this.staticObjects.values()) so.ensurePosition(Global.ar);
    }

    /**
     * Ensures the static object with the given tag is positioned according to its position settings. This is good to
     * call after modifying the size of a static object's corresponding game object in any fashion (scale, width, new
     * Model, etc.) or if the object with the given tag is dependent on a different object's position which has recently
     * changed
     *
     * @param tag the tag of the object whose placement should be ensured. If no object exists with the given tag, the
     *            occurrence will be logged and ignored
     */
    public void ensurePlacement(int tag) {
        if (this.staticObjects.get(tag) != null) // if a static object exists with that tag
            getStaticObject(tag).ensurePosition(Global.ar); // ensure its position
        else // otherwise
            Utils.log("Attempted to ensure the placement of an item with a non-existent tag: " + tag +
                    ". Ignoring", this.getClass(), "ensurePlacement", false); // log and ignore
    }

    /**
     * Changes the positioning settings of the static object with the given tag with an optional positional animation
     *
     * @param tag      the tag of the static object to move
     * @param settings the new settings
     * @param duration the duration of animation to undergo when changing (<= 0f will result in no animation)
     */
    public void moveStaticObject(int tag, PositionSettings settings, float duration) {
        StaticObject so = getStaticObject(tag); // attempt to get static object
        so.settings = settings; // update settings
        if (duration > 0f) { // if animated change
            Pair<Float> pos = so.settings.getCorrectPosition(so.o, Global.ar); // get correct position
            so.o.givePosAnim(new PositionalAnimation(pos.x, pos.y, null, duration)); // start animation
        } else { // if not an animated change
            so.ensurePosition(Global.ar); // just change position immediately
        }
    }

    /**
     * Adds the given game object to the ROC as a world object. This should be called as opposed to getting the world
     * first and then adding directly to the world, in case the object being added is able to be interacted with by a
     * mouse. If not added through the ROC, it also won't be added to the mouse input engine and mouse interaction will
     * not occur properly. In addition, this will automatically add the object's texture to the ROC's animated
     * texture list if is animated and is not already in the list. If this is not done, any animated objects may not
     * animate correctly because their textures are not being updated
     *
     * @param wo the world object to add
     */
    public void addToWorld(WorldObject wo) {
        if (this.gameWorld == null) { // if game world hasn't been instantiated
            Utils.log("Attempted to add an object to a ROC's game world withouth calling useGameWorld() first." +
                    "Ignoring request", this.getClass(), "addToWorld", false); // log occurrence
            return; // and return without crashing
        }
        Texture t = wo.getMaterial().getTexture(); // get the texture of the object's material
        if (t instanceof AnimatedTexture) { // if it's an animated texture
            AnimatedTexture at = (AnimatedTexture) t; // cast it to an animated texture
            if (!this.ats.contains(at)) this.ats.add(at); // add it to animated textures list if not there already
        }
        this.gameWorld.addObject(wo); // add the object to the game world
        if (wo instanceof MouseInputEngine.MouseInteractive) // if object is able to be interacted with by a mouse
            this.mip.add((MouseInputEngine.MouseInteractive) wo, true); // add to MIP with cam flag set to true
    }

    /**
     * Adds the given game object to the ROC as a static object
     *
     * @param o        the game object to add
     * @param tag      the tag to use to identify the static object
     * @param replace  whether to replace a theoretically already existing static object with the same tag
     * @param settings the settings to use to place the game object
     */
    public void addStaticObject(GameObject o, int tag, boolean replace, PositionSettings settings) {
        // if a static object with the same tag already exists and replacing is disabled, return
        if (!replace && this.staticObjects.get(tag) != null) return;
        StaticObject so = new StaticObject(o, settings); // wrap object and settings into single object
        if (o instanceof MouseInputEngine.MouseInteractive) // if object is able to be interacted with by a mouse
            this.mip.add((MouseInputEngine.MouseInteractive) o, false); // add to MIP with cam flag set to false
        so.ensurePosition(Global.ar); // position the object according to its settings
        this.staticObjects.put(tag, so); // add object to static objects map
        Texture t = o.getMaterial().getTexture(); // get the texture of the object's material
        if (t instanceof AnimatedTexture) { // if it's an animated texture
            AnimatedTexture at = (AnimatedTexture) t; // cast it to an animated texture
            if (!this.ats.contains(at)) this.ats.add(at); // add it to animated textures list if not there already
        }
        if (o instanceof ListObject) this.lists.add((ListObject)o); // if list object, add to list of list objectss
    }

    /**
     * Attempts to acquire the static object with the given tag, throwing an error if it doesn't existss
     *
     * @param tag the tag of the object to get
     */
    private StaticObject getStaticObject(int tag) {
        StaticObject so = this.staticObjects.get(tag); // try to retrieve the static object
        if (so == null) Utils.handleException(new Exception("No static object found for tag '" + tag + "'"),
                this.getClass(), "getStaticObject", true); // crash program if the static object is null
        return so; // return the static object
    }

    /**
     * Attempts to acquire the static object's game object with the given tag
     *
     * @param tag the tag of the item to return
     * @return the static game object at the corresponding index
     */
    public GameObject getStaticGameObject(int tag) {
        return getStaticObject(tag).o; // get static object's game object and return
    }

    /**
     * @return the ROC's game world. Note that this will return null if useGameWorld() hasn't been called
     */
    public GameWorld getGameWorld() {
        return this.gameWorld;
    }

    /**
     * @return whether or not the ROC is performing a fade
     */
    public boolean fading() {
        return this.fadeBox != null; // if the fade box isn't null, a fade is being performed
    }

    /**
     * Cleans up the ROC by cleaning up static objects and the game world
     */
    public void cleanup() {
        if (this.sp != null) this.sp.cleanup(); // cleanup shader program
        if (this.gameWorld != null) this.gameWorld.cleanup(); // cleanup game worlds
        if (this.fadeBox != null) this.fadeBox.cleanup(); // cleanup fade box
        for (StaticObject so : this.staticObjects.values()) {
            if (so.o != Global.debugInfo) // if the static object is not the debug info
                so.o.cleanup(); // cleanup static objects
        }
        AnimatedTexture.texCoords = new HashMap<>(); // reset animated texture texture coordinate VBOs
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
         *
         * @param ox             the object x
         * @param oy             the object y
         * @param accountForSize whether to account for the size of the corresponding game object when calculating the
         *                       position. For example, if x and y are -1f, only the top-right of the game object would
         *                       normally be visible because (0, 0) is at the center of the model. If this parameter is
         *                       set to true, the position will be modified so as to bump the object into view
         *                       perfectly. This essentially allows for very easy binding of game objects to the edge of
         *                       the window
         * @param padding        the amount of padding to use if accounting for size
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
         *
         * @param px      the parent game object whose x position determines these settings' corresponding game object's
         *                x position
         * @param py      the parent game object whose y position determines these settings' corresponding game object's
         *                y position
         * @param ox      if px is not null, how many widths away o should be from px (where the width is the average
         *                of both
         *                of their widths). If px is null, the object x position for this game object
         * @param oy      if py is not null, how many widths away o should be from py (where the height is the
         *                average of
         *                both of their heights). If py is null, the object y position for this game object
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
         *
         * @param o  the object whose position to calculate
         * @param ar the aspect ratio of the window
         * @return a coordinate containing the correct coordinates
         */
        public Pair<Float> getCorrectPosition(GameObject o, float ar) {

            // create Pair object and start with independent object coordinates
            Pair<Float> pos = new Pair<>(this.ox, this.oy); // create new Pair object with o-coordinates to start
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
         *
         * @param o        the game object
         * @param settings the settings
         */
        public StaticObject(GameObject o, PositionSettings settings) {
            this.o = o;
            this.settings = settings;
        }

        /**
         * Ensures that the game object is correctly positioned according to its settings
         *
         * @param ar the aspect ratio of the window
         */
        public void ensurePosition(float ar) {
            Pair<Float> correctPos = this.settings.getCorrectPosition(this.o, ar); // calculate correct position
            o.setX(correctPos.x); // set x
            o.setY(correctPos.y); // set y
        }
    }
}
