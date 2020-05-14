package graphics;

import org.apache.commons.io.IOUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL32;
import org.lwjgl.system.MemoryStack;
import utils.Utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13C.glActiveTexture;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBImage.*;

/*
 * Texture.java
 * Ambulare
 * Jacob Oaks
 * 4/16/20
 */

/**
 * Represents a Texture
 */
public class Texture {

    /**
     * Makes a texture that is a sheet where the given material is rendered in a grid-like fashion according to the
     * given parameters, using aggregation shaders. Optionally, as described in the aggregation shaders, a fade of
     * transparency can be applied
     *
     * @param m       the the material to render onto the sheet
     * @param cols    how many columns of the given material should be rendered onto the sheet
     * @param rows    how many rows of the given material should be rendered onto the sheet
     * @param mw      the width to consider the material to be, in pixels
     * @param mh      the height to consider the material to be, in pixels
     * @param fadeDir the direction to apply a transparency fade based off of: 0 - no fade; 1 - fade out to left; 2 -
     *                fade out to right; 3 - fade out above; 4 - fade out below
     * @param corners whether to apply corners on the caps of the texture when fadin
     * @return a texture holding the final product
     */
    public static Texture makeSheet(Material m, Window w, int cols, int rows, int mw, int mh, int fadeDir,
                                    boolean corners) {

        // create the aggregation shader program
        ShaderProgram sp = new ShaderProgram(new Utils.Path("/shaders/aggregate_vertex.glsl", true),
                new Utils.Path("/shaders/aggregate_fragment.glsl", true));
        sp.registerUniform("texSampler"); // register the texture sampler uniform
        sp.registerUniform("color"); // register the material color uniform
        sp.registerUniform("isTextured"); // register the texture flag uniform
        sp.registerUniform("blend"); // register the material blend uniform
        sp.registerUniform("fadeDir"); // register fade direction uniform
        sp.registerUniform("corners"); // register fade corner uniform
        sp.registerUniform("w");
        sp.registerUniform("h");
        // register offset uniforms
        sp.registerUniform("x");
        sp.registerUniform("y");

        // create other necessary items
        int[] IDs = Utils.createFBOWithTextureAttachment(cols * mw, rows * mh); // create FBO w/ texture attached
        Model mod = Model.getStdGridRect(2, 2); // create model
        mod.setScale((float) mw / (float) (cols * mw), (float) mh / (float) (rows * mh)); // scale down appropriately

        // pre-render
        glBindFramebuffer(GL_FRAMEBUFFER, IDs[0]); // bind the frame buffer object
        glViewport(0, 0, cols * mw, rows * mh); // set the viewport to the size of the texture
        glClear(GL_COLOR_BUFFER_BIT); // clear color
        sp.bind(); // bind the shader program
        sp.setUniform("texSampler", 0); // tell the texture sampler to look in texture bank 0
        sp.setUniform("fadeDir", fadeDir); // set the fade direction uniform
        sp.setUniform("corners", corners ? 1 : 0); // set the fade corner uniform
        sp.setUniform("w", mw * cols);
        sp.setUniform("h", mh * rows);
        m.setUniforms(sp); // set material uniforms

        // render and fill
        for (int col = 0; col < cols; col++) { // go through each column
            float x = ((float) col + 0.5f - (float) cols / 2) / ((float) cols / 2); // calculate the norm x
            for (int row = 0; row < rows; row++) { // go through each row in that column
                float y = ((float) row + 0.5f - (float) rows / 2) / ((float) rows / 2); // calculate the norm y
                // set x/y offset uniforms
                sp.setUniform("x", x);
                sp.setUniform("y", y);
                mod.render(); // render the model
            }
        }

        // post-render
        sp.unbind(); // unbind shader program
        sp.cleanup(); // cleanup shader program
        Texture t = new Texture(IDs[1], cols * mw, rows * mh); // create final texture
        glBindFramebuffer(GL_FRAMEBUFFER, 0); // unbind the frame buffer object
        glDeleteFramebuffers(IDs[0]); // delete the frame buffer object
        glViewport(0, 0, w.getFBWidth(), w.getFBHeight()); // reset viewport to window's framebuffer size
        return t; // return the final texture
    }

    /**
     * Members
     */
    private final int id, w, h; // texture ID, width, and height

    /**
     * Constructor
     *
     * @param path the path to the image
     */
    public Texture(Utils.Path path) {

        // create buffers to hold texture info
        ByteBuffer buf = null; // create buffer for texture data
        MemoryStack stack = MemoryStack.stackPush(); // push memory stack for buffers
        IntBuffer w = stack.mallocInt(1); // create buffer for texture width
        IntBuffer h = stack.mallocInt(1); // create buffer for texture height
        IntBuffer channels = stack.mallocInt(1); // create buffer to hold channel amount (4 if rgba)

        // attempt to load texture
        try {
            ByteBuffer buff = Utils.pathContentsToByteBuffer(path, 1024); // convert image to byte buffer
            buf = stbi_load_from_memory(buff, w, h, channels, 4); // load image into texture buffer
        } catch (Exception e) { // if exception
            Utils.handleException(new Exception("Unable to load texture at '" + path + "' for reason: " +
                    e.getMessage()), this.getClass(), "Texture", true); // throw exception if unable to load
        }

        // save info, create texture, cleanup
        this.w = w.get(); // save width
        this.h = h.get(); // save height
        this.id = glGenTextures(); // generate texture object

        glBindTexture(GL_TEXTURE_2D, id); // bind new texture object
        glPixelStoref(GL_UNPACK_ALIGNMENT, 1); // tell GL that each component will be one byte in size
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST); // this makes pixels clear and un-blurred
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST); // this makes pixels clear and un-blurred
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, this.w, this.h, 0, GL_RGBA, GL_UNSIGNED_BYTE,
                buf); // generate texture
        glGenerateMipmap(GL_TEXTURE_2D); // generate mip maps
        stbi_image_free(buf); // cleanup by freeing image memory
    }

    /**
     * Construct a texture with the given GL texture ID, width and height.
     *
     * @param id the OpenGL id of the texture
     * @param w  the width of the texture in pixels
     * @param h  the height of the texture in pixels
     */
    public Texture(int id, int w, int h) {
        this.id = id; // save id as member
        this.w = w; // save width as member
        this.h = h; // save height as member
    }

    /**
     * Turns a texture into an animated one using the given animation properties
     *
     * @param frames    how many frames the animation should have
     * @param frameTime how much time (in seconds) each frame should last
     * @param randStart whether the animation should start at a random point or not
     * @return the texture turned into an animated texture. Note that this will still be using the same OpenGL texture
     * id as the non-animated texture before
     */
    public AnimatedTexture animate(int frames, float frameTime, boolean randStart) {
        return new AnimatedTexture(this.id, this.w, this.h, frames, frameTime, randStart);
    }

    /**
     * @return the texture's ID
     */
    public int getID() {
        return this.id;
    }

    /**
     * @return the texture's width
     */
    public int getWidth() {
        return this.w;
    }

    /**
     * @return the texture's height
     */
    public int getHeight() {
        return this.h;
    }

    /**
     * Calculates and returns appropriate model coords for a model to have the correct size to use the texture in its
     * native aspect ratio
     *
     * @param relativeTo what to calculate the model coordinates relative to. For example, if this is set to 32 and the
     *                   texture's width is 64 and the height is 128, the model will be 2 wide and 4 tall. Smaller
     *                   values of relativeTo will lead to larger models
     * @return the model coordinates described above
     */
    public float[] getModelCoords(float relativeTo) {
        float w2 = (this.w / relativeTo / 2); // calculate half width of model
        float h2 = (this.h / relativeTo / 2); // calculate half height of model
        return new float[]{ // create array with correct model coordinates and return it
                -w2, h2, // bottom left
                -w2, -h2, // top left
                w2, -h2, // top right
                w2, h2 // bottom right
        };
    }

    /**
     * Cleans up the texture
     */
    public void cleanup() {
        glDeleteTextures(this.id);
    }
}
