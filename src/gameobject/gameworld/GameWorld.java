package gameobject.gameworld;

import graphics.Camera;
import graphics.ShaderProgram;
import utils.Global;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;

/**
 * Game worlds hold and render world objects
 */
public class GameWorld {

    /**
     * Members
     */
    private List<WorldObject> objects; // the world objects in the game world
    private ShaderProgram sp;          // the shader program used to render the game world
    private Camera cam;                // the camera used to see the game world

    /**
     * Constructor
     * @param windowHandle the handle of the GLFW window
     */
    public GameWorld(long windowHandle) {
        this.objects = new ArrayList<>();
        this.cam = new Camera();
        // register GLFW window scroll callback for camera zoom
        glfwSetScrollCallback(windowHandle, (w, x, y) -> { // when the user scrolls
            this.cam.zoom(y > 0 ? 1.15f : 0.85f); // zoom on camera
        });
        this.initSP(); // initialize shader program
    }

    /**
     * Initializes the game world's shader program
     */
    private void initSP() {
        // create the shader program and then register the uniforms
        this.sp = new ShaderProgram("/shaders/vertex.glsl", "/shaders/fragment.glsl");
        sp.registerUniform("ar"); // register aspect ratio uniform
        sp.registerUniform("arAction"); // register aspect ratio action uniform
        sp.registerUniform("x"); // register object x uniform
        sp.registerUniform("y"); // register object y uniform
        sp.registerUniform("isTextured"); // register texture flag uniform
        sp.registerUniform("color"); // register material color uniform
        sp.registerUniform("blend"); // register material blend uniform
        sp.registerUniform("texSampler"); // register texture sampler uniform
        sp.registerUniform("camX"); // register camera x uniform
        sp.registerUniform("camY"); // register camera y uniform
        sp.registerUniform("camZoom"); // register camera zoom uniform
    }

    /**
     * Updates the world objects and camera in the game world
     * @param interval the amount of time (in seconds) to account for
     */
    public void update(float interval) {
        for (WorldObject po : this.objects) po.update(interval); // update the world objects
        this.cam.update(); // update camera
    }

    /**
     * Renders all the world objects
     */
    public void render() {
        this.sp.bind(); // bind shader program
        this.sp.setUniform("texSampler", 0); // set texture sampler uniform to use texture unit 0
        this.sp.setUniform("ar", Global.ar); // set aspect ratio uniform
        this.sp.setUniform("arAction", Global.arAction ? 1 : 0); // set aspect ratio action uniform
        this.sp.setUniform("camX", this.cam.getX()); // set camera x uniform
        this.sp.setUniform("camY", this.cam.getY()); // set camera y uniform
        this.sp.setUniform("camZoom", this.cam.getZoom()); // set camera zoom uniform
        for (WorldObject o : this.objects) o.render(this.sp); // render world objects
        this.sp.unbind(); // unbind shader program
    }

    /**
     * Adds a world object to the game world
     * @param wo the object to add
     */
    public void addObject(WorldObject wo) {
        wo.setCollidables(this.objects); // give it the game world's collidables to use
        this.objects.add(wo); // add it to the list
    }

    /**
     * @return the game world's camera
     */
    public Camera getCam() { return this.cam; }

    /**
     * Cleans up the game world
     */
    public void cleanup() {
        if (this.sp != null) this.sp.cleanup(); // cleanup shader program
        for (WorldObject o : this.objects) o.cleanup(); // cleanup world objects
    }
}
