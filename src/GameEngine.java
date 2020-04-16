import graphics.Window;
import logic.GameLogic;
import utils.Timer;
import utils.Utils;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Controls and brings together the entire program
 */
public class GameEngine {

    /**
     * Data
     */
    private static final int MAX_FPS = 60; // target frames per second (if v-sync is off)
    private static boolean recordingFPS; // whether to record frames/loops per second
    private final Window window; // the GLFW window
    private final Timer timer; // timer for smooth looping
    private GameLogic logic; // the logic the engine is currently following

    /**
     * Constructs this GameEngine
     * @param logic the starting logic this GameEngine should follow
     */
    public GameEngine(GameLogic logic) {
        recordingFPS = false; // disable FPS recording by default (can be enabled by pushing R)
        this.window = new Window("Game", true); // create graphics.Window
        this.timer = new Timer(); // create utils.Timer
        this.logic = logic; // set logic reference
    }

    /**
     * This starts the engine by initializing it and then beginning the game loop
     */
    public void start() {
        this.init(); // initialize engine
        this.loop(); // begin loop
        this.cleanup(); // cleanup after loop ends
    }

    /**
     * Initializes this GameEngine
     */
    private void init() {
        this.window.init(); // initialize GLFW window
        this.timer.init(); // initialize timer
        this.logic.init(this.window); // initialize logic
        this.window.registerKeyControl(new Window.KeyControl() { // register key to toggle FPS logging
            public int action() { return GLFW_RELEASE; } // upon release
            public int key() { return GLFW_KEY_F; } // for F key
            public void reaction() { GameEngine.recordingFPS = !GameEngine.recordingFPS; } // toggle fps recording
        });
    }

    /**
     * Represents the game loop
     * This game uses a fixed-step game loop. It consists of four phases: gathering input, updating, rendering, and syncing
     */
    private void loop() {

        // timekeeping variables
        float elapsedTime; // how much time has passed since last loop
        float accumulator = 0f; // how much time is unaccounted for
        float interval = 1f / MAX_FPS; // how much time there should be between loops
        float[] fpsInfo = new float[] { 0.0f, 0.0f, 0.0f }; // FPS info to be used for FPS recording (explained in FPSRecord())

        // loop
        while (!this.window.shouldClose()) { // while the user hasn't exited

            // timekeeping
            elapsedTime = this.timer.getElapsedTime(); // get elapsed time since last loop
            if (recordingFPS) FPSRecord(elapsedTime, fpsInfo); // record FPS if enabled
            accumulator += elapsedTime; // add elapsed time to an accumulator which keeps track of how much time has been unaccounted for

            // four phases of loop
            this.input(); // gather input
            while(accumulator >= interval) { // while the amount of unaccounted for time is greater than how much one loop should be
                this.update(interval); // do an update
                accumulator -= interval; // account for a single loop
            }
            this.render(); // render - we can render outside of the above while loop because we don't need to render outdated frames
            if (!this.window.usesVSync()) this.sync(interval); // sync the loop - V-Sync should do this for us (if it's enabled)
        }
    }

    /**
     * Records FPS if the setting is enabled
     * @param elapsedTime the amount of time since the last loop
     * @param fpsInfo info about the FPS recording
     *  fpsInfo[0] - accumulator - how much time since last FPS recording
     *  fpsInfo[1] - current sum of FPS since last record
     *  fpsInfo[2] - amount of FPS values that have been added to the sum [1]
     *  this data is useful so that the program can then calculate and report average FPS every second
     */
    private void FPSRecord(float elapsedTime, float[] fpsInfo) {
        fpsInfo[0] += elapsedTime; // keep time between recordings
        fpsInfo[1] += (1 / elapsedTime); // add to FPS sum
        fpsInfo[2] += 1; // keep track of how many FPS values are in sum
        if (fpsInfo[0] > 1f) { // if it has been a second since the last FPS recording
            Utils.log("Average FPS of last second: " + (fpsInfo[1] / fpsInfo[2]), "GameEngine", "FPSRecord()", false); // record the FPS
            fpsInfo[0] = fpsInfo[1] = fpsInfo[2] = 0.0f; // reset FPS info array
        }
    }

    /**
     * Gathers input from user
     */
    private void input() {
        this.window.pollEvents(); // poll for window events such as key press, resizes, etc.
        this.logic.input(this.window); // allow logic to check input
    }

    /**
     * Updates everything that needs updated
     * @param interval the amount of time in seconds to account for
     */
    private void update(float interval) {
        this.logic.update(interval); // allow logic to update
    }

    /**
     * Renders everything that needs rendered
     */
    private void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the screen
        if (this.window.resized(true)) glViewport(0, 0, this.window.getWidth(), this.window.getHeight());
        this.logic.render(); // allow the logic to render
        this.window.swapBuffers(); // refresh the window
    }

    /**
     * Syncs the game loop by sleeping for any leftover time in between updates
     * @param interval how much time there should be between frames
     */
    private void sync(float interval) {
        double loopEnd = this.timer.getLastLoop() + interval; // calculate time when the current loop should end
        while (this.timer.getTime() < loopEnd) { // while there is leftover time for this loop
            try { Thread.sleep(1); } // sleep
            catch (Exception e) { Utils.handleException(e, "GameEngine", "sync(float)", true); } // handle exceptions when trying to sleep
        }
    }

    /**
     * Cleans up components of the game that need cleaned up
     */
    private void cleanup() { this.logic.cleanup(); }
}
