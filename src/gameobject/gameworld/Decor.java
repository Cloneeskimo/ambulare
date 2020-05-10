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
 * Decor.java
 * Ambulare
 * Jacob Oaks
 * 4/27/2020
 */

/**
 * An abstract class that provides functionality for loading Decor
 */
public abstract class Decor {

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
        Map<Character, DecorInfo> key = parseKeyData(decorKey); // parse key
        /* this is a mapping from decor info and texture to material is used to minimize the amount of materials needed
           to represent the decor. This essentially allows the algorithm to ask, 'does a material already exist for this
           and this texture? If so, use it. If not, create it and then put it in the map to use next time we encounter
           this decor info and texture' */
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
     * @param materialMap the mapping of decor info, texture, and modularization to materials to use
     * @param decor       the decor list to populate
     * @param layout      the layout to load
     * @param key         the decor key
     * @param ats         the list of animated textures to populate
     * @param blockMap    the block map to use for pinning decors - should be the middleground block map
     */
    public static void loadLayoutLayerDecor(Map<DecorInfo, Map<String, Material>> materialMap,
                                            List<GameObject> decor, Node layout,
                                            Map<Character, DecorInfo> key, List<AnimatedTexture> ats,
                                            boolean[][] blockMap) {
        List<Node> rows = layout.getChildren(); // get the rows of the layout
        int diff = blockMap[0].length - rows.size(); // find diff in rows of the current layer and the overall layout
        for (int i = 0; i < rows.size(); i++) { // go through each row
            String row = rows.get(rows.size() - 1 - i).getValue(); // get the row
            int y = i + diff; // the y for this row is i + the difference in rows
            for (int x = 0; x < row.length(); x++) { // loop through each character in the row
                DecorInfo di = key.get(row.charAt(x)); // get the decor info for that character
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
     * Parses decor key data for loading a layout
     *
     * @param keyData the key child node from the area node-file
     * @return the parsed key, mapping from character in the layout to corresponding decor info
     */
    private static Map<Character, DecorInfo> parseKeyData(Node keyData) {
        Map<Character, DecorInfo> key = new HashMap<>(); // start as empty hashmap
        // create a decor info for each child and put it in the key
        for (Node c : keyData.getChildren()) key.put(c.getName().charAt(0), new DecorInfo(c));
        return key; // return the compiled key
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
     * Encapsulates info about a decor as laid out in a node-file. For info on node-files, see utils.Node.java. For info
     * on how to format a decor info node-file, see decor info's constructor. When the corresponding object is created,
     * the decor info is no longer used. Thus, decor info is just used for loading purposes
     */
    private static class DecorInfo {

        /**
         * Members
         */
        public final List<String> texPaths = new ArrayList<>(); // list of texture paths to be randomized over
        public float[] color = new float[]{1f, 1f, 1f, 1f};     // decor color
        public Material.BlendMode bm = Material.BlendMode.NONE; /* how to blend color and texture in the material. For
            info on how colors and textures can be blended, see graphics.Material.java's BlendMode enum */
        public float frameTime = 1f;                            // how long each frame should last if decor is animated
        public int animFrames = 1;                              // how many frames there should be if decor is animated
        public boolean texResPath = true;                       // whether the texture paths are resource-relative
        public boolean animated = false;                        // whether the decor is animated

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
         * statements cannot be chained together. A decor info node can have the following children:
         * <p>
         * - color [optional][default: 1f 1f 1f 1f]: specifies what color to assign to the decor
         * <p>
         * - texture_path [optional][default: no texture]: specifies what path to look for textures in. There may be
         * more than one path listed. If this is the case, a random texture path from the set of given paths will be
         * chosen
         * <p>
         * - resource_relative [optional][default: true]: specifies whether the given texture path is relative to
         * Ambulare's resource path. If this is false, the given path must be relative to Ambulare's data folder (in
         * the user's home folder)
         * <p>
         * - blend_mode [optional][default: none]: specifies how to blend color and texture. The options are: (1) none -
         * no blending will occur. The decor will appear as its texture if it has one, or its color if there is no
         * texture. (2) multiplicative - the components of the decor color and the components of the texture will be
         * multiplied to create a final color. (3) averaged - the components of the decor color and the components of
         * the texture will be averaged to create a final color
         * <p>
         * - animation_frames [optional][default: 1]: specifies how many animation frames are in the texture. Frames
         * should be placed in horizontal order and should be equal widths. If 1 (by default) no animation will occur
         * <p>
         * - animation_time [optional][default: 1.0f]: specifies how long (in seconds) each frame should appear if the
         * decor is animated
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
                            "DecorInfo", "invalid path in (res)from statement: " + value,
                            false)), "gameobject.gameworld.Decor.DecorInfo", "DecorInfo(Node)", true);
            }

            // parse node
            try { // surround in try/catch so as to intercept and log any issues
                if (!info.hasChildren()) return; // if no children, just return with default properties
                for (Node c : info.getChildren()) { // go through each child
                    if (!parseChild(c)) { // parse it
                        Utils.log("Unrecognized child given for decor info:\n" + c + "Ignoring.",
                                "gameobject.gameworld.Decor.DecorInfo", "DecorInfo(Node)",
                                false); // and log it if is not recognized
                    }
                }
            } catch (Exception e) { // if any other exceptions occur
                Utils.handleException(new Exception(Utils.getImproperFormatErrorLine("DecorInfo",
                        "DecorInfo", e.getMessage(), false)),
                        "gameobject.gameworld.Decor.DecorInfo", "DecorInfo(Node)", true); // crash
            }
        }

        /**
         * Parses an individual child of a decor info node and applies the setting it represents to the decor info
         *
         * @param c the child to parse
         * @return whether the child was recognized
         */
        protected boolean parseChild(Node c) {
            String n = c.getName(); // get the name of the child
            if (n.equals("color")) { // color
                float[] color = Utils.strToColor(c.getValue()); // try to convert to a float array of color components
                if (color == null) // if conversion was unsuccessful
                    Utils.log(Utils.getImproperFormatErrorLine("color", "DecorInfo",
                            "must be four valid floating point numbers separated by spaces",
                            true), "gameobject.gameworld.Decor.DecorInfo",
                            "parseChild(Node)", false); // log as much
                else this.color = color; // otherwise save the color
            } else if (n.equals("texture_path")) this.texPaths.add(c.getValue()); // texture path
                // resource relative texture path flag
            else if (n.equals("resource_relative")) this.texResPath = Boolean.parseBoolean(c.getValue());
            else if (n.equals("blend_mode")) { // blend mode
                try {
                    this.bm = Material.BlendMode.valueOf(c.getValue().toUpperCase()); // try to convert to blend mode
                } catch (Exception e) { // if conversion was unsuccessful
                    Utils.log(Utils.getImproperFormatErrorLine("blend_mode", "DecorInfo",
                            "must be either: none, multiplicative, or averaged", true),
                            "gameobject.gameworld.Decor.DecorInfo", "parseChild(Node)",
                            false); // log as much
                }
            } else if (n.equals("animation_frames")) { // animation frames
                try {
                    this.animFrames = Integer.parseInt(c.getValue()); // try to convert to an integer
                } catch (Exception e) { // if conversion was unsuccessful
                    Utils.log(Utils.getImproperFormatErrorLine("animation_frame_count",
                            "DecorInfo", "must be a proper integer greater than 0",
                            true), "gameobject.gameworld.Decor.DecorInfo",
                            "parseChild(Node)", false); // log as much
                }
                if (this.animFrames < 1) { // if the amount of frames is invalid
                    Utils.log(Utils.getImproperFormatErrorLine("animation_frame_count",
                            "DecorInfo", "must be a proper integer greater than 0",
                            true), "gameobject.gameworld.Decor.DecorInfo",
                            "parseChild(Node)", false); // log as much
                    this.animFrames = 1; // and return to default amount of frames
                }
                this.animated = this.animFrames > 1; // update animated flag based on amount of frames
            } else if (n.equals("animation_time")) { // animation time
                try {
                    this.frameTime = Float.parseFloat(c.getValue()); // try to convert to a float
                } catch (Exception e) { // if conversion was unsuccessful
                    Utils.log(Utils.getImproperFormatErrorLine("animation_frame_time",
                            "DecorInfo",
                            "must be a proper floating pointer number greater than 0", true),
                            "gameobject.gameworld.Decor.DecorInfo", "parseChild(Node)",
                            false); // log as much
                }
                if (this.frameTime <= 0f) { // if the frame time is invalid
                    Utils.log(Utils.getImproperFormatErrorLine("animation_frame_time",
                            "DecorInfo",
                            "must be a proper floating pointer number greater than 0", true),
                            "gameobject.gameworld.Decor.DecorInfo", "parseChild(Node)",
                            false); // log as much
                    this.frameTime = 1f; // and return to default frame time
                }
            } else if (n.equals("pin")) { // pin
                String v = c.getValue().toUpperCase(); // get the pin value and set the decor info pin based off it
                if (v.equals("NONE")) this.pin = 0; // none
                else if (v.equals("LEFT")) this.pin = 1; // left
                else if (v.equals("ABOVE")) this.pin = 2; // above
                else if (v.equals("RIGHT")) this.pin = 3; // right
                else if (v.equals("BELOW")) this.pin = 4; // below
                else Utils.log(Utils.getImproperFormatErrorLine("pin", "DecorInfo",
                            "pin must be one of the following: none, left, above, right, or below",
                            true), "gameobject.gameworld.Decor.DecorInfo",
                            "parseChild(c)", false); // if none of the above, log and ignore
            } else if (n.equals("x_offset")) { // horizontal offset
                try {
                    this.xOffset = Float.parseFloat(c.getValue()); // try to convert to a float
                } catch (Exception e) { // if conversion was unsuccessful
                    Utils.log(Utils.getImproperFormatErrorLine("x_offset", "DecorInfo",
                            "must be a proper floating pointer number", true),
                            "gameobject.gameworld.Decor.DecorInfo", "parseChild(Node)",
                            false); // log as much
                }
            } else if (n.equals("y_offset")) { // vertical offset
                try {
                    this.yOffset = Float.parseFloat(c.getValue()); // try to convert to a float
                } catch (Exception e) { // if conversion was unsuccessful
                    Utils.log(Utils.getImproperFormatErrorLine("y_offset", "DecorInfo",
                            "must be a proper floating pointer number", true),
                            "gameobject.gameworld.Decor.DecorInfo", "parseChild(Node)",
                            false); // log as much
                }
            } else if (n.equals("x_random_interval")) { // random horizontal offset
                try {
                    this.xRandInterval = Math.abs(Float.parseFloat(c.getValue())); // try to convert to a float
                } catch (Exception e) { // if conversion was unsuccessful
                    Utils.log(Utils.getImproperFormatErrorLine("x_random_interval", "DecorInfo",
                            "must be a proper floating pointer number", true),
                            "gameobject.gameworld.Decor.DecorInfo", "parseChild(Node)",
                            false); // log as much
                }
            } else if (n.equals("y_random_interval")) { // random vertical offset
                try {
                    this.yRandInterval = Math.abs(Float.parseFloat(c.getValue())); // try to convert to a float
                } catch (Exception e) { // if conversion was unsuccessful
                    Utils.log(Utils.getImproperFormatErrorLine("y_random_interval", "DecorInfo",
                            "must be a proper floating pointer number", true),
                            "gameobject.gameworld.Decor.DecorInfo", "parseChild(Node)",
                            false); // log as much
                }
            } else if (n.equals("x_light_offset")) { // light x offset
                try {
                    this.lightXOffset = Float.parseFloat(c.getValue()); // try to convert to a float
                } catch (Exception e) { // if conversion was unsuccessful
                    Utils.log(Utils.getImproperFormatErrorLine("x_light_offset", "DecorInfo",
                            "must be a proper floating pointer number", true),
                            "gameobject.gameworld.Decor.DecorInfo", "parseChild(Node)",
                            false); // log as much
                }
            } else if (n.equals("y_light_offset")) { // light y offset
                try {
                    this.lightYOffset = Float.parseFloat(c.getValue()); // try to convert to a float
                } catch (Exception e) { // if conversion was unsuccessful
                    Utils.log(Utils.getImproperFormatErrorLine("y_light_offset", "DecorInfo",
                            "must be a proper floating pointer number", true),
                            "gameobject.gameworld.Decor.DecorInfo", "parseChild(Node)",
                            false); // log as much
                }
            } else if (n.equals("light_source")) this.light = new LightSource(c); // light source
            else return false; // if not any of the above, return that the child is unrecognized
            return true; // return true if the child was recognized
        }
    }
}