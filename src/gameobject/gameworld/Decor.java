package gameobject.gameworld;

import gameobject.GameObject;
import graphics.*;
import utils.*;

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
     * Static Data
     */
    private static final int NO_TEXTURE = -1;              // used to denote that a block info is not textured

    /**
     * Loads decor of an area's layout for all three layout layers - background, middleground, and foreground. Note that
     * all middleground decor will be placed in the background decor lists
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
        Map<List<Object>, Material> mm = new HashMap<>(); /* maps from a list of properties of a decor to a
            corresponding material. This is used to maintain high space efficiency and low memory usage by minimizing
            the amount of materials needed, as opposed to creating a new material for each decor. This works because
            lists are hashed based off of their contained values rather than their references. The order of objects that
            are entered into this map is as follows:
            [0] - [DecorInfo]: decor information
            [1] - [Utils.Path]: texture path (or NO_TEXTURE if the block info is not textured). This requires a separate
            field because some block information specifies multiple textures to be randomized over */

        // load decor from all three layers, putting middleground decor into the background
        if (background != null) loadLayoutLayerDecor(mm, decor[0], background, key, ats, blockMap); // load background
        loadLayoutLayerDecor(mm, decor[0], middleground, key, ats, blockMap); // load middleground decor, place in back
        if (foreground != null) loadLayoutLayerDecor(mm, decor[1], foreground, key, ats, blockMap); // load foreground

        // log decor loading metrics
        int totalDecor = decor[0].size() + decor[1].size(); // count total decor
        Utils.log("Finished loading decor layout with:\n" + mm.values().size() + " resulting material instances\n"
                + totalDecor + " total decor", Block.class, "loadLayoutDecor", false); // log metrics
    }

    /**
     * Loads decor of an area's layout for only a single layer
     *
     * @param mm       the material map to draw from and populate. See loadLayoutDecor() for more information
     * @param decor    the decor list to populate
     * @param layout   the layout to load from
     * @param key      the decor key to use
     * @param ats      the list of animated textures to populate
     * @param blockMap the block map to use for pinning decors - should be the middleground block map
     */
    public static void loadLayoutLayerDecor(Map<List<Object>, Material> mm, List<GameObject> decor,
                                            Node layout, Map<Character, DecorInfo> key, List<AnimatedTexture> ats,
                                            boolean[][] blockMap) {
        List<Node> rows = layout.getChildren(); // get the rows of the layout
        int diff = blockMap[0].length - rows.size(); // find diff in rows of the current layer and the overall layout
        for (int i = 0; i < rows.size(); i++) { // go through each row
            String row = rows.get(rows.size() - 1 - i).getValue(); // get the row
            int y = i + diff; // the y for this row is i + the difference in rows
            for (int x = 0; x < row.length(); x++) { // loop through each character in the row
                DecorInfo di = key.get(row.charAt(x)); // get the decor info for that character
                if (di != null) { // if there is decor there

                    // create material map key to populate to check if corresponding material exists yet
                    List<Object> mmKey = new ArrayList<>(); // create new list of objects to serve as a key in mat map
                    mmKey.add(di); // add decor info to key
                    if (di.texturePaths.size() > 0) mmKey.add(di.texturePaths.get((int) (Math.random() *
                            di.texturePaths.size()))); // if there are textures, choose a random one to use
                    else mmKey.add(NO_TEXTURE); // otherwise use the no texture flag to denote a lack of textures

                    // get the material
                    Material m = mm.get(mmKey); // see if the corresponding material exists yet
                    if (m == null) { // if the material doesn't exist yet, need to create it
                        Texture t = null; // create texture reference as null for now
                        if (di.texturePaths.size() > 0) { // if the decor is textured
                            t = new Texture((Utils.Path) mmKey.get(1)); // create the texture
                            // apply animation if the block is animated
                            if (di.animated()) t = t.animate(di.animFrames, di.animTime, true);
                        }
                        if (di.light != null) { // if the decor should emit light
                            m = new LightSourceMaterial(t, di.color, di.bm, di.light); // create material with light
                            ((LightSourceMaterial) m).setOffset(di.lightXOffset, di.lightYOffset); // add light offset
                        } else m = new Material(t, di.color, di.bm); // otherwise create a normal material
                        mm.put(mmKey, m); // save in material map
                    }

                    // create a game object using the material, scaling it to a 32x32 bit resolution per grid cell
                    GameObject go = new GameObject(Model.getStdGridRect(1, 1), m);
                    if (m.isTextured()) go.setScale(m.getTexture().getWidth() / 32f, m.getTexture().getHeight() / 32f);

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
                    if (di.xRandInterval != 0f) // apply random horizontal offset
                        pos.x += (float) (Math.random() * 2 * di.xRandInterval) - di.xRandInterval;
                    if (di.yRandInterval != 0f) // apply random vertical offset
                        pos.y += (float) (Math.random() * 2 * di.yRandInterval) - di.yRandInterval;
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
        public final List<Utils.Path> texturePaths = new ArrayList<>(); // list of texture paths to be randomized over
        public float[] color = new float[]{1f, 1f, 1f, 1f};             // decor color
        public Material.BlendMode bm = Material.BlendMode.NONE;         // how to blend color and texture
        public float animTime;                                          // frame length if animated
        public int animFrames;                                          // frame count if animated
        private float xOffset, yOffset;                                 // offset values for placement (in grid cells)
        private float xRandInterval, yRandInterval;                     // intervals for random additional offset
        private int pin;                                                /* defines how the decor pins to nearby blocks.
                                                                           The following values can be used: (0) - none,
                                                                           (1) - left, (2) - above, (3) - right,
                                                                           (4) - below */
        private LightSource light;                                      // a light source if the decor should emit light
        private float lightXOffset;                                     // x offset of the light source if exists
        private float lightYOffset;                                     // y offset of the light source if exists

        /**
         * Constructs the decor info by compiling the information from a given node. DecorInfo nodes can use (res)from
         * statements. See utils.NodeLoader for more info on res(from) statements. A decor info node can have the
         * following children:
         * <p>
         * - color [optional][default: 1f 1f 1f 1f]: specifies what color to assign to the decor
         * <p>
         * - texture_paths [optional][default: no texture]: specifies what paths to look for textures at. This node
         * itself should have one or more children nodes formatted as path nodes. If more than one texture path is
         * specified, a random one will be chosen when the corresponding decor is created. See utils.Utils.Path for
         * more information on path nodes
         * <p>
         * - blend_mode [optional][default: none]: specifies how to blend color and texture. The options are: (1) 'none'
         * - no blending will occur. The appearance will simple be the texture if there is one, or the color if there is
         * no texture. (2) 'multiplicative' - the components of the color and the components of the texture will be
         * multiplied to create a final color. (3) 'averaged' - the components of the color and the components of the
         * texture will be averaged to create a final color
         * <p>
         * - animation_frames [optional][default: 1][1, 30]: specifies how many animation frames are in the texture.
         * Frames should be placed in horizontal order and should be equal widths. If 1 (by default) no animation will
         * occur
         * <p>
         * - animation_time [optional][default: 1.0f][0.01f, INFINITY]: specifies how long (in seconds)
         * each frame should appear if the decor is animated
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
         * should be formatted as a light source node
         * <p>
         * - x_light_offset [optional][default: 0f]: defines the horizontal offset, in blocks, of the center of the
         * light source if the decor has one. For example, -1f will place the center of the light one block to the left
         * of the center of the decor itself
         * <p>
         * - y_light_offset [optional][default: 0f]: defines the vertical offset, in blocks, of the center of the light
         * source if the decor has one. For example, .6f will place the center of the light 6/10 of a block above the
         * center of the decor itself
         *
         * @param data the node to use to construct the decor info
         */
        public DecorInfo(Node data) {

            /*
             * Load decor information using node loader
             */
            data = NodeLoader.checkForFromStatement("DecorInfo", data);
            Map<String, Object> decorInfo = NodeLoader.loadFromNode("DecorInfo", data,
                    new NodeLoader.LoadItem[]{
                            new NodeLoader.LoadItem<>("texture_paths", null, Node.class)
                                    .useTest((v, sb) -> {
                                boolean issue = false;
                                for (Node child : ((Node) v).getChildren()) {
                                    Utils.Path p = new Utils.Path(child);
                                    if (!p.exists()) {
                                        sb.append("Texture at path does not exist: '").append(p).append('\n');
                                        issue = true;
                                    } else this.texturePaths.add(p);
                                }
                                return !issue;
                            }),
                            new NodeLoader.LoadItem<>("color", "1f 1f 1f 1f", String.class)
                                    .useTest((v, sb) -> {
                                float[] c = Utils.strToColor(v);
                                if (c == null) {
                                    sb.append("Must be four valid rgba float values separated by a space");
                                    sb.append("\nFor example: '1f 0f 1f 0.5' for a half-transparent purple");
                                    return false;
                                }
                                this.color = c;
                                return true;
                            }),
                            new NodeLoader.LoadItem<>("blend_mode", "none", String.class)
                                    .setAllowedValues(new String[]{"none", "multiplicative", "averaged"}),
                            new NodeLoader.LoadItem<>("animation_frames", 1, Integer.class)
                                    .setLowerBound(1).setUpperBound(30),
                            new NodeLoader.LoadItem<>("animation_time", 1f, Float.class)
                                    .setLowerBound(0.01f),
                            new NodeLoader.LoadItem<>("pin", "none", String.class)
                                    .setAllowedValues(new String[]{"none", "left", "right", "above", "below"}),
                            new NodeLoader.LoadItem<>("x_offset", 0f, Float.class),
                            new NodeLoader.LoadItem<>("y_offset", 0f, Float.class),
                            new NodeLoader.LoadItem<>("x_random_interval", 0f, Float.class),
                            new NodeLoader.LoadItem<>("y_random_interval", 0f, Float.class),
                            new NodeLoader.LoadItem<>("light_source", null, Node.class),
                            new NodeLoader.LoadItem<>("x_light_offset", 0f, Float.class),
                            new NodeLoader.LoadItem<>("y_light_offset", 0f, Float.class)
                    });

            /*
             * Apply loaded information
             */
            // save blend mode as member
            this.bm = Material.BlendMode.valueOf(((String) decorInfo.get("blend_mode")).toUpperCase());
            this.animFrames = (Integer) decorInfo.get("animation_frames"); // save animation frames as member
            this.animTime = (Float) decorInfo.get("animation_time"); // save animation time as member
            String pin = (String) decorInfo.get("pin"); // get pin info and set pin integer flag based on string value
            if (pin.equals("left")) this.pin = 1; // left -> 1
            else if (pin.equals("above")) this.pin = 2; // above -> 2
            else if (pin.equals("right")) this.pin = 3; // right -> 3
            else if (pin.equals("below")) this.pin = 4; // below -> 4
            this.xOffset = (Float) decorInfo.get("x_offset"); // save x offset as member
            this.yOffset = (Float) decorInfo.get("y_offset"); // save y offset as member
            this.xRandInterval = (Float) decorInfo.get("x_random_interval"); // save x random interval as member
            this.yRandInterval = (Float) decorInfo.get("y_random_interval"); // save y random interval as member
            Node lightSource = (Node) decorInfo.get("light_source"); // get light source info
            if (lightSource != null) this.light = new LightSource(lightSource); // if there was light info, create light
            this.lightXOffset = (Float) decorInfo.get("x_light_offset"); // save x light offset as member
            this.lightYOffset = (Float) decorInfo.get("y_light_offset"); // save y light offset as member
        }

        /**
         * @return whether the corresponding decor is animated (whether there is more than one animation frame)
         */
        public boolean animated() {
            return this.animFrames > 1;
        }
    }
}