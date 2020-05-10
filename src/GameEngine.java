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
    private static boolean reportingFPS = false; // whether or not FPS is being reported
    private final Window window;                 // the window being used
    private final Timer timer;                   // timer used for timekeeping and accurate FPS reporting and looping
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
        this.timer = new Timer();
        this.logic = logic;
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
        float[] fpsInfo = new float[]{0.0f, 0.0f, 0.0f}; // FPS info to be used for FPS recording

        // game loop
        while (!this.window.shouldClose()) { // while the Window shouldn't close

            // timekeeping
            elapsedTime = this.timer.getElapsedTime(true); // get elapsed time since last loop
            if (GameEngine.reportingFPS) reportFPS(elapsedTime, fpsInfo); // record FPS if enabled
            accumulator += elapsedTime; // add elapsed time to the accumulator for time unaccounted for

            // four phases of loop
            this.window.pollEvents(); // gather input by polling for GLFW window events
            while (accumulator >= interval) { // while there is a sufficient amount of unaccounted for time
                this.update(interval); /* doing multiple updates for the same smaller interval is preferable to doing
                    one large update with a huge interval because such large updates can be game-breaking, especially
                    when it comes to things like collision. */
                accumulator -= interval; // account for the time for the update
            }
            this.render();  /* render outside of the above loop because, as opposed to updating, outdated renders are
                useless wastes of GPU power */
            // attempt to manually sync loop unless the window has vertical sync enabled
            if (!this.window.usesVSync()) this.sync(1 / (float) Global.TARGET_FPS);
        }
    }

    /**
     * Records and reports average frames-per-second measurements
     *
     * @param elapsedTime the amount of time since the last game loop
     * @param fpsInfo     info about the FPS report, laid out as:
     *                    fpsInfo[0] - accumulator - how much time since last FPS report
     *                    fpsInfo[1] - current sum of FPSes since last report
     *                    fpsInfo[2] - amount of FPS values that have been added to the sum at index 1
     *                    this info is useful to provide average FPS calculations over an entire second instead of
     *                    constant instantaneous ones
     */
    private void reportFPS(float elapsedTime, float[] fpsInfo) {
        fpsInfo[0] += elapsedTime; // keep time between reports
        fpsInfo[1] += (1 / elapsedTime); // add to sum of FPSs
        fpsInfo[2] += 1; // keep track of how many FPS values are in sum
        if (fpsInfo[0] > Global.TIME_BETWEEN_FPS_REPORTS) { // if sufficient amount of time since last report
            this.logic.reportFPS(fpsInfo[1] / fpsInfo[2]); // calculate and tell logic the FPS
            Utils.log("Average FPS of last second: " + (fpsInfo[1] / fpsInfo[2]), "GameEngine",
                    "FPSRecord()", false); // log fps
            fpsInfo[0] = fpsInfo[1] = fpsInfo[2] = 0.0f; // reset FPS info array
        }
    }

    /**
     * Phase 1 of loop: responding to input (this is called asynchronously though)
     * Occurs when keyboard events occur in the GLFW window (pressing, releasing, repeating). It will call the
     * logic's keyboard input method and allow it to handle the input accordingly, as well as checking if the FPS
     * toggle button was pressed and reacting accordingly
     *
     * @param key    the key
     * @param action the action of the key (GLFW_PRESS, GLFW_RELEASE, GLFW_REPEAT)
     */
    private void keyboardInput(int key, int action) {
        if (key == Global.FPS_REPORTING_TOGGLE_KEY && action == GLFW_RELEASE) { // if FPS report toggling key,
            GameEngine.reportingFPS = !GameEngine.reportingFPS; // toggle static flag
            if (!GameEngine.reportingFPS) logic.reportFPS(null); // if toggled off, tell logic reporting has stopped
        } else if (key == Global.POLYGON_MODE_TOGGLE_KEY && action == GLFW_RELEASE) { // if polygon toggle key pressed
            Global.togglePolygonMode(); // toggle the polygon mode
        }
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
        while (this.timer.getTime() < loopEnd) { // while there is leftover time for this loop
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
