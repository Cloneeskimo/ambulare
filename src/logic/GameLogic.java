package logic;

import gameobject.GameObject;
import gameobject.ROC;
import gameobject.ui.TextObject;
import graphics.Material;
import graphics.Model;
import graphics.ShaderProgram;
import graphics.Window;
import utils.Global;
import utils.Node;
import utils.Utils;

import java.util.ArrayList;
import java.util.List;

/*
 * GameLogic.java
 * Ambulare
 * Jacob Oaks
 * 4/15/20
 */

/**
 * The game logic defines how the engine will interact with the game. To aid in the process of designing a game world
 * and interface, this abstract class has an ROC to store, interact with, and render game objects in a wide variety of
 * ways without being too intensive (see ROC class). If the extending class doesn't want to use an ROC, a flag to
 * enable/disable rendering of the ROC exists as well. There is also a static logic change member which can be set from
 * anywhere and which contains information necessary for the engine to switch logic sets. It is up to the engine to
 * check when this logic change information has been set. The idea is for different game states to be represented by
 * extensions of this very general logic class
 */
public abstract class GameLogic {

    /**
     * Static Data
     */
    public static LogicChange logicChange = null; /* info about log change. See class info above and
                                                     GameLogic.LogicChange */
    private static final int TAG_DEBUG_INFO = -9; // ROC tag for the debugging info text objects

    /**
     * Members
     */
    protected ROC roc;                        // holds and renders all objects. See gameobject.ROC
    protected Node transferData;              /* when a logic is switched to, this will store any transfer data from
                                                 the logic change. This may be null if no transfer data was given */
    protected boolean renderROC = true;       /* extending classes can disable ROC rendering if they want to render in
                                                 some other manner that does not involve an ROC */
    private DebugInfo debugInfo;              // debugging info text objects to display when the engine gives info

    /**
     * Gives the game logic transfer data to use when initializing
     *
     * @param transferData data to transfer to the logic. If null, it will be assumed that there is no transfer data.
     *                     Extending classes can access this data by accessing the transferData member
     */
    public void giveTransferData(Node transferData) {
        this.transferData = transferData; // save as members
    }

    /**
     * Initializes the logic. This method is the only entry point into the logic other then input, update, and render
     * Extending classes cannot override this method. However, this method will call initOthers() which can be
     * overridden by extending classes and should be used for additional initialization
     */
    public final void init() {
        this.roc = new ROC(); // initialize ROC
        this.initOthers(); // allow extending classes to initialize other members
    }

    /**
     * Extending classes should initialize any game objects to be placed in the ROC or any additional members here
     * In order for debug info to properly display in the HUD, this super method should be called
     */
    protected void initOthers() {
        // create debug info
        this.debugInfo = new DebugInfo(0.01f, new Material(new float[]{0f, 0f, 0f, 0.4f}));
        this.debugInfo.setVisibility(false); // don't make visible until engine reports debugging data
        this.debugInfo.setScale(0.45f, 0.45f); // make small
        this.roc.addStaticObject(debugInfo, TAG_DEBUG_INFO, false, new ROC.PositionSettings(-1f, 1f,
                true, 0.05f)); // add debug info text to ROC
    }

    /**
     * Receives keyboard input from the engine
     * Extending classes should certainly override this method to repsond to keyboard events
     *
     * @param key    the key in question
     * @param action the action of the key (GLFW_PRESS, GLFW_RELEASE, GLFW_REPEAT)
     */
    public void keyboardInput(int key, int action) {
    }

    /**
     * Receives mouse input from the engine and notifies the ROC of the input
     * Extending classes can certainly override this method to change how they react to mouse input. If super is not
     * called, or the ROC is not manually notified, it may not work as intended
     *
     * @param x      the normalized and de-aspected x position of the mouse if hover event, 0 otherwise
     * @param y      the normalized and de-aspected y position of the mouse if hover event, 0 otherwise
     * @param action the nature of the mouse input (GLFW_PRESS, GLFW_RELEASE, or GLFW_HOVERED)
     */
    public void mouseInput(float x, float y, int action) {
        this.roc.mouseInput(x, y, action); // notify ROC of input
    }

    /**
     * Updates this logic by updating the ROC
     * Extending classes can certainly override this but unless updated in the overriding method (or super.update()
     * is called), the ROC will no longer be updated
     *
     * @param interval the amount of time to account for
     */
    public void update(float interval) {
        this.roc.update(interval); // update ROC
    }

    /**
     * Renders the ROC, and then will render anything else that extending classes wish to render by calling
     * renderOthers()
     * Extending classes cannot override this method. If extending classes wish to NOT render the ROC, they can toggle
     * the renderROC flag and provide their own rendering process in renderOthers()
     */
    public final void render() {
        if (this.renderROC) this.roc.render(); // render ROC if enabled
        this.renderOthers(); // allow extending class to render
    }

    /**
     * Extending classes should override this method if they desire to render in other procedures beside the default
     * world and HUD rendering
     */
    protected void renderOthers() {
    }

    /**
     * Reacts to the window resizing by notifying the ROC
     * Extending classes can override this method but should definitely call super.resize() or the ROC may not respond
     * to the resize
     */
    public void resized() {
        this.roc.resized(); // notify ROC of resize
    }

    /**
     * This is called by the engine when there is new debugging information. The game logic passes this along to a game
     * object called DebugInfo which aggregates text objects that display this info in the logic's HUD
     *
     * @param info the new debugging information, where each index is as outlined in the Engine.updateDebugMetrics()
     */
    public void reportDebugInfo(String info[]) {
        if (this.debugInfo != null) {
            this.debugInfo.updateInfo(info); // pass info to debug info object
            this.roc.ensurePlacement(TAG_DEBUG_INFO); // ensure its position
        }
    }

    /**
     * Cleans up this logic
     * Extending classes should override this to cleanup any additional members they need to do, but they should ALWAYS
     * call super.cleanup() to clean up the base logic members as well
     */
    public void cleanup() {
        this.roc.cleanup(); // cleanup ROC
    }

    /**
     * Outlines necessary data to perform a logic change in the engine. By setting GameLogic.logicChange to an
     * instance of this, the engine will change its logic. LogicChanges can be given a transition time before the actual
     * change occurs. This allows for animations/fades. Transfer data can also be given in the form of a Node which will
     * be given to the new logic
     */
    public static class LogicChange {

        /**
         * Members
         */
        private GameLogic newLogic; // the new logic to switch to
        private Node transferData;  // data to give to the new logic
        private float transition;   // the amount of time to take before the transition between logics occurs

        /**
         * Constructor
         *
         * @param newLogic the new logic to switch to
         */
        public LogicChange(GameLogic newLogic, float transition) {
            this.newLogic = newLogic;
            this.transition = transition;
        }

        /**
         * Sets transfer data to be passed to the new logic
         *
         * @param data the data to pass
         */
        public void useTransferData(Node data) {
            this.transferData = data; // save as member
        }

        /**
         * @return the new logic of the logic change
         */
        public GameLogic getNewLogic() {
            return this.newLogic;
        }

        /**
         * @return the logic change's transfer data, or null if there is no transfer data
         */
        public Node getTransferData() {
            return this.transferData;
        }

        /**
         * @return the transition time for the logic change
         */
        public float getTransitionTime() {
            return this.transition;
        }
    }

    /**
     * An game object that serves as an aggregation of text objects which display debug information as passed through
     * from the engine. Like ListObjects, DebugInfos cannot be rotated
     */
    public static class DebugInfo extends GameObject {

        /**
         * Members
         */
        private TextObject[] info; // text objects to display debugging information
        float padding;             // padding between text and around the edges

        /**
         * Constructor
         *
         * @param padding    the amount of padding to place between the text objects and around the edges of the object
         * @param background the material to render as a background to the debug info text objects
         */
        public DebugInfo(float padding, Material background) {
            super(Model.getStdGridRect(1, 1), background); // call super with square model and background mat.
            this.info = new TextObject[6]; // create array for info
            // separate counters from the static text to improve updating the text's efficiency
            // create FPS objects
            this.info[0] = new TextObject(Global.FONT, "FPS: ");
            this.info[1] = new TextObject(Global.FONT, "N/A");
            // create update time objects
            this.info[2] = new TextObject(Global.FONT, "Update: ");
            this.info[3] = new TextObject(Global.FONT, "N/A");
            // create render time objects
            this.info[4] = new TextObject(Global.FONT, "Render: ");
            this.info[5] = new TextObject(Global.FONT, "N/A");
            this.padding = padding; // save padding as member
            this.position(); // position everything
        }

        /**
         * Updates the debug info object's model and positions all of the contained text objects
         */
        private void position() {
            // calculate width and height
            float w = 0f;
            float h = 0f;
            for (int i = 0; i < this.info.length; i += 2) { // for each debug metric
                float rowWidth = this.info[i].getWidth() + this.info[i + 1].getWidth(); // get that metric row's width
                if (rowWidth > w) w = rowWidth; // if its wider than the current max width, record it as new width
                h += this.info[i].getHeight(); // keep running total of row heights
            }
            h += 4 * padding; // account for padding between rows and above/below object
            w += 2 * padding; // account for padding to left and right of object
            this.model.setScale(w, h); // scale object to correctly fit all text objects
            float x = this.getX() - (w / 2f) + padding; // the left-most x of the object, as a starting point for text
            float y = this.getY() + h / 2f - padding; // the top-most y of the object, to be updated for placement
            for (int i = 0; i < this.info.length; i += 2) { // for each item
                float h2 = this.info[i].getHeight() / 2f; // calculate its half-height
                float w2 = this.info[i].getWidth() / 2f; // calculate its half-width
                y -= h2; // update y based on half height
                this.info[i].setPos(x + w2, y); // set the static text object's position
                // set the actual counter's position next to the static object
                this.info[i + 1].setPos(this.info[i].getX() + w2 + this.info[i + 1].getWidth() / 2f, y);
                // update y for the next object
                y -= (h2 + padding);
            }
        }

        /**
         * Updates the debug info object's text objects based on the given debugging info
         *
         * @param info the debugging info, formatted as specified by GameEngine.updateDebugMetrics(). If null, this
         *             object will make itself invisible since reporting has stopped
         */
        public void updateInfo(String[] info) {
            if (info == null) { // if null
                this.visible = false; // make self invisible
                return; // and return
            }
            // if this was invisible and actual values were given, make self visible again
            if (!this.visible) this.visible = true;
            // update the text objects with the info
            for (int i = 0; i < info.length; i++) this.info[1 + (i * 2)].setText(info[i]);
            this.position(); // re-position
        }

        /**
         * Renders the background and the text objects
         *
         * @param sp the shader program to use to render the game object
         */
        @Override
        public void render(ShaderProgram sp) {
            super.render(sp); // call super's render to render background
            if (this.visible) for (TextObject to : this.info) to.render(sp); // render the text objects
        }

        /**
         * Updates positional animations by excluding rotation
         *
         * @param interval the amount of time, in seconds, to account for
         */
        @Override
        protected void updatePosAnim(float interval) {
            this.posAnim.update(interval); // update animation
            this.setX(this.posAnim.getX()); // set x position
            this.setY(this.posAnim.getY()); // set y position
            if (this.posAnim.finished()) { // if animation is over
                this.setX(this.posAnim.getFinalX()); // make sure at the correct ending x
                this.setY(this.posAnim.getFinalY()); // make sure at the correct ending y
                this.posAnim = null; // delete the animation
            }
        }

        /**
         * Responds to movement by re-positioning the text objects
         */
        @Override
        protected void onMove() {
            this.position(); // position text objects
        }

        /**
         * Responds to horizontal scaling by horizontally scaling the containing text objects and re-positioning
         *
         * @param x the x scaling factor to use
         */
        @Override
        public void setXScale(float x) {
            for (TextObject to : this.info) to.setXScale(x); // scale containing text objects
            this.position(); // position text objects
        }

        /**
         * Responds to vertical scaling by vertically scaling the containing text objects and re-positioning
         *
         * @param y the y scaling factor to use
         */
        @Override
        public void setYScale(float y) {
            for (TextObject to : this.info) to.setYScale(y); // scale containing text objects
            this.position(); // position text objects
        }

        /**
         * Responds to scaling by scaling the containing text objects and re-positioning
         *
         * @param x the x scaling factor to use
         * @param y the y scaling factor to use
         */
        @Override
        public void setScale(float x, float y) {
            // scale containing text objects
            for (TextObject to : this.info) to.setXScale(x);
            for (TextObject to : this.info) to.setYScale(y);
            this.position(); // position text objects
        }

        /**
         * Responds to attempts to rotate by logging and ignoring the occurrence
         *
         * @param r the new rotation value in radians
         */
        @Override
        public void setRotRad(float r) {
            Utils.log("Attempted to rotate a debug info. Ignoring.", this.getClass(), "setRotRad",
                    false); // log and ignore the attempt to rotate
        }
    }
}
