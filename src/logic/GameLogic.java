package logic;

import gameobject.GameObject;
import gameobject.ROC;
import gameobject.ui.EnhancedTextObject;
import gameobject.ui.TextObject;
import graphics.Material;
import graphics.Model;
import graphics.ShaderProgram;
import utils.Global;
import utils.Node;
import utils.Utils;

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
    protected ROC roc;                          // holds and renders all objects. See gameobject.ROC
    protected Node transferData;                /* when a logic is switched to, this will store any transfer data from
                                                   the logic change. This may be null if no transfer data was given */
    protected boolean renderROC = true;         /* extending classes can disable ROC rendering if they want to render in
                                                   some other manner that does not involve an ROC */
    public static EnhancedTextObject debugInfo; // debugging info to display when the engine gives info

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
        debugInfo = new EnhancedTextObject(new Material(new float[] {0f, 0f, 0f, 0.5f}),
                Global.font, " \n \n ", EnhancedTextObject.Line.DEFAULT_PADDING * 2);
        debugInfo.setVisibility(false); // don't make visible until engine reports debugging data
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
     * This is called by the engine when there is new debugging information. The game logic passes this along to an
     * enhanced text object
     *
     * @param info the new debugging information, where each index is as outlined in the Engine.updateDebugMetrics()
     */
    public void reportDebugInfo(String[] info) {
        if (debugInfo != null) { // if the debug info enhanced text object is instantiated
            for (int i = 0; i < info.length; i++) // for each line of info
                debugInfo.setLineText(i,
                        (i == 0 ? "FPS: " : i == 1 ? "Update: " : "Render: ")
                                + info[i]); // update the corresponding line in the enhanced text object
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
}
