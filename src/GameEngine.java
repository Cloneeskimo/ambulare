import graphics.Window;
import logic.GameLogic;
import utils.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Abstracts away many of the lower level game functions and operates the program based on whichever GameLogic
 * is currently active
 */
public class GameEngine {

    /**
     * Data
     */
    private static boolean reportingFPS = false; // flag representing whether or not FPS is being reported
    private final Window window;                 // the window we are using
    private final Timer timer;                   // object used to keep track of time for looping purposes
    private GameLogic logic;                     // the logic that the engine follows

    /**
     * Constructor
     * @param logic the starting logic to follow
     */
    public GameEngine(GameLogic logic) {
        this.window = new Window(Global.WINDOW_TITLE, Global.V_SYNC); // window with correct name and v-sync setting
        this.timer = new Timer(); // create timer
        this.logic = logic; // save logic reference
        FPSReportControl FPSRC = new FPSReportControl(); /* this is an implementation of the Window.KeyControl interface
                                                            which basically lays out a keyboard control and how to
                                                            handle it. This is instantiated because it needs to be able
                                                            to have a reference to the logic in order to tell it when
                                                            the setting is toggled */
        FPSRC.logic = this.logic; // tell the FPSRC which logic to notify when the setting is toggled
        this.window.registerKeyControl(FPSRC); // register the new control with the window
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
        this.window.init(); // initialize the window
        this.initMouseInput(); // initialize mouse input callbacks
        this.timer.init(); // initialize the timer
        Global.init(); // initialize global members
        this.logic.init(this.window); // initialize starting logic
    }

    /**
     * Creates GLFW callbacks for mouse movement or button events
     */
    private void initMouseInput() {
        glfwSetCursorPosCallback(window.getHandle(), (w, x, y) -> { // create mouse position callback
            this.mouseInput((float)x, (float)y, GLFW_HOVERED); // forward input to mouse input method
        });
        glfwSetMouseButtonCallback(window.getHandle(), (w, b, a, i) -> { // create mouse button callback
            this.mouseInput(0f, 0f, a); // forward input to mouse input method
        });
    }

    /**
     * Represents and lays out the game loop
     * This game uses a fixed-step game loop. It consists of four phases: gathering input, updating, rendering, and
     * syncing
     */
    private void loop() {

        // timekeeping variables
        float elapsedTime; // how much time has passed since last loop
        float accumulator = 0f; // how much time is unaccounted for
        float interval = 1f / Global.TARGET_UPS; // how much time there should be between loops
        float[] fpsInfo = new float[] { 0.0f, 0.0f, 0.0f }; // FPS info to be used for FPS recording (see FPSRecord())

        // game loop
        while (!this.window.shouldClose()) { // while the Window shouldn't close

            // timekeeping
            elapsedTime = this.timer.getElapsedTime(); // get elapsed time since last loop
            if (GameEngine.reportingFPS) reportFPS(elapsedTime, fpsInfo); // record FPS if enabled
            accumulator += elapsedTime; /* add elapsed time to an accumulator which keeps track of how much time has
                                           unaccounted for */

            // four phases of loop
            this.input(); // gather input
            while(accumulator >= interval) { // while there is a sufficient amount of unaccounted for time
                this.update(interval); // do an game update
                accumulator -= interval; // account for a single loop interval amount of time
            }
            this.render(); // render - we render outside of the above loop to avoid rendering outdated frames
            if (!this.window.usesVSync()) this.sync(1 / (float)Global.TARGET_FPS); /* sync loop - V-Sync will do
                                                                                             this for us if it's
                                                                                             enabled */
        }
    }

    /**
     * Reports FPS if the setting is enabled
     * @param elapsedTime the amount of time since the last loop
     * @param fpsInfo info about the FPS report
     *  fpsInfo[0] - accumulator - how much time since last FPS report
     *  fpsInfo[1] - current sum of FPSes since last report
     *  fpsInfo[2] - amount of FPS values that have been added to the sum at index 1
     *                this data is useful so that the program can then calculate and report average FPS every second
     *                instead of tirelessly and constantly calculating instantaneous FPS values
     */
    private void reportFPS(float elapsedTime, float[] fpsInfo) {
        fpsInfo[0] += elapsedTime; // keep time between reports
        fpsInfo[1] += (1 / elapsedTime); // add to sum
        fpsInfo[2] += 1; // keep track of how many FPS values are in sum
        if (fpsInfo[0] > Global.TIME_BETWEEN_FPS_REPORTS) { // if sufficient amount of time since last report
            this.logic.reportFPS(fpsInfo[1] / fpsInfo[2]); // calculate and tell logic the FPS
            Utils.log("Average FPS of last second: " + (fpsInfo[1] / fpsInfo[2]), "GameEngine",
                    "FPSRecord()", false); // log fps
            fpsInfo[0] = fpsInfo[1] = fpsInfo[2] = 0.0f; // reset FPS info array
        }
    }

    /**
     * Phase 1 of loop: gathering input
     */
    private void input() {
        this.window.pollEvents(); // poll for GLFW events such as key press, resizes, etc.
        this.logic.input(this.window); // allow the logic to check input
    }

    /**
     * This method gets called for mouse events (movement, pressing, and releasing). It will call the logic's mouse
     * input method and allow it to handle the input accordingly. If a hovering event, it will convert the mouse
     * position to projected normalized coordinates
     * @param x the normalized and projected x position of the mouse if hover event, 0 otherwise
     * @param y the normalized and projected y position of the mouse if hover event, 0 otherwise
     * @param action the action of the mouse (GLFW_HOVER, GLFW_PRESS, or GLFW_RELEASE)
     */
    private void mouseInput(float x, float y, int action) {
        if (action == GLFW_HOVERED) { // if hover,
            Pair pos = new Pair(x, y); // bundle into coordinate object
            Transformation.normalize(pos, window.getWidth(), window.getHeight()); // normalize mouse position
            Transformation.deaspect(pos, (float) window.getFBWidth() / (float) window.getFBHeight()); // project position
            x = pos.x; y = pos.y; // extract x and y
        }
        logic.mouseInput(x, y, action); // notify logic of input
    }

    /**
     * Phase 2 of loop: updating the game
     * @param interval the amount of time to account for
     */
    private void update(float interval) { this.logic.update(interval); } // allow the logic to update

    /**
     * Phase 3 of loop: rendering the game
     */
    private void render() {
        //System.out.println(new Pair(this.window.getWidth(), this.window.getHeight()));
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the screen
        if (this.window.resized(true)) { // if the window was resized
            glViewport(0, 0, this.window.getFBWidth(), this.window.getFBHeight()); // change the GL viewport to match
            this.logic.resized(this.window.getFBWidth(), this.window.getFBHeight()); // notify the logic of the resize
        }
        this.logic.render(); // allow the logic to render
        this.window.swapBuffers(); // refresh the window
    }

    /**
     * Phase 4 of loop: syncing
     * Syncs the game loop by sleeping for any leftover time in between updates
     * This doesn't occur if vertical sync is enabled because vertical sync will take care of it
     * @param interval how much time there should be between frames
     */
    private void sync(float interval) {
        double loopEnd = this.timer.getLastLoop() + interval; // calculate time when the current loop should end
        while (this.timer.getTime() < loopEnd) { // while there is leftover time for this loop
            try { Thread.sleep(1); } // sleep
            catch (Exception e) { Utils.handleException(e, "GameEngine", "sync(float)", true); } // handle exceptions
        }
    }

    /**
     * Cleans up the engine after the loop ends
     */
    private void cleanup() { this.logic.cleanup(); }

    /**
     * A KeyControl that allows notification of the engine's logic when the fps changes and when the setting is toggled
     * See GameEngine's constructor for more info
     */
    private class FPSReportControl implements Window.KeyControl {
        GameLogic logic; // the logic to notify about FPS
        @Override
        public int key() { return Global.FPS_REPORTING_TOGGLE_KEY; } // toggle when the key matches setting
        @Override
        public int action() { return GLFW_RELEASE; } // toggle when key released
        @Override
        public void reaction() {
            GameEngine.reportingFPS = !GameEngine.reportingFPS; // toggle static flag
            if (!GameEngine.reportingFPS) logic.reportFPS(null); // if toggled off, tell logic reporting has stopped
        }
    }
}
