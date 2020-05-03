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

/*
 * AreaLayoutLoader.java
 * Ambulare
 * Jacob Oaks
 * 4/27/2020
 */

/**
 * This utility class is used for loading various layout-related things for areas. See the area class for more info on
 * what exactly areas hold and how to format area node-files. There is a lot to area layout loading, hence the
 * offloading to static methods in this class instead of clogging the area class with the code
 */
public class AreaLayoutLoader {

    /**
     * Modularization is a word that doesn't really exist but I think it sounds cool so it's what I use for the process
     * by which edges and corners are detected and the textures of the corresponding objects are updated accordingly.
     * This enum defines the different types of modularization and thus the different textures that modularizable
     * objects can provide. Most of them are self-explanatory. Here, insets are defined as blocks that are covered
     * in all eight directions (including diagonals) except for a single diagonal direction
     */
    private enum ModNameExt {
        topleft, top, topright, right, bottomright, bottom, bottomleft, left, column, row, columntop, columnbottom,
        rowrightcap, rowleftcap, insettopleft, insettopright, insetbottomleft, insetbottomright, NONE
    }

    /**
     * Loads blocks of an area's layout for all three layout layers - background, middleground, and foreground.
     *
     * @param blockKey     the block_key child node from the area node-file
     * @param background   the background layout layer child node from the area node-file. May be null if no
     *                     background layout layer was specified for the area
     * @param middleground the middleground layout layer child node from the area node-file. May not be null as a
     *                     middleground layout layer is required for all areas
     * @param foreground   the foreground layout layer child node from the area node-file. May be null if no
     *                     foreground layout layer was specified for the area
     * @param blocks       the material to block maps to populate. Should be a length 3 array where blocks[0] is for
     *                     the background blocks, blocks[1] is for the middleground blocks, and blocks[2] is for the
     *                     foreground blocks
     * @param ats          the list of animated textures to populate
     * @return the block map for the middleground layer to be used for block collision
     */
    public static boolean[][] loadLayoutBlocks(Node blockKey, Node background, Node middleground, Node foreground,
                                               Map<Material, List<Pair<Integer>>>[] blocks, List<AnimatedTexture> ats) {
        // initialize variables and load key
        Map<Character, TileInfo> key = parseKeyData(blockKey, false); // parse block key first
        /* this maps tile info to a mapping of texture to a mapping of modularization piece to materials. This is to
           ensure the least possible amount of material creation. This is confusing what it basically allows the code to
           do is ask: For this tile info, this texture, and this modularization, what material should I use? */
        Map<TileInfo, Map<String, Map<ModNameExt, Material>>> materialMap = new HashMap<>(); // create empty map for now

        /* calculate block map width and height. This will be calculated as the largest width and height of a row/column
           across all provided layers */
        int bmw = 0, bmh = 0; // initialize width and height to zero
        if (background != null) { // if a background was specified
            List<Node> rows = background.getChildren(); // get the rows of the background
            if (rows.size() > bmh) bmh = rows.size(); // if there are more rows, record new height
            // loop through each row and see if a wider row is found
            for (Node node : rows) if (node.getValue().length() > bmw) bmw = node.getValue().length();
        }
        List<Node> rows = middleground.getChildren(); // get the rows of the middle ground
        if (rows.size() > bmh) bmh = rows.size(); // if there are more rows, record new height
        // loop through each row and see if a wider row is found
        for (Node node : rows) if (node.getValue().length() > bmw) bmw = node.getValue().length();
        if (foreground != null) { // if a foreground was specified
            rows = foreground.getChildren(); // get the rows of the foreground
            if (rows.size() > bmh) bmh = rows.size(); // if there are more rows, record new height
            // loop through each row and see if a wider row is found
            for (Node node : rows) if (node.getValue().length() > bmw) bmw = node.getValue().length();
        }

        // load layouts for each layer
        loadLayoutLayerBlocks(materialMap, blocks[0], background, key, ats, bmw, bmh); // load background
        // load middle ground and save the block map for collision
        boolean[][] blockMap = loadLayoutLayerBlocks(materialMap, blocks[1], middleground, key, ats, bmw, bmh);
        loadLayoutLayerBlocks(materialMap, blocks[2], foreground, key, ats, bmw, bmh); // load foreground
        return blockMap; // return the middleground block map to use for collision
    }

    /**
     * Loads blocks of an area's layout for only a single layer
     *
     * @param materialMap the mapping of tile info, texture, and modularization to materials to use
     * @param blocks      the block list to populate
     * @param layout      the layout to load
     * @param key         the block key
     * @param ats         the list of animated textures to populate
     * @param bmw         the width of the block map
     * @param bmh         the height of the block map
     * @return the created and populated block map
     */
    private static boolean[][] loadLayoutLayerBlocks(Map<TileInfo, Map<String, Map<ModNameExt, Material>>> materialMap,
                                                     Map<Material, List<Pair<Integer>>> blocks, Node layout,
                                                     Map<Character, TileInfo> key, List<AnimatedTexture> ats, int bmw,
                                                     int bmh) {

        // populate block map based on where blocks are
        List<Node> rows = layout.getChildren(); // get the rows of the layout
        boolean[][] blockMap = new boolean[bmw][bmh]; // create an appropriately sized block map
        for (int y = 0; y < rows.size(); y++) { // for each row
            String row = rows.get(rows.size() - 1 - y).getValue(); // get the row
            for (int x = 0; x < row.length(); x++) { // for each character in the row
                // if the block info for that character isn't null, then there is a block there, so the update block map
                blockMap[x][y] = (key.get(row.charAt(x))) != null;
            }
        }

        // load blocks
        for (int y = 0; y < rows.size(); y++) { // go through each row
            String row = rows.get(rows.size() - 1 - y).getValue(); // get the row
            for (int x = 0; x < row.length(); x++) { // loop through each character in the row
                TileInfo bi = key.get(row.charAt(x)); // get the tile info for that character
                if (bi != null) { // if the tile info isn't null (there is a block there)

                    // see if the map has materials for that tile info yet. If not, create an empty map for it
                    Map<String, Map<ModNameExt, Material>> forThatTileInfo =
                            materialMap.computeIfAbsent(bi, k -> new HashMap<>());

                    // get the texture to use
                    String texPath = "null"; // if no texture, use "null" string
                    List<String> possibleTextures = bi.texPaths; // get the possible textures
                    if (possibleTextures.size() > 0) { // if there is at least one texture
                        if (possibleTextures.size() > 1) { // if there is more than one texture
                            // choose a random one
                            texPath = possibleTextures.get((int) (Math.random() * possibleTextures.size()));
                        } else texPath = possibleTextures.get(0); // otherwise choose the only one
                    }

                    // see if the map has materials for that texture yet. If not, create an empty map for it
                    Map<ModNameExt, Material> forThatTexture =
                            forThatTileInfo.computeIfAbsent(texPath, k -> new HashMap<>());

                    // apply modularization - if not textured, assume no modularization
                    ModNameExt[] mods = texPath.equals("null") ? new ModNameExt[]{ModNameExt.NONE} :
                            getPreferredModularization(x, y, blockMap); // get preferred modularization
                    boolean materialChosen = false; // no material chosen yet
                    /* getPreferredModularization may return multiple possible types of modularization so here we loop
                       them all and take the first one that has a corresponding texture */
                    for (int i = 0; i < mods.length && !materialChosen; i++) {

                        // see if a material already exists for that kind of modularization
                        Material m = forThatTexture.get(mods[i]);
                        if (m == null) { // if it doesn't yet exist, need to create it
                            if (!texPath.equals("null")) { // if textured
                                // get info for texture path
                                String[] fileInfo = Utils.getFileInfoForPath(bi.texResPath, texPath);
                                // compile the supposed path to the image file for that modularization
                                String path = fileInfo[0] + fileInfo[1] + (mods[i].equals(ModNameExt.NONE)
                                        ? "" : "_" + mods[i].toString()) + fileInfo[2];
                                if (Utils.fileExists(path, bi.texResPath)) { // if the texture for this mod. exists
                                    Texture t; // create the new texture
                                    if (bi.animated) { // if animated
                                        t = new AnimatedTexture(path, bi.texResPath, bi.animFrames, bi.frameTime,
                                                true); // initialize as an animated texture
                                        ats.add((AnimatedTexture) t); // and add to list of animated textures
                                    } else t = new Texture(path, bi.texResPath); // if not, init as regular texture
                                    m = new Material(t, bi.color, bi.bm); // create material with the correct texture
                                    forThatTexture.put(mods[i], m); // put it in the map for this modularization
                                    // create new list for blocks with the corresponding material
                                    blocks.put(m, new ArrayList<>());
                                }
                            } else { // if not textured
                                m = new Material(bi.color); // create material with just color
                                forThatTexture.put(mods[i], m); // put it in the map
                                // create new list for blocks with the corresponding material
                                blocks.put(m, new ArrayList<>());
                            }
                        }
                        if (m != null) { // if a material now exists for that mod
                            blocks.computeIfAbsent(m, k -> new ArrayList<>());
                            blocks.get(m).add(new Pair<>(x, y)); // add the block pos to list for that material
                            materialChosen = true; // flag that a material has been chosen to move onto next block
                        }
                    }
                }
            }
        }
        return blockMap;
    }

    /**
     * Loads decor of an area's layout for all three layout layers - background, middleground, and foreground. Note that
     * all middleground decor will be placed in the background game object list
     *
     * @param decorKey     the decor_key child node from the area node-file
     * @param background   the background layout layer child node from the area node-file. May be null if no
     *                     background layout layer was specified for the area
     * @param middleground the middleground layout layer child node from the area node-file. May not be null as a
     *                     middleground layout layer is required for all areas
     * @param foreground   the foreground layout layer child node from the area node-file. May be null if no
     *                     foreground layout layer was specified for the area
     * @param decor        the two lists of game objects to populate with decor where decor[0] should be background
     *                     decor and decor[1] should be foreground decor
     * @param ats          the list of animated textures to populate
     * @param blockMap     the block map to use for pinning decors - should be the middleground block map
     */
    public static void loadLayoutDecor(Node decorKey, Node background, Node middleground, Node foreground,
                                       List<GameObject>[] decor, List<AnimatedTexture> ats, boolean[][] blockMap) {
        Map<Character, TileInfo> key = parseKeyData(decorKey, true); // parse key
        /* similar to block loading, a mapping from decor info and texture to material is used to minimize the amount of
           materials needed to represent the decor. This essentially allows the algorithm to ask, 'does a material
           already exists for this decor info and this texture? If so, use it. If not, create it and then put it in the
           map to use next time we encounter this decor info and texture' */
        Map<DecorInfo, Map<String, Material>> materialMap = new HashMap<>(); // initialize map as empty at first
        // if there is a background layer, load decor from it
        if (background != null) loadLayoutLayerDecor(materialMap, decor[0], background, key, ats, blockMap);
        // load middleground decor and place it in background
        loadLayoutLayerDecor(materialMap, decor[0], middleground, key, ats, blockMap);
        // if there is a foreground layer, load decor from it
        if (foreground != null) loadLayoutLayerDecor(materialMap, decor[1], foreground, key, ats, blockMap);
    }

    /**
     * Loads decor of an area's layout for only a single layer
     *
     * @param materialMap the mapping of tile info, texture, and modularization to materials to use
     * @param decor       the decor list to populate
     * @param layout      the layout to load
     * @param key         the block key
     * @param ats         the list of animated textures to populate
     * @param blockMap    the block map to use for pinning decors - should be the middleground block map
     */
    public static void loadLayoutLayerDecor(Map<DecorInfo, Map<String, Material>> materialMap,
                                                        List<GameObject> decor, Node layout,
                                                        Map<Character, TileInfo> key, List<AnimatedTexture> ats,
                                                        boolean[][] blockMap) {
        List<Node> rows = layout.getChildren(); // get the rows of the layout

        for (int y = 0; y < rows.size(); y++) { // go through each row
            String row = rows.get(rows.size() - 1 - y).getValue(); // get the row
            for (int x = 0; x < row.length(); x++) { // loop through each character in the row
                DecorInfo di = (DecorInfo) key.get(row.charAt(x)); // get the decor info for that character
                if (di != null) { // if there is decor there

                    // see if materials exists for that decor info yet. If not, create a new map for them
                    Map<String, Material> forThatDecorInfo = materialMap.computeIfAbsent(di, k -> new HashMap<>());

                    // get the texture (or lack thereof)
                    String texPath = "null"; // if no texture, use "null" string
                    List<String> possibleTextures = di.texPaths; // get the possible textures
                    if (possibleTextures.size() > 0) { // if there is at least one texture
                        if (possibleTextures.size() > 1) { // if there is more than one texture
                            // choose a random one
                            texPath = possibleTextures.get((int) (Math.random() * possibleTextures.size()));
                        } else texPath = possibleTextures.get(0); // otherwise choose the only one
                    }

                    // get the material
                    Material m = forThatDecorInfo.get(texPath); // see if a material exists for that texture yet
                    if (m == null) { // if the material doesn't exist yet, need to create it
                        if (!texPath.equals("null")) { // if textured
                            Texture t; // create the texture
                            if (di.animated) { // if animated
                                t = new AnimatedTexture(texPath, di.texResPath, di.animFrames, di.frameTime,
                                        true); // initialize as an animated texture
                                ats.add((AnimatedTexture) t); // add to list of animated textures
                            } else t = new Texture(texPath, di.texResPath); // if not, initialize as a regular texture
                            if (di.light != null) { // if the decor should emit light
                                m = new LightSourceMaterial(t, di.color, di.bm, di.light); // create material with light
                                ((LightSourceMaterial) m).setOffset(di.lightXOffset, di.lightYOffset); // add offset
                            } else m = new Material(t, di.color, di.bm); // otherwise create as a normal material
                        } else { // if no texture create the material with just color
                            if (di.light != null) { // if the decor should emit light
                                m = new LightSourceMaterial(di.color, di.light); // create material with light
                                ((LightSourceMaterial) m).setOffset(di.lightXOffset, di.lightYOffset); // add offset
                            } else m = new Material(di.color); // otherwise create as a normal material
                        }
                        forThatDecorInfo.put(texPath, m); // put it in the map for that texture/color
                    }

                    // create a game object using the material
                    GameObject go = new GameObject(Model.getStdGridRect(1, 1), m);
                    go.setScale(m.getTexture().getWidth() / 32f, m.getTexture().getHeight() / 32f);

                    // pin the game object according to the deco info's pin options (if there os a pin)
                    Pair<Integer> fbid = di.pin == 0 ? new Pair<>(x, y) : lastFreeBlockInDirection(blockMap, x, y,
                            di.pin == 1 ? -1 : (di.pin == 3 ? 1 : 0),
                            di.pin == 2 ? 1 : (di.pin == 4 ? -1 : 0)
                    ); // get the nearest grid cell not containing a block in the direction of the pin
                    Pair<Float> pos = Transformation.getCenterOfCell(fbid); // get the center of the cell
                    switch (di.pin) { // switch on the pin and translate the object accordingly
                        case 1: // left
                            pos.x += (go.getWidth() / 2) - 0.5f;
                            break;
                        case 2: // above
                            pos.y += -(go.getHeight() / 2) + 0.5f;
                            break;
                        case 3: // right
                            pos.x += -(go.getWidth() / 2) + 0.5f;
                            break;
                        case 4: // below
                            pos.y += (go.getHeight() / 2) - 0.5f;
                    }

                    // apply offset and set final position
                    pos.x += di.xOffset; // apply horizontal offset
                    pos.y += di.yOffset; // apply vertical offset
                    if (di.xRandInterval != 0f) { // apply random horizontal offset
                        pos.x += (float) (Math.random() * 2 * di.xRandInterval) - di.xRandInterval;
                    }
                    if (di.yRandInterval != 0f) { // apply random vertical offset
                        pos.y += (float) (Math.random() * 2 * di.yRandInterval) - di.yRandInterval;
                    }
                    go.setPos(pos); // set the updated position for the decor
                    decor.add(go); // add to the decor list
                }
            }
        }
    }

    /**
     * Parses key data for loading a layout
     *
     * @param keyData the key child node from the area node-file
     * @param decor   whether the key is for decor (if false, the method assumes the key is for blocks)
     * @return the parsed key, mapping from character in the layout to corresponding tile info
     */
    private static Map<Character, TileInfo> parseKeyData(Node keyData, boolean decor) {
        Map<Character, TileInfo> key = new HashMap<>(); // start as empty hashmap
        // create a tile info for each child and put it in the key
        for (Node c : keyData.getChildren()) key.put(c.getName().charAt(0), decor ? new DecorInfo(c) : new TileInfo(c));
        return key; // return the compiled key
    }

    /**
     * Given the block map and a position to consider, will calculate a list of kinds of modularization in order of
     * preference
     *
     * @param x        the x of the grid cell to consider
     * @param y        the y of the grid cell to consider
     * @param blockMap the block map to use for edge/corner/inset detection
     * @return an array containing kinds of modularization in preferred order. The last one in the list will always be
     * "NONE" signifying to just use the default texture if none of the predecessors work
     */
    private static ModNameExt[] getPreferredModularization(int x, int y, boolean[][] blockMap) {
        boolean left = x > 0 && (blockMap[x - 1][y]); // determine if a block exists to the left
        boolean right = x < blockMap.length - 1 && (blockMap[x + 1][y]); // determine if a block exists to the right
        boolean below = y > 0 && (blockMap[x][y - 1]); // determine if a block exists below
        boolean above = y < blockMap[0].length - 1 && (blockMap[x][y + 1]); // determine if a block exists above
        // if surrounded on all four sides, calculate diagonals for inset modularization
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
        // if none of the above kinds of modularization fit, just return no modularization
        return new ModNameExt[]{ModNameExt.NONE};
    }

    /**
     * Calculates the last free position (containing no block) in the given direction from the given starting block
     *
     * @param blockMap the block map to use for checking
     * @param x        the starting x
     * @param y        the starting y
     * @param dx       the change in x for the direction
     * @param dy       the change in y for the direction
     * @return a pair of integers containing the last free position (containing no block) in the given direction. This
     * will not take into account whether the starting position itself is free
     */
    private static Pair<Integer> lastFreeBlockInDirection(boolean[][] blockMap, int x, int y, int dx, int dy) {
        while (!blockMap[x + dx][y + dy]) { // while there is no block in the given direction
            // keep going in that direction
            x += dx;
            y += dy;
            // if the next move is out of bounds, break from the loop and declare this as the last free block
            if (x + dx < 0 || x + dx >= blockMap.length) break;
            if (y + dy < 0 || y + dy >= blockMap[0].length) break;
        }
        return new Pair<>(x, y); // return last free block
    }

    /**
     * Encapsulates info about a tile as laid out in a node-file. For info on node-files, see utils.Node.java. For info
     * on how to format a tile info node-file, see tile info's constructor. When the corresponding object is created,
     * the TileInfo is no longer used. Thus, TileInfo is just used for loading purposes
     */
    private static class TileInfo {

        /**
         * Members
         */
        public final List<String> texPaths = new ArrayList<>(); // list of texture paths to be randomized over
        public float[] color = new float[]{1f, 1f, 1f, 1f};     // tile color
        public Material.BlendMode bm = Material.BlendMode.NONE; /* how to blend color and texture in the material. For
            info on how colors and textures can be blended, see graphics.Material.java's BlendMode enum */
        public float frameTime = 1f;                            // how long each frame should last if tile is animated
        public int animFrames = 1;                              // how many frames there should be if tile is animated
        public boolean texResPath = true;                       // whether the texture paths are resource-relative
        public boolean animated = false;                        // whether the tile is animated

        /**
         * Constructs the tile info by compiling the information from a given node. If the value of the root node
         * starts with the statements 'from' or 'resfrom', the next statement will be assumed to be a different path at
         * which to find the tile info. This is useful for reusing the same tile info in multiple settings. 'from'
         * assumes the following path is relative to the Ambulare data folder (in the user's home folder) while
         * 'resfrom' assumes the following path is relative to the Ambulares's resource path. Note that these kinds of
         * statements cannot be chained together. A tile info node can have the following children:
         * <p>
         * - color [optional][default: 1f 1f 1f 1f]: specifies what color to assign to the tile
         * <p>
         * - texture_path [optional][default: no texture]: specifies what path to look for textures in. There may be
         * more than one path listed. If this is the case, a random texture path from the set of given paths will be
         * chosen. For automatic corner/edge detection (modularization), other files can be listed in the same directory
         * with the following naming scheme: [original name]_[corner/edge type].[original extension]. For example, if
         * the default texture is named dirt.png but it is in the top left corner of a group of blocks, an image named
         * dirt_topleft.png will be searched for in the same directory as dirt.png. If a corner/edge piece is not found,
         * the default image will be used. Here is the full list of accepted corner/edge type extensions to file names:
         * topleft, top, topright, right, bottomright, bottom, bottomleft, left, column, row, columntop, columnbottom,
         * rowrightcap, rowleftcap, insettopleft, insettopright, insetbottomleft, insetbottomright. Corner/edge
         * detection (modularization) only occurs when loading blocks (not decor). For more information on
         * modularization, see gameobject.gameworld.AreaLayoutLoader.java's ModNameExt enum
         * <p>
         * - resource_relative [optional][default: true]: specifies whether the given texture path is relative to
         * Ambulare's resource path. If this is false, the given path must be relative to Ambulare's data folder (in
         * the user's home folder)
         * <p>
         * - blend_mode [optional][default: none]: specifies how to blend color and texture. The options are: (1) none -
         * no blending will occur. The tile will appear as its texture if it has one, or its color if there is no
         * texture. (2) multiplicative - the components of the tile color and the components of the texture will be
         * multiplied to create a final color. (3) averaged - the components of the tile color and the components of
         * the texture will be averaged to create a final color
         * <p>
         * - animation_frames [optional][default: 1]: specifies how many animation frames are in the texture. Frames
         * should be placed in horizontal order and should be equal widths. If 1 (by default) no animation will occur
         * <p>
         * - animation_time [optional][default: 1.0f]: specifies how long (in seconds) each frame should appear if the
         * tile is animated
         * <p>
         * Note that, if any of the info above is improperly formatted, a message saying as much will be logged. As
         * such, when designing tiles to be loaded into the game, the logs should be checked often to make sure the
         * loading process is unfolding correctly
         *
         * @param info the node containing the info to create the tile info with
         */
        public TileInfo(Node info) {

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
                            "TileInfo", "invalid path in (res)from statement: " + value, false)),
                            "gameobject.gameworld.AreaLayoutLoader", "TileInfo(Node)", true);
            }

            // parse node
            try { // surround in try/catch so as to intercept and log any issues
                if (!info.hasChildren()) return; // if no children, just return with default properties
                for (Node c : info.getChildren()) { // go through each child
                    if (!parseChild(c)) { // parse it
                        Utils.log("Unrecognized child given for tile info:\n" + c + "Ignoring.",
                                "gameobject.gameworld.AreaLayoutLoader", "TileInfo(Node)",
                                false); // and log it if is not recognized
                    }
                }
            } catch (Exception e) { // if any other exceptions occur, handle them
                Utils.handleException(new Exception(Utils.getImproperFormatErrorLine("TileInfo",
                        "TileInfo", e.getMessage(), false)), "gameobject.gameworld.AreaLayoutLoader",
                        "TileInfo(Node)", true); // unrecognized exceptions cause crashes
            }
        }

        /**
         * Parses an individual child of a tile info node and applies the setting it represents to the tile info
         *
         * @param c the child to parse
         * @return whether the child was recognized
         */
        protected boolean parseChild(Node c) {
            String n = c.getName(); // get the name of the child
            if (n.equals("color")) { // color
                float[] color = Utils.strToColor(c.getValue()); // try to convert to a float array of color components
                if (color == null) // if conversion was unsuccessful
                    Utils.log(Utils.getImproperFormatErrorLine("color", "TileInfo",
                            "must be four valid floating point numbers separated by spaces",
                            true), "gameobject.gameworld.AreaLayoutLoader",
                            "parseChild(Node)", false); // log as much
                else this.color = color; // otherwise save the color
            } else if (n.equals("texture_path")) this.texPaths.add(c.getValue()); // texture path
                // resource relative texture path flag
            else if (n.equals("resource_relative")) this.texResPath = Boolean.parseBoolean(c.getValue());
            else if (n.equals("blend_mode")) { // blend mode
                try { // try to convert to a material's blend mode
                    this.bm = Material.BlendMode.valueOf(c.getValue().toUpperCase());
                } catch (Exception e) { // if conversion was unsuccessful
                    Utils.log(Utils.getImproperFormatErrorLine("blend mode", "TileInfo",
                            "must be either: none, multiplicative, or averaged", true),
                            "gameobject.gameworld.AreaLayoutLoader", "parseChild(Node)",
                            false); // log as much
                }
            } else if (n.equals("animation_frames")) { // animation frames
                try { // try to convert to an integer
                    this.animFrames = Integer.parseInt(c.getValue());
                } catch (Exception e) { // if conversion was unsuccessful
                    Utils.log(Utils.getImproperFormatErrorLine("animation frame count",
                            "TileInfo", "must be a proper integer greater than 0",
                            true), "gameobject.gameworld.AreaLayoutLoader",
                            "parseChild(Node)", false); // log as much
                }
                if (this.animFrames < 1) { // if the amount of frames is invalid
                    Utils.log(Utils.getImproperFormatErrorLine("animation frame count",
                            "TileInfo", "must be a proper integer greater than 0",
                            true), "gameobject.gameworld.AreaLayoutLoader",
                            "parseChild(Node)", false); // log as much
                    this.animFrames = 1; // and return to default amount of frames
                }
                this.animated = this.animFrames > 1; // update animated flag based on amount of frames
            } else if (n.equals("animation_time")) { // animation time
                try { // try to convert to a float
                    this.frameTime = Float.parseFloat(c.getValue());
                } catch (Exception e) { // if conversion was unsuccessful
                    Utils.log(Utils.getImproperFormatErrorLine("animation frame time",
                            "TileInfo",
                            "must be a proper floating pointer number greater than 0", true),
                            "gameobject.gameworld.AreaLayoutLoader", "parseChild(Node)",
                            false); // log as much
                }
                if (this.frameTime <= 0f) { // if the frame time is invalid
                    Utils.log(Utils.getImproperFormatErrorLine("animation frame time",
                            "TileInfo",
                            "must be a proper floating pointer number greater than 0", true),
                            "gameobject.gameworld.AreaLayoutLoader", "parseChild(Node)",
                            false); // log as much
                    this.frameTime = 1f; // and return to default frame time
                }
            } else return false; // return false if unrecognized child
            return true; // return true if the child was recognized
        }
    }

    /**
     * Extends TileInfo by providing additional customization for designing decor in an area's layout. For specifics,
     * see DecorInfo's members and constructor
     */
    private static class DecorInfo extends TileInfo {

        /**
         * Members
         */
        private float xOffset, yOffset;             // offset values for placement (in units of grid cells)
        private float xRandInterval, yRandInterval; // intervals for random additional offset values for placement
        private int pin;                            /* defines how the decor pins to nearby blocks. The following values
                                                       can be used: (0) - none, (1) - left, (2) - above, (3) - right,
                                                       (4) - below */
        private LightSource light;                  // a light source if the decor should emit light
        private float lightXOffset;                 // x offset of the light if the decor should emit light
        private float lightYOffset;                 // y offset of the light if the decor should emit light

        /**
         * Constructs the decor info by compiling the information from a given node. If the value of the root node
         * starts with the statements 'from' or 'resfrom', the next statement will be assumed to be a different path at
         * which to find the decor info. This is useful for reusing the same decor info in multiple settings. 'from'
         * assumes the following path is relative to the Ambulare data folder (in the user's home folder) while
         * 'resfrom' assumes the following path is relative to the Ambulares's resource path. Note that these kinds of
         * statements cannot be chained together. A decor info node can have any child that a tile info can have as
         * well as:
         * <p>
         * - pin [optional][default: none]: defines how the object will pin to nearby blocks. The options are as
         * follows: right, left, above, below, none. If none, the object will be centered exactly at the position it's
         * character is at in the layout. For any of the other options, it will pin to the nearest block in that
         * direction. For example, if set to below, the object will be on the ground/floor beneath it
         * <p>
         * - x_offset [optional][default: 0f]: defines the horizontal offset to use when placing the object (in amount
         * of blocks). Positive values correspond to moving the decor to the right while negative values correspond to
         * moving the decor to the left. For example, an xoffset of -0.33f will move the decor 1/3 of a block to the
         * left
         * <p>
         * - y_offset [optional][default: 0f]: defines the vertical offset to use when placing the object (in amount of
         * blocks). Positive values correspond to moving the object up while negative values correspond to moving the
         * object down. For example, a yoffset of 1.4f will move the decor 1.4 blocks upwards
         * <p>
         * - x_random_interval [optional][default: 0f]: defines the horizontal interval from which a random offset value
         * will be chosen and applied in addition to x_offset. For example, an x_random_interval of 0.5f would generate
         * a random offset between -0.5f and 0.5f to apply on top of x_offset. Sign does not matter here
         * <p>
         * - y_random_interval [optional][default: 0f]: defines the vertical interval from which a random offset value
         * will be chosen and applied in addition to y_offset. For example, a y_random_inteval of -0.25f would generate
         * a random offset between -0.25f and 0.25f to apply on top of y_offset. Sign does not matter here
         * <p>
         * - light_source [optional][default: none]: defines a light source that should come from the decor. This node
         * should have a child formatted as a light source node
         * <p>
         * - x_light_offset [optional][default: 0f]: defines the horizontal offset, in blocks, of the center of the
         * light source if the decor has one. For example, -1f will place the center of the light one block to the left
         * of the center of the decor itself
         * <p>
         * - y_light_offset [optional][default: 0f]: defines the vertical offset, in blocks, of the center of the light
         * source if the decor has one. For example, .6f will place the center of the light 6/10 of a block above the
         * center of the decor itself
         * <p>
         * Note that, if any of the info above is improperly formatted, a message saying as much will be logged. As
         * such, when designing decor to be loaded into the game, the logs should be checked often to make sure the
         * loading process is unfolding correctly
         *
         * @param info the node containing the info to create the decor info with
         */
        public DecorInfo(Node info) {
            super(info);
        }

        /**
         * Parses an individual child and applies the setting it represents to the decor info
         *
         * @param c the child to parse
         * @return whether the child was recognized
         */
        @Override
        protected boolean parseChild(Node c) {
            if (!super.parseChild(c)) { // first try to parse as a tile info child
                String n = c.getName(); // get name of the child
                if (n.equals("pin")) { // pin
                    String v = c.getValue().toUpperCase(); // get the pin value and set the decor info pin based off it
                    if (v.equals("NONE")) this.pin = 0; // none
                    else if (v.equals("LEFT")) this.pin = 1; // left
                    else if (v.equals("ABOVE")) this.pin = 2; // above
                    else if (v.equals("RIGHT")) this.pin = 3; // right
                    else if (v.equals("BELOW")) this.pin = 4; // below
                    else Utils.log(Utils.getImproperFormatErrorLine("pin", "DecorInfo",
                                "pin must be one of the following: none, left, above, right, or below",
                                true), "gameobject.gameworld.AreaLayoutLoader", "parseChild(c)",
                                false); // if none of the above, log and ignore
                } else if (n.equals("x_offset")) { // horizontal offset
                    try { // try to convert to a float
                        this.xOffset = Float.parseFloat(c.getValue());
                    } catch (Exception e) { // if conversion was unsuccessful
                        Utils.log(Utils.getImproperFormatErrorLine("x_offset", "DecorInfo",
                                "must be a proper floating pointer number", true),
                                "gameobject.gameworld.AreaLayoutLoader", "parseChild(Node)",
                                false); // log as much
                    }
                } else if (n.equals("y_offset")) { // vertical offset
                    try { // try to convert to a float
                        this.yOffset = Float.parseFloat(c.getValue());
                    } catch (Exception e) { // if conversion was unsuccessful
                        Utils.log(Utils.getImproperFormatErrorLine("y_offset", "DecorInfo",
                                "must be a proper floating pointer number", true),
                                "gameobject.gameworld.AreaLayoutLoader", "parseChild(Node)",
                                false); // log as much
                    }
                } else if (n.equals("x_random_interval")) { // random horizontal offset
                    try { // try to convert to a float
                        this.xRandInterval = Math.abs(Float.parseFloat(c.getValue()));
                    } catch (Exception e) { // if conversion was unsuccessful
                        Utils.log(Utils.getImproperFormatErrorLine("x_random_interval", "DecorInfo",
                                "must be a proper floating pointer number", true),
                                "gameobject.gameworld.AreaLayoutLoader", "parseChild(Node)",
                                false); // log as much
                    }
                } else if (n.equals("y_random_interval")) { // random vertical offset
                    try { // try to convert to a float
                        this.yRandInterval = Math.abs(Float.parseFloat(c.getValue()));
                    } catch (Exception e) { // if conversion was unsuccessful
                        Utils.log(Utils.getImproperFormatErrorLine("y_random_interval", "DecorInfo",
                                "must be a proper floating pointer number", true),
                                "gameobject.gameworld.AreaLayoutLoader", "parseChild(Node)",
                                false); // log as much
                    }
                } else if (n.equals("x_light_offset")) { // light x offset
                    try { // try to convert to a float
                        this.lightXOffset = Float.parseFloat(c.getValue());
                    } catch (Exception e) { // if conversion was unsuccessful
                        Utils.log(Utils.getImproperFormatErrorLine("x_light_offset", "DecorInfo",
                                "must be a proper floating pointer number", true),
                                "gameobject.gameworld.AreaLayoutLoader", "parseChild(Node)",
                                false); // log as much
                    }
                } else if (n.equals("y_light_offset")) { // light y offset
                    try { // try to convert to a float
                        this.lightYOffset = Float.parseFloat(c.getValue());
                    } catch (Exception e) { // if conversion was unsuccessful
                        Utils.log(Utils.getImproperFormatErrorLine("y_light_offset", "DecorInfo",
                                "must be a proper floating pointer number", true),
                                "gameobject.gameworld.AreaLayoutLoader", "parseChild(Node)",
                                false); // log as much
                    }
                } else if (n.equals("light_source")) this.light = new LightSource(c); // light source
                else return false; // if not any of the above, return that the child is unrecognized
                return true; // return that it was recognized
            } else return true; // if tile info class recognized it, return true
        }
    }
}