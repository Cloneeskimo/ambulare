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

/**
 * Game worlds hold, update, and render world objects and blocks. For block collision to work, the game world's
 * createBlockMap method must be called.
 */
public class GameWorld {

    /**
     * Members
     */
    private List<WorldObject> objects; // the world objects in the game world
    private List<Block> blocks;        // the blocks to render
    private Block[][] blockMap;        // block map for collision detection with blocks
    private ShaderProgram sp;          // the shader program used to render the game world
    private Camera cam;                // the camera used to see the game world

    /**
     * Constructor
     * @param windowHandle the handle of the GLFW window
     */
    public GameWorld(long windowHandle) {
        this.objects = new ArrayList<>();
        this.blocks = new ArrayList<>();
        this.cam = new Camera();
        // register GLFW window scroll callback for camera zoom
        glfwSetScrollCallback(windowHandle, (w, x, y) -> { // when the user scrolls
            this.cam.zoom(y > 0 ? 1.15f : 0.85f); // zoom on camera
        });
        this.initSP(); // initialize shader program
    }

    /**
     * Creates a block map of the game world for the physics engine to use for detecting collisions with blocks. Note
     * that, in order for block maps to work, all blocks must have 0 <= x < (width of block map) and 0 <= y <
     * (height of block map). If this is ever violated, the game will crash
     * @param w the width of the block map in grid cells
     * @param h the height of the block map in grid cells
     */
    public void createBlockMap(int w, int h) {
        this.blockMap = new Block[w][h]; // create the block map with the specified size
        for (Block b : this.blocks) { // if any blocks already exist
            // get their grid positions
            int x = b.getX();
            int y = b.getY();
            // if they are out of bounds, throw exception
            if (x < 0 || x >= w)
                Utils.handleException(new Exception("Block map given to game world where blocks exist outside" +
                        " of map"), "gameobject.gameworld.GameWorld", "giveBlockMap(int, int)", true);
            if (y < 0 || y >= h)
                Utils.handleException(new Exception("Block map given to game world where blocks exist outside" +
                        " of map"), "gameobject.gameworld.GameWorld", "giveBlockMap(int, int)", true);
            this.blockMap[x][y] = b; // put in map
        }
        PhysicsEngine.giveBlockMap(this.blockMap); // give the block map to the physics engine to use
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
        for (Block b : this.blocks) b.update(interval); // update the blocks
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
        for (Block b : this.blocks) Block.renderBlock(this.sp, b); // render blocks
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
     * Adds a block to the game world. If the game world has a block map, it will be put in the map as well. If the
     * game world has a block map and the given block is out of the bounds of the block map, the game will crash
     * @param b the block to add
     */
    public void addBlock(Block b) {
        this.blocks.add(b); // add to list of blocks
        if (this.blockMap != null) { // if the game world has a block map
            if (b.getX() < 0 || b.getX() >= this.blockMap.length ||
                    b.getY() < 0 || b.getY() >= this.blockMap[0].length) { // check if out of bounds
                Utils.handleException(new Exception("Invalid placement for new block: " + new Pair(b.getX(), b.getY()) +
                    "is out of the block map's bounds"), "gameobject.gameworld.GameWorld", "addBlock(Block, int, int)",
                        true); // throw exception if out of bounds
            }
            this.blockMap[b.getX()][b.getY()] = b; // place in block map
        }
    }

    /**
     * @return the game world's camera
     */
    public Camera getCam() { return this.cam; }

    /**
     * Grabs the world object at the given index
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
     * Grabs the block at the given grid position
     * @param x the grid x
     * @param y the grid y
     * @return the block at that position (or null if nothing is there)
     */
    public Block getBlock(int x, int y) {
        return this.blockMap[x][y];
    }

    /**
     * Cleans up the game world
     */
    public void cleanup() {
        if (this.sp != null) this.sp.cleanup(); // cleanup shader program
        for (Block b : this.blocks) b.cleanup(); // cleanup blocks
        for (WorldObject o : this.objects) o.cleanup(); // cleanup world objects
    }
}
