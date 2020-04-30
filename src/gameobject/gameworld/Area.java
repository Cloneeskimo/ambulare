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

/**
 * Areas hold, efficiently update, and render blocks and their corresponding materials in a block map. They must be
 * loaded from a node-file
 */
public class Area {

    /**
     * Static data
     */
    public static final BlockModel bm = new BlockModel(); // all blocks use same 1x1 square model

    /**
     * Renders the set of blocks corresponding to the given set of block positions very efficiently using the given
     * shader program
     *
     * @param sp             the shader program to render with
     * @param blockPositions the list of block positions grouped together by material. The reason it is set up up as a
     *                       map from material to pair lists is because rendering all blocks of a certain material
     *                       at once, repetitive calls can be avoided
     */
    public static void renderBlocks(ShaderProgram sp, Map<Material, List<Pair<Integer>>> blockPositions) {
        for (Material m : blockPositions.keySet()) { // for each material
            Texture t = m.getTexture(); // get texture for material
            // if the texture is animated, tell the model which texture coordinates to use
            if (t instanceof AnimatedTexture) bm.useTexCoordVBO(((AnimatedTexture) t).getTexCoordVBO());
            else bm.useTexCoordVBO(AnimatedTexture.getTexCoordVBO(0, 1));
            m.setUniforms(sp); // set the appropriate uniforms
            bm.renderBlocks(sp, blockPositions.get(m)); // render all the blocks with that material at once
        }
    }

    /**
     * Members
     */
    private final Map<Material, List<Pair<Integer>>> blockPositions; // a list of block positions grouped by material
    private final List<AnimatedTexture> ats;                         // a list of animated textures to update
    private final List<GameObject> decor;                            // a list of decor in the area
    private final boolean[][] blockMap;                              // block map for collision detection with blocks
    private String name = "Unnamed";                                 // the name of the area
    private LightSource[] ls;                                        // the set of light sources

    /**
     * Constructs the area by compiling the information from a given node. Here are a list of children that a areas node
     * can have:
     * <p>
     * - block_key [required]: this key maps characters in the layout to tile info that describes the corresponding
     * block. Each child of the block_key child should have its name be a single character that appears in the layout
     * (if there are more than one character, only the first will be used) and the rest of the child should be formatted
     * as a tile info node. See AreaLayoutLoader.TileInfo's constructor for more information.
     * <p>
     * - layout [required]: the layout should contain a child for each row, where the value of the row is the set of
     * characters describing that row for which corresponding tiles will be searched for in the keys. Rows will be read
     * and loaded top-down and left-to-right so that they appear in the same order as they do in the node-file. If a
     * character is encountered that is not in the key, it will be ignored. The space character will also be ignored and
     * interpreted as empty
     * <p>
     * - layout_key [optional][default: no decor]: this kep maps characters in the layout to decor info that describes
     * the corresponding decor. Each child of the decor_key child should have its name be a single character that
     * appears in the layout (if there are more than one character, only the first will be used) and the resst of the
     * child should be formtated as a decor info node. See AreaLayoutLoader.DecorInfo's constructor for more information
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

        // create some lights for demonstration
        this.ls = new LightSource[]{
                new LightSource(new float[]{1.3f, 1.3f, 1.0f, 1.0f}, 5f, 1f),
                new LightSource(new float[]{1f, 3f, 1f, 1f}, 6f, 1f),
                new LightSource(new float[]{2.4f, 2.4f, 2.4f, 1f}, 4f, 1f)
        };

        // load blocks
        Node blockKey = node.getChild("block_key"); // get block key child
        if (blockKey == null) Utils.handleException(new Exception("Area node-file does not contain a block key. A " +
                "block key is required"), "gameobject.gameworld.Area", "Area(Node)", true); // if no key child, crash
        Node layoutData = node.getChild("layout");
        if (layoutData == null) Utils.handleException(new Exception("Area node-file did not produce a layout. A " +
                "layout is required"), "gameobject.gameworld.Area", "Area(Node)", true); // no layout child -> crash
        this.blockPositions = new HashMap<>();
        this.ats = new ArrayList<>();
        this.blockMap = AreaLayoutLoader.loadBlocks(blockKey, layoutData, this.blockPositions, this.ats);

        // load decor
        Node decorKey = node.getChild("decor_key"); // get decor key child
        // if there is a key for decor, load the decor
        if (decorKey != null) this.decor = AreaLayoutLoader.loadDecor(decorKey, layoutData, this.ats, this.blockMap);
        else this.decor = new ArrayList<>(); // if no decor, use an empty list

        // load other area properties
        for (Node c : node.getChildren()) { // go through each child and parse the values
            String n = c.getName(); // get name of child
            if (n.equals("name")) { // if thee child is for area name
                this.name = c.getValue(); // save name
            } else { // if none of the above and if not a key or layout,
                if (!n.equals("layout") && !(n.equals("block_key")) && !(n.equals("decor_key")))
                    Utils.log("Unrecognized child given for area info:\n" + c + "Ignoring.",
                            "gameobject.gameworld.Area", "Area(Node)", false); // log unused child
            }
        }
    }

    /**
     * Updates everything in the area
     *
     * @param interval the amount of time to account for
     */
    public void update(float interval) {
        for (AnimatedTexture at : this.ats) at.update(interval); // update animated texturese
        for (GameObject o : this.decor) o.update(interval); // update decor
    }

    /**
     * Renders everything in the area
     *
     * @param sp the shader program to use for rendering
     */
    public void render(ShaderProgram sp) {
        sp.putInLightArrayUniform(this.ls[0], 4f, 4f);
        sp.putInLightArrayUniform(this.ls[1], 18f, 3f);
        sp.putInLightArrayUniform(this.ls[2], 32f, 3f);
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
     * Cleans up the area
     */
    public void cleanup() {
        for (AnimatedTexture at : this.ats) at.cleanup(); // cleanup animated textures
        for (GameObject o : this.decor) o.cleanup(); // cleanup decor
    }

    /**
     * Extends model by providing optimizations for rendering many blocks at once
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
                // set their position in the shader program
                sp.setUniform("x", Transformation.getCenterOfCellComponent((int) b.x));
                sp.setUniform("y", Transformation.getCenterOfCellComponent((int) b.y));
                glDrawElements(GL_TRIANGLES, this.idx, GL_UNSIGNED_INT, 0); // draw model
            }
            glDisableVertexAttribArray(0); // disable model coordinate vbo
            glDisableVertexAttribArray(1); // disable texture coordinate vbo
            glBindVertexArray(0); // disable vao
        }
    }
}
