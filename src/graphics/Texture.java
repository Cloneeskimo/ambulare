package graphics;

import org.lwjgl.system.MemoryStack;
import utils.Utils;

import java.io.File;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Paths;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;

/**
 * Represents a Texture
 */
public class Texture {

    /**
     * Data
     */
    private final int id, w, h; // texture ID, width, and height

    /**
     * Constructs this Texture.
     * @param resPath the resource-relative path of the texture image
     */
    public Texture(String resPath) {

        // create buffers to hold texture info
        ByteBuffer buf = null; // create buffer for texture data
        MemoryStack stack = MemoryStack.stackPush(); // push memory stack for buffers
        IntBuffer w = stack.mallocInt(1); // create buffer for texture width
        IntBuffer h = stack.mallocInt(1); // create buffer for texture height
        IntBuffer channels = stack.mallocInt(1); // create buffer to hold channel amount (should be 4 - r, g, b, and a)

        // attempt to load texture
        try {
            URL url = Texture.class.getResource(resPath); // get url to texture
            File file = Paths.get(url.toURI()).toFile(); // convert to file
            buf = stbi_load(file.getAbsolutePath(), w, h, channels, 4); // load texture into buffer
            if (buf == null) Utils.handleException(new Exception("Unable to load texture with path '" + file.getAbsolutePath() + "' for reason: " + stbi_failure_reason()),
                    "Texture", "Texture(String", true); // throw exception if unable to load texture
        } catch (Exception e) { // if exception
            Utils.handleException(e, "Texture", "Texture(String", true); // handle exception
        }

        // save info, create texture, cleanup
        this.w = w.get(); // save width
        this.h = h.get(); // save height
        this.id = glGenTextures(); // generate texture object
        glBindTexture(GL_TEXTURE_2D, id); // bind new texture object
        glPixelStoref(GL_UNPACK_ALIGNMENT, 1); // tell GL that each component will be one byte in size
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, this.w, this.h, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf); // generate texture
        // parameters for glTexImage2D:
        // target - target texture (the type) - GL_TEXTURE_2D is what we're working with here
        // level - the level-of-detail number - o is the base image. level n is the nth mipmap reduction
        // internal format - the number of color components (RGBA in this case)
        // width, height - the width and height of the image
        // border - no idea but we set it to zero
        // format - specifies format of the pixel data we give it (RGBA)
        // type - specifies the data type of the pixel data (bytes)
        // data - the actual image data
        glGenerateMipmap(GL_TEXTURE_2D); // generate mip maps
        stbi_image_free(buf); // cleanup
    }

    /**
     * @return this Texture's ID
     */
    public int getID() { return this.id; }

    /**
     * Cleans up this Texture
     */
    public void cleanup() { glDeleteTextures(this.id); }
}
