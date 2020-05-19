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
     * @param gates        a list of gates to populate
     * @param ats          the list of animated textures to populate
     * @param blockMap     the block map to use for pinning decors
     * @param slopeMap     the slope map to use for pinning decor
     */
    public static void loadLayoutDecor(Node decorKey, Node background, Node middleground, Node foreground,
                                       List<GameObject>[] decor, List<Area.Gate> gates, List<AnimatedTexture> ats,
                                       boolean[][] blockMap, PhysicsEngine.SlopeType[][] slopeMap) {
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
        if (background != null) loadLayoutLayerDecor(mm, decor[0], gates, background, key, ats, blockMap, slopeMap);
        loadLayoutLayerDecor(mm, decor[0], gates, middleground, key, ats, blockMap, slopeMap); // middle decor to back
        if (foreground != null) loadLayoutLayerDecor(mm, decor[1], gates, foreground, key, ats, blockMap, slopeMap);

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
     * @param gates    a list of gates to populate
     * @param layout   the layout to load from
     * @param key      the decor key to use
     * @param ats      the list of animated textures to populate
     * @param blockMap the block map to use for pinning decors
     * @param slopeMap the slope map to use for pinning decors
     */
    public static void loadLayoutLayerDecor(Map<List<Object>, Material> mm, List<GameObject> decor,
                                            List<Area.Gate> gates, Node layout, Map<Character, DecorInfo> key,
                                            List<AnimatedTexture> ats, boolean[][] blockMap,
                                            PhysicsEngine.SlopeType[][] slopeMap) {
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
                            if (di.animated()) { // if the texture is animated
                                t = t.animate(di.animFrames, di.animTime, true); // animate it
                                ats.add((AnimatedTexture)t); // and add it to the animated textures list
                            }
                        }
                        if (di.light != null) { // if the decor should emit light
                            m = new LightSourceMaterial(t, di.color, di.bm, di.light); // create material with light
                            ((LightSourceMaterial) m).setOffset(di.lightXOffset, di.lightYOffset); // add light offset
                        } else m = new Material(t, di.color, di.bm); // otherwise create a normal material
                        mm.put(mmKey, m); // save in material map
                    }

                    // create a game object using the material, scaling it to a 32x32 bit resolution per grid cell
                    GameObject go = null;
                    if (di.gatePath != null) { // if the decor is a gate
                        // create the game object as a gate
                        go = new Area.Gate(Model.getStdGridRect(1, 1), m, di.gatePath, di.gatePos);
                        gates.add((Area.Gate)go); // and add it to the gates list
                    }
                    else go = new GameObject(Model.getStdGridRect(1, 1), m);
                    if (m.isTextured()) go.setScale((m.getTexture().getWidth() / (float)di.animFrames) / 32f,
                            m.getTexture().getHeight() / 32f); // scale to correct resolution if decor is textured

                    // get the last free cell in the pin's direction (or the placement cell if no pinning)
                    PhysicsEngine.SlopeType[] slopeFound = new PhysicsEngine.SlopeType[1];
                    Pair<Integer> fcid = di.pin == 0 ? new Pair<>(x, y) : lastFreeCellInDirection(blockMap, slopeMap,
                            slopeFound, x, y,
                            di.pin == 1 ? -1 : di.pin == 3 ? 1 : 0,
                            di.pin == 2 ? 1 : di.pin == 4 ? -1 : 0
                    ); // get the nearest cell not containing a block or slope in the direction of the pin
                    Pair<Float> pos = Transformation.getCenterOfCell(fcid); // get the center of the cell

                    // apply offset
                    pos.x += di.xOffset; // apply horizontal offset
                    pos.y += di.yOffset; // apply vertical offset
                    if (di.xRandInterval != 0f) // apply random horizontal offset
                        pos.x += (float) (Math.random() * 2 * di.xRandInterval) - di.xRandInterval;
                    if (di.yRandInterval != 0f) // apply random vertical offset
                        pos.y += (float) (Math.random() * 2 * di.yRandInterval) - di.yRandInterval;

                    // apply pin and slope rotation (if flag set
                    switch (di.pin) { // switch on the pin and translate the object accordingly
                        case 1: // left
                            pos.x += (go.getWidth() / 2) - 0.5f;
                            if (slopeFound[0] == PhysicsEngine.SlopeType.NegativeBottom) { // on neg bottom slope
                                pos.x -= (pos.y - (float)fcid.y); // translate x accordingly
                                if (di.rotateOnSlope) go.setRotDeg(-45f); // rotate if flag set
                            } else if (slopeFound[0] == PhysicsEngine.SlopeType.PositiveTop) { // on pos top slope
                                pos.x -= 1 - (pos.y - (float)fcid.y); // translate x accordingly
                                if (di.rotateOnSlope) go.setRotDeg(45f); // rotate if flag set
                            }
                            break;
                        case 2: // above
                            pos.y += -(go.getHeight() / 2) + 0.5f;
                            if (slopeFound[0] == PhysicsEngine.SlopeType.PositiveTop) { // on positive top slope
                                pos.y += (pos.x - (float)fcid.x); // translate y accordingly
                                if (di.rotateOnSlope) go.setRotDeg(45f); // rotate if flag set
                            } else if (slopeFound[0] == PhysicsEngine.SlopeType.NegativeTop) { // on neg top sslope
                                pos.y += 1 - (pos.x - (float)fcid.x); // translate y accordingly
                                if (di.rotateOnSlope) go.setRotDeg(-45f); // rotate if flag set
                            }
                            break;
                        case 3: // right
                            pos.x += -(go.getWidth() / 2) + 0.5f;
                            if (slopeFound[0] == PhysicsEngine.SlopeType.PositiveBottom) { // on positive bottom slope
                                pos.x += (pos.y - (float)fcid.y); // translate x accordingly
                                if (di.rotateOnSlope) go.setRotDeg(45f); // rotate if flag set
                            } else if (slopeFound[0] == PhysicsEngine.SlopeType.NegativeTop) { // on neg top slope
                                pos.x += 1 - (pos.y - (float)fcid.y); // translate x accordingly
                                if (di.rotateOnSlope) go.setRotDeg(-45f); // rotate if flag set
                            }
                            break;
                        case 4: // below
                            pos.y += (go.getHeight() / 2) - 0.5f;
                            if (slopeFound[0] == PhysicsEngine.SlopeType.PositiveBottom) { // on positive bottom slope
                                pos.y -= 1 - (pos.x - (float)fcid.x); // translate y accordingly
                                if (di.rotateOnSlope) go.setRotDeg(45f); // rotate if flag set
                            } else if (slopeFound[0] == PhysicsEngine.SlopeType.NegativeBottom) { // on neg bottom slope
                                pos.y -= (pos.x - (float)fcid.x); // translate y accordingly
                                if (di.rotateOnSlope) go.setRotDeg(-45f); // rotate if flag set
                            }
                    }

                    // set final position
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
     * Calculates the last free cell (containing no block or slope) in the given direction from the given starting cell
     *
     * @param blockMap   the block map to use for checking
     * @param slopeMap   the slope map to use for checking
     * @param slopeFound if a slope is found in the given direction, the type of the slope will be placed at index 0 of
     *                   this array
     * @param x          the starting x
     * @param y          the starting y
     * @param dx         the change in x for the direction
     * @param dy         the change in y for the direction
     * @return a pair of integers containing the last free position (containing no block) in the given direction. This
     * will not take into account whether the starting position itself is free
     */
    private static Pair<Integer> lastFreeCellInDirection(boolean[][] blockMap, PhysicsEngine.SlopeType[][] slopeMap,
                                                         PhysicsEngine.SlopeType[] slopeFound, int x, int y, int dx,
                                                         int dy) {
        if (x + dx < 0 || x + dx >= blockMap.length || y + dy < 0 || y + dy >= blockMap[0].length) // if at edge already
            return new Pair<>(x, y); // return given position
        while (!blockMap[x + dx][y + dy]) { // while there is no block in the given direction
            slopeFound[0] = slopeMap[x + dx][y + dy]; // record slope in that direction if there is one
            if (slopeFound[0] != null) break; // if there is one
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
        private final List<Utils.Path> texturePaths = new ArrayList<>(); // list of texture paths to be randomized over
        private float[] color = new float[]{1f, 1f, 1f, 1f};             // decor color
        private LightSource light;                                       // a light source if decor should emit light
        private String gatePath;                                         /* if the decor is a gate to another area, this
                                                                            will hold the path (relative to the story
                                                                            folder) to the area's node-file */
        private Pair<Integer> gatePos;                                   /* if the decor is a gate to another area, this
                                                                            will hold the starting position to place the
                                                                            player at when it enters the area */
        private Material.BlendMode bm;                                   // how to blend color and texture
        private float xOffset, yOffset;                                  // offset values for placement (in grid cells)
        private float xRandInterval, yRandInterval;                      // intervals for random additional offset
        private float animTime;                                          // frame length if animated
        private float lightXOffset;                                      // x offset of the light source if exists
        private float lightYOffset;                                      // y offset of the light source if exists
        private int animFrames;                                          // frame count if animated
        private int pin;                                                 /* defines how the decor pins to nearby blocks.
                                                                            The following values can be used: (0) -
                                                                            none, (1) - left, (2) - above, (3) - right,
                                                                            (4) - below */
        private boolean rotateOnSlope;                                   // whether to rotate when the decor is on slope

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
         * - gate [optional][default: none]: defines the decor to be a gate to another area. The value should be the
         * path to the area's node-file, relative to the story folder, followed by a starting position for when the
         * player enters the gate. For example: 'gate: /areas/new_area.node 5 6'
         * <p>
         * - rotate_on_slope [optional][default: false]: dictates whether the decor will rotate when on a slope. This is
         * most useful for objects with flat side in the direction of their pin. Note that this rotation will only occur
         * if the decor is pinned
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
                            new NodeLoader.LoadItem<>("rotate_on_slope", false, Boolean.class),
                            new NodeLoader.LoadItem<>("x_offset", 0f, Float.class),
                            new NodeLoader.LoadItem<>("y_offset", 0f, Float.class),
                            new NodeLoader.LoadItem<>("x_random_interval", 0f, Float.class),
                            new NodeLoader.LoadItem<>("y_random_interval", 0f, Float.class),
                            new NodeLoader.LoadItem<>("light_source", null, Node.class),
                            new NodeLoader.LoadItem<>("x_light_offset", 0f, Float.class),
                            new NodeLoader.LoadItem<>("y_light_offset", 0f, Float.class),
                            new NodeLoader.LoadItem<>("gate", null, String.class)
                            .useTest((v, sb) -> {
                                String tokens[] = ((String)v).split(" ");
                                if (tokens.length < 3) {
                                    sb.append("Not enough information. The value should contain the path to the gate's")
                                            .append(" area's node-file and then a starting position. For example:\n")
                                            .append("'gate: /areas/new_area.node 5 6'");
                                    return false;
                                }
                                try {
                                    Pair<Integer> sp = new Pair<> (Integer.parseInt(tokens[1]),
                                            Integer.parseInt(tokens[2]));
                                    this.gatePos = sp;
                                    this.gatePath = tokens[0];
                                    return true;
                                } catch (Exception e) {
                                    sb.append("Could not read starting position. Make sure the starting position is")
                                            .append(" two integers separated by spaces");
                                    return false;
                                }
                            })
                    });

            /*
             * Apply loaded information
             */
            // save blend mode as member
            this.bm = Material.BlendMode.valueOf(((String) decorInfo.get("blend_mode")).toUpperCase());
            this.animFrames = (Integer) decorInfo.get("animation_frames"); // save animation frames as member
            this.animTime = (Float) decorInfo.get("animation_time"); // save animation time as member
            this.rotateOnSlope = (Boolean) decorInfo.get("rotate_on_slope"); // save slope rotation flag as member
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