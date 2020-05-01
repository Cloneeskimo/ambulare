package gameobject.gameworld;

import gameobject.GameObject;
import graphics.*;
import utils.Node;
import utils.Pair;
import utils.Transformation;
import utils.Utils;

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
 * Areas hold, efficiently update, and render the following components that constitute an area:
 * <p>
 * - Blocks: blocks are always 1 grid cell in size, are always collidable, and do not move. Blocks are represented simply
 * as grid cell positions indexed by the material to use when rendering a block in that grid cell. This is efficient and
 * minimalistic which helps to be able to have large quantities of blocks in an area. There is also a block map which is
 * just a two-dimensional array of booleans specifying which cells in the area are occupied by a block. This is to allow
 * the physics engine to do constant-time, efficient collision detection with all blocks in the area.
 * <p>
 * - Decor: decor can be any size, can emit light, and has additional properties that allow it to be more customized.
 * Decor does not cause collisions. See DecorInfo for all of the properties decor can have. Decor is represented as a
 * simple list of game objects to update and render every game loop
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
            if (t instanceof AnimatedTexture) bm.useTexCoordVBO(((AnimatedTexture) t).getTexCoordVBO());
            // if the texture isn't animated, just usse the entire texture
            else bm.useTexCoordVBO(AnimatedTexture.getTexCoordVBO(0, 1));
            m.setUniforms(sp); // set the appropriate material uniforms
            bm.renderBlocks(sp, blockPositions.get(m)); // render all the blocks with that material at once
        }
    }

    /**
     * Members
     */
    private final Map<Material, List<Pair<Integer>>> blockPositions; /* a list of block positions grouped by material.
        Blocks are represented this way to facilitate efficient rendering of large quantities of blocks. See the
        renderBlocks() method for more info */
    private final List<AnimatedTexture> ats;                         // a list of animated textures to update
    private final List<GameObject> decor;                            // a list of decor in the area
    private final boolean[][] blockMap;                              // block map for collision detection with blocks
    private String name = "Unnamed";                                 // the name of the area

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
     * - layout [required]: the layout should contain a child for each row, where the value of the row is the set of
     * characters describing that row for which corresponding tiles will be searched for in the keys. Rows will be read
     * and loaded top-down and left-to-right so that they appear in the same order as they do in the node-file. If a
     * character is encountered that is not in any key, it will be ignored. The space character will also be ignored and
     * interpreted as empty space
     * <p>
     * - decor_key [optional][default: no decor]: this kep maps characters in the layout to decor info that describes
     * the corresponding decor. Each child of the decor_key child should have its name be a single character that
     * appears in the layout (if there are more than one character, only the first will be used) and the rest of the
     * child should be formatted as a decor info node. See AreaLayoutLoader.DecorInfo's constructor for more information
     * <p>
     * - name [optional][default: Unnamed]: the name of the area
     * <p>
     * Note that, if any of the info above is improperly formatted, a message saying as much will be logged. As
     * such, when designing areas to be loaded into the game, the logs should be checked often to make sure the
     * loading process is unfolding correctly
     *
     * @param node the node to create the area from
     */
    public Area(Node node) {

        // load blocks
        Node blockKey = node.getChild("block_key"); // get block key child
        if (blockKey == null) Utils.handleException(new Exception("Area node-file does not contain a block key. A " +
                "block key is required"), "gameobject.gameworld.Area", "Area(Node)", true); // if no key child, crash
        Node layoutData = node.getChild("layout"); // get layout child
        if (layoutData == null) Utils.handleException(new Exception("Area node-file did not produce a layout. A " +
                "layout is required"), "gameobject.gameworld.Area", "Area(Node)", true); // if no layout child -> crash
        this.blockPositions = new HashMap<>();
        this.ats = new ArrayList<>();
        // use the area layout loader to actually load the blocks
        this.blockMap = AreaLayoutLoader.loadBlocks(blockKey, layoutData, this.blockPositions, this.ats);

        // load decor
        Node decorKey = node.getChild("decor_key"); // get decor key child
        // if there is a key for decor, load the decor using the area layout loader
        if (decorKey != null) this.decor = AreaLayoutLoader.loadDecor(decorKey, layoutData, this.ats, this.blockMap);
        else this.decor = new ArrayList<>(); // if no decor, use an empty list

        // load other area properties
        for (Node c : node.getChildren()) { // go through each child and parse the values
            String n = c.getName(); // get name of child
            if (n.equals("name")) { // if the child is for the area's name
                this.name = c.getValue(); // save name
            } else { // if the child is unrecognized
                if (!n.equals("layout") && !(n.equals("block_key")) && !(n.equals("decor_key")))
                    Utils.log("Unrecognized child given for area info:\n" + c + "Ignoring.",
                            "gameobject.gameworld.Area", "Area(Node)", false); // log and ignore
            }
        }
    }

    /**
     * Updates the area's animated textures and decor
     *
     * @param interval the amount of time to account for
     */
    public void update(float interval) {
        for (AnimatedTexture at : this.ats) at.update(interval); // update animated textures
        for (GameObject o : this.decor) o.update(interval); // update decor
    }

    /**
     * Renders the area's blocks and decor
     *
     * @param sp the shader program to use for rendering
     */
    public void render(ShaderProgram sp) {
        renderBlocks(sp, this.blockPositions); // render the blocks
        for (GameObject o : this.decor) o.render(sp); // render the decor
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
        for (GameObject o : this.decor) o.cleanup(); // cleanup decor
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
         * @param sp             the shader program to use to render
         * @param blockPositions the positions of the blocks to render
         */
        public void renderBlocks(ShaderProgram sp, List<Pair<Integer>> blockPositions) {
            glBindVertexArray(this.ids[0]); // bind vao
            glEnableVertexAttribArray(0); // enable model coordinate vbo
            glEnableVertexAttribArray(1); // enable texture coordinate vbo
            for (Pair<Integer> b : blockPositions) { // loop through all blocks
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
}
