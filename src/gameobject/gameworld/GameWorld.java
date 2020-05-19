package gameobject.gameworld;

import gameobject.ROC;
import graphics.AnimatedTexture;
import graphics.Camera;
import graphics.ShaderProgram;
import story.Story;
import utils.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

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
     * Static Data
     */
    public static final float AREA_CHANGE_TRANSITION = 3f; // time between area change transitions
    private static final int ENTER_GATE_KEY = GLFW_KEY_E;    // key to enter a gate

    /**
     * Members
     */
    private List<WorldObject> objects;          // the world objects in the game world
    private ROC roc;                            // reference to the ROC for fading
    private MouseInputEngine mip;               // a reference to the ROC's mouse input engine
    private Global.Callback areaChangeCallback; // invoked when the area changes
    private Story story;                        // a reference to the current story
    private Entity player;                      // the player
    private DayNightCycle dnc;                  // the world's day/night cycle
    private ShaderProgram sp;                   // the shader program used to render the game world
    private Camera cam;                         // the camera used to see the game world
    private Area area;                          // the area currently in use in the game world
    private Area.Gate enteredGate;              // when switching areas, this stores the gate entered
    private float timer;                        // timer kept for area changes

    /**
     * Constructor
     *
     * @param mip the mouse input engine to use for mouse input
     * @param player the player to place into the game world
     * @param startingArea the the starting of the game world
     */
    public GameWorld(MouseInputEngine mip, Entity player, Area startingArea, ROC roc) {
        this.objects = new ArrayList<>(); // create empty objects list
        this.initSP(); // initialize shader program
        this.area = startingArea; // save the starting area as a member
        this.area.useCam(this.cam = new Camera()); // give area a reference to the camera
        this.area.useMouseInputEngine(this.mip = mip);
        float ssr = area.getStartingSunRotation(); // get the starting sun rotation from the area
        this.dnc = new DayNightCycle(ssr < 0 ? 0f : ssr, area.getSunSpeed()); // initialize day/night cycle
        PhysicsEngine.giveBlockMap(this.area.getBlockMap()); // give the area's block map to the physics engine
        PhysicsEngine.giveSlopeMap(this.area.getSlopeMap()); // give the area's slope map to the physics engine
        // register GLFW window scroll callback for camera zoom
        glfwSetScrollCallback(Global.gameWindow.getHandle(), (w, x, y) -> { // when the user scrolls
            this.cam.aestheticZoom(y > 0 ? 1.1f : (1f / 1.1f)); // zoom on camera
        });
        if ((this.player = player) != null) { // if the player isn't null
            this.objects.add(player); // add it to world objects
            this.cam.follow(this.player); // and tell the camera to follow it
        }
        this.roc = roc; // save reference to ROC
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
        sp.registerUniform("flicker"); // register flicker uniform
        sp.registerLightArrayUniform(); // register light array uniform
    }

    /**
     * Tells the game world which story it is a part of. If this isn't called, various story-related functions will not
     * work properly
     * @param story the story that the game world is a part of
     */
    public void useStory(Story story) {
        this.story = story; // save story as member
        this.area.useStoryPath(story.getFolderPath()); // tell the area the path to the story's folder
    }

    /**
     * Switches the current area of focus in the game world
     * @param path the path to the new area's node-file
     * @param startingPos the position to place the player at in the new area, if player is not null
     */
    public void switchAreas(Utils.Path path, Pair<Integer> startingPos) {
        this.area.cleanup(); // cleanup old area
        this.area = new Area(Node.pathContentsToNode(path)); // create new area with given path
        this.area.useCam(this.cam); // give camera to the area
        this.area.useMouseInputEngine(this.mip); // give mouse input engine to area
        float ssr = area.getStartingSunRotation(); // get the starting sun rotation from the area
        this.dnc = new DayNightCycle(ssr < 0 ? this.dnc.sunAngle : ssr, area.getSunSpeed()); // update day/night cycle
        PhysicsEngine.giveBlockMap(this.area.getBlockMap()); // give the area's block map to the physics engine
        PhysicsEngine.giveSlopeMap(this.area.getSlopeMap()); // give the area's slope map to the physics engine
        if (this.player != null) // if the game world has a player
            this.player.setPos((float)startingPos.x + 0.5f, (float)startingPos.y + 0.5f); // move to new pos
        if (this.story != null) this.area.useStoryPath(this.story.getFolderPath()); // give new area the story path
        this.enteredGate = null; // reset entered gate reference
        this.timer = 0f; // reset time
        this.sp.cleanup(); // cleanup SP to remove old area's lights
        this.initSP(); // re-initialize
    }

    /**
     * Responds to keyboard input by checking if the enter gate key was released. If so, asks the area to check if the
     * player is nearby a gate, and changes areas if so
     * @param key the keyboard key in question
     * @param action the action performed on the keyboard key in question
     */
    public void keyboardInput(int key, int action) {
        if (key == ENTER_GATE_KEY && action == GLFW_RELEASE) { // if gate entering key pressed
            this.enteredGate = this.area.enterGate(player.getX(), player.getY()); // ask area to try to enter a gate
            this.roc.fadeOut(new float[]{0f, 0f, 0f, 0f}, AREA_CHANGE_TRANSITION); // fade out the ROC
        }
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
        if (this.enteredGate != null) { // if in the middle of an area change
            this.timer += interval; // keep track of time
            if (this.timer >= AREA_CHANGE_TRANSITION) { // if enough time has passed
                this.switchAreas(this.enteredGate.getPath(), this.enteredGate.getStartingPos()); // switch areas
                Global.resetAccumulator = true; // reset accumulator
                this.roc.fadeIn(new float[]{0f, 0f, 0f, 1f}, AREA_CHANGE_TRANSITION); // fade in the ROC
                if (this.areaChangeCallback != null) this.areaChangeCallback.invoke(); // invoke area change callback
            }
        }
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
        this.area.render(this.sp, this.objects, this.cam.getView(), this.cam); // render the area with the current world objects
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
     * Specifies a callback to invoke whenever the area of focus in the game world changes
     * @param cb the callback
     */
    public void useAreaChangeCallback(Global.Callback cb) {
        this.areaChangeCallback = cb; // save callback as member
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
