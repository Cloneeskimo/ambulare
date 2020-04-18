package gameobject;

import graphics.Camera;
import graphics.ShaderProgram;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;

/**
 * Encapsulates a collection of GameObjects that react to a Camera when rendered
 * Any items added to this World are considered to be in world space when received and will maintained as such
 */
public class World {

    /**
     * Data
     */
    protected List<GameObject> gameObjects; // game objects
    protected Camera cam; // camera to use for view positioning
    protected ShaderProgram sp; // shader program to use for rendering
    private float ar; // the Window's aspect ratio
    private boolean arAction; // aspect ratio action (for projection)

    /**
     * Constructs this World
     * @param windowHandle the Window's handle
     * @param ar the Window's aspect ratio
     * @param arAction aspect ratio action (for projection)
     */
    public World(long windowHandle, float ar, boolean arAction) {
        this.ar = ar; // save aspect ratio for rendering
        this.arAction = arAction; // save aspect ratio action for rendering
        this.gameObjects = new ArrayList<>(); // create GameObject list
        this.cam = new Camera(); // create Camera
        this.initSP(); // initialize ShaderProgram
        glfwSetScrollCallback(windowHandle, (w, x, y) -> { // when the user scrolls
            this.cam.zoom(y > 0 ? 1.15f : 0.85f); // zoom on camera
        });
    }

    /**
     * Initializes this World's
     */
    protected void initSP() {
        this.sp = new ShaderProgram("/shaders/worldV.glsl", "/shaders/worldF.glsl"); // create ShaderProgram
        this.sp.registerUniform("x"); // register world x uniform
        this.sp.registerUniform("y"); // register world y uniform
        this.sp.registerUniform("scaleX"); // register x scale uniform
        this.sp.registerUniform("scaleY"); // register y scale uniform
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
     * Updates this World
     * @param interval the amount of time to account for
     */
    public void update(float interval) {
        for (GameObject o : this.gameObjects) o.update(interval); // update GameObjects
        this.cam.update(); // update camera
    }

    /**
     * Renders this World
     */
    public void render() {
        this.sp.bind(); // bind shader program
        this.sp.setUniform("texSampler", 0); // set texture sampler uniform to use texture unit 0
        this.sp.setUniform("ar", this.ar); // set aspect ratio uniform
        this.sp.setUniform("arAction", this.arAction ? 1 : 0); // set aspect ratio action uniform
        this.sp.setUniform("camX", this.cam.getX()); // set camera x uniform
        this.sp.setUniform("camY", this.cam.getY()); // set camera y uniform
        this.sp.setUniform("camZoom", this.cam.getZoom()); // set camera zoom uniform
        for (GameObject o : this.gameObjects) o.render(this.sp); // render game objects
        this.sp.unbind(); // unbind shader program
    }

    /**
     * Handles a resize of the Window
     * @param ar the new aspect ratio
     * @param arAction the new aspect ratio action
     */
    public void resized(float ar, boolean arAction) {
        this.ar = ar; // save new aspect ratio
        this.arAction = arAction; // save new aspect ratio action
    }

    /**
     * Adds the given GameObject to the world
     * @param o the GameObject to add
     */
    public void addObject(GameObject o) { this.gameObjects.add(o); }

    /**
     * Finds and returns the GameObject at the given index
     * @param i the index to find the GameObject at
     * @return the GameObject
     */
    public GameObject getObject(int i) { return this.gameObjects.get(i); }

    /**
     * @return this World's Camera
     */
    public Camera getCam() { return this.cam; }

    /**
     * Cleans up this World
     */
    public void cleanup() {
        if (this.sp != null) this.sp.cleanup(); // cleanup ShaderProgram
        for (GameObject o : this.gameObjects) o.cleanup(); // cleanup GameObjects
    }
}
