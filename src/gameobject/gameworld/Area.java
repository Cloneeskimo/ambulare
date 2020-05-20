package gameobject.gameworld;

import gameobject.GameObject;
import gameobject.ui.EnhancedTextObject;
import gameobject.ui.TextObject;
import graphics.*;
import utils.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static gameobject.gameworld.Block.renderBlocks;
import static gameobject.ui.EnhancedTextObject.Line.DEFAULT_PADDING;
import static gameobject.ui.EnhancedTextObject.Line.DEFAULT_SCALE;

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
 * with all blocks in the middleground. For more information on blocks, see gameobject.gameworld.Block.BlockInfo
 * <p>
 * - Decor: decor can be any size, can emit light, and has additional properties that allow it to be more customized.
 * Decor does not cause collisions. Decor is represented as a simple list of game objects to update and render every
 * game loop. Decor can either exist in the background or the foreground. Any decor put in the middleground layer in the
 * area node-file will be assumed to be background decor. For more information on decor, see
 * gameobject.gameworld.Decor.DecorInfo
 * <p>
 * The idea behind areas is to hold only a portion of the game's world and to be interconnected. The GameWorld class
 * only renders and updates one area at a time, the idea being that many areas can exist and be interconnected but only
 * the one that the player is in should be rendered and updated. Areas also have fairly extensive backdrop options.
 * Areas are loaded via node-files. For more info on what constitutes an area and how to format an area node-file, see
 * Area's constructor
 */
public class Area {

    /**
     * Static Data
     */
    private static final float GATE_ENTER_DIST_THRESHOLD = 3f; /* the maximum distance a player should be away from a
                                                                  gate to enter it when the gate enter key is pressed */

    /**
     * Members
     */
    private final Map<Material, List<Pair<Integer>>>[] blocks;  /* an array of lists of block positions grouped by
        material. Blocks are indexed by material in this way this way to facilitate efficient rendering of large
        quantities of blocks. See the renderBlocks() method for more info on efficient block rendering. There are three
        different maps in this array where blocks[0] represents the background blocks, blocks[1] represents the
        middleground blocks, and blocks[2] represents the foreground blocks */
    private final List<GameObject>[] decor;                     /* two lists of decor in the area where decor[0] is
                                                                   background decor and decor[1] is foreground decor */
    private final List<Gate> gates;                             // a list of gates to other areas
    private final List<AnimatedTexture> ats;                    // a list of animated textures to update
    private final boolean[][] blockMap;                         // block map of the middleground for collision
    private final PhysicsEngine.SlopeType[][] slopeMap;         // slope map of the middleground for collision
    private final Block.BlockModel bm = new Block.BlockModel(); // all blocks use same 1x1 square model
    private final String name;                                  // the name of the area
    private final float startingSunRotation;                    /* the rotation to set the sun to when the area is
                                                                   entered. If -1, the sun's rotation will not be
                                                                   changed */
    private final float sunSpeed;                               // the sun's speed while in this area
    private final boolean lightForeground;                      // whether to apply lights to objects in the foreground
    private BackDrop backdrop;                                  // the backdrop rendered behind the world

    /**
     * Constructs the area by compiling information from a given node. Most of the loading process is actually done in
     * the decor class or the block class to avoid cluttering the area class with a bunch of large static methods. An
     * area node can have the following children:
     * <p>
     * - block_key [required]: this key maps characters in the layout to block info that describes the corresponding
     * block. Each child of the block_key child should have its name be a single character that appears in the layout
     * (if there is more than one character, only the first will be used) and the rest of the child should be formatted
     * as a block info node
     * <p>
     * - middleground_layout [required]: the middleground layout should contain a child for each row, where the value of
     * the row is the set of characters describing that row for which corresponding items will be searched for in the
     * keys to build the middleground. Rows will be read and loaded top-down and left-to-right so that they appear in
     * the same order as they do in the node-file. Extra empty rows at the bottom are not necessary, however empty rows
     * at the top are necessary to line up with the background and the foreground. If a character is encountered that is
     * not in any key, it will be ignored. The space character will also be ignored and interpreted as empty space. If
     * decor is found in the middleground layout, it will be placed in the background
     * <p>
     * - decor_key [optional][default: no decor]: this kep maps characters in the layout to decor info that describes
     * the corresponding decor. Each child of the decor_key child should have its name be a single character that
     * appears in the layout (if there is more than one character, only the first will be used) and the rest of the
     * child should be formatted as a decor info node
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
     * - backdrop [optional][default: white material backdrop]: the backdrop to use for the area. The value of this
     * child should specify what kind of backdrop to use (either 'material_backdrop' or 'block_backdrop'). The rest of
     * the node should then be formatted as a node of the corresponding kind of backdrop. For more info on the two kinds
     * of backdrops, see gameobject.gameworld.BlockBackDrop and gameobject.gameworld.MaterialBackdrop
     * <p>
     * - starting_sun_rotation [optional][0, 359.99][default: the current rotation of the world's sun]: the rotation (in
     * non-negative degrees) to set the sun to when the area is entered
     * <p>
     * - sun_speed [optional][default: 2f][0, 100f]: the speed of the sun. This setting can be combined with
     * sun_rotation to effectively simulate indoor settings (sun rotation of 270 and sun_speed of 0) where windows could
     * then be decor light sources, or settings where the sun is always setting, or settings where the sun moves super
     * quickly, etc
     *
     * @param data the node to create the area from
     */
    public Area(Node data) {
        double time = Timer.getTimeMilliseconds(); // record time at beginning of area loading

        /*
         * Load area information using node loader
         */
        Map<String, Object> area = NodeLoader.loadFromNode("area", data, new NodeLoader.LoadItem[]{
                new NodeLoader.LoadItem<>("background_layout", null, Node.class),
                new NodeLoader.LoadItem<>("middleground_layout", null, Node.class).makeRequired(),
                new NodeLoader.LoadItem<>("foreground_layout", null, Node.class),
                new NodeLoader.LoadItem<>("block_key", null, Node.class).makeRequired(),
                new NodeLoader.LoadItem<>("decor_key", null, Node.class),
                new NodeLoader.LoadItem<>("name", "Unnamed", String.class).makeValueSensitive(),
                new NodeLoader.LoadItem<>("light_foreground", false, Boolean.class),
                new NodeLoader.LoadItem<>("starting_sun_rotation", -1f, Float.class)
                        .setLowerBound(0f).setUpperBound(359.99f),
                new NodeLoader.LoadItem<>("sun_speed", 2f, Float.class)
                        .setLowerBound(0f).setUpperBound(100f),
                new NodeLoader.LoadItem<>("backdrop", new Node("backdrop", "material_backdrop"),
                        Node.class).useTest((v, sb) -> {
                    String val = v.getValue().toLowerCase();
                    if (val.equals("material_backdrop") || val.equals("block_backdrop")) return true;
                    sb.append("'").append(val).append("' is not a valid type of backdrop. Valid backdrop types");
                    sb.append("are either 'material_backdrop' or 'block_backdrop");
                    return false;
                })
        });

        /*
         * Apply loaded information
         */
        this.blocks = new Map[]{new HashMap<>(), new HashMap<>(), new HashMap<>()}; // block pos for each layout layer
        this.decor = new List[]{new ArrayList<>(), new ArrayList<>()}; // decor list for background and foreground
        this.gates = new ArrayList<>(); // initialize gates array list
        this.ats = new ArrayList<>(); // create new list to animated textures to update
        Object[] maps = Block.loadLayoutBlocks((Node) (area.get("block_key")),
                (Node) (area.get("background_layout")), (Node) (area.get("middleground_layout")),
                (Node) (area.get("foreground_layout")), this.blocks, this.ats); // load block layout
        this.blockMap = (boolean[][])maps[0]; // save block map
        this.slopeMap = (PhysicsEngine.SlopeType[][])maps[1]; // save slope map
        // load decor using decor key and Decor class
        if (area.get("decor_key") != null) Decor.loadLayoutDecor((Node) (area.get("decor_key")),
                (Node) (area.get("background_layout")), (Node) (area.get("middleground_layout")),
                (Node) (area.get("foreground_layout")), this.decor, this.gates, this.ats, this.blockMap, this.slopeMap);
        this.name = (String) (area.get("name")); // save name
        this.lightForeground = (Boolean) (area.get("light_foreground")); // save foreground lighting flag
        this.startingSunRotation = (float) (area.get("starting_sun_rotation")); // save starting sun rotation
        this.sunSpeed = (float) (area.get("sun_speed")); // save sun speed
        String type = ((Node) (area.get("backdrop"))).getValue().toLowerCase(); // get type of backdrop
        if (type.equals("material_backdrop")) this.backdrop = new MaterialBackDrop(((Node) (area.get("backdrop"))),
                this.blockMap.length, this.blockMap[0].length); // if material backdrop, create it
        else if (type.equals("block_backdrop")) this.backdrop = new BlockBackDrop(((Node) (area.get("backdrop"))),
                this.blockMap.length, this.blockMap[0].length); // if block backdrop, create it
        Utils.log("Finished loading area '" + this.name + "' in " + String.format("%.2f",
                (Timer.getTimeMilliseconds() - time)) + "ms", this.getClass(), "Area",
                false); // log time it took to load area
        Global.debugInfo.setField("area animated textures", Integer.toString(this.ats.size()));
    }

    /**
     * Tells the area which mouse input engine to add any mouse interaction items to
     * @param mip the mouse input engine to use
     */
    public void useMouseInputEngine(MouseInputEngine mip) {
        for (Gate g : this.gates) mip.add(g, true); // add gates to the mip
    }

    /**
     * Tells the area the path to the story folder
     * @param storyPath the path to the story folder
     */
    public void useStoryPath(Utils.Path storyPath) {
        for (Gate g : this.gates) g.initLabels(storyPath); // give to the gates
    }

    /**
     * Tells the area which camera is being used by the game world
     *
     * @param cam the game world's camera
     */
    public void useCam(Camera cam) {
        // if the area has a material backdrop, give it a reference to the camera to use for scrolling
        if (this.backdrop instanceof MaterialBackDrop) this.backdrop.useCam(cam);
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
    }

    /**
     * This method should be called when the key to enter a gate has been pressed. Using the given player position, this
     * method will check if the player is close enough to a gate to enter it
     * @param playerX the x position of the player
     * @param playerY the y position of the player
     * @return the entered gate if the player was close enough to one to enter, or null otherwise
     */
    public Gate enterGate(float playerX, float playerY) {
        for (Gate g : this.gates) { // for each gate
            // calculate the distance
            float dx = g.getX() - playerX;
            float dy = g.getY() - playerY;
            if (Math.sqrt(dx * dx + dy * dy) <= GATE_ENTER_DIST_THRESHOLD) return g; // return gate if close enough
        }
        return null; // no gates close enough -> no transition
    }

    /**
     * Renders the area's blocks and decor
     *
     * @param sp      the shader program to use for rendering
     * @param os      the lists of world objects to render in the area
     * @param camView the camera view's axis-aligned bounding box which will be used to only render objects within view.
     *                This cuts rendering time down by a lot (half the rendering time in some cases according to my
     *                tests!)
     */
    public void render(ShaderProgram sp, List<WorldObject> os, PhysicsEngine.AABB camView, Camera cam) {
        int blocksRendered = 0, decorRendered = 0; // keep track of block render count and decor render count
        // all blocks are the same size so a smaller view can be used to cull out the blocks that to not need rendered
        PhysicsEngine.AABB blockView = new PhysicsEngine.AABB(camView); // copy camera view
        blockView.add(1f / cam.getZoom()); // only add enough to catch blocks
        camView.scale(2f); // scale other objects' camera-view to avoid clipping large objects
        sp.setUniform("useDNC", 1); // enable day/night cycle usage
        sp.setUniform("useLights", 1); // enable usage of single lights after backdrop has been rendered
        this.backdrop.render(sp); // render the backdrop
        blocksRendered += renderBlocks(this.bm, sp, this.blocks[0], blockView); // render the background blocks
        for (GameObject o : this.decor[0]) { // for each decor piece, if its within view or produces light
            if (camView.contains(o.getX(), o.getY()) || o.getMaterial() instanceof LightSourceMaterial) {
                o.render(sp); // render it
                decorRendered++; // iterate decor rendered counter
            }
        }
        //for (GameObject o : this.decor[0]) o.render(sp);
        blocksRendered += renderBlocks(this.bm, sp, this.blocks[1], blockView); // render the middleground blocks
        // render world objects (middleground) from the game world that are within the camera's view
        for (WorldObject wo : os) if (camView.contains(wo.getX(), wo.getY())) wo.render(sp);
        // disable light usage for foreground objects if the setting is set to false
        if (!this.lightForeground) sp.setUniform("useLights", 0);
        blocksRendered += renderBlocks(this.bm, sp, this.blocks[2], blockView); // render the foreground blocks
        // render foreground decor that is within the camera's view
        for (GameObject o : this.decor[1]) { // for each decor piece, if its within view or produces light
            if (camView.contains(o.getX(), o.getY()) || o.getMaterial() instanceof LightSourceMaterial) {
                o.render(sp); // render it
                decorRendered++; // iterate decor rendered counter
            }
        }
        Global.debugInfo.setField("decor", Integer.toString(decorRendered)); // show decor render count in debug
        Global.debugInfo.setField("blocks", Integer.toString(blocksRendered)); // show block render count in debug
        // reset lighting flags to false to render post-renders
        sp.setUniform("useLights", 0); // turn off individual light usage
        sp.setUniform("useDNC", 0); // turn off day/night cycle usage
        sp.renderPostRenders(); // render post-renders
    }

    /**
     * Handles a window resize by notifying the backdrop of the resize
     */
    public void resized() {
        this.backdrop.resized(); // tell the backdrop about the resize
    }

    /**
     * @return the area's block map
     */
    public boolean[][] getBlockMap() {
        return this.blockMap;
    }

    /**
     * @return the area's slope map
     */
    public PhysicsEngine.SlopeType[][] getSlopeMap() {
        return this.slopeMap;
    }

    /**
     * @return the name of the area
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return the rotation the sun should be at (in non-negative degrees) when the area is entered
     */
    public float getStartingSunRotation() {
        return this.startingSunRotation;
    }

    /**
     * @return the speed the sun should move at when in the area
     */
    public float getSunSpeed() {
        return this.sunSpeed;
    }

    /**
     * Cleans up the area's animated textures and decor
     */
    public void cleanup() {
        for (AnimatedTexture at : this.ats) at.cleanup(); // cleanup animated textures
        for (GameObject o : this.decor[0]) o.cleanup(); // cleanup background decor
        for (GameObject o : this.decor[1]) o.cleanup(); // cleanup foreground decor
        // cleanup block materials
        for (Map<Material, List<Pair<Integer>>> bs : this.blocks) for (Material m : bs.keySet()) m.cleanup();
        this.bm.cleanup(); // cleanup the block model
        this.backdrop.cleanup(); // cleanup the backdrop
        AnimatedTexture.texCoords = new HashMap<>(); // reset animated texture texture coordinate VBOs
    }

    /**
     * Represents a physical gate between areas
     */
    public static class Gate extends GameObject implements MouseInputEngine.MouseInteractive{

        /**
         * Members
         */
        // an array of mouse callbacks as specified by
        private Pair<Integer> pos;       // the position to start the player at in the new area
        private String fPath;            // the path to the new area's node-file, relative to the story folder
        private Utils.Path path;         // the path to the new area's node-file
        private GameObject label;        // a label above the gate

        /**
         * Constructs the gate
         * @param model the model to use for the gate
         * @param material the material to use for the gate
         * @param fPath the path to the gate's area, relative to the story folder
         * @param pos the position to start the player at in the new area when the gate is entered
         */
        public Gate(Model model, Material material, String fPath, Pair<Integer> pos) {
            super(model, material); // call super on game object
            this.fPath = fPath; // save new area's node-file path, relative to the story folder, as member
            this.pos = pos; // save starting position as member
        }

        /**
         * Initializes the labels above the gate to show the player when moused over
         * @param storyFolderPath the path to the story folder to use to retrieve the new area's name
         */
        public void initLabels(Utils.Path storyFolderPath) {
            List<EnhancedTextObject.Line> lines = new ArrayList<>(); // create list for lines of ETO
            lines.add(new EnhancedTextObject.Line("to: ???", null, EnhancedTextObject.Line.Alignment.CENTER,
                    DEFAULT_SCALE, DEFAULT_PADDING)); // first line of ETO, twice as big as second
            lines.add(new EnhancedTextObject.Line("<press e to enter>", null,
                    EnhancedTextObject.Line.Alignment.CENTER, DEFAULT_SCALE / 2, DEFAULT_PADDING)); // second line
            EnhancedTextObject label = new EnhancedTextObject(new Material(new float[] {0f, 0f, 0f, 0.55f}),
                    Global.font, lines, DEFAULT_PADDING * 3f); // createe ETO
            this.path = storyFolderPath.add(this.fPath); // get and save new area's noe-file's path
            if (!this.path.exists()) // if the path doesn't exist
                Utils.handleException(new Exception(NodeLoader.getMessage("Gate", null,
                        "Invalid path to linked to area node-file: " + this.path, false)),
                        this.getClass(), "initLabel", true); // crash
            else { // if the path does exist
                Node areaData = Node.pathContentsToNode(this.path); // get the new area's data
                Node nameNode = areaData.getChild("name"); // get the name of the new area
                // if the name node exists, update the label to contain the name
                if (nameNode != null) label.setLineText(0, "to: " + nameNode.getValue());
            }
            label.alignAll(EnhancedTextObject.Line.Alignment.CENTER); // align text to center
            this.label = label.solidify(); // solidify enhanced text object
            this.label.setScale(5f, 5f); // scale up
            this.label.setVisibility(false); // invisible by default
            label.cleanup(); // cleanup old label
            this.positionText(); // position the text objects
        }

        /**
         * Repositions the text above the gate
         */
        private void positionText() {
            if (this.label != null) // reposition if the label exists
                this.label.setPos(this.getX(), // x aligns with gate
                        this.getY() + this.getHeight() / 2 + this.label.getHeight() / 2 + 0.010f); // above gate
        }

        /**
         * Reacts to gate movement by position the text above the ate
         */
        @Override
        protected void onMove() {
            super.onMove(); // call super's onMove
            this.positionText(); // position text above gate
        }

        /**
         * Renders the gate and its text objects
         * @param sp the shader program to use to render the game object
         */
        @Override
        public void render(ShaderProgram sp) {
            super.render(sp); // render gate
            if (this.label != null) sp.addToPostRender(this.label); // render label in post-render
        }

        /**
         * Gates do not take callbacks
         */
        @Override
        public void giveCallback(MouseInputEngine.MouseInputType type, MouseInputEngine.MouseCallback mc) {}

        /**
         * Responds to mouse interaction by determining visibility for the text objects
         * @param type the type of mouse input that occurred
         * @param x    the x position of the mouse in world coordinate or camera-view coordinates, depending on the mouse
         *             input engine's camera usage flag for this particular implementing object
         * @param y    the y position of the mouse in world coordinate or camera-view coordinates, depending on the mouse
         */
        @Override
        public void mouseInteraction(MouseInputEngine.MouseInputType type, float x, float y) {
            if (type == MouseInputEngine.MouseInputType.HOVER) // if hovered
                if (this.label != null) this.label.setVisibility(true); // make label visible
            if (type == MouseInputEngine.MouseInputType.DONE_HOVERING) // if done hovering
                this.label.setVisibility(false); // make label invisible
        }

        /**
         * @return the path of the new area through the gate
         */
        public Utils.Path getPath() {
            return this.path;
        }

        /**
         * @return the position to start the player at in the new are
         */
        public Pair<Integer> getStartingPos() {
            return this.pos;
        }

        /**
         * Cleans up the gate and its text objects
         */
        @Override
        public void cleanup() {
            super.cleanup(); // clean up gate
            if (this.label != null) this.label.cleanup(); // clean up label
        }
    }

    /**
     * Defines functionality that a backdrop should have in order to be used by the area
     */
    public interface BackDrop {

        /**
         * This gives the backdrop a reference to the camera used for rendering the area. This is called as soon as the
         * game world gives the area a reference to the camera
         *
         * @param cam the camera used for rendering the area
         */
        void useCam(Camera cam);

        /**
         * Backdrops should render whatever components they need to here, using the given world shader program
         *
         * @param sp the world shader program
         */
        void render(ShaderProgram sp);

        /**
         * Backdrops should respond appropriately to window resizes in this method. This is called whenever the area is
         * told about a window resize
         */
        void resized();

        /**
         * Backdrops should cleanup any components that need cleaned up here
         */
        void cleanup();
    }
}
