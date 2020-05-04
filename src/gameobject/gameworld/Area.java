package gameobject.gameworld;

import gameobject.GameObject;
import gameobject.TextObject;
import graphics.*;
import utils.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGetIntegeri_v;

/*
 * Area.java
 * Ambulare
 * Jacob Oaks
 * 4/25/2020
 */

/**
 * A class for holding, updating, and efficiently rendering objects that make up an area/level. The area's layout is
 * broken up into three layers: background, middleground, and foreground. Entities are always in the middle ground,
 * and the middle ground is the only place that collision occurs. The background is drawn behind the middleground and
 * the foreground is drawn in front of the middleground. These three layers can be made up of the following components:
 * <p>
 * - Blocks: blocks are always 1 grid cell in size, are collidable if in the middleground, and do not move. Blocks are
 * represented simply as grid cell positions indexed by the material to use when rendering a block in that grid cell.
 * This is efficient and minimalistic which helps to be able to have large quantities of blocks in an area. There is
 * also a block map which is just a two-dimensional array of booleans specifying which cells in the area are occupied by
 * a block in the middleground. This is to allow the physics engine to do constant-time, efficient collision detection
 * with all blocks in the middleground
 * <p>
 * - Decor: decor can be any size, can emit light, and has additional properties that allow it to be more customized.
 * Decor does not cause collisions. See DecorInfo for all of the properties decor can have. Decor is represented as a
 * simple list of game objects to update and render every game loop. Decor can either exist in the background or the
 * foreground. Any decor put in the middleground layout will be assumed to be background decor
 * <p>
 * The idea behind areas is to hold only a portion of the game's world and to be interconnected. The GameWorld class
 * only renders and updates one area at a time, the idea being that many areas can exist and be interconnected but only
 * the one that the player is in should be rendered and updated. Some game world mechanics that are independent of area
 * (such as the day/night cycle) are stored in the GameWorld class instead. Areas are loaded through the uses of a
 * node-file. See utils.Node for information on node files and see Area's constructor for how area node-files should be
 * laid out
 */
public class Area {

    /**
     * Static data
     */
    public static final BlockModel bm = new BlockModel(); /* all blocks use same 1x1 square model. See BlockModel for
        more information on how it extends a regular model */

    /**
     * Renders the set of blocks corresponding to the given set of block positions very efficiently using the given
     * shader program
     *
     * @param sp             the shader program to render with
     * @param blockPositions the list of block positions grouped together by material. The reason it is set up as a map
     *                       from material to pair lists is because by rendering all blocks of a certain material at
     *                       once, repetitive calls can be avoided
     */
    public static void renderBlocks(ShaderProgram sp, Map<Material, List<Pair<Integer>>> blockPositions) {
        for (Material m : blockPositions.keySet()) { // for each material
            Texture t = m.getTexture(); // get texture for material
            // if the texture is animated, tell the model which texture coordinates (frame) to use
            if (t instanceof AnimatedTexture) bm.useTexCoordVBO(((AnimatedTexture) t).getTexCoordVBO(), false);
                // if the texture isn't animated, just usse the entire texture
            else bm.useTexCoordVBO(AnimatedTexture.getTexCoordVBO(0, 1), false);
            m.setUniforms(sp); // set the appropriate material uniforms
            bm.renderBlocks(sp, blockPositions.get(m)); // render all the blocks with that material at once
        }
    }

    /**
     * Members
     */
    private final Map<Material, List<Pair<Integer>>>[] blocks; /* an array of lists of block positions grouped by
        material. Blocks are indexed by material in this way this way to facilitate efficient rendering of large
        quantities of blocks. See the renderBlocks() method for more info on efficient block rendering. There are three
        different maps in this array where blocks[0] represents the background blocks, blocks[1] represents the
        middleground blocks, and blocks[2] represents the foreground blocks */
    private final List<GameObject>[] decor;                   /* twos list of decor in the area where decor[0] is
                                                                 background decor and decor[1] is foreground decor */
    private final List<AnimatedTexture> ats;                  // a list of animated textures to update
    private final boolean[][] blockMap;                       // block map of middleground maps for collision detection
    private BackDrop backdrop;                                // the scrolling backdrop rendered behind the world
    private String name = "Unnamed";                          // the name of the area
    private boolean lightForeground = false;                  // whether to apply lights to objects is the foreground

    /**
     * Constructs the area by compiling the information from a given node. Most of the loading process is actually done
     * in the area layout loader to avoid cluttering the area class with a bunch of large static methods. An area node
     * can have the following children:
     * <p>
     * - block_key [required]: this key maps characters in the layout to tile info that describes the corresponding
     * block. Each child of the block_key child should have its name be a single character that appears in the layout
     * (if there are more than one character, only the first will be used) and the rest of the child should be formatted
     * as a tile info node. See AreaLayoutLoader.TileInfo's constructor for more information.
     * <p>
     * - middleground_layout [required]: the middleground layout should contain a child for each row, where the value of
     * the row is the set of characters describing that row for which corresponding tiles will be searched for in the
     * keys to build the middleground. Rows will be read and loaded top-down and left-to-right so that they appear in
     * the same order as they do in the node-file. If a character is encountered that is not in any key, it will be
     * ignored. The space character will also be ignored and interpreted as empty space. If decor is found in the
     * middleground layout, it will be placed in the background
     * <p>
     * - decor_key [optional][default: no decor]: this kep maps characters in the layout to decor info that describes
     * the corresponding decor. Each child of the decor_key child should have its name be a single character that
     * appears in the layout (if there are more than one character, only the first will be used) and the rest of the
     * child should be formatted as a decor info node. See AreaLayoutLoader.DecorInfo's constructor for more information
     * <p>
     * - background_layout [optional][default: no background]: the background layout should contain children formatted
     * in the same way as the middleground layout. Anything placed in the background will be rendered before the
     * middleground is rendered. Background objects do not cause collisions
     * <p>
     * - foreground_layout [optional][default: no foreground]: the foreground layout should contain children formatted
     * in the same way as the middleground layout. Anything placed in the foreground will be rendered after the
     * middleground is rendered. Foreground objects do not cause collisions
     * <p>
     * - name [optional][default: Unnamed]: the name of the area
     * <p>
     * - light_foreground [optional][default: false]: whether to apply lights to the objects in the foreground
     * <p>
     * Note that, if any of the info above is improperly formatted, a message saying as much will be logged. As
     * such, when designing areas to be loaded into the game, the logs should be checked often to make sure the
     * loading process is unfolding correctly
     *
     * @param node the node to create the area from
     */
    public Area(Node node) {

        // load three layers of layout
        Node background = node.getChild("background_layout"); // get background layout child
        if (background == null) Utils.log(Utils.getImproperFormatErrorLine("background_layout", "area",
                "no background provided: assuming no background", true),
                "gameobject.gameworld.Area", "Area(Node)", false); // if none, log and ignore
        Node middleground = node.getChild("middleground_layout"); // get middleground layout child
        if (middleground == null) Utils.handleException(new Exception(Utils.getImproperFormatErrorLine(
                "middleground_layout", "area", "a middleground layout is required",
                false)), "gameobject.gameworld.Area", "Area(Node)", true); // if middleground layouts, crash
        Node foreground = node.getChild("foreground_layout"); // get foreground layout child
        if (foreground == null) Utils.log(Utils.getImproperFormatErrorLine("foreground_layout", "area",
                "no foreground provided: assuming no foreground", true),
                "gameobject.gameworld.Area", "Area(Node)", false); // if none, log and ignore

        // load blocks of layouts
        Node blockKey = node.getChild("block_key"); // get block key child
        if (blockKey == null) Utils.handleException(new Exception(Utils.getImproperFormatErrorLine("block_key",
                "area", "a block key is required", false)), "gameobject.gameworld.Area",
                "Area(Node)", true); // if no block key child, crash
        // create three empty block lists: background, middleground, and foreground
        this.blocks = new Map[]{new HashMap<>(), new HashMap<>(), new HashMap<>()};
        this.ats = new ArrayList<>(); // create animated textures list
        // use the area layout loader to actually load the blocks
        this.blockMap = AreaLayoutLoader.loadLayoutBlocks(blockKey, background, middleground, foreground, blocks, ats);

        // load decor
        Node decorKey = node.getChild("decor_key"); // get decor key child
        // create two empty decor lists: background and foreground
        this.decor = new List[]{new ArrayList<>(), new ArrayList<>()};
        if (decorKey != null) AreaLayoutLoader.loadLayoutDecor(decorKey, background, middleground, foreground,
                this.decor, this.ats, this.blockMap); // if there is a key for decor, load decor

        // load other area properties
        for (Node c : node.getChildren()) { // go through each child and parse the values
            String n = c.getName(); // get name of child
            if (n.equals("name")) { // if the child is for the area's name
                this.name = c.getValue(); // save name
            } else if (n.equals("light_foreground")) { // light foreground
                this.lightForeground = Boolean.parseBoolean(c.getValue()); // save value
            } else { // if the child is unrecognized
                if (!n.equals("layout") && !(n.equals("block_key")) && !(n.equals("decor_key")) &&
                        !(n.equals("background_layout")) && !(n.equals("middleground_layout")) && !(n.equals(
                        "foreground_layout")))
                    Utils.log("Unrecognized child given for area info:\n" + c + "Ignoring.",
                            "gameobject.gameworld.Area", "Area(Node)", false); // log and ignore
            }
        }

        // initialize backdrop
        this.backdrop = new BackDrop("/textures/backdrop.jpg", true, this.blockMap.length,
                this.blockMap[0].length);
    }

    /**
     * Updates the area's animated textures and decor
     *
     * @param interval the amount of time to account for
     */
    public void update(float interval) {
        this.backdrop.update(interval); // update backdrop
        for (AnimatedTexture at : this.ats) at.update(interval); // update animated textures
        for (GameObject o : this.decor[0]) o.update(interval); // update background decor
        for (GameObject o : this.decor[1]) o.update(interval); // update foreground decor
    }

    /**
     * Renders the area's blocks and decor
     *
     * @param sp the shader program to use for rendering
     * @param os the lists of world objects to render in the area
     */
    public void render(ShaderProgram sp, Camera cam, List<WorldObject> os) {
        sp.setUniform("camZoom", 1f); // set to 1 to render backdrop
        this.backdrop.setPos(cam.getX(), cam.getY()); // set the backdrop position to the camera's position
        this.backdrop.render(sp); // render the backdrop
        sp.setUniform("camZoom", cam.getZoom()); // use the correct zoom then
        sp.setUniform("useLights", 1); // use lights for background/middleground objects
        renderBlocks(sp, this.blocks[0]); // render the background blocks
        for (GameObject o : this.decor[0]) o.render(sp); // render the background decor
        renderBlocks(sp, this.blocks[1]); // render the middleground blocks
        for (WorldObject wo : os) wo.render(sp); // render world objects (middleground) from the game world
        // disable light usage for foreground objects if the setting is set to false
        if (!this.lightForeground) sp.setUniform("useLights", 0);
        renderBlocks(sp, this.blocks[2]); // render the foreground blocks
        for (GameObject o : this.decor[1]) o.render(sp); // render the foreground decor
    }

    public void useCam(Camera cam) {
        this.backdrop.useCam(cam);
    }

    /**
     * Resizes the backdrop to fit the screen
     */
    public void resized() {
        this.backdrop.resized();
    }

    /**
     * @return the area's block map
     */
    public boolean[][] getBlockMap() {
        return this.blockMap;
    }

    /**
     * @return the name of the area
     */
    public String getName() {
        return this.name;
    }

    /**
     * Cleans up the area's animated textures and decor
     */
    public void cleanup() {
        for (AnimatedTexture at : this.ats) at.cleanup(); // cleanup animated textures
        for (GameObject o : this.decor[0]) o.cleanup(); // cleanup background decor
        for (GameObject o : this.decor[1]) o.cleanup(); // cleanup foreground decor
    }

    /**
     * Extends model by providing optimizations for rendering many blocks at once. Specifically, it will simply
     * bind the single static block model and enable the correct vertex attribute arrays and then render a bunch of
     * blocks at once, only updating the position in between each rather than enabling and then disabling for each
     * single block
     */
    private static class BlockModel extends Model {

        /**
         * Constructor
         */
        public BlockModel() {
            super(Model.getGridRectModelCoords(1, 1), Model.getStdRectTexCoords(), Model.getStdRectIdx());
        }

        /**
         * Renders the set of blocks corresponding to the given list of block positions. Note that all blocks in the
         * given list should have the same material and that this method should be called once for each material
         *
         * @param sp     the shader program to use to render
         * @param blocks the positions of the blocks to render
         */
        public void renderBlocks(ShaderProgram sp, List<Pair<Integer>> blocks) {
            glBindVertexArray(this.ids[0]); // bind vao
            glEnableVertexAttribArray(0); // enable model coordinate vbo
            glEnableVertexAttribArray(1); // enable texture coordinate vbo
            for (Pair<Integer> b : blocks) { // loop through all blocks
                // set the position of the block in the shader program
                sp.setUniform("x", Transformation.getCenterOfCellComponent((int) b.x));
                sp.setUniform("y", Transformation.getCenterOfCellComponent((int) b.y));
                glDrawElements(GL_TRIANGLES, this.idx, GL_UNSIGNED_INT, 0); // draw block model at that position
            }
            glDisableVertexAttribArray(0); // disable model coordinate vbo
            glDisableVertexAttribArray(1); // disable texture coordinate vbo
            glBindVertexArray(0); // disable vao
        }
    }

    // todo
    private static class BackDrop extends GameObject {

        private Camera cam;
        private int areaWidth, areaHeight;
        private float texAr;

        public BackDrop(String texPath, boolean resRelative, int areaWidth, int areaHeight) {
            super(Model.getStdGridRect(1, 1), new Material(new Texture(texPath, resRelative)));
            this.resized();
            this.areaWidth = areaWidth;
            this.areaHeight = areaHeight;
            this.texAr = (float)this.getMaterial().getTexture().getWidth() /
                         (float)this.getMaterial().getTexture().getHeight();
        }

        public void resized() {
            this.setScale(2f * (Global.ar > 1f ? Global.ar : 1), 2f / (Global.ar < 1f ? Global.ar : 1));
        }

        @Override
        public void render(ShaderProgram sp) {
            if (this.cam != null) super.render(sp);
        }

        public void useCam(Camera cam) {
            this.cam = cam;
            this.setPos(cam.getX(), cam.getY());
        }

        @Override
        public void update(float interval) {
            if (this.cam != null) {
                this.setPos(cam.getX(), cam.getY());
                this.model.useTexCoords(getAppropriateTexCoords(this.texAr, this.getX() / (float)this.areaWidth,
                        1f - this.getY() / (float)this.areaHeight, this.cam, 1f, 0f));
            }
            super.update(interval);
        }

        private static float[] getAppropriateTexCoords(float texAr, float xProp, float yProp, Camera cam,
                                                       float scale, float zoomFactor) {

            xProp = Math.max(0f, Math.min(1f, xProp));
            yProp = Math.max(0f, Math.min(1f, yProp));
            float viewWidth = 1f, viewHeight = 1f;

            if (Global.ar > texAr) { // screen is wider than backdrop in proportion to height
                viewHeight = texAr / Global.ar; // ratio of ratios
            } else { // backdrop is wider than screen in proportion to height
                viewWidth = Global.ar / texAr; // ratio of ratios
            }

            float maxScale = (1f / viewWidth);
            maxScale = Math.min(maxScale, 1f / viewHeight);

            float lz = 1f - (cam.getLinearZoom(1.15f) - 0.5f);
            scale = Math.min(maxScale, scale + (lz * zoomFactor));

            // apply scale
            viewWidth *= scale;
            viewHeight *= scale;
            float lx = xProp * (1 - viewWidth); // calculate left x tex coordinate
            float ly = yProp * (1 - viewHeight); // calculate lower y tex coordinate
            return new float[] { // compile info into finalized texture coordinate float array
                    lx, ly + viewHeight,
                    lx, ly,
                    lx + viewWidth, ly,
                    lx + viewWidth, ly + viewHeight
            };


//            if (Global.ar > 1f) interval *= Global.ar;
//
//            float lx = (xProp * (1 - interval));
//            float ux = lx + xInterval;
//            float uy = ly + yInterval;
//            return new float[]{
//                    lx, 1f,
//                    lx, 0f,
//                    ux, 0f,
//                    ux, 1f
//            };
        }
    }
}
