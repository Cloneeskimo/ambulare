package gameobject.gameworld;

import graphics.*;
import utils.Transformation;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.glActiveTexture;

/**
 * Encapsulates the information necessary to represent a grid-aligned block. There are a lot of blocks in a game world,
 * so they are kept very minimal. They only have a material and an integer-locked position. The physics engine takes
 * advantage of this by performing constant time collision checks for blocks using a block-map (2D array mapping
 * positions to blocks). They also all use the same model
 */
public class Block {

    /**
     * Static data
     */
    private static final Model blockModel = Model.getStdGridRect(1, 1); // all blocks use same 1x1 square model

    /**
     * Renders the given block using the given shader program
     * @param sp the shader program to render with
     * @param b the block to render
     */
    public static void renderBlock(ShaderProgram sp, Block b) {
        Material m = b.getMaterial();
        if (m.isTextured()) { // if the object's material is textured
            sp.setUniform("isTextured", 1); // set textured flag to true
            glActiveTexture(GL_TEXTURE0); // set active texture to one in slot 0
            glBindTexture(GL_TEXTURE_2D, m.getTexture().getID()); // bind texture
        } else sp.setUniform("isTextured", 0); // set textured flag to false otherwise
        if (m.isColored()) { // if the object's material is colored
            float[] color = m.getColor(); // get color
            sp.setUniform("color", color[0], color[1], color[2], color[3]); // set color uniform
        }
        Material.BLEND_MODE bm = m.getBlendMode(); // get blend mode of the object's material
        sp.setUniform("blend", bm == Material.BLEND_MODE.NONE ? 0 : (bm == Material.BLEND_MODE.MULTIPLICATIVE ? 1
                : 2)); // set blend uniform
        // set position
        sp.setUniform("x", Transformation.getCenterOfCellComponent(b.getX()));
        sp.setUniform("y", Transformation.getCenterOfCellComponent(b.getY()));
        if (b instanceof AnimatedBlock) { // if this block is animated
            AnimatedBlock ab = ((AnimatedBlock)b); // get a reference to it as an animated block
            MultiTexCoordModel MTCM = AnimatedBlock.models.get(ab.getFrames()); // get MTCM with matching frame count
            if (MTCM == null) { // if no MTCM with that count yet
                MTCM = MultiTexCoordModel.getStdMultiTexGridRect(1, 1, ab.getFrames()); // create one
                AnimatedBlock.models.put(ab.getFrames(), MTCM); // and put it in the hash map
            }
            MTCM.setFrame(ab.getCurrentFrame()); // set the appropriate frame for the MTCM
            MTCM.render(); // and then render the animated block
        }
        else blockModel.render(); // if not animated, render using the normal block model
    }

    /**
     * Members
     */
    private Material material; // a material used for rendering
    private int x, y;          // an integer-locked grid position

    /**
     * Constructs the block with the texture with the given resource-relative path
     * @param texPath the path to the texture
     * @param resPath whether the given path is resource-relative or not
     * @param x the x grid position of the block
     * @param y the y grid position of the block
     */
    public Block(String texPath, boolean resPath, int x, int y) {
        this.material = new Material(new Texture(texPath, resPath)); // create and use material based on texture
        this.x = x;
        this.y = y;
    }

    /**
     * Constructs the block with a custom material
     * @param material the material to use for the block
     * @param x the x grid position of the block
     * @param y the y grid position of the block
     */
    public Block(Material material, int x, int y) {
        this.material = material;
        this.x = x;
        this.y = y;
    }

    /**
     * Updates the block
     * @param interval the amount of time to account for
     */
    public void update(float interval) {

    }

    /**
     * @return the block's material for rendering
     */
    public Material getMaterial() { return this.material; }

    /**
     * @return the block's grid position's x
     */
    public int getX() { return this.x; }

    /**
     * @return the block's grid position's y
     */
    public int getY() { return this.y; }

    /**
     * Cleans up the block
     */
    public void cleanup() {
        this.material.cleanup(); // clean up the material
    }
}
