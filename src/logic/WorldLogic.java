package logic;

import gameobject.GameObject;
import graphics.Model;
import graphics.ShaderProgram;
import graphics.Window;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Lays out logic for engine to follow while in the game world
 */
public class WorldLogic implements GameLogic {

    /**
     * Data
     */
    private ShaderProgram sp; // shader program to use for world
    private GameObject player; // player
    private float ar; // aspect ratio of window
    private boolean arAction; // aspect ratio action (for projection)

    /**
     * Initializes this WorldLogic
     * @param window the window
     */
    @Override
    public void init(Window window) {
        this.initSP(window); // initialize shader program
        Model m = new Model( // create player model
            new float[] { // rectangle positions
                -0.5f,  0.5f,
                -0.5f, -0.5f,
                 0.5f, -0.5f,
                 0.5f,  0.5f
            },
            new float[] { // rectangle colors
                    1.0f, 0.0f, 0.0f, 1.0f,
                    0.0f, 1.0f, 0.0f, 1.0f,
                    0.0f, 0.0f, 1.0f, 1.0f,
                    1.0f, 1.0f, 1.0f, 1.0f

            },
            new int[] { 0, 1, 3, 3, 1, 2 } // rectangle indices
        );
        this.player = new GameObject(0f, 0f, m); // create player
    }

    /**
     * Initializes the world ShaderProgram
     * @param window the Window
     */
    private void initSP(Window window) {
        this.sp = new ShaderProgram("/shaders/worldV.glsl", "/shaders/worldF.glsl"); // create ShaderProgram
        this.sp.registerUniform("x"); // register x offset uniform
        this.sp.registerUniform("y"); // register y offset uniform
        this.sp.registerUniform("ar"); // register aspect ratio uniform
        this.sp.registerUniform("arAction"); // register aspect ratio action uniform
        this.ar = (float)window.getWidth() / (float)window.getHeight(); // calculate aspect ratio
        this.arAction = (this.ar < 1.0f); // if ar < 1.0f (height > width) then we will make objects shorter to compensate
    }

    /**
     * Gathers window input
     * @param window the window
     */
    @Override
    public void input(Window window) {
        this.player.setVX(0f); // reset horizontal velocity to 0
        this.player.setVY(0f); // reset vertical velocity to 0
        if (window.isKeyPressed(GLFW_KEY_W)) this.player.incrementVY(0.05f); // w -> up
        if (window.isKeyPressed(GLFW_KEY_S)) this.player.incrementVY(-0.05f); // s -> down
        if (window.isKeyPressed(GLFW_KEY_D)) this.player.incrementVX(0.05f); // d -> right
        if (window.isKeyPressed(GLFW_KEY_A)) this.player.incrementVX(-0.05f); // a -> left
    }

    /**
     * Updates this WorldLogic
     * @param interval the amount of time in seconds since the last update call
     */
    @Override
    public void update(float interval) {
        this.player.update(); // update player
    }

    /**
     * Renders all game objects
     */
    @Override
    public void render() {
        this.sp.bind(); // bind shader program
        this.sp.setUniform("ar", this.ar); // set aspect ratio uniform
        this.sp.setUniform("arAction", this.arAction ? 1 : 0); // set aspect ratio action uniform
        this.player.render(this.sp); // render player
        this.sp.unbind(); // unbind shader program
    }

    /**
     * Reacts to the window resizing
     * @param w the new window width
     * @param h the new window height
     */
    @Override
    public void resized(int w, int h) {
        this.ar = (float)w / (float)h; // calculate aspect ratio
        this.arAction = (this.ar < 1.0f); // if ar < 1.0f (height > width) then we will make objects shorter to compensate
    }

    /**
     * Clean up components of this WorldLogic that need cleaned up
     */
    @Override
    public void cleanup() {
        if (this.sp != null) this.sp.cleanup(); // cleanup shader programs
        this.player.cleanup(); // cleanup mesh
    }
}
