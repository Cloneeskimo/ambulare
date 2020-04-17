package logic;

import gameobject.GameObject;
import graphics.*;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;

/**
 * Lays out and abstracts away many lower-level details and capabilities that a game logic should have. Notably:
 *  - Has a ShaderProgram member (sp) used for rendering. sp is created and initialized in initSP(), so for a custom
 *    ShaderProgram, an extending class should override that method.
 */
public abstract class GameLogic {

    /**
     * Data
     */
    protected List<GameObject> gameObjects; // game objects
    protected ShaderProgram sp; // shader program to use for rendering
    protected Camera cam; // camera to use for view positioning
    protected float ar; // aspect ratio of window - this is used by the default ShaderProgram
    protected boolean arAction; // aspect ratio action (for projection) - this is used by the default ShaderProgram

    /**
     * Initializes this GameLogic. This method is the only entry point into the GameLogic and it is not able to be
     * overridden by extending classes. However, this method will call initSP() and initItems() - two methods which
     * ARE able to be overridden by extending classes.
     * @param window the window
     */
    public final void init(Window window) {
        this.ar = (float)window.getWidth() / (float)window.getHeight(); // calculate aspect ratio
        this.arAction = (this.ar < 1.0f); // if ar < 1.0f (height > width) then we will make objects shorter to compensate
        this.gameObjects = new ArrayList<>(); // initialize GameObject list
        this.cam = new Camera(); // create camera
        this.initSP(window); // initialize shader program
        this.initItems(window); // initialize game objects
        glfwSetScrollCallback(window.getHandle(), (w, x, y) -> { // when the user scrolls
            this.cam.zoom(y > 0 ? 1.15f : 0.85f); // zoom on camera
        });
    }

    /**
     * Extending classes should initialize any GameObjects or other important members here.
     */
    protected void initItems(Window window) {}

    /**
     * Initializes the world ShaderProgram
     * Extending classes can override this, but if no ShaderProgram is assigned to sp, the program will likely crash
     * Extending classes should only initialize things related to the ShaderProgram here. For other initializations,
     * initItems() is recommended.
     * @param window the Window
     */
    protected void initSP(Window window) {
        this.sp = new ShaderProgram("/shaders/worldV.glsl", "/shaders/worldF.glsl"); // create ShaderProgram
        this.sp.registerUniform("x"); // register world x uniform
        this.sp.registerUniform("y"); // register world y uniform
        this.sp.registerUniform("ar"); // register aspect ratio uniform
        this.sp.registerUniform("arAction"); // register aspect ratio action uniform
        this.sp.registerUniform("isTextured"); // register texture flag uniform
        this.sp.registerUniform("color"); // register color uniform
        this.sp.registerUniform("blend"); // register blend uniform
        this.sp.registerUniform("texSampler"); // register texture sampler uniform
        this.sp.registerUniform("camX"); // register camera world x uniform
        this.sp.registerUniform("camY"); // register camera world y uniform
        this.sp.registerUniform("camZoom"); // register camera zoom uniform
    }

    /**
     * Extending classes should override this to use the window reference to respond to any input they so desire to
     * respond to
     * @param window the window
     */
    public void input(Window window) {}

    /**
     * Updates this GameLogic by updating each of its GameObjects
     * Extending classes can certainly override this but super.update() should definitely be called
     */
    public void update() {
        for (GameObject o : this.gameObjects) o.update(); // update GameObjects
        this.cam.update(); // update camera
    }

    /**
     * Wraps the rendering process by binding and unbinding the ShaderProgram before and after rendering, respectively
     * Extending classes cannot override this method, but they can override render() below
     */
    public final void wrapRender() {
        this.sp.bind(); // bind shader program
        this.render(); // render
        this.sp.unbind(); // unbind shader program
    }

    /**
     * Sets appropriate ShaderProgram uniforms and renders this GameLogic's game objects
     * Extending classes can certainly override this method, but super.render() should be called unless the extending
     * class wishes to directly modify the rendering process. If the extending class has an additional/separate
     * ShaderProgram, sp should be unbound first (this.sp.unbind() should be called) and then the other ShaderProgram
     * should be bound and its uniforms appropriately set before rendering using it
     */
    protected void render() {
        this.sp.setUniform("texSampler", 0); // set texture sampler uniform to use texture unit 0
        this.sp.setUniform("ar", this.ar); // set aspect ratio uniform
        this.sp.setUniform("arAction", this.arAction ? 1 : 0); // set aspect ratio action uniform
        this.sp.setUniform("camX", this.cam.getX()); // set camera x uniform
        this.sp.setUniform("camY", this.cam.getY()); // set camera y uniform
        this.sp.setUniform("camZoom", this.cam.getZoom()); // set camera zoom uniform
        for (GameObject o : this.gameObjects) o.render(this.sp); // render game objects
    }

    /**
     * Reacts to the window resizing by updating the aspect ratio member of GameLogic
     * Extending classes cannot override this method
     * @param w the new window width
     * @param h the new window height
     */
    public final void resized(int w, int h) {
        this.ar = (float)w / (float)h; // calculate aspect ratio
        this.arAction = (this.ar < 1.0f); // if ar < 1.0f (height > width) then we will make objects shorter to compensate
    }

    /**
     * Clean up components of this GameLogic that need cleaned up
     * Extending classes should override this to cleanup any additional members they need to do, but they should
     * always call super.cleanup() to clean up the base GameLogic members
     */
    public void cleanup() {
        if (this.sp != null) this.sp.cleanup(); // cleanup shader programs
        for (GameObject o : this.gameObjects) o.cleanup();
    }
}
