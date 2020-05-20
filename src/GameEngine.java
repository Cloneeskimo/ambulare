import graphics.Window;
import logic.GameLogic;
import utils.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static utils.Settings.SCROLL;

/*
 * GameEngine.java
 * Ambulare
 * Jacob Oaks
 * 4/14/20
 */

/**
 * Abstracts away many of the lower level game functions and operates the program based on whichever logic is
 * currently active. Specifically, the engine takes care of time keeping, the game loop, and window management
 */
public class GameEngine {

    /**
     * Static Data
     */

    public static final float TIME_BETWEEN_METRIC_REPORTS = 0.75f; // time between debug metric reports/average calcs

    /**
     * Members
     */
    private final Timer timer;                   // timer used for accurate debugging and loop
    private double[] debug;                      // info about debugging. See loop() for more info
    private GameLogic logic;                     // the logic the engine should follow
    private float logicTransitionTime;           // a timer for logic transitions
    private boolean debugging;                   // whether or not the engine is reporting debugging info to the logic

    /**
     * Constructor
     *
     * @param logic the starting logic to follow
     */
    public GameEngine(GameLogic logic) {
        Settings.load(); // load settings
        Global.gameWindow = new Window(Global.WINDOW_TITLE, // create window using the global window title
                (Integer)Settings.getSetting(Settings.Setting.STARTING_WINDOW_WIDTH), // use correct starting width
                (Integer)Settings.getSetting(Settings.Setting.STARTING_WINDOW_HEIGHT), // use correct starting height
                (Boolean)Settings.getSetting(Settings.Setting.V_SYNC)); // use correct v-sync option
        this.timer = new Timer(); // initialize timer
        this.logic = logic; // save starting logic as member
    }

    /**
     * This starts the engine by initializing it and then beginning the game loop
     */
    public void start() {
        this.init(); // initialize the engine
        this.loop(); // begin game loop
        this.cleanup(); // cleanup after loop ends
    }

    /**
     * Initializes the engine
     */
    private void init() {
        SoundManager.init(); // initialize sound manager
        Global.gameWindow.init(); // initialize the window
        Global.updateAr(); // update global aspect ratio variable
        this.initInput(); // initialize mouse and keyboard input callbacks
        Global.init(); // initialize global members
        this.logic.init(); // initialize starting logic
        this.timer.init(); // initialize the timer
    }

    /**
     * Creates GLFW window callbacks for mouse input and keyboard input
     */
    private void initInput() {
        // funnel both mouse movement and buttons events into a single method called mouseInput()
        glfwSetCursorPosCallback(Global.gameWindow.getHandle(), (w, x, y) -> { // create GLFW callback for cursor position
            // pass along cursor position to mouseInput() and use GLFW_HOVERED as the action
            this.mouseInput((float) x, (float) y, GLFW_HOVERED);
        });
        glfwSetMouseButtonCallback(Global.gameWindow.getHandle(), (w, b, a, i) -> {
            // mouse button events don't have mouse positions, so just send zero and hope mouseInput checks the action
            this.mouseInput(0f, 0f, a);
        });
        glfwSetScrollCallback(Global.gameWindow.getHandle(), (w, x, y) -> {
            // pass along scroll factor and SCROLL flag as action to mouse input
            this.mouseInput((float) x, (float) y, SCROLL);
        });
        glfwSetKeyCallback(Global.gameWindow.getHandle(), (w, k, s, a, m) -> {
            this.keyboardInput(k, a); // pass keyboard input to the keyboardInput() function
        });
    }

    /**
     * Defines the game loop
     * This game uses a fixed-step game loop. It consists of four phases: gathering input, updating, rendering, and
     * syncing
     */
    private void loop() {

        // timekeeping variables
        float elapsedTime; // how much time has passed since last loop
        float accumulator = 0f; // how much time is unaccounted for
        float interval = 1f / Global.TARGET_UPS; // how much time there should be between updates
        debug = new double[12]; /* debug information array where indices 0-9 are used by the updateDebugMetrics() method
            and indices 10-11 are used for timestamps to calculate update and render times, respectively */
        debug[1] = Double.POSITIVE_INFINITY; // initialize lowest FPS to infinity for proper worst calculations

        // game loop
        while (!Global.gameWindow.shouldClose()) { // while the Window shouldn't close

            // timekeeping
            elapsedTime = this.timer.getElapsedTime(true); // get elapsed time since last loop
            if (this.debugging) { // if debugging is enabled
                debug[0] += elapsedTime; // update the accumulator
                updateDebugMetrics(1 / elapsedTime, debug, 0); // update FPS metrics
            }
            accumulator += elapsedTime; // add elapsed time to the accumulator for time unaccounted for

            // phase 1: input
            Global.gameWindow.pollEvents(); // gather input by polling for GLFW window events
            // if debugging, record time in milliseconds before update

            // phase 2: update
            if (this.debugging) debug[10] = (float) Timer.getTimeMilliseconds();
            if (Global.resetAccumulator) { // if the accumulator needs to be reset
                accumulator = 0f; // reset accumulator
                Global.resetAccumulator = false; // reset accumulator resetting flag
            }
            while (accumulator >= interval) { // while there is a sufficient amount of unaccounted for time
                this.update(interval); /* doing multiple updates for the same smaller interval is preferable to doing
                    one large update with a huge interval because such large updates can be game-breaking, especially
                    when it comes to things like collision */
                accumulator -= interval; // account for the time for the update
            }
            if (this.debugging) { // if debugging, calculate update time and report it to logic
                debug[11] = (float) Timer.getTimeMilliseconds(); // get time after update/before render
                // update debugging metrics with update time
                this.updateDebugMetrics(debug[11] - debug[10], debug, 1);
            }

            // phase 3: render
            this.render();  /* render outside of the above loop because, as opposed to updating, outdated renders are
                useless wastes of GPU power */
            if (this.debugging) // if debugging, update debugging metrics with render time
                this.updateDebugMetrics(Timer.getTimeMilliseconds() - debug[11], debug, 2);

            // phase 4: sync
            if (!Global.gameWindow.usesVSync()) this.sync(1 / (float) Global.TARGET_FPS);
        }
    }

    /**
     * Updates debugging metrics by keeping track of an average and a worst for each value. If the globally set amount
     * of time between reports has passed, information is update in the global debug info. This information takes the
     * form of an  array of strings where each string contains the average and worst values for three debugging metrics
     * in the following order: FPS, update time, and render time. The worst values are maintained over a period of calls
     * to this method where type is not -1. That is, to reset the worst values, call this method with type set to -1
     *
     * @param val       the value of the new debugging calculation
     * @param debugInfo the debug info array where index 0 should be an accumulator of time updated outside of this
     *                  method, and following index 0 should be nine untouched indices used for calculations. Anything
     *                  outside of indices 0-9 will not be touched by the method
     * @param type      the type of reported metric where 0 is FPS, 1 is update time, and 2 is render time. Additionally, -1
     *                  can be given to signify that the worst metrics should be reset. If an invalid value is given, the
     *                  occurrence will be logged and ignored
     */
    private void updateDebugMetrics(double val, double[] debugInfo, int type) {
        if (type == -1) { // if the call was to reset the worst values
            for (int i = 0; i < 3; i++) debugInfo[1 + i * 3] = i == 0 ? Double.POSITIVE_INFINITY : 0f; // reset worsts
            return; // and return
        }
        if (type > 2 || type < 0) { // if an invalid type was given
            Utils.log("Invalid debugging info metric type given: " + type + ". Ignoring.", this.getClass(),
                    "updateDebugMetrics", false); // log the occurrence
            return; // and return
        }
        int i = 1 + (type * 3); // calculate starting index in debug info array for info for the given metric type
        if ((i == 1 && val < debugInfo[i]) || (i > 1 && val > debugInfo[i])) debugInfo[i] = val; // record new max/min
        debugInfo[i + 1] += val; // update sum of metrics
        debugInfo[i + 2] += 1; // update count of values included in sum
        if (debugInfo[0] > GameEngine.TIME_BETWEEN_METRIC_REPORTS) { // if a new report is due
            debugInfo[0] = 0; // reset the accumulator
            String[] info = new String[3]; // create info array
            for (int j = 0; j < info.length; j++) { // for each metric
                // compile a string containing the average and worst measures with two decimal places
                info[j] = String.format("%.2f", debugInfo[(2 + (j * 3))] / debugInfo[(3 + (j * 3))])
                        + (j > 0 ? " ms " : " ") + "(worst: " + String.format("%.2f", debugInfo[1 + (j * 3)]) + ")";
                // reset the corresponding metric's values in the debug info array
                debugInfo[2 + (j * 3)] = debugInfo[3 + (j * 3)] = 0;
            }
            // update fields in the global debug info enhanced text object
            Global.debugInfo.setField("fps", info[0]);
            Global.debugInfo.setField("update", info[1]);
            Global.debugInfo.setField("render", info[2]);
        }
    }

    /**
     * Phase 1 of loop: responding to input (this is called asynchronously though)
     * Occurs when keyboard events occur in the GLFW window (pressing, releasing, repeating). It will call the
     * logic's keyboard input method and allow it to handle the input accordingly, as well as checking if the debug
     * toggle button was pressed and reacting accordingly
     *
     * @param key    the key
     * @param action the action of the key (GLFW_PRESS, GLFW_RELEASE, GLFW_REPEAT)
     */
    private void keyboardInput(int key, int action) {
        if (key == Global.DEBUG_TOGGLE_KEY && action == GLFW_RELEASE) { // if debug info toggling key,
            this.debugging = !this.debugging; // toggle static flag
            Global.debugInfo.setVisibility(this.debugging); // update vis
            if (!this.debugging && debug != null) this.updateDebugMetrics(0, this.debug, -1); // reset
        } else if (key == Global.POLYGON_MODE_TOGGLE_KEY && action == GLFW_RELEASE) // if polygon toggle key pressed
            Global.togglePolygonMode(); // toggle the polygon mode
        logic.keyboardInput(key, action); // notify logic of input
    }

    /**
     * Phase 1 of loop: responding to input (this is called asynchronously though)
     * Occurs when mouse events occur in the GLFW window (movement, pressing, and releasing). It will call the logic's
     * mouse input method and allow it to handle the input accordingly. If a hover event, it will convert the mouse
     * position to projected normalized coordinates
     *
     * @param x      the x of the mouse in window coordinates, or the horizontal scroll factor if scrolling input
     * @param y      the y of the mouse in window coordinates, or the vertical scroll factor if scrolling input
     * @param action the action of the mouse (GLFW_HOVERED, GLFW_PRESS, GLFW_RELEASE, or SCROLL)
     */
    private void mouseInput(float x, float y, int action) {
        if (action == GLFW_HOVERED) { // if hover event
            Pair<Float> pos = new Pair<>(x, y); // bundle into coordinate object
            // normalize mouse position
            Transformation.normalize(pos, Global.gameWindow.getWidth(), Global.gameWindow.getHeight());
            Transformation.deaspect(pos, Global.ar); // project position
            x = pos.x;
            y = pos.y; // extract x and y
        }
        logic.mouseInput(x, y, action); // notify logic of input
    }

    /**
     * Phase 2 of loop: updating the game
     *
     * @param interval the amount of time to account for
     */
    private void update(float interval) {
        if (GameLogic.logicChange != null) { // if a logic change is pending
            this.logicTransitionTime += interval; // keep track of time for transition
            if (this.logicTransitionTime >= GameLogic.logicChange.getTransitionTime()) { // if the transition is over
                performLogicChange(); // perform the logic change
                this.logicTransitionTime = 0f; // reset logic transition timer
            }
        }
        this.logic.update(interval); // allow the logic to update
    }

    /**
     * Performs a logic change by using GameLogic's static logic change data. This will also clean up the old logic
     * and initialize the new logic
     */
    private void performLogicChange() {
        this.logic.cleanup(); // cleanup old logic
        this.logic = GameLogic.logicChange.getNewLogic(); // grab new logic
        this.logic.giveTransferData(GameLogic.logicChange.getTransferData()); // give new logic the transfer data
        this.logic.init(); // initialize new logic
        GameLogic.logicChange = null; // delete logic change data
        Utils.log("Logic change performed successfully", this.getClass(), "performLogicChange",
                false); // log successful logic change
    }

    /**
     * Phase 3 of loop: rendering the game
     * Clears the screen, checks for resizes, uses logic to render, and then swaps the buffers
     */
    private void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the screen
        if (Global.gameWindow.resized(true)) { // if the window was resized
            // change GL viewport to fit window's frame buffer
            glViewport(0, 0, Global.gameWindow.getFBWidth(), Global.gameWindow.getFBHeight());
            Global.updateAr(); // update global aspect ratio variable
            this.logic.resized(); // notify the logic of the resize
        }
        this.logic.render(); // allow the logic to render
        Global.gameWindow.swapBuffers(); // refresh the window
    }

    /**
     * Phase 4 of loop: syncing
     * Syncs the game loop by sleeping for any leftover time in between updates
     *
     * @param interval how much time there should be between frames
     */
    private void sync(float interval) {
        double loopEnd = this.timer.getTimestamp() + interval; // calculate time when the current loop should end
        while (Timer.getTimeSeconds() < loopEnd) { // while there is leftover time for this loop
            try {
                Thread.sleep(1);
            } // sleep
            catch (Exception e) {
                Utils.handleException(e, this.getClass(), "sync", true);
            } // handle exceptions
        }
    }

    /**
     * Cleans up the engine after the loop ends
     */
    private void cleanup() {
        Settings.save(); // save settings
        this.logic.cleanup(); // tell logic to cleanup
        Global.cleanup(); // cleanup global members
        SoundManager.cleanup(); // cleanup the sound manager
    }
}
