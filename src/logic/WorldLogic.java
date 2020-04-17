package logic;

import gameobject.GameObject;
import graphics.*;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Lays out logic for engine to follow while in the game world
 */
public class WorldLogic implements GameLogic {

    /**
     * Data
     */
    private ShaderProgram sp; // shader program to use for world
    private GameObject player, dirt, bDirt; // player, dirt, and blue dirt
    private float ar; // aspect ratio of window
    private boolean arAction; // aspect ratio action (for projection)

    /**
     * Initializes this WorldLogic
     * @param window the window
     */
    @Override
    public void init(Window window) {
        this.initSP(window); // initialize shader program

        // create game object components
        Model m = Model.getStdSquare(); // get standard square model
        Material pMat = new Material(new float[] {1.0f, 0.0f, 0.0f, 1.0f}); // player should just be a red square
        Texture dirt = new Texture("/textures/dirt.png"); // create dirt texture
        Material dMat = new Material(dirt); // dirt should just be a dirt square
        Material bdMat = new Material(dirt, new float[] {0.0f, 0.0f, 1.0f, 1.0f}, Material.BLEND_MODE.NONE); // blue dirt should just be a blue dirt square

        // create game objects
        this.player = new GameObject(0f, 0f, m, pMat); // create player
        this.dirt = new GameObject(1.3f, 0f, m, dMat); // create dirt
        this.bDirt = new GameObject(-1.3f, 0f, m, bdMat); // create blue dirt
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
        this.sp.registerUniform("isTextured"); // register texture flag uniform
        this.sp.registerUniform("color"); // register color uniform
        this.sp.registerUniform("blend"); // register blend uniform
        this.sp.registerUniform("texSampler"); // register texture sampler uniform
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
        this.sp.setUniform("texSampler", 0); // set texture sampler uniform to use texture unit 0
        this.sp.setUniform("ar", this.ar); // set aspect ratio uniform
        this.sp.setUniform("arAction", this.arAction ? 1 : 0); // set aspect ratio action uniform
        this.player.render(this.sp); // render player
        this.dirt.render(this.sp); // render dirt
        this.bDirt.render(this.sp); // render blue dirt
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
