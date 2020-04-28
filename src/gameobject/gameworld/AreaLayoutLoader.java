package gameobject.gameworld;

import graphics.AnimatedTexture;
import graphics.Material;
import graphics.Texture;
import utils.Node;
import utils.Pair;
import utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class used for loading area layout
 */
public class AreaLayoutLoader {

    /**
     * All possible types of modularization and the corresponding image file name extension (except for NONE which
     * represents no extension)
     */
    private enum ModNameExt {
        topleft, top, topright, right, bottomright, bottom, bottomleft, left, column, row, columntop, columnbottom,
        rowrightcap, rowleftcap, insettopleft, insettopright, insetbottomleft, insetbottomright, NONE
    }

    /**
     * Loads a layout using the given key and layout nodes
     *
     * @param keyData        the key child node from the area node-file
     * @param layoutData     the layout child node from the area node-file
     * @param blockPositions the material to positions map to populate
     * @param ats            the list of animated textures to populate
     * @return the block map for the loaded layout
     */
    public static boolean[][] loadLayout(Node keyData, Node layoutData,
                                         Map<Material, List<Pair<Integer>>> blockPositions, List<AnimatedTexture> ats) {
        Map<Character, BlockInfo> key = parseKeyData(keyData); // parse key

        // create empty block map with appropriate size
        List<Node> rows = layoutData.getChildren(); // get all rows
        int w = 0;
        for (Node node : rows) { // loop through each row and keep track of the widest one
            if (node.getValue().length() > w) w = node.getValue().length();
        }
        boolean[][] blockMap = new boolean[w][rows.size()]; // create the correctly sized block map

        // populate block map based on where blocks are
        for (int y = 0; y < rows.size(); y++) { // for each row
            String row = rows.get(rows.size() - 1 - y).getValue(); // get the row
            for (int x = 0; x < row.length(); x++) { // for each character in the row
                // if the block info for that character isn't null, then there is a block there
                blockMap[x][y] = (key.get(row.charAt(x))) != null;
            }
        }
        // create materials and block positions
        /* maps block info to a mapping of texture to a mapping of modularization piece to materials. This is used to
           ensure the least possible amount of material creation */
        Map<BlockInfo, Map<String, Map<ModNameExt, Material>>> materialMap = new HashMap<>();
        for (int y = 0; y < rows.size(); y++) { // go through each row
            String row = rows.get(rows.size() - 1 - y).getValue(); // get the row
            for (int x = 0; x < row.length(); x++) { // loop through each character in the row
                BlockInfo bi = key.get(row.charAt(x)); // get the block info for that character
                if (bi != null) { // if the block info isn't null

                    /* get the texture-> modularization piece ->material map for that block info and create it if
                       it doesn't exist */
                    Map<String, Map<ModNameExt, Material>> forThatBlockInfo =
                            materialMap.computeIfAbsent(bi, k -> new HashMap<>());

                    // get the texture (or lack thereof)
                    String texPath = "null"; // if no texture, use "null" string
                    List<String> possibleTextures = bi.texPaths; // get the possible textures
                    if (possibleTextures.size() > 0) { // if there is at least one texture
                        if (possibleTextures.size() > 1) { // if there is more than one texture
                            // choose a random one
                            texPath = possibleTextures.get((int) (Math.random() * possibleTextures.size()));
                        } else texPath = possibleTextures.get(0); // otherwise choose the only one
                    }

                    // get the mod->material map for that texture and create it if it doesn't exist
                    Map<ModNameExt, Material> forThatTexture =
                            forThatBlockInfo.computeIfAbsent(texPath, k -> new HashMap<>());

                    // apply modularization - if not textured, assume no modularization
                    ModNameExt[] mods = texPath.equals("null") ? new ModNameExt[]{ModNameExt.NONE} :
                            getPreferredModularization(x, y, blockMap); // get preferred modularization
                    boolean materialChosen = false; // no material chosen yet
                    // while no material has been chosen and there are still different kinds of modularization to try
                    for (int i = 0; i < mods.length && !materialChosen; i++) {

                        // get the material for the modularization
                        Material m = forThatTexture.get(mods[i]);
                        if (m == null) { // if it doesn't yet exist, need to create it

                            if (!texPath.equals("null")) { // if textured
                                // get info for texture path
                                String[] fileInfo = Utils.getFileInfoForPath(bi.texResPath, texPath);
                                // compile the supposed path to the image file for that modularization
                                String path = fileInfo[0] + fileInfo[1] + (mods[i].equals(ModNameExt.NONE)
                                        ? "" : "_" + mods[i].toString()) + fileInfo[2];
                                if (Utils.fileExists(path, bi.texResPath)) { // if the file at that path exists
                                    Texture t; // start at null
                                    if (bi.animated) { // if animated
                                        t = new AnimatedTexture(path, bi.texResPath, bi.animFrames, bi.frameTime,
                                                true); // create animated texture
                                        ats.add((AnimatedTexture) t); // add to list of animated textures
                                    } else t = new Texture(path, bi.texResPath); // if not, create regular texture
                                    m = new Material(t, bi.color, bi.bm); // create material with the correct texture
                                    forThatTexture.put(mods[i], m); // put it in the map from mod -> material
                                    // create new list for blocks with the corresponding material
                                    blockPositions.put(m, new ArrayList<>());
                                }
                            } else { // if not textured
                                m = new Material(bi.color); // create material with just color
                                forThatTexture.put(mods[i], m); // put it in the map from mod -> material
                                // create new list for blocks with the corresponding material
                                blockPositions.put(m, new ArrayList<>());
                            }
                        }
                        if (m != null) { // if a material now exists for that mod
                            blockPositions.get(m).add(new Pair<>(x, y)); // add the block pos to list for that material
                            materialChosen = true; // flag that a material has been chosen
                        }
                    }
                }
            }
        }
        return blockMap; // return the block map
    }

    /**
     * Parses key data for loading a layout
     *
     * @param keyData the key child node from the area node-file
     * @return the parsed key, mapping from character in the layout to corresponding block info
     */
    private static Map<Character, BlockInfo> parseKeyData(Node keyData) {
        Map<Character, BlockInfo> key = new HashMap<>(); // start as empty hashmap
        // create a block info for each child and put it in the key
        for (Node c : keyData.getChildren()) key.put(c.getName().charAt(0), new BlockInfo(c));
        return key;
    }

    /**
     * Given the block map and a position to consider, will calculate preferred kinds of modularization
     *
     * @param x        the x of the grid cell to consider
     * @param y        the y of the grid cell to consider
     * @param blockMap the block map to use for consideration
     * @return an array containing kinds of modularization in preferred order
     */
    private static ModNameExt[] getPreferredModularization(int x, int y, boolean[][] blockMap) {
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
                return new ModNameExt[]{ModNameExt.insetbottomleft, ModNameExt.NONE}; // prefer an inset bottom left
            if (topRight && bottomRight && bottomLeft && !topLeft) // if top left is the only corner not occupied
                return new ModNameExt[]{ModNameExt.insettopleft, ModNameExt.NONE}; // prefer an inset top left
            if (bottomRight && bottomLeft && topLeft && !topRight) // if above right is the only corner not occupied
                return new ModNameExt[]{ModNameExt.insettopright, ModNameExt.NONE}; // prefer an inset top right
            if (bottomLeft && topLeft && topRight && !bottomRight) // if bottom right is the only corner not occupied
                return new ModNameExt[]{ModNameExt.insetbottomright, ModNameExt.NONE}; // prefer an inset bottom right
            return new ModNameExt[]{ModNameExt.NONE}; // if no inset, return no modularization
        }
        // if surrounded on the left, below, and on the right -> prefer a top
        if (left && right && below) return new ModNameExt[]{ModNameExt.top, ModNameExt.NONE};
        // if surrounded on the left, below, and on the right -> prefer a bottom
        if (left && right && above) return new ModNameExt[]{ModNameExt.bottom, ModNameExt.NONE};
        // if surrounded below, above, and to the left -> prefer a right
        if (below && above && left) return new ModNameExt[]{ModNameExt.right, ModNameExt.NONE};
        // if surrounded below, above, and to the right -> prefer a left
        if (below && above && right) return new ModNameExt[]{ModNameExt.left, ModNameExt.NONE};
        // if only surrounded to the left and right -> prefer a row but settle with a top
        if (left && right) return new ModNameExt[]{ModNameExt.row, ModNameExt.top, ModNameExt.NONE};
        // if only surrounded above and below -> prefer a column
        if (below && above) return new ModNameExt[]{ModNameExt.column, ModNameExt.NONE};
        // if only surrounded above and to the left -> prefer a bottom right but settle with a bottom
        if (above && left) return new ModNameExt[]{ModNameExt.bottomright, ModNameExt.bottom, ModNameExt.NONE};
        // if only surrounded above and to the right -> prefer a bottom left but settle with a bottom
        if (above && right) return new ModNameExt[]{ModNameExt.bottomleft, ModNameExt.bottom, ModNameExt.NONE};
        // if only surrounded below and to the left -> prefer a top right but settle with a top
        if (below && left) return new ModNameExt[]{ModNameExt.topright, ModNameExt.top, ModNameExt.NONE};
        // if only surrounded below and to the right -> prefer a top left but settle with a top
        if (below && right) return new ModNameExt[]{ModNameExt.topleft, ModNameExt.top, ModNameExt.NONE};
        // if only surrounded above -> prefer a column bottom but settle with a column
        if (above) return new ModNameExt[]{ModNameExt.columnbottom, ModNameExt.column, ModNameExt.NONE};
        // if only surrounded below -> prefer a column top but settle with a column
        if (below) return new ModNameExt[]{ModNameExt.columntop, ModNameExt.column, ModNameExt.NONE};
        // if only surrounded to the left -> prefer a row right cap but settle with a row
        if (left) return new ModNameExt[]{ModNameExt.rowrightcap, ModNameExt.row, ModNameExt.NONE};
        // if only surrounded to the right -> prefer a row left cap but settle with a row
        if (right) return new ModNameExt[]{ModNameExt.rowleftcap, ModNameExt.row, ModNameExt.NONE};
        return new ModNameExt[]{ModNameExt.NONE};
    }

    /**
     * Encapsulates info about a block as laid out in a node-file. When the blocks are actually created, they are
     * reduced to simple pairs representing their position, indexed by material in a map. BlockInfo is solely used for
     * loading purposes
     */
    public static class BlockInfo {

        /**
         * Members
         */
        private final List<String> texPaths = new ArrayList<>(); // list of texture paths to be randomized over
        private float[] color = new float[]{1f, 1f, 1f, 1f};  // block color
        private Material.BlendMode bm = Material.BlendMode.NONE; // how to blend color and texture in the material
        private float frameTime = 1f;                            // how long each frame should be if block is animated
        private int animFrames = 1;                              // how many frames there are if block is animated
        private boolean texResPath = true;                       // whether the texture paths are resource-relative
        private boolean animated = false;                        // whether the block is animated

        /**
         * Constructs the block info by compiling the information from a given node. If the value of the root node
         * starts with the statements 'from' or 'resfrom', the next statement will be assumed to be a different path at
         * which to find the block info. This is useful for reusing the same block info in multiple settings. 'from'
         * assumes the following path is relative to the Ambulare data folder (in the user's home folder) while
         * 'resfrom' assumes the following path is relative to the Ambulares's resource path. Note that these kinds of
         * statements cannot be chained together. Here are a list of children that a block info node can have:
         * <p>
         * - color [optional][default: 1f 1f 1f 1f]: specifies what color to assign to the block
         * <p>
         * - texture_path [optional][default: no texture]: specifies what path to look for textures in. There may be
         * more than one path listed. If this is the gave, a random texture path from the set of given paths will be
         * chosen. For automatic corner/edge detection (modularization), other files can be listed in the same directory
         * with the following naming scheme: [original name]_[corner/edge type].[original extension]. For example, if
         * the default texture is named dirt.png but it is in the top left corner of a group of blocks, an image named
         * dirt_topleft.png will be searched for in the same directory as dirt.png. If a corner/edge piece is not found,
         * the default image will be used. Here is the full list of accepted corner/edge type extensions to file names:
         * topleft, top, topright, right, bottomright, bottom, bottomleft, left, column, row, columntop, columnbottom,
         * rowrightcap, rowleftcap, insettopleft, insettopright, insetbottomleft, insetbottomright.
         * <p>
         * - resource_relative [optional][default: true]: specifies whether the given texture path is relative to
         * Ambulare's resource path. If this is false, the given path must be relative to Ambulare's data folder (in
         * the user's home folder).
         * <p>
         * - blend_mode [optional][default: none]: specifies how to blend color and texture. The options are: (1) none -
         * no blending will occur. The block will appear as its texture if it has one, or its color if there is no
         * texture. (2) multiplicative - the components of the block color and the components of the texture will be
         * multiplied to create a final color. (3) averaged - the components of the block color and the components of
         * the texture will be averaged to create a final color.
         * <p>
         * - animation_frames [optional][default: 1]: specifies how many animation frames are in the texture. Frames
         * should be placed in horizontal order and should be equal widths. If 1 (by default) no animation will occur
         * <p>
         * - animation_time [optional][default: 1.0f]: specifies how long (in seconds) each frame should appear if the
         * block is animated
         * <p>
         * Note that, if any of the info above is improperly formatted, a message saying as much will be logged. As
         * such, when designing blocks to be loaded into the game, the logs should be checked often to make sure the
         * loading process is unfolding correctly
         *
         * @param info the node containing the info to create the corresponding block with
         */
        public BlockInfo(Node info) {

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
                            "BlockInfo", "invalid path in (res)from statement: " + value, false)),
                            "gameobject.gameworld.AreaLayoutLoader", "BlockInfo(Node)", true);
            }

            // parse node
            try { // surround in try/catch so as to intercept and log any issues
                if (!info.hasChildren()) return;
                for (Node c : info.getChildren()) { // for each child
                    String n = c.getName(); // get the name of the child
                    if (n.equals("color")) { // color
                        float[] color = Utils.strToColor(c.getValue()); // try to convert to a float array
                        if (color == null) // log if unsuccessful
                            Utils.log(Utils.getImproperFormatErrorLine("BlockInfo", "color",
                                    "must be four valid floating point numbers separated by spaces",
                                    true), "gameobject.gameworld.AreaLayoutLoader",
                                    "BlockInfo(Node)", false);
                        else this.color = color;
                    } else if (n.equals("texture_path")) this.texPaths.add(c.getValue()); // texture path
                        // resource relative texture path flag
                    else if (n.equals("resource_relative")) this.texResPath = Boolean.parseBoolean(c.getValue());
                    else if (n.equals("blend_mode")) { // blend mode
                        // try to convert to a material's blend mode
                        try {
                            this.bm = Material.BlendMode.valueOf(c.getValue().toUpperCase());
                        } catch (Exception e) { // log if unsuccessful
                            Utils.log(Utils.getImproperFormatErrorLine("BlockInfo", "blend mode",
                                    "must be either: none, multiplicative, or averaged", true),
                                    "gameobject.gameworld.AreaLayoutLoader", "BlockInfo(Node)",
                                    false);
                        }
                    } else if (n.equals("animation_frames")) { // animation frames
                        try {
                            this.animFrames = Integer.parseInt(c.getValue()); // try to convert to integer
                        } catch (Exception e) { // log if unsuccessful
                            Utils.log(Utils.getImproperFormatErrorLine("BlockInfo",
                                    "animation frame count", "must be a proper integer greater than 0",
                                    true), "gameobject.gameworld.AreaLayoutLoader",
                                    "BlockInfo(Node)", false);
                        }
                        if (this.animFrames < 1) { // if the amount of frames is invalid, log and ignore
                            Utils.log(Utils.getImproperFormatErrorLine("BlockInfo",
                                    "animation frame count", "must be a proper integer greater than 0",
                                    true), "gameobject.gameworld.AreaLayoutLoader",
                                    "BlockInfo(Node)", false);
                            this.animFrames = 1;
                        }
                        this.animated = this.animFrames > 1; // update animated flag based on amount of frames
                    } else if (n.equals("animation_time")) { // animation time
                        try {
                            this.frameTime = Float.parseFloat(c.getValue()); // try to convert to a float
                        } catch (Exception e) { // log if unsuccessful
                            Utils.log(Utils.getImproperFormatErrorLine("BlockInfo",
                                    "animation frame time",
                                    "must be a proper floating pointer number greater than 0", true),
                                    "gameobject.gameworld.AreaLayoutLoader", "BlockInfo(Node)",
                                    false);
                        }
                        if (this.frameTime <= 0f) { // if the frame time is invalid, log and ignore
                            Utils.log(Utils.getImproperFormatErrorLine("BlockInfo",
                                    "animation frame time",
                                    "must be a proper floating pointer number greater than 0", true),
                                    "gameobject.gameworld.AreaLayoutLoader", "BlockInfo(Node)",
                                    false);
                            this.frameTime = 1f;
                        }
                    } else { // if an unrecognized child is given, log and ignore
                        Utils.log("Unrecognized block info child: " + n + ". Ignoring",
                                "gameobject.gameworld.AreaLayoutLoader", "BlockInfo(Node)", false);
                    }
                }
            } catch (Exception e) { // if any other exceptions occur, handle them
                Utils.handleException(new Exception(Utils.getImproperFormatErrorLine("BlockInfo",
                        "BlockInfo", e.getMessage(), false)), "gameobject.gameworld.AreaLayoutLoader",
                        "BlockInfo(Node)", true);
            }
        }
    }
}