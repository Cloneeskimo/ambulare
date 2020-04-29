package gameobject.gameworld;

import graphics.Camera;
import graphics.ShaderProgram;
import utils.Global;
import utils.PhysicsEngine;
import utils.Utils;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;
import static org.lwjgl.opengl.GL11.glClearColor;

/**
 * Game worlds hold, update, and render world objects and blocks. For block collision to work, the game world's
 * createBlockMap method must be called.
 */
public class GameWorld {

    /**
     * Static Data
     */
    private static final int MAX_LIGHTS = 5; // maximum amount of lights in the game world

    /**
     * Members
     */
    private List<WorldObject> objects; // the world objects in the game world
    private DayNightCycle dnc;         // the world's day/night cycle
    private ShaderProgram sp;          // the shader program used to render the game world
    private Camera cam;                // the camera used to see the game world
    private Area area;                 // the area currently in use in the game world

    /**
     * Constructor
     *
     * @param windowHandle the handle of the GLFW window
     */
    public GameWorld(long windowHandle, Area startingArea) {
        this.objects = new ArrayList<>();
        this.dnc = new DayNightCycle(0f, 20f, new float[]{
                0.53f, 0.81f, 0.92f, 0.0f}, new float[]{0.1f, 0.1f, 0.1f, 0.0f}); // initialize D/N cycle settings
        this.initSP(); // initialize shader program
        this.cam = new Camera();
        this.area = startingArea;
        PhysicsEngine.giveBlockMap(this.area.getBlockMap()); // give the area's block map to the physics engine
        // register GLFW window scroll callback for camera zoom
        glfwSetScrollCallback(windowHandle, (w, x, y) -> { // when the user scrolls
            this.cam.zoom(y > 0 ? 1.15f : 0.85f); // zoom on camera
        });
    }

    /**
     * Initializes the game world's shader program
     */
    private void initSP() {
        // create the shader program and then register the uniforms
        this.sp = new ShaderProgram("/shaders/world_vertex.glsl", "/shaders/world_fragment.glsl");
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
        sp.registerUniform("sunPresence"); // register sun presence uniform
        for (int i = 0; i < MAX_LIGHTS; i++) sp.registerLightUniform("lights[" + i + "]"); // register lights
    }

    /**
     * Updates the world objects and camera in the game world
     *
     * @param interval the amount of time (in seconds) to account for
     */
    public void update(float interval) {
        this.dnc.update(interval); // update the day/night cycle
        this.area.update(interval); // update the area
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
        this.sp.setUniform("sunPresence", this.dnc.getSunPresence()); // set sun presence uniform
        this.area.render(this.sp); // render the area
        for (WorldObject o : this.objects) o.render(this.sp); // render world objects
        this.sp.unbind(); // unbind shader program
    }

    /**
     * Adds a world object to the game world
     *
     * @param wo the object to add
     */
    public void addObject(WorldObject wo) {
        wo.setCollidables(this.objects); // give it the game world's collidables to use
        this.objects.add(wo); // add it to the list
    }

    /**
     * Grabs the world object at the given index
     *
     * @param i the index to look for
     * @return the object at index i
     */
    public WorldObject getWorldObject(int i) {
        if (i < 0 || i >= this.objects.size())
            Utils.handleException(new Exception("Unable to get world object at index: " + i + "; out of bounds"),
                    "gameobject.gameworld.GameWorld", "getObject(i)", true);
        return this.objects.get(i);
    }

    /**
     * @return the game world's camera
     */
    public Camera getCam() {
        return this.cam;
    }

    /**
     * @return the game world's current are
     */
    public Area getArea() {
        return this.area;
    }

    /**
     * Cleans up the game world
     */
    public void cleanup() {
        if (this.sp != null) this.sp.cleanup(); // cleanup shader program
        this.area.cleanup(); // cleanup area
        for (WorldObject o : this.objects) o.cleanup(); // cleanup world objects
    }

    /**
     * Represents and performs a day/night cycle in the game world
     */
    private static class DayNightCycle {

        /**
         * Calculates how present a sun is given its angle (see sunPresence member)
         *
         * @param sunAngle the angle of the sun in degrees [0, 360)
         * @return how present the sun is from [0.0, 1.0]
         */
        private static float calcSunPresence(float sunAngle) {
            if (sunAngle > 35 && sunAngle < 145) return 1f; // if day time, sun presence is 1.0f
            if (sunAngle > 215 && sunAngle < 325) return 0f; // if night time, sun presence is 0.0f
            // if sunrise, sun presence is some value between 0.0f and 1.0f depending on the sunrise progress
            if (sunAngle <= 35) return 0.5f + (sunAngle / 70);
            if (sunAngle >= 325) return (sunAngle - 325) / 70;
            // if sunset, sun presence is some value between 0.0f and 1.0f depending on the sunset progress
            return 1f - (sunAngle - 145) / 70;
        }

        /**
         * Members
         */
        private float sunAngle;      // the current angle of the sun in degrees [0-360) in the day/night cycle
        private float sunSpeed;      // how fast the sun advances in the day/night cycle
        private float sunPresence;   /* a measurement from [0-1] measuring how present the sun is. 1.0 means that it is
                                        day time and 0.0 means that it is night time. In between values represent sunset
                                        and sunrise*/
        private float[] dayColor;    // the color the background is set to during day
        private float[] nightColor;  // the color the background is set to during night

        /**
         * Constructor
         *
         * @param startingSunAngle     the angle the sun should start at in degrees [0-360)
         * @param sunSpeed             the speed at which the sun sshould progress
         * @param dayBackgroundColor   the color to set the background to during day
         * @param nightBackgroundColor the color to set the background to during night
         */
        public DayNightCycle(float startingSunAngle, float sunSpeed, float[] dayBackgroundColor,
                             float[] nightBackgroundColor) {
            this.sunAngle = startingSunAngle;
            this.sunSpeed = sunSpeed;
            this.nightColor = nightBackgroundColor;
            this.dayColor = dayBackgroundColor;
        }

        /**
         * Updates the day/night cycle
         *
         * @param interval the amount of time to account for
         */
        private void update(float interval) {
            this.sunAngle += sunSpeed * interval; // update the sun's angle
            if (this.sunAngle >= 360f) this.sunAngle = 0f; // reset angle if a full rotation has occurred
            float newSunPresence = calcSunPresence(this.sunAngle); // calculate the new presence based on the new angle
            if (newSunPresence != this.sunPresence) { // if the presence has changed
                this.sunPresence = newSunPresence; // save the new sun presence
                // update background color
                glClearColor(this.dayColor[0] * sunPresence + this.nightColor[0] * (1 - sunPresence),
                        this.dayColor[1] * sunPresence + this.nightColor[1] * (1 - sunPresence),
                        this.dayColor[2] * sunPresence + this.nightColor[2] * (1 - sunPresence),
                        this.dayColor[3] * sunPresence + this.nightColor[3] * (1 - sunPresence));
            }
        }

        /**
         * @return the presence of the sun based on the day/night cycle
         */
        public float getSunPresence() {
            return this.sunPresence;
        }
    }
}
