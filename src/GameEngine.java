import static org.lwjgl.opengl.GL11.*;

/**
 * Controls and brings together the entire program
 */
public class GameEngine {

    /**
     * Data
     */
    private final Window window; // the GLFW window

    /**
     * Constructs this GameEngine
     */
    public GameEngine() {
        this.window = new Window("Game", true); // create window
    }

    /**
     * Initializes this GameEngine
     */
    public void init() {
        this.window.init(); // initialize glfw window
    }

    /**
     * Begins the game loop
     */
    public void run() {
        while (!this.window.shouldClose()) { // while the user hasn't exited

            // render and update every loop
            this.render();
            this.update();
        }
    }

    /**
     * Updates everything that needs updated
     */
    private void update() {
        this.window.update();
    }

    /**
     * Renders everything that needs rendered
     */
    private void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }
}
