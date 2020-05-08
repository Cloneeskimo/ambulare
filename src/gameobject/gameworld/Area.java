package gameobject.gameworld;

import gameobject.GameObject;
import gameobject.ui.ListObject;
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
 * Decor does not cause collisions. See AreaLayoutLoader.DecorInfo for a list of properties. Decor is represented as a
 * simple list of game objects to update and render every game loop. Decor can either exist in the background or the
 * foreground. Any decor put in the middleground layer in the area node-file will be assumed to be background decor
 * <p>
 * The idea behind areas is to hold only a portion of the game's world and to be interconnected. The GameWorld class
 * only renders and updates one area at a time, the idea being that many areas can exist and be interconnected but only
 * the one that the player is in should be rendered and updated. Some game world mechanics that are independent of area
 * (such as the day/night cycle) are stored in the GameWorld class instead. Areas are loaded through the use of a
 * node-file. See utils.Node for information on node files and see Area's constructor for how area node-files should be
 * laid out
 */
public class Area {

    /**
     * Renders the set of blocks corresponding to the given set of block positions very efficiently using the given
     * shader program
     *
     * @param bm             the block model to render
     * @param sp             the shader program to render with
     * @param blockPositions the list of block positions grouped together by material. The reason it is set up as a map
     *                       from material to pair lists is because by rendering all blocks of a certain material at
     *                       once, repetitive calls can be avoided
     */
    public static void renderBlocks(BlockModel bm, ShaderProgram sp,
                                    Map<Material, List<Pair<Integer>>> blockPositions) {
        for (Material m : blockPositions.keySet()) { // for each material
            Texture t = m.getTexture(); // get texture for material
            if (t instanceof AnimatedTexture) bm.useTexCoordVBO(((AnimatedTexture) t).getTexCoordVBO(false),
                    false); // if the texture is animated, tell the model which tex coords (frame) to use
                // if the texture isn't animated, just use the entire texture
            else bm.useTexCoordVBO(AnimatedTexture.getTexCoordVBO(0, 1, false), false);
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
    private final List<GameObject>[] decor;                   /* two lists of decor in the area where decor[0] is
                                                                 background decor and decor[1] is foreground decor */
    private final List<AnimatedTexture> ats;                  // a list of animated textures to update
    private final boolean[][] blockMap;                       // block map of the middleground for collision detection
    private final BlockModel bm = new BlockModel();           /* all blocks use same 1x1 square model. See BlockModel
                                                                 for more info on how it extends a regular model */
    private BackDrop backdrop;                                // the scrolling backdrop rendered behind the world
    private String name = "Unnamed";                          // the name of the area
    private boolean lightForeground = false;                  // whether to apply lights to objects in the foreground

    /**
     * Constructs the area by compiling information from a given node. Most of the loading process is actually done in
     * the area layout loader to avoid cluttering the area class with a bunch of large static methods. An area node can
     * have the following children:
     * <p>
     * - block_key [required]: this key maps characters in the layout to tile info that describes the corresponding
     * block. Each child of the block_key child should have its name be a single character that appears in the layout
     * (if there is more than one character, only the first will be used) and the rest of the child should be formatted
     * as a tile info node. See AreaLayoutLoader.TileInfo's constructor for more information.
     * <p>
     * - middleground_layout [required]: the middleground layout should contain a child for each row, where the value of
     * the row is the set of characters describing that row for which corresponding tiles will be searched for in the
     * keys to build the middleground. Rows will be read and loaded top-down and left-to-right so that they appear in
     * the same order as they do in the node-file. Extra empty rows at the bottom are not necessary, however empty rows
     * at the top are necessary to line up with the background and the foreground. If a character is encountered that is
     * not in any key, it will be ignored. The space character will also be ignored and interpreted as empty space. If
     * decor is found in the middleground layout, it will be placed in the background
     * <p>
     * - decor_key [optional][default: no decor]: this kep maps characters in the layout to decor info that describes
     * the corresponding decor. Each child of the decor_key child should have its name be a single character that
     * appears in the layout (if there is more than one character, only the first will be used) and the rest of the
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
     * - backdrop [optional][default: default backdrop]: the backdrop to use for the area. This node should be
     * formatted as a backdrop node. See Area.BackDrop's constructor for more information
     * <p>
     * Note that, if any of the info above is improperly formatted, a message saying as much will be logged. As
     * such, when designing areas to be loaded into the game, the logs should be checked often to make sure the
     * loading process is unfolding correctly
     *
     * @param info the node to create the area from
     */
    public Area(Node info) {

        // load three layers of layout
        Node background = info.getChild("background_layout"); // get background layout child
        if (background == null) Utils.log(Utils.getImproperFormatErrorLine("background_layout", "area",
                "no background provided: assuming no background", true),
                "gameobject.gameworld.Area", "Area(Node)", false); // if none, log and ignore
        Node middleground = info.getChild("middleground_layout"); // get middleground layout child
        if (middleground == null) Utils.handleException(new Exception(Utils.getImproperFormatErrorLine(
                "middleground_layout", "area", "a middleground layout is required",
                false)), "gameobject.gameworld.Area", "Area(Node)", true); // if no middleground layout, crash
        Node foreground = info.getChild("foreground_layout"); // get foreground layout child
        if (foreground == null) Utils.log(Utils.getImproperFormatErrorLine("foreground_layout", "area",
                "no foreground provided: assuming no foreground", true),
                "gameobject.gameworld.Area", "Area(Node)", false); // if none, log and ignore

        // load blocks
        Node blockKey = info.getChild("block_key"); // get block key child
        if (blockKey == null) Utils.handleException(new Exception(Utils.getImproperFormatErrorLine("block_key",
                "area", "a block key is required", false)), "gameobject.gameworld.Area",
                "Area(Node)", true); // if no block key child, crash
        // create three empty block lists: background, middleground, and foreground
        this.blocks = new Map[]{new HashMap<>(), new HashMap<>(), new HashMap<>()};
        this.ats = new ArrayList<>(); // create animated textures list
        // use the area layout loader to actually load the blocks
        this.blockMap = AreaLayoutLoader.loadLayoutBlocks(blockKey, background, middleground, foreground, blocks, ats);

        // load decor
        Node decorKey = info.getChild("decor_key"); // get decor key child
        // create two empty decor lists: background and foreground
        this.decor = new List[]{new ArrayList<>(), new ArrayList<>()};
        if (decorKey != null) AreaLayoutLoader.loadLayoutDecor(decorKey, background, middleground, foreground,
                this.decor, this.ats, this.blockMap); // if there is a key for decor, load decor

        // parse other area properties
        try { // wrap entire parsing in a try/catch to catch and log any problems
            for (Node c : info.getChildren()) { // go through each child and parse the values
                String n = c.getName(); // get name of child
                if (n.equals("name")) { // if the child is for the area's name
                    this.name = c.getValue(); // save name
                } else if (n.equals("light_foreground")) { // light foreground
                    this.lightForeground = Boolean.parseBoolean(c.getValue()); // save value
                } else if (n.equals("backdrop")) { // backdrop
                    this.backdrop = new BackDrop(c, this.blockMap.length, this.blockMap[0].length); // create with node
                } else { // if the child is unrecognized
                    if (!(n.equals("block_key")) && !(n.equals("decor_key")) && !(n.equals("background_layout")) &&
                            !(n.equals("middleground_layout")) && !(n.equals("foreground_layout")))
                        Utils.log("Unrecognized child given for area info:\n" + c + "Ignoring.",
                                "gameobject.gameworld.Area", "Area(Node)", false); // log and ignore
                }
            }
            if (this.backdrop == null) this.backdrop = new BackDrop(new Node(), this.blockMap.length,
                    this.blockMap[0].length); // if no backdrop was loaded, create and use default one
        } catch (Exception e) { // if any strange exceptions occur
            Utils.handleException(new Exception(Utils.getImproperFormatErrorLine("Area", "Area",
                    e.getMessage(), false)), "gameobject.gameworld.Area", "Area(Node)", true); // log and crash
        }
    }

    /**
     * Updates the area's animated textures and decor
     *
     * @param interval the amount of time to account for
     */
    public void update(float interval) {
        for (AnimatedTexture at : this.ats) at.update(interval); // update animated textures
        for (GameObject o : this.decor[0]) o.update(interval); // update background decor
        for (GameObject o : this.decor[1]) o.update(interval); // update foreground decor
        this.backdrop.update(interval); // update backdrop
    }

    /**
     * Renders the area's blocks and decor
     *
     * @param sp the shader program to use for rendering
     * @param os the lists of world objects to render in the area
     */
    public void render(ShaderProgram sp, List<WorldObject> os) {
        this.backdrop.render(sp); // render the backdrop
        renderBlocks(this.bm, sp, this.blocks[0]); // render the background blocks
        for (GameObject o : this.decor[0]) o.render(sp); // render the background decor
        renderBlocks(this.bm, sp, this.blocks[1]); // render the middleground blocks
        for (WorldObject wo : os) wo.render(sp); // render world objects (middleground) from the game world
        // disable light usage for foreground objects if the setting is set to false
        if (!this.lightForeground) sp.setUniform("useLights", 0);
        renderBlocks(this.bm, sp, this.blocks[2]); // render the foreground blocks
        for (GameObject o : this.decor[1]) o.render(sp); // render the foreground decor
    }

    /**
     * Tells the Area which camera is being used. This is necessary for the backdrop to scroll properly if it is
     * textured
     *
     * @param cam the camera being used
     */
    public void useCam(Camera cam) {
        this.backdrop.useCam(cam);
    }

    /**
     * Handles a window resize by telling the backdrop it needs to resize to fit the new window size
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
        this.bm.cleanup(); // cleanup the block model
    }

    /**
     * Extends a normal model by providing optimizations for rendering many blocks at once. Specifically, it will simply
     * render a bunch of blocks at once using the same model, only updating the position in between each rather than
     * enabling and then disabling VAOs and VBOs for each individual block
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

    /**
     * Represents a scrollable backdrop that is rendered behind everything in an area. These are loaded from node-files.
     * See the constructor for more details on the how to format a backdrop node. Note that a backdrop must be given a
     * camera to follow by calling useCam() or it will not render at all
     */
    private static class BackDrop extends GameObject {

        /**
         * Members
         */
        private Camera cam;                 // the camera to follow and use for scrolling calculations
        private int areaWidth, areaHeight;  // the width and height of the area to consider for scrolling
        private float texAr;                // the aspect ratio of the texture if the backdrop is textured
        private float viewScale = 1f;       /* how much of the width/height of the texture to show depending on aspect
                                               ratios. See the constructor for more info on view scale */
        private float zoomFactor;           /* how much camera zoom affects view scale. See the constructor for more
                                               info on zoom factor */

        /**
         * Constructs the backdrop by compiling information from a given node. If the value of the root node starts with
         * the statements 'from' or 'resfrom', the next statement will be assumed to be a different path at which to
         * find the tile info. This is useful for reusing the same backdrop in multiple settings. 'from' assumes the
         * following path is relative to the Ambulare data folder (in the user's home folder) while 'resfrom' assumes
         * the following path is relative to Ambulares's resource path. Note that these kinds of statements cannot be
         * chained together. A backdrop node can have the following children:
         * <p>
         * - color [optional][default: 1f 1f 1f 1f]: specifies what color to assign to the backdrop
         * <p>
         * - texture_path [optional][default: no texture]: specifies the path to look for the backdrop texture in
         * <p>
         * - resource_relative [optional][default: true]: specifies whether the given texture path is relative to
         * Ambulare's resource path. If this is false, the given path must be relative to Ambulare's data folder (in
         * the user's home folder)
         * <p>
         * - blend_mode [optional][default: none]: specifies how to blend color and texture. The options are: (1) none -
         * no blending will occur. The backdrop will appear as its texture if it has one, or its color if there is no
         * texture. (2) multiplicative - the components of the backdrop color and the components of the texture will be
         * multiplied to create a final color. (3) averaged - the components of the backdrop color and the components of
         * the texture will be averaged to create a final color
         * <p>
         * - view_scale [optional][default: 1f]: If the backdrop has a texture, this specifies how much of the
         * width/height of the backdrop to show at once. This will apply to both width and height, but it directly
         * determines how much of either width or height will be shown. For example, a scale of 1f which will show the
         * entire width or height of the texture at all times depending on the ratio of the texture's aspect ratio to
         * the window's aspect ratio. If the texture is proportionally wider than the window and scale is set to 1f, the
         * entire height of the backdrop will always be seen but will be scrolled horizontally as the player moves. It
         * follows logically that lower scales will make scroll speed quicker. View scales must be within the interval
         * of (0f, 1f]
         * <p>
         * - zoom_factor [optional][default: 0f]: If the backdrop has a texture, this specifies how much camera zoom
         * affects the zoom on the backdrop. Note that whatever the value, this will not allow the texture to be zoomed
         * out so much that it would wrap or repeat and break the visual. Zoom factors cannot be negative
         * <p>
         * Note that, if any of the info above is improperly formatted, a message saying as much will be logged. As
         * such, when designing backdrops to be loaded into the game, the logs should be checked often to make sure the
         * loading process is unfolding correctly
         *
         * @param info       the node to create the backdrop from
         * @param areaWidth  how wide the area for scrolling should be. Note that this will assume the area's x-axis
         *                   space is [0f, areaWidth]
         * @param areaHeight how tall the area for scrolling should be. Note that this will assume the area's y-axis
         *                   space is [0f, areaHeight]
         */
        public BackDrop(Node info, int areaWidth, int areaHeight) {
            super(Model.getStdGridRect(1, 1), null); // start with no material and a default square model

            // load from elsewhere if from or resfrom statement used
            String value = info.getValue(); // get value
            if (value != null) { // if there is a value
                // check for a from statement
                if (value.length() >= 4 && value.substring(0, 4).toUpperCase().equals("FROM"))
                    // update info with node at the given path in the from statement
                    info = Node.fileToNode(info.getValue().substring(5), true);
                    // check for a resfrom statement
                else if (value.length() >= 7 && value.substring(0, 7).toUpperCase().equals("RESFROM"))
                    // update info with node at the given path in the from statement
                    info = Node.resToNode(info.getValue().substring(8));
                if (info == null) // if the new info is null, then throw an exception stating the path is invalid
                    Utils.handleException(new Exception(Utils.getImproperFormatErrorLine("(res)from statement",
                            "BackDrop", "invalid path in (res)from statement: " + value,
                            false)), "gameobject.gameworld.Area.BackDrop", "BackDrop(Node, int, int)", true);
            }

            // relevant variables to hold loaded information
            float[] color = new float[]{1f, 1f, 1f, 1f}; // holds the material color
            String texPath = null; // holds the path to the material texture
            boolean texResPath = true; // holds whether a given texture path is resource-relative
            Material.BlendMode bm = Material.BlendMode.NONE; // holds the material's blend mode

            // parse node
            try { // wrap entire parsing in a try-catch to make sure all issues are caught and logged
                for (Node c : info.getChildren()) { // go through each child
                    String n = c.getName(); // get the name of the child
                    if (n.equals("color")) { // color
                        color = Utils.strToColor(c.getValue()); // try to convert to a float array of color components
                        if (color == null) // if conversion was unsuccessful
                            Utils.log(Utils.getImproperFormatErrorLine("color", "BackDrop",
                                    "must be four valid floating point numbers separated by spaces",
                                    true), "gameobject.gameworld.Area.BackDrop",
                                    "BackDrop(Node, int, int)", false); // log as much
                        else color = color; // otherwise save the color
                    } else if (n.equals("texture_path")) texPath = c.getValue(); // texture path
                        // resource relative texture path flag
                    else if (n.equals("resource_relative")) texResPath = Boolean.parseBoolean(c.getValue());
                    else if (n.equals("blend_mode")) { // blend mode
                        try { // try to convert to a material's blend mode
                            bm = Material.BlendMode.valueOf(c.getValue().toUpperCase());
                        } catch (Exception e) { // if conversion was unsuccessful, log as much
                            Utils.log(Utils.getImproperFormatErrorLine("blend_mode", "BackDrop",
                                    "must be either: none, multiplicative, or averaged", true),
                                    "gameobject.gameworld.Area.BackDrop", "Area(Node, int, int)",
                                    false);
                        }
                    } else if (n.equals("view_scale")) { // view scale
                        try {
                            this.viewScale = Float.parseFloat(c.getValue()); // try to convert to a float
                        } catch (Exception e) { // if conversion was unsuccessful, log as much
                            Utils.log(Utils.getImproperFormatErrorLine("view_scale", "BackDrop",
                                    "must be a proper floating pointer number", true),
                                    "gameobject.gameworld.Area.BackDrop", "BackDrop(Node, int, int)",
                                    false);
                        }
                        if (this.viewScale <= 0f || this.viewScale >= 1f) { // if not in correct range, log as much
                            Utils.log(Utils.getImproperFormatErrorLine("view_scale", "BackDrop",
                                    "must be within the range (0f, 1f]", true),
                                    "gameobject.gameworld.Area.BackDrop", "BackDrop(Node, int, int)",
                                    false);
                            this.viewScale = 1f; // and return to default
                        }
                    } else if (n.equals("zoom_factor")) { // zoom factor
                        try {
                            this.zoomFactor = Float.parseFloat(c.getValue()); // try to convert to a float
                        } catch (Exception e) { // if conversion was unsuccessful
                            Utils.log(Utils.getImproperFormatErrorLine("zoom_factor", "BackDrop",
                                    "must be a proper floating pointer number greater than or equal to 0f",
                                    true), "gameobject.gameworld.Area.BackDrop",
                                    "BackDrop(Node, int, int)", false); // log as much
                        }
                        if (this.zoomFactor < 0f) { // if zoom factor is negative
                            Utils.log(Utils.getImproperFormatErrorLine("zoom_factor", "BackDrop",
                                    "must be a proper floating pointer number greater than or equal to 0f",
                                    true), "gameobject.gameworld.Area.BackDrop",
                                    "BackDrop(Node, int, int)", false); // log as much
                            this.zoomFactor = 1f; // and return to default
                        }
                    } else // if unrecognized child is found
                        Utils.log("Unrecognized child given for backdrop:\n" + c + "Ignoring.",
                                "gameobject.gameworld.Area.BackDrop", "BackDrop(Node, int, int)",
                                false); // log it
                }
            } catch (Exception e) { // if any strange exceptions occur
                Utils.handleException(new Exception(Utils.getImproperFormatErrorLine("BackDrop",
                        "BackDrop", e.getMessage(), false)), "gameobject.gameworld.Area.BackDrop",
                        "BackDrop(Node, int, int)", true); // log and crash
            }

            // create the correct material for the backdrop
            this.material = new Material(texPath == null ? null : new Texture(texPath, texResPath), color, bm);
            if (this.material.isTextured()) this.texAr = (float) this.material.getTexture().getWidth() /
                    (float) this.material.getTexture().getHeight(); // save texture aspect ratio if textured
            this.resized(); // scale to the appropriate size to fit the window
            this.areaWidth = areaWidth; // save area width as member
            this.areaHeight = areaHeight; // save area height as member
        }

        /**
         * Reacts to window resizing by resizing the backdrop to fit the window perfectly
         */
        public void resized() {
            this.setScale(2f * (Global.ar > 1f ? Global.ar : 1), 2f / (Global.ar < 1f ? Global.ar : 1));
        }

        /**
         * Renders the backdrop if it has been given a camera to follow
         *
         * @param sp the shader program to use to render the game object
         */
        @Override
        public void render(ShaderProgram sp) {
            if (this.cam != null) { // only render if a camera has been given
                sp.setUniform("useLights", 0); // disable light usage for backdrop
                sp.setUniform("camZoom", 1f); // set camera zoom to 1 to render backdrop
                super.render(sp); // render
                sp.setUniform("camZoom", cam.getZoom()); // return the zoom to the correct camera zoom
                sp.setUniform("useLights", 1); // turn light usage back on
            } else // if a render attempt was made without a camera being given first
                Utils.log("Attempted to render a backdrop without providing a camera to follow",
                        "gameobject.gameworld.Area.BackDrop", "render(ShaderProgram)", false); // log
        }

        /**
         * Tells the backdrop which camera to follow. This must be called before a backdrop will render
         *
         * @param cam the camera the backdrop should follow
         */
        public void useCam(Camera cam) {
            this.cam = cam; // save the camera reference
            this.setPos(cam.getX(), cam.getY()); // move to the camera position
        }

        /**
         * Updates the backdrop by following the camera and changing the scroll if textured
         *
         * @param interval the amount of time to account for
         */
        @Override
        public void update(float interval) {
            if (this.cam != null) { // if a camera has been given
                this.setPos(cam.getX(), cam.getY()); // follow it
                // if the backdrop is textured, make sure the correct texture coordinates are being used for scrolling
                if (this.material.isTextured()) this.updateTexCoords();
            }
            super.update(interval); // update other game object properties
        }

        /**
         * Updates the texture coordinates of the backdrop's model depending on the followed camera's position to
         * simulate scrolling
         */
        private void updateTexCoords() {

            // calculate how far the camera is in proportion to the area width and height
            float xProp = Math.max(0f, Math.min(1f, this.getX() / (float) this.areaWidth));
            float yProp = Math.max(0f, Math.min(1f, 1f - this.getY() / (float) this.areaHeight));

            // calculate the width and height of the view depending on the ratio of aspect ratios
            float viewWidth = 1f, viewHeight = 1f;
            if (Global.ar > texAr) { // screen is wider than backdrop in proportion to height
                viewHeight = texAr / Global.ar; // apply ratio of ratios on view height
            } else { // backdrop is wider than screen in proportion to height
                viewWidth = Global.ar / texAr; // apply ratio of ratios on view width
            }

            // calculate max scaling that can occur before more than entire image is shown for either width or height
            float maxScale = (1f / viewWidth); // max scaling for width
            maxScale = Math.min(maxScale, 1f / viewHeight); // max scaling for width and height

            // apply zoom factor
            float lz = (1f - cam.getLinearZoom(1.15f)) - 0.5f; // get the linear zoom of the camera
            float vs = Math.abs(Math.min(maxScale, this.viewScale + (lz * zoomFactor))); // get scaling from zoom factor
            viewWidth *= vs; // apply zoom factor to view width
            viewHeight *= vs; // apply zoom factor to view height

            // calculate final texture coordinates
            float lx = xProp * (1 - viewWidth); // calculate left x tex coordinate
            float ly = yProp * (1 - viewHeight); // calculate lower y tex coordinate
            this.model.useTexCoords(new float[]{ // compile info into finalized texture coordinate float array
                    lx, ly + viewHeight, // top left
                    lx, ly, // bottom left
                    lx + viewWidth, ly, // bottom right
                    lx + viewWidth, ly + viewHeight // top right
            });
        }
    }
}
