import graphics.Window;
import logic.GameLogic;
import utils.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

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
     * Members
     */
    private static boolean debugging;            // whether or not the engine is reporting debugging info to the logic
    private double[] debug;                      // info about debugging. See loop() for more info
    private final Window window;                 // the window being used
    private final Timer timer;                   // timer used for accurate debugging and loop
    private GameLogic logic;                     // the logic the engine should follow
    private float logicTransitionTime;           // a timer for logic transitions

    /**
     * Constructor
     *
     * @param logic the starting logic to follow
     */
    public GameEngine(GameLogic logic) {
        int w = -1, h = -1; // if there is no saved window data, use -1 to denote default width and height
        Node wd = Node.fileToNode("wd.node", true); // try to load window data
        if (wd != null) { // if window data was found
            w = Integer.parseInt(wd.getChild("width").getValue()); // use saved width of window
            h = Integer.parseInt(wd.getChild("height").getValue()); // ues saved height of window
        }
        this.window = new Window(Global.WINDOW_TITLE, w, h, Global.V_SYNC); // create window with determined size
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
        this.window.init(); // initialize the window
        Global.updateAr(this.window); // update global aspect ratio variable
        this.initInput(); // initialize mouse and keyboard input callbacks
        Global.init(); // initialize global members
        this.logic.init(this.window); // initialize starting logic
        this.timer.init(); // initialize the timer
    }

    /**
     * Creates GLFW window callbacks for mouse input and keyboard input
     */
    private void initInput() {
        // funnel both mouse movement and buttons events into a single method called mouseInput()
        glfwSetCursorPosCallback(window.getHandle(), (w, x, y) -> { // create GLFW callback for cursor position
            // pass along cursor position to mouseInput() and use GLFW_HOVERED as the action
            this.mouseInput((float) x, (float) y, GLFW_HOVERED);
        });
        glfwSetMouseButtonCallback(window.getHandle(), (w, b, a, i) -> {
            // mouse button events don't have mouse positions, so just send zero and hope mouseInput checks the action
            this.mouseInput(0f, 0f, a);
        });
        glfwSetKeyCallback(window.getHandle(), (w, k, s, a, m) -> {
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
        while (!this.window.shouldClose()) { // while the Window shouldn't close

            // timekeeping
            elapsedTime = this.timer.getElapsedTime(true); // get elapsed time since last loop
            if (GameEngine.debugging) { // if debugging is enabled
                debug[0] += elapsedTime; // update the accumulator
                updateDebugMetrics(1 / elapsedTime, debug, 0); // update FPS metrics
            }
            accumulator += elapsedTime; // add elapsed time to the accumulator for time unaccounted for

            // four phases of loop
            this.window.pollEvents(); // gather input by polling for GLFW window events
            // if debugging, record time in milliseconds before update
            if (GameEngine.debugging) debug[10] = (float) Timer.getTimeMilliseconds();
            while (accumulator >= interval) { // while there is a sufficient amount of unaccounted for time
                this.update(interval); /* doing multiple updates for the same smaller interval is preferable to doing
                    one large update with a huge interval because such large updates can be game-breaking, especially
                    when it comes to things like collision */
                accumulator -= interval; // account for the time for the update
            }
            if (GameEngine.debugging) { // if debugging, calculate update time and report it to logic
                debug[11] = (float) Timer.getTimeMilliseconds(); // get time after update/before render
                // update debugging metrics with update time
                this.updateDebugMetrics(debug[11] - debug[10], debug, 1);
            }
            this.render();  /* render outside of the above loop because, as opposed to updating, outdated renders are
                useless wastes of GPU power */
            if (GameEngine.debugging) // if debugging, update debugging metrics with render time
                this.updateDebugMetrics(Timer.getTimeMilliseconds() - debug[11], debug, 2);
            // attempt to manually sync loop unless the window has vertical sync enabled
            if (!this.window.usesVSync()) this.sync(1 / (float) Global.TARGET_FPS);
        }
    }

    /**
     * Updates debugging metrics by keeping track of an average and a worst for each value. If the globally set amount
     * of time between reports has passed, information is reported to the logic. This information takes the form of an
     * array of strings where each string contains the average and worst values for three debugging metrics in the
     * following order: FPS, update time, and render time. The worst values are maintained over a period of calls to
     * this method where type is not -1. That is, to reset the worst values, call this method with type set to -1
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
            Utils.log("Invalid debugging info metric type given: " + type + ". Ignoring.", "GameEngine.java",
                    "updateDebugMetrics(double, double[], int)", false); // log the occurrence
            return; // and reeturn
        }
        int i = 1 + (type * 3); // calculate starting index in debug info array for info for the given metric type
        if ((i == 1 && val < debugInfo[i]) || (i > 1 && val > debugInfo[i])) debugInfo[i] = val; // record new max/min
        debugInfo[i + 1] += val; // update sum of metrics
        debugInfo[i + 2] += 1; // update count of values included in sum
        if (debugInfo[0] > Global.TIME_BETWEEN_METRIC_REPORTS) { // if a new report is due
            debugInfo[0] = 0; // reset the accumulator
            String[] info = new String[3]; // create info array
            for (int j = 0; j < info.length; j++) { // for each metric
                // compile a string containing the average and worst measures with two decimal places
                info[j] = String.format("%.2f", debugInfo[(2 + (j * 3))] / debugInfo[(3 + (j * 3))])
                        + (j > 0 ? " ms " : " ") + "(worst: " + String.format("%.2f", debugInfo[1 + (j * 3)]) + ")";
                // reset the corresponding metric's values in the debug info array
                debugInfo[2 + (j * 3)] = debugInfo[3 + (j * 3)] = 0;
            }
            this.logic.reportDebugInfo(info); // report the info to the logic
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
            GameEngine.debugging = !GameEngine.debugging; // toggle static flag
            if (!GameEngine.debugging) { // if debugging was turned off,
                logic.reportDebugInfo(null); // tell logic that debug reporting has ended for now
                if (debug != null) this.updateDebugMetrics(0, this.debug, -1); // reset worst metrics
            }
        } else if (key == Global.POLYGON_MODE_TOGGLE_KEY && action == GLFW_RELEASE) // if polygon toggle key pressed
            Global.togglePolygonMode(); // toggle the polygon mode
        logic.keyboardInput(key, action); // notify logic of input
    }

    /**
     * Phase 1 of loop: responding to input (this is called asynchronously though)
     * Occurs when mouse events occur in the GLFW window (movement, pressing, and releasing). It will call the logic's
     * mouse input method and allow it to handle the input accordingly. If a hovering event, it will convert the mouse
     * position to projected normalized coordinates
     *
     * @param x      the x of the mouse in window coordinates
     * @param y      the y of the mouse in window coordinates
     * @param action the action of the mouse (GLFW_HOVER, GLFW_PRESS, or GLFW_RELEASE)
     */
    private void mouseInput(float x, float y, int action) {
        if (action == GLFW_HOVERED) { // if hover,
            Pair<Float> pos = new Pair<>(x, y); // bundle into coordinate object
            Transformation.normalize(pos, window.getWidth(), window.getHeight()); // normalize mouse position
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
        this.logic.init(this.window); // initialize new logic
        GameLogic.logicChange = null; // delete logic change data
    }

    /**
     * Phase 3 of loop: rendering the game
     * Clears the screen, checks for resizes, uses logic to render, and then swaps the buffers
     */
    private void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the screen
        if (this.window.resized(true)) { // if the window was resized
            glViewport(0, 0, this.window.getFBWidth(), this.window.getFBHeight()); // change GL viewport to
            Global.updateAr(this.window); // update global aspect ratio variable
            this.logic.resized(); // notify the logic of the resize
        }
        this.logic.render(); // allow the logic to render
        this.window.swapBuffers(); // refresh the window
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
                Utils.handleException(e, "GameEngine", "sync(float)", true);
            } // handle exceptions
        }
    }

    /**
     * Cleans up the engine after the loop ends
     */
    private void cleanup() {
        Node wd = new Node("window data"); // create a node to hold window data
        wd.addChild("width", Integer.toString(window.getWidth())); // add window width
        wd.addChild("height", Integer.toString(window.getHeight())); // add window height
        Node.nodeToFile(wd, "/wd.node", true); // and save node
        this.logic.cleanup(); // tell logic to cleanup
        SoundManager.cleanup(); // cleanup the sound manager
    }
}
