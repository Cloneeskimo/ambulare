package gameobject.gameworld;

import gameobject.GameObject;
import graphics.*;
import org.lwjgl.system.CallbackI;
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
     * more information on how to format block info nodes. Animation properties will be ignored and textures with
     * multiple frames will not display properly
     * <p>
     * border_background_block [optional][default: white block]: the block to repeat as the border background around
     * the outside of the block map. This node should be properly formatted as a block info node. See Block.BlockInfo
     * for more information on how to format block info nodes. Animation properties will be ignored and textures with
     * multiple frames will not display properly
     * <p>
     * border_thickness [optional][default: 5][1, 20]: how many blocks thick the border around the block map should be
     *
     * @param data the node to use to construct the block backdrop
     * @param bmw  the width of the area's block map
     * @param bmh  the height of the area's block map
     */
    public BlockBackDrop(Node data, int bmw, int bmh) {

        /*
         * Load block backdrop information using node loader
         */
        data = NodeLoader.checkForFromStatement("BlockBackDrop", data);
        Map<String, Object> blockBackDrop = NodeLoader.loadFromNode("BlockBackDrop", data,
                new NodeLoader.LoadItem[]{
                        new NodeLoader.LoadItem<>("blockmap_background_block", null, Node.class),
                        new NodeLoader.LoadItem<>("border_background_block", null, Node.class),
                        new NodeLoader.LoadItem<>("border_thickness", 5, Integer.class)
                                .setLowerBound(1).setUpperBound(20)
                });

        /*
         * Apply loaded information
         */
        Node bmBackNode = (Node) blockBackDrop.get("blockmap_background_block"); // create blockmap background block node
        Block.BlockInfo bmBI = new Block.BlockInfo(bmBackNode == null ? new Node() : bmBackNode); // create BI from it
        Node borderBackNode = (Node) blockBackDrop.get("border_background_block"); // get border background block node
        // make block info from border background block node
        Block.BlockInfo borderBI = new Block.BlockInfo(borderBackNode == null ? new Node() : borderBackNode);
        this.createBackgrounds(bmBI.createMaterial(false), borderBI.createMaterial(false),
                (Integer) blockBackDrop.get("border_thickness"), bmw, bmh); // create backgrounds
    }

    /**
     * Creates the block map background game object and the four border background game objects by aggregating blocks
     * into larger textures for each
     *
     * @param bmBack     the material to use for the block map background blocks
     * @param borderBack the material to use for the border background
     * @param thickness  the thickness of the border around the block map
     * @param bmw        the width of the area's block map
     * @param bmh        the height of the area's block map
     */
    private void createBackgrounds(Material bmBack, Material borderBack, int thickness, int bmw, int bmh) {
        // create models for vertical and horizontal borders
        Model ver = Model.getStdGridRect(thickness, bmh + (2 * thickness));
        Model hor = Model.getStdGridRect(bmw, thickness);
        // calculate width/height of an individual block's texture (32 if no texture)
        int w = borderBack.isTextured() ? borderBack.getTexture().getWidth() : 32;
        int h = borderBack.isTextured() ? borderBack.getTexture().getHeight() : 32;
        Model mod = Model.getStdGridRect(2, 2);
        this.backgrounds = new GameObject[]{ // compile background game objects
                // blockmap background
                new GameObject((float) bmw / 2, (float) bmh / 2, Model.getStdGridRect(bmw, bmh),
                        new Material(Texture.makeSheet(bmBack, mod, bmw, bmh,
                                bmBack.isTextured() ? bmBack.getTexture().getWidth() : 32,
                                bmBack.isTextured() ? bmBack.getTexture().getHeight() : 32, 0, false))),
                // left border
                new GameObject(-(float) thickness / 2f, ((float) bmh) / 2f, ver,
                        new Material(Texture.makeSheet(borderBack, mod, thickness, bmh + (2 * thickness),
                                w, h, 1, true))),
                // right border
                new GameObject((float) bmw + (float) thickness / 2, ((float) bmh) / 2f, ver,
                        new Material(Texture.makeSheet(borderBack, mod, thickness, bmh + (2 * thickness),
                                w, h, 2, true))),
                // top border
                new GameObject(((float) bmw) / 2f, (float) bmh + (float) thickness / 2f, hor,
                        new Material(Texture.makeSheet(borderBack, mod, bmw, thickness,
                                w, h, 3, false))),
                // bottom border
                new GameObject(((float) bmw) / 2f, -(float) thickness / 2f, hor,
                        new Material(Texture.makeSheet(borderBack, mod, bmw, thickness,
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