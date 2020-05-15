package gameobject.gameworld;

import graphics.Camera;
import graphics.ShaderProgram;
import utils.Global;
import utils.Pair;
import utils.PhysicsEngine;
import utils.Utils;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;
import static org.lwjgl.opengl.GL11.glClearColor;

/*
 * GameWorld.java
 * Ambulare
 * Jacob Oaks
 * 4/22/2020
 */

/**
 * Game worlds hold, update, and render world objects and an area. Game worlds have a day/night cycle and a camera to
 * use for rendering
 */
public class GameWorld {

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
     * @param startingArea the the starting of the game world
     */
    public GameWorld(long windowHandle, Area startingArea) {
        this.objects = new ArrayList<>(); // create empty objects list
        this.initSP(); // initialize shader program
        this.cam = new Camera(); // create the camera
        this.area = startingArea; // save the starting area as a member
        this.area.useCam(this.cam); // give area a reference to the camera
        float ssr = area.getStartingSunRotation(); // get the starting sun rotation from the area
        this.dnc = new DayNightCycle(ssr < 0 ? 0f : ssr, area.getSunSpeed()); // initialize day/night cycle
        PhysicsEngine.giveBlockMap(this.area.getBlockMap()); // give the area's block map to the physics engine
        int[][] slopeMap = new int[this.area.getBlockMap().length][this.area.getBlockMap()[0].length];
        slopeMap[45][3] = 1;
        slopeMap[46][4] = 1;
        slopeMap[49][2] = 1;
        slopeMap[49][4] = 1;
        slopeMap[34][3] = 2;
        slopeMap[33][4] = 2;
        slopeMap[31][2] = 2;
        slopeMap[31][4] = 2;
        PhysicsEngine.giveSlopeMap(slopeMap); // give the area's slope map to the physics engine
        // register GLFW window scroll callback for camera zoom
        glfwSetScrollCallback(windowHandle, (w, x, y) -> { // when the user scrolls
            this.cam.aestheticZoom(y > 0 ? 1.1f : (1f / 1.1f)); // zoom on camera
        });
    }

    /**
     * Initializes the game world's shader program
     */
    private void initSP() {
        // create the shader program with the appropriate source files
        this.sp = new ShaderProgram(new Utils.Path("/shaders/world_vertex.glsl", true),
                new Utils.Path("/shaders/world_fragment.glsl", true));
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
        sp.registerUniform("useDNC"); // register day/night cycle usage uniform
        sp.registerUniform("useLights"); // register light usage uniform
        sp.registerLightArrayUniform(); // register light array uniform
    }

    /**
     * Updates the day/night cycle, world objects, camera, and area in use in the game world
     *
     * @param interval the amount of time (in seconds) to account for
     */
    public void update(float interval) {
        this.dnc.update(interval); // update the day/night cycle
        for (WorldObject po : this.objects) po.update(interval); // update the world objects
        this.cam.update(interval); // update camera
        this.area.update(interval); // update the area
    }

    /**
     * Sets up world rendering and then gives the world objects to the area to render at the appropriate time
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
        this.area.render(this.sp, this.objects, this.cam.getView()); // render the area with the current world objects
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
        if (i < 0 || i >= this.objects.size()) // if the given index is invalid
            Utils.handleException(new Exception("Unable to get world object at index: " + i + "; out of bounds"),
                    this.getClass(), "getWorldObject", true); // crash
        return this.objects.get(i); // otherwise return the corresponding world object
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
     * Represents and keeps track of a day/night cycle in the game world
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
        private final float sunSpeed; // how fast the sun advances in the day/night cycle
        private float sunAngle;       // the current angle of the sun in degrees [0-360) in the day/night cycle
        private float sunPresence;    /* a measurement from [0-1] measuring how present the sun is. 1.0 means that it is
                                         day time and 0.0 means that it is night time. In between values represent
                                         sunset and sunrise */

        /**
         * Constructor
         *
         * @param startingSunAngle the angle the sun should start at in degrees [0-360)
         * @param sunSpeed         the speed at which the sun should progress
         */
        public DayNightCycle(float startingSunAngle, float sunSpeed) {
            this.sunAngle = startingSunAngle; // set sun angle to the given starting angle
            this.sunSpeed = sunSpeed; // save sun speed as member
        }

        /**
         * Updates the day/night cycle
         *
         * @param interval the amount of time to account for
         */
        private void update(float interval) {
            this.sunAngle += sunSpeed * interval; // update the sun's angle
            if (this.sunAngle >= 360f) this.sunAngle = 0f; // reset angle to 0 degrees if a full rotation has occurred
            float newSunPresence = calcSunPresence(this.sunAngle); // calculate the sun presence based on the new angle
            if (newSunPresence != this.sunPresence) this.sunPresence = newSunPresence; // save sun presence if new
        }

        /**
         * @return the presence of the sun based on the day/night cycle
         */
        public float getSunPresence() {
            return this.sunPresence;
        }
    }
}
