package gameobject.gameworld;

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

/**
 * Areas hold, efficiently update, and render blocks and their corresponding materials in a block map. They must be
 * loaded from a node-file
 */
public class Area {

    /**
     * All possible file name extensions for various modularization pieces
     */
    private enum MOD_NAME_EXT {
        topleft, top, topright, right, bottomright, bottom, bottomleft, left, column, row, columntop, columnbottom,
        rowrightcap, rowleftcap, insettopleft, insettopright, insetbottomleft, insetbottomright
    }

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
     * Parses key data from an area node-file by populating a key
     *
     * @param keyData     the node containing the key data
     * @param key         the key to populate
     * @param modFileData a map of material to file data for modularization to populate
     */
    private static void parseKeyData(Node keyData, Map<Character, Material> key, Map<Material, String[]> modFileData) {
        for (Node c : keyData.getChildren()) { // go through children
            Node materialData = null;
            String v = c.getValue(); // get value of the child
            if (v.length() >= 4) { // if a from or resfrom statement is used
                String[] tokens = v.split(" "); // split up the (res)from from the path
                if (tokens.length < 2) // if no path was given
                    Utils.log("'" + tokens[0] + "' statement given in area info without specifying where to " +
                                    "find the material. Ignoring.", "gameobject.gameworld.Area",
                            "parseKeyData(Node, Map<Character, Material>, Map<Material, String[]>",
                            false); // log and ignore
                else if (tokens[0].toUpperCase().equals("FROM")) // if value specifies to load material from path
                    // load material data from path
                    materialData = Node.fileToNode(tokens[1], true);
                else if (tokens[0].toUpperCase().equals("RESFROM")) // if specifies to load material from res-path
                    // load material data from res-path
                    materialData = Node.resToNode(tokens[1]);
                else // log and ignore
                    Utils.log("Unrecognized value for a key child in area info: " + tokens[0] + ". Ignoring.",
                            "gameobject.gameworld.Area",
                            "parseKeyData(Node, Map<Character, Material>, Map<Material, String[]>", false);
            } else {
                // if no value, must be an explicitly stated material - load directly
                if (c.getChildren().size() < 1) // if there is no explicitly laid out material
                    Utils.log("Key in area info contains character '" + c.getName().charAt(0) + "' but " +
                                    "provide no material. Ignoring.", "gameobject.gameworld.Area",
                            "parseKeyData(Node, Map<Character, Material>, Map<Material, String[]>",
                            false); // log and ignore
                else materialData = c.getChild(0); // material data is child itself
            }
            Material m = new Material(materialData); // create the material
            key.put(c.getName().charAt(0), m); // put material in map
            Node tp = materialData.getChild("texture_path"); // get texture path
            // if the material is textured, store file path info for modularization
            if (tp != null) {
                Node resPath = materialData.getChild("resource_relative"); // get resource-relative flag
                // determine if the path is resource-relative
                boolean resRelative = resPath == null || Boolean.parseBoolean(resPath.getValue());
                modFileData.put(m, Utils.getFileInfoForPath(resRelative, resRelative ? tp.getValue() :
                        Utils.getDataDir() + tp.getValue())); // store modularization file data
            } else modFileData.put(m, null); // otherwise store null
        }
    }

    /**
     * Modularizes all the blocks in the area. I'm pretty sure this is a made-up term but it sounds cool so I use it
     * anyways. In this case, it just refers to checking for edges and corners in the blockmap and updating the
     * textures (and thereby materials) accordingly
     *
     * @param blockMap       the block map to use for surrounding checks
     * @param blockPositions the map from original materials to block positions of blocks with that material
     * @param modFileData    the file data for modularization of each material
     */
    private static void modularize(boolean[][] blockMap, Map<Material, List<Pair<Integer>>> blockPositions,
                                   Map<Material, String[]> modFileData) {
        List<Material> originalMaterials = new ArrayList<>(blockPositions.keySet()); // get original materials
        for (Material m : originalMaterials) { // for each original material
            // this will map types of modularization to a material to use for any given block
            Map<MOD_NAME_EXT, Material> update = new HashMap<>();
            String[] modData = modFileData.get(m); // get the modularization file data for the current material
            if (modData != null) { // if there is modularization file data for the current material
                List<Pair<Integer>> blocks = blockPositions.get(m);
                for (int j = 0; j < blocks.size(); j++) { // for each block with the current original material
                    MOD_NAME_EXT[] modularization = getPreferredModularization(blocks.get(j).x, blocks.get(j).y,
                            blockMap); // calculate the preferred modularization for the block
                    if (modularization != null) { // if a modularization is preferred
                        boolean chosen = false; // flag to indicate if done modularizing the current block
                        for (int i = 0; i < modularization.length && !chosen; i++) { // for each preferred mod
                            Material updatedMaterial = update.get(modularization[i]); // attempt to get updated material
                            if (updatedMaterial == null) { // if there does not exist one for that modularization yet
                                String supposedPath = modData[0] + modData[1] + "_" + modularization[i].toString() +
                                        modData[2]; // calculate the supposed path of the correct image
                                if (Utils.fileExists(supposedPath, modData[3].equals("true"))) { // if image exists
                                    updatedMaterial = new Material(m); // create duplicate material
                                    // update the texture to the modularized texture
                                    updatedMaterial.setTexture(new Texture(supposedPath, modData[3].equals("true")));
                                    blockPositions.put(updatedMaterial, new ArrayList<>()); // add to block positions
                                    update.put(modularization[i], updatedMaterial); // put into update map
                                }
                            }
                            if (updatedMaterial != null) { // if we end up having an updated material
                                blockPositions.get(updatedMaterial).add(blocks.get(j)); // add to the new material
                                blockPositions.get(m).remove(blocks.get(j)); // remove from the original material
                                chosen = true; // flag that a modularized material has been chosen
                                j--; // decrement j
                            }
                        }
                    }
                }
            }
        }
        // get the current list of materials after modularization
        List<Material> materials = new ArrayList<>(blockPositions.keySet());
        for (int i = 0; i < materials.size(); i++) { // loop through them
            if (blockPositions.get(materials.get(i)).size() < 1) { // if there are no more blocks to that material
                blockPositions.remove(materials.get(i)); // remove that material from consideration
            }
        }
    }

    /**
     * Given the block map and a position to consider, will calculate preferred modularizations
     *
     * @param x        the x of the grid cell to consider
     * @param y        the y of the grid cell to consider
     * @param blockMap the block map to use for consideration
     * @return an array containing modularizations in preferred order, or null if no modularization is suggested
     */
    private static MOD_NAME_EXT[] getPreferredModularization(int x, int y, boolean[][] blockMap) {
        boolean left = x > 0 && (blockMap[x - 1][y]); // determine if a block exists to the left
        boolean right = x < blockMap.length - 1 && (blockMap[x + 1][y]); // determine if a block exists to the right
        boolean below = y > 0 && (blockMap[x][y - 1]); // determine if a block exists below
        boolean above = y < blockMap[0].length - 1 && (blockMap[x][y + 1]); // determine if a block exists above
        // if surrounded on all four sides, calculate diagonals to for inset modularization
        if (left && right && below && above) {
            boolean topLeft = blockMap[x - 1][y + 1]; // determine if a block exists above and to the left
            boolean topRight = blockMap[x + 1][y + 1]; // determine if a block exists above and to the right
            boolean bottomLeft = blockMap[x - 1][y - 1]; // determine if a block exists below and to the left
            boolean bottomRight = blockMap[x + 1][y - 1]; // determine if a block exists below and to the right
            if (topLeft && topRight && bottomRight && !bottomLeft) // if bottom left is the only corner not occupied
                return new MOD_NAME_EXT[]{MOD_NAME_EXT.insetbottomleft}; // prefer an inset bottom left
            if (topRight && bottomRight && bottomLeft && !topLeft) // if top left is the only corner not occupied
                return new MOD_NAME_EXT[]{MOD_NAME_EXT.insettopleft}; // prefer an inset top left
            if (bottomRight && bottomLeft && topLeft && !topRight) // if above right is the only corner not occupied
                return new MOD_NAME_EXT[]{MOD_NAME_EXT.insettopright}; // prefer an inset top right
            if (bottomLeft && topLeft && topRight && !bottomRight) // if bottom right is the only corner not occupied
                return new MOD_NAME_EXT[]{MOD_NAME_EXT.insetbottomright}; // prefer an inset bottom right
            return null; // if no inset, return no modularization
        }
        // if surrounded on the left, below, and on the right -> prefer a top
        if (left && right && below) return new MOD_NAME_EXT[]{MOD_NAME_EXT.top};
        // if surrounded on the left, below, and on the right -> prefer a bottom
        if (left && right && above) return new MOD_NAME_EXT[]{MOD_NAME_EXT.bottom};
        // if surrounded below, above, and to the left -> prefer a right
        if (below && above && left) return new MOD_NAME_EXT[]{MOD_NAME_EXT.right};
        // if surrounded below, above, and to the right -> prefer a left
        if (below && above && right) return new MOD_NAME_EXT[]{MOD_NAME_EXT.left};
        // if only surrounded to the left and right -> prefer a row but settle with a top
        if (left && right) return new MOD_NAME_EXT[]{MOD_NAME_EXT.row, MOD_NAME_EXT.top};
        // if only surrounded above and below -> prefer a column
        if (below && above) return new MOD_NAME_EXT[]{MOD_NAME_EXT.column};
        // if only surrounded above and to the left -> prefer a bottom right but settle with a bottom
        if (above && left) return new MOD_NAME_EXT[]{MOD_NAME_EXT.bottomright, MOD_NAME_EXT.bottom};
        // if only surrounded above and to the right -> prefer a bottom left but settle with a bottom
        if (above && right) return new MOD_NAME_EXT[]{MOD_NAME_EXT.bottomleft, MOD_NAME_EXT.bottom};
        // if only surrounded below and to the left -> prefer a top right but settle with a top
        if (below && left) return new MOD_NAME_EXT[]{MOD_NAME_EXT.topright, MOD_NAME_EXT.top};
        // if only surrounded below and to the right -> prefer a top left but settle with a top
        if (below && right) return new MOD_NAME_EXT[]{MOD_NAME_EXT.topleft, MOD_NAME_EXT.top};
        // if only surrounded above -> prefer a column bottom but settle with a column
        if (above) return new MOD_NAME_EXT[]{MOD_NAME_EXT.columnbottom, MOD_NAME_EXT.column};
        // if only surrounded below -> prefer a column top but settle with a column
        if (below) return new MOD_NAME_EXT[]{MOD_NAME_EXT.columntop, MOD_NAME_EXT.column};
        // if only surrounded to the left -> prefer a row right cap but settle with a row
        if (left) return new MOD_NAME_EXT[]{MOD_NAME_EXT.rowrightcap, MOD_NAME_EXT.row};
        // if only surrounded to the right -> prefer a row left cap but settle with a row
        if (right) return new MOD_NAME_EXT[]{MOD_NAME_EXT.rowleftcap, MOD_NAME_EXT.row};
        return null;
    }

    /**
     * Members
     */
    private String name;                                       // the name of the area
    private boolean[][] blockMap;                              // the block map for collision detection with blocks
    private Map<Material, List<Pair<Integer>>> blockPositions; // a list of block positions grouped by material

    /**
     * Creates an area from the given node. The required children of the node are listed below:
     * - key: contains children that map letters in the 'layout' child to materials to use for the block placed there.
     * - each child's name should be a single character. If more characters are provided, only the first character is
     * used. If multiple materials are mapped to a single character, only the last one read will be used
     * - each child must either (A) have a child of its own that is a parse-able material node, (B) have a value
     * that specifies where to look for a parse-able material node, or (C) have the value 'empty' which denotes
     * that no block should be placed there (space also represents a lack of block but this may not work properly
     * for leading spaces). Case (B) is useful for materials that will be used many places throughout many
     * different areas. For case (B), the value should be formatted as follows: "[resfrom:from] [path]" where
     * [path] is the path to look for the material and [resfrom/from] denotes that the path is resource-relative
     * or not resource-relative, respectively. If not resource-relative, the path should be relative to the
     * Ambulare data folder. In order for textures for edges/corners to be used during modularization, there must exist
     * images that have the same name as the image file given but with one of the following intuitive extensions: _top,
     * _bottom, _left, _right, _topleft, _topright, _bottomleft, _bottomright, _column, _row, _columntop, _columnbottom,
     * _rowcapleft, _rowcapright, _insettopleft, _insettopright, _insetbottomleft, _insetbottomright. If a child is
     * improperly formatted, it will be ignored but the occurrence may be logged
     * - layout: contains children that are sequences of characters that will be converted into blocks using the key
     * child. the value of the children is the character sequence and the name means nothing. they are read in
     * the order that they are listed (higher children are considered to be in a higher row than lower children,
     * and the same principle applies horizontally within each child). If a character appears that is not in the
     * key, it will be ignored and interpreted as empty space
     * The optional children of the node are listed below:
     * - name: the name of the area. If not provided, the area will be named "Unnamed"
     * If areas have errors while parsing, depending on the error, crashes may occur
     *
     * @param node the node to create the area from
     */
    public Area(Node node) {
        try {

            /*
             *  Parse Key
             */
            Map<Character, Material> key = new HashMap<>(); // use hashmap to hold key
            Map<Material, String[]> modFileData = new HashMap<>(); // hashmap to store file data for modularization
            Node keyData = node.getChild("key"); // get key child
            if (keyData == null) Utils.handleException(new Exception("Area node-file did not produce a key. A key is " +
                    "required"), "gameobject.gameworld.Area", "Area(Node)", true); // if no key child, crash
            parseKeyData(keyData, key, modFileData); // parse key data

            /*
             *  Parse Layout
             */
            this.blockPositions = new HashMap<>(); // use hashmap to map material to block positions
            for (Material m : key.values()) this.blockPositions.put(m, new ArrayList<>()); // new list for each material
            Node layoutData = node.getChild("layout"); // get layout child
            if (layoutData == null) Utils.handleException(new Exception("Area node-file did not produce a layout. A " +
                    "layout is required"), "gameobject.gameworld.Area", "Area(Node)", true); // no layout child -> crash
            List<Node> rows = layoutData.getChildren(); // get rows as a separate list
            int w = 0;
            for (int y = 0; y < rows.size(); y++) { // loop through each row
                // figure out the widest row so that block map can be created with appropriate width
                if (rows.get(y).getValue().length() > w) w = rows.get(y).getValue().length();
            }

            /*
             *  Build Block Map
             */
            this.blockMap = new boolean[w][rows.size()]; // create block map
            for (int y = 0; y < rows.size(); y++) { // loop through each row
                Node row = rows.get(rows.size() - 1 - y); // get that row
                for (int x = 0; x < row.getValue().length(); x++) { // loop through each character in the rpw
                    Material m = key.get(row.getValue().charAt(x)); // get the material at
                    // that spot
                    if (m != null) { // if not empty
                        blockMap[x][y] = true; // put into block map
                        this.blockPositions.get(m).add(new Pair(x, y)); // add to list
                    }
                }
            }
            modularize(this.blockMap, this.blockPositions, modFileData); // modularize the materials

            /*
             *  Parse Other Area Data
             */
            this.name = "Unnamed"; // name starts as unnamed
            for (Node c : node.getChildren()) { // go through each child and parse the values
                String n = c.getName(); // get name of child
                if (n.equals("name")) { // if thee child is for area name
                    this.name = c.getValue(); // save name
                } else { // if none of the above
                    if (!n.equals("layout") && !(n.equals("key"))) // if it's not layout or key
                        Utils.log("Unrecognized child given for area info: " + c + ". Ignoring.",
                                "gameobject.gameworld.Area", "Area(Node)", false); // log unused child
                }
            }
        } catch (Exception e) { // if error while parsing
            Utils.handleException(new Exception("Unable to parse the following area info: " + node.toString() + "\n " +
                    "for reason: " + e.getMessage()), "gameobject.gameworld.Area", "Area(Node)", true); // crash
        }
    }

    /**
     * Updates everything in the area
     *
     * @param interval the amount of time to account for
     */
    public void update(float interval) {

        // todo : update animated textures for blocks

    }

    /**
     * Renders everything in the area
     *
     * @param sp the shader program to use for rendering
     */
    public void render(ShaderProgram sp) {
        renderBlocks(sp, this.blockPositions); // render the blocks
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

        // todo : cleanup animated textures

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
