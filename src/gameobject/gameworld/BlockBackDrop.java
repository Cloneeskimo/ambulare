package gameobject.gameworld;

import gameobject.GameObject;
import graphics.*;
import utils.*;

import java.util.*;

import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL30.*;

/*
 * BlockBackDrop.java
 * Ambulare
 * Jacob Oaks
 * 5/12/2020
 */

/**
 * Blocks backdrops render a repeated block behind the whole of the blockmap as a background. This is an efficient and
 * more convenient alternative to filling out the background layer of the area node-file with the block. This also frees
 * up the background layer for more decorations. In addition, a border block is defined (which can be the same as or
 * different from the blockmap background block) that will be used as a border around the blockmap. This border's
 * thickness is configurable. The border fades out to black for an aesthetic touch. Block backdrops are loaded from
 * node-files. For information on how to format a block backdrop node-file, see the constructor
 */
public class BlockBackDrop implements Area.BackDrop {

    /**
     * Static Data
     */
    private static final int MAX_BORDER_THICKNESS = 20; /* border backgrounds cannot be thicker than this, in blocks */

    /**
     * Members
     */
    private GameObject[] backgrounds; /* the background game objects to render, where backgrounds[0] is the blockmap
        background, backgrounds[1] is the left-side border, backgrounds[2] is the right-side border, backgrounds[3]
        is the top-side border, and backgrounds[4] is the bottom-side border. Each game object is created by aggregating
        the corresponding blocks into a single large texture */

    /**
     * Constructs the block backdrop by compiling information from a given node. If the value of the root node starts
     * with the statements 'from' or 'resfrom', the next statement will be assumed to be a different path at which to
     * find the block backdrop node-file. This is useful for reuse of the same block backdrop in many settings. 'from'
     * assumes the following path is relative to the Ambulare data folder (in the user's home folder) while 'resfrom'
     * assumes the following path is relative to the Ambulares's resource path. Note that these kinds of statements
     * cannot be chained together. A block backdrop node can have the following children:
     * <p>
     * blockmap_background_block [optional][default: white block]: the block to repeat as the background within the
     * bounds of the block map. This node should be properly formatted as a block info node. See Block.BlockInfo for
     * more information on how to format block info nodes. Animated blocks cannot be used
     * <p>
     * border_background_block [optional][default: white block]: the block to repeat as the border background around
     * the outside of the block map. This node should be properly formatted as a block info node. See Block.BlockInfo
     * for more information on how to format block info nodes. Animated blocks cannot be used
     * <p>
     * border_thickness [optional][default: 5]: how many blocks thick the border around the block map should be. This
     * must be at least 1 and at most MAX_BORDER_THICKNESS
     * <p>
     * Note that, if any of the info above is improperly formatted, a message saying as much will be logged. As
     * such, when designing block backdrops to be loaded into the game, the logs should be checked often to make sure
     * the loading process is unfolding correctly
     *
     * @param info the node to use to construct the block backdrop
     * @param w    the window in use
     * @param bmw  the width of the area's block map
     * @param bmh  the height of the area's block map
     */
    public BlockBackDrop(Node info, Window w, int bmw, int bmh) {

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
            if (info == null) // if the new info is null
                Utils.handleException(new Exception(Utils.getImproperFormatErrorLine("(res)from statement",
                        "BlockBackDrop", "invalid path in (res)from statement: " + value,
                        false)), "gameobject.gameworld.BlockBackDrop", "BlockBackDrop(Node, Window, int, int)",
                        true); // throw an exception stating the path is invalid
        }

        // relevant variables to populate via loading from the node
        Material bmMat = null, borderMat = null;
        int borderThickness = 5;

        // load information from node
        try {
            for (Node c : info.getChildren()) { // loop through children
                String n = c.getName().toLowerCase(); // get child name in lowercase
                if (n.equals("blockmap_background_block")) { // blockmap background block
                    Block.BlockInfo bi = new Block.BlockInfo(c); // create block info from node
                    bmMat = new Material(bi.texPaths.size() > 0 ? new Texture(bi.texPaths.get((int) (Math.random() *
                            bi.texPaths.size())), bi.texResPath) : null, bi.color, bi.bm); // create material from bi
                } else if (n.equals("border_background_block")) { // border background block
                    Block.BlockInfo bi = new Block.BlockInfo(c); // create block info from node
                    borderMat = new Material(bi.texPaths.size() > 0 ? new Texture(bi.texPaths.get((int) (Math.random() *
                            bi.texPaths.size())), bi.texResPath) : null, bi.color, bi.bm); // create material from bi
                } else if (n.equals("border_thickness")) { // border thickness
                    try {
                        borderThickness = Integer.parseInt(c.getValue()); // try to convert to integere
                    } catch (Exception e) { // if unable to convert
                        Utils.log(Utils.getImproperFormatErrorLine("border_thickness", "BlockBackDrop",
                                "must be a proper integer greater than zero and less than " +
                                        MAX_BORDER_THICKNESS, true), "gameobject.gameworld.BlockBackDrop",
                                "BlockBackDrop(Node, Window, int, int)", false); // log as much
                    }
                    if (borderThickness < 1 || borderThickness > MAX_BORDER_THICKNESS) { // if out of bounds
                        Utils.log(Utils.getImproperFormatErrorLine("border_thickness", "BlockBackDrop",
                                "must be a proper integer greater than zero and less than " +
                                        MAX_BORDER_THICKNESS + 1, true),
                                "gameobject.gameworld.BlockBackDrop",
                                "BlockBackDrop(Node, Window, int, int)", false); // log as much
                        borderThickness = 5; // reset to default
                    }
                } else // if an unrecognized child appears
                    Utils.log("Unrecognized child given for block backdrop info:\n" + c + "Ignoring.",
                            "gameobject.gameworld.BlockBackDrop", "BlockBackDrop(Node, Window, int, int)",
                            false); // log and ignore
            }
        } catch (Exception e) { // if any strange exceptions occur
            Utils.handleException(new Exception(Utils.getImproperFormatErrorLine("BlockBackDrop",
                    "BlockBackDrop", e.getMessage(), false)), "gameobject.gameworld.BlockBackDrop",
                    "BlockBackDrop(Node, Window, int, int)", true); // log and crash
        }
        // if block map material is null, use a white block by default
        if (bmMat == null) bmMat = new Material(Global.getThemeColor(Global.ThemeColor.WHITE));
        // if border material is null, use a white block by default
        if (borderMat == null) borderMat = new Material(Global.getThemeColor(Global.ThemeColor.WHITE));
        this.createBackgrounds(bmMat, borderMat, w, borderThickness, bmw, bmh); // create backgrounds
    }

    /**
     * Creates the block map background game object and the four border background game objects by aggregating blocks
     * into larger textures for each
     *
     * @param bmBack     the material to use for the block map background blocks
     * @param borderBack the material to use for the border background
     * @param window     the window whose framebuffer is currently in use
     * @param thickness  the thickness of the border around the block map
     * @param bmw        the width of the area's block map
     * @param bmh        the height of the area's block map
     */
    private void createBackgrounds(Material bmBack, Material borderBack, Window window, int thickness, int bmw,
                                   int bmh) {
        // create models for vertical and horizontal borders
        Model ver = Model.getStdGridRect(thickness, bmh + (2 * thickness));
        Model hor = Model.getStdGridRect(bmw, thickness);
        // calculate width/height of an individual block's texture (32 if no texture)
        int w = borderBack.isTextured() ? borderBack.getTexture().getWidth() : 32;
        int h = borderBack.isTextured() ? borderBack.getTexture().getHeight() : 32;
        this.backgrounds = new GameObject[]{ // compile background game objects
                // blockmap background
                new GameObject((float) bmw / 2, (float) bmh / 2, Model.getStdGridRect(bmw, bmh),
                        new Material(Texture.makeSheet(bmBack, window, bmw, bmh,
                                bmBack.isTextured() ? bmBack.getTexture().getWidth() : 32,
                                bmBack.isTextured() ? bmBack.getTexture().getHeight() : 32, 0, false))),
                // left border
                new GameObject(-(float) thickness / 2f, ((float) bmh) / 2f, ver,
                        new Material(Texture.makeSheet(borderBack, window, thickness, bmh + (2 * thickness),
                                w, h, 1, true))),
                // right border
                new GameObject((float) bmw + (float) thickness / 2, ((float) bmh) / 2f, ver,
                        new Material(Texture.makeSheet(borderBack, window, thickness, bmh + (2 * thickness),
                                w, h, 2, true))),
                // top border
                new GameObject(((float) bmw) / 2f, (float) bmh + (float) thickness / 2f, hor,
                        new Material(Texture.makeSheet(borderBack, window, bmw, thickness,
                                w, h, 3, false))),
                // bottom border
                new GameObject(((float) bmw) / 2f, -(float) thickness / 2f, hor,
                        new Material(Texture.makeSheet(borderBack, window, bmw, thickness,
                                w, h, 4, false)))
        };
    }

    /**
     * Block backdrops do not a use a camera
     *
     * @param cam the camera used for rendering the area
     */
    public void useCam(Camera cam) {
    }

    /**
     * Block backdrops do not respond to resizes
     */
    public void resized() {
    }

    /**
     * Renders the block backdrop by rendering the background game objects
     *
     * @param sp the world shader program
     */
    public void render(ShaderProgram sp) {
        for (GameObject background : this.backgrounds) background.render(sp); // render background game objects
    }

    /**
     * Cleans up the block backdrop by cleaning up its background game objects
     */
    public void cleanup() {
        for (GameObject background : this.backgrounds) background.cleanup(); // clean up background game objects
    }
}
