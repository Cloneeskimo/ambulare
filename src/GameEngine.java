import static org.lwjgl.opengl.GL11.*;

/**
 * Controls and brings together the entire program
 */
public class GameEngine {

    /**
     * Data
     */
    private static final int MAX_FPS = 60; // target frames per second
    private final Window window; // the GLFW window
    private final Timer timer; // timer for smooth looping

    /**
     * Constructs this GameEngine
     */
    public GameEngine() {
        this.window = new Window("Game", false); // create Window
        this.timer = new Timer(); // create Timer
    }

    /**
     * This starts the engine by initializing it and then beginning the game loop
     */
    public void start() {
        this.init(); // initialize engine
        this.loop(); // begin loop
    }

    /**
     * Initializes this GameEngine
     */
    private void init() {
        this.window.init(); // initialize GLFW window
        this.timer.init(); // initialize Timer
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

        while (!this.window.shouldClose()) { // while the user hasn't exited

            // timekeeping
            elapsedTime = this.timer.getElapsedTime(); // get elapsed time since last loop
            System.out.println("FPS: " + (1 / elapsedTime));
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
     * Gathers input from user
     */
    private void input() {

    }

    /**
     * Updates everything that needs updated
     * @param interval the amount of time in seconds to account for
     */
    private void update(float interval) {
        this.window.update(); // update the window
    }

    /**
     * Renders everything that needs rendered
     */
    private void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the screen
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
}
