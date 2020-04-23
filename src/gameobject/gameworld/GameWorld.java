package gameobject.gameworld;

import gameobject.GameObject;
import gameobject.ROC;
import graphics.Camera;
import graphics.ShaderProgram;
import utils.Global;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;

/**
 * Game worlds hold physics objects and check for collisions efficiently by only checking collisions for any given
 * physics object with other physics objects in the same chunk, one chunk to the, and one chunk to the right
 */
public class GameWorld {

    private List<PhysicsObject> objects;
    private ShaderProgram sp;
    private Camera cam;

    public GameWorld(long windowHandle) {
        this.objects = new ArrayList<>();
        this.cam = new Camera();
        glfwSetScrollCallback(windowHandle, (w, x, y) -> { // when the user scrolls
            this.cam.zoom(y > 0 ? 1.15f : 0.85f); // zoom on camera
        });
        this.initSP();
    }
    private void initSP() {
        this.sp = new ShaderProgram("/shaders/vertex.glsl", "/shaders/fragment.glsl");
        sp.registerUniform("ar"); // register aspect ratio uniform
        sp.registerUniform("arAction"); // register aspect ratio action uniform
        sp.registerUniform("x"); // register object x uniform
        sp.registerUniform("y"); // register object y uniform
        sp.registerUniform("isTextured"); // register texture flag uniform
        sp.registerUniform("color"); // register color uniform
        sp.registerUniform("blend"); // register blend uniform
        sp.registerUniform("texSampler"); // register texture sampler uniform
        sp.registerUniform("camX"); // register camera x uniform
        sp.registerUniform("camY"); // register camera y uniform
        sp.registerUniform("camZoom"); // register camera zoom uniform
    }

    public void update(float interval) {
        for (PhysicsObject po : this.objects) po.update(interval);
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
        for (PhysicsObject o : this.objects) o.render(this.sp); // render world objects
        this.sp.unbind(); // unbind shader program
    }

    public void addObject(PhysicsObject o) {
        o.setCollidables(this.objects);
        this.objects.add(o);
    }

    public Camera getCam() { return this.cam; }

    /**
     * Cleans up the HUD
     */
    public void cleanup() {
        if (this.sp != null) this.sp.cleanup(); // cleanup static object shader program
        for (PhysicsObject o : this.objects) o.cleanup(); // cleanup world objects
    }

}
