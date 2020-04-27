package graphics;

import utils.Node;
import utils.Utils;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.glActiveTexture;

/**
 * Defines how a game object will render. Materials can be animated, but note that they must be updated in order for
 * the animation ot occur. It is a bad idea to design game objects to do this because it means that the animation
 * will go quicker if more than one object uses the material and updates it every loop. It would be a better idea to
 * keep a list of materials and update that list every loop
 */
public class Material {

    /**
     * Static Data
     */
    private static Map<Integer, int[]> texCoords = new HashMap<>(); /* this maps from amount of frames of an
        animation to set of texture coordinate VBOs to used when rendering a frame. Since many materials may be
        animated, this saves from having tons of repeat lists/arrays of texture coordinates. The fact that there is a
        different set for each amount of frames also means that less variance in frame count is more space efficient */

    /**
     * Members
     */
    private float[] color;          // the color of this material if it has one
    private Texture texture;        // the texture of this material if it has one
    private BLEND_MODE blendMode;   // how this Material blends its texture and color when it has both

    /**
     * Animation Members
     */
    protected int frames = 1;       // total amount of frames and current frame
    protected int frame = 0;        // current frame of animation
    protected float frameTime = 1f; // amount of time per frame
    protected float frameTimeLeft;  // amount of time left for the current frame

    /**
     * Constructs the material based on the given texture, color, and blend flag
     *
     * @param texture   the texture to use
     * @param color     the 4-dimensional color to use (must be a length-4 float array)
     * @param blendMode what blending mode to use when both a Texture and a color are present (described above)
     */
    public Material(Texture texture, float[] color, BLEND_MODE blendMode) {
        this.texture = texture; // set texture
        this.blendMode = blendMode; // set blend mode
        if (color != null) { // if a color is provided
            if (color.length == 4) this.color = color; // use this color if it is properly formatted
            else { // if an invalid color array is given, just log it
                if (this.texture == null) // if there is no texture either, throw an exception
                    Utils.handleException(new Exception("Material with no texture given an invalid color: " + color),
                            "graphics.Material", "Material(Texture, float[], boolean)", true);
                Utils.log("Invalid color array given: " + color + ", assuming colorless", "graphics.Material",
                        "Material(Texture, float[], boolean)", false); // if texture, ignore color
            }
        }
    }

    /**
     * Constructs the material based on the given texture, color, blend flag, and animation settings
     *
     * @param texture   the texture to use
     * @param color     the 4-dimensional color to use (must be a length-4 float array)
     * @param blendMode what blending mode to use when both a Texture and a color are present (described above)
     * @param frames    the amount of frames of animation
     * @param frameTime the amount of time each frame should have
     * @param randStart whether to start the animation at a random frame and time
     */
    public Material(Texture texture, float[] color, BLEND_MODE blendMode, int frames, float frameTime,
                    boolean randStart) {
        this(texture, color, blendMode);
        this.frames = Math.max(frames, 1);
        this.frameTime = frameTime;
        this.frame = randStart ? ((int) (Math.random() * frames)) : 0; // calc starting frame
        this.frameTimeLeft = randStart ? ((float) Math.random() * frameTime) : frameTime; // calc starting time left
    }

    /**
     * Constructs the material with only a texture
     *
     * @param texture the texture to use
     */
    public Material(Texture texture) {
        this(texture, null, BLEND_MODE.NONE);
    }

    /**
     * Constructs the material with only a color
     *
     * @param color the 4-dimensional color to use (must be a length-4 float array)
     */
    public Material(float[] color) {
        this(null, color, BLEND_MODE.NONE);
    }

    /**
     * Constructs the material by exactly duplicating the given other material
     *
     * @param other the other material to duplicate
     */
    public Material(Material other) {
        this(other.texture, other.color, other.blendMode);
        this.frameTime = other.frameTime;
        this.frames = other.frames;
        this.frame = other.frame;
        this.frameTimeLeft = other.frameTimeLeft;
    }

    /**
     * Creates a material from a node. The optional children of the node are listed below:
     * - texture_path: the path to the texture for the material. If no texture path is given, no texture will be used.
     * - resource_relative: whether the given texture path is resource relative. If this flag is not set, the path will
     * be assumed to be resource-relative. If not resource-relative, the path should be relative to the Ambulare data
     * folder
     * - color: the color to use for the material. If this is not set and there is a texture, no color will be used. If
     * this is not set and there is no texture, white will be used.
     * - blend_mode: the blend mode to use when there is both a color and a texture ("none", "multiplicative", or
     * "averaged"). If this is not set, "none" will be used
     * - frames: the amount of animated frames in the material's texture. If this is not set, 1 will be used
     * - frame_time: the amount of time (in seconds) each animated frame should last. If this is not set, 1f will be
     * used
     * - rand_start: whether the animation should start at a random point. If this is not set, it will start at the
     * first frame
     * If materials have errors while parsing, no crashes will occur
     *
     * @param node the node to create the material from
     */
    public Material(Node node) {
        String texPath = null; // texture path starts as null
        boolean resPath = true; // resource-relative starts as true
        boolean randStart = false; // random animation starting point starts as false
        this.blendMode = BLEND_MODE.NONE; // blend mode starts at no blending
        try {
            for (Node c : node.getChildren()) { // go through each child and parse the values
                String n = c.getName(); // get name
                // parse the data
                if (n.equals("texture_path")) texPath = c.getValue();
                else if (n.equals("resource_relative")) resPath = Boolean.parseBoolean(c.getValue());
                else if (n.equals("color")) this.color = Utils.strToColor(c.getValue());
                else if (n.equals("blend_mode")) this.blendMode = BLEND_MODE.valueOf(c.getValue().toUpperCase());
                else if (n.equals("frames")) this.frames = Integer.parseInt(c.getValue());
                else if (n.equals("frame_time")) this.frameTime = Float.parseFloat(c.getValue());
                else if (n.equals("rand_start")) randStart = Boolean.parseBoolean(c.getValue());
                else // if unrecognized child, log but don't crash
                    Utils.log("Unrecognized child given for material info: " + c + ". Ignoring.",
                            "graphics.Material", "Material(Node)", false);
            }
        } catch (Exception e) { // log any errors but don't crash
            Utils.log("Unable to parse the following material info: " + node.toString() + "\n for reason: " +
                    e.getMessage(), "graphics.Material", "Material(Node)", false);
        }
        // if a texture was given, use the texture
        if (texPath != null) this.texture = new Texture(resPath ? texPath : Utils.getDataDir() + texPath, resPath);
        // if no texture or color was specified, default to white
        if (!this.isColored() && !this.isTextured()) this.color = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
        if (this.isAnimated()) { // if the material is animated
            this.frame = randStart ? ((int) (Math.random() * frames)) : 0; // calc starting frame
            this.frameTimeLeft = randStart ? ((float) Math.random() * frameTime) : frameTime; // calc starting time left
        }
    }

    /**
     * Updates the material by updating the animation if it has one
     */
    public void update(float interval) {
        if (this.frames > 1) { // if this material is animated
            this.frameTimeLeft -= interval; // account for time in animation
            if (this.frameTimeLeft < 0f) { // if frame time for current frame is up
                this.frameTimeLeft = frameTime; // reset frame time counter
                this.frame++; // go to the next frame
                if (this.frame >= this.frames) this.frame = 0; // go back to start after last frame
            }
        }
    }

    /**
     * Will get the correct texture coordinate vertex buffer object to give to a model given the current frame of the
     * material and the total amount of frames
     *
     * @return the correct texture coordinate VBO as described above
     */
    public int getTexCoordVBO() {
        int[] texCoordVBOs = Material.texCoords.get(this.frames); // try to get the set of VBOs
        if (texCoordVBOs == null) { // if this set of texture coordinate VBOs hasn't been calculated yet
            texCoordVBOs = Model.calcTexCoordVBOs(this.frames); // calculate the tex coords for that amount of frames
            Material.texCoords.put(this.frames, texCoordVBOs); // save to map
        }
        return texCoordVBOs[this.frame]; // get and return the texture coordinates for the current frame
    }

    /**
     * Sets the necessary uniforms in the given shader program to render using this material
     *
     * @param sp the shader program whose uniforms are to be sed
     */
    public void setUniforms(ShaderProgram sp) {
        if (this.isTextured()) { // if the material is textured
            sp.setUniform("isTextured", 1); // set textured flag to true
            glActiveTexture(GL_TEXTURE0); // set active texture to one in slot 0
            glBindTexture(GL_TEXTURE_2D, this.texture.getID()); // bind texture
        } else sp.setUniform("isTextured", 0); // set textured flag to false otherwise
        if (this.isColored()) // if the material is colored
            sp.setUniform("color", this.color[0], this.color[1], this.color[2], this.color[3]); // color uniforms
        sp.setUniform("blend", this.blendMode == Material.BLEND_MODE.NONE ? 0 :
                (this.blendMode == Material.BLEND_MODE.MULTIPLICATIVE ? 1 : 2)); // set blend uniform
    }

    /**
     * Updates the material's color
     *
     * @param color the new color
     */
    public void setColor(float[] color) {
        this.color = color;
    }

    /**
     * Updates the material's textures
     *
     * @param texture the new texture to use
     */
    public void setTexture(Texture texture) {
        this.texture = texture;
    }

    /**
     * @return whether the material is textured
     */
    public boolean isTextured() {
        return this.texture != null;
    }

    /**
     * @return whether the material is colored
     */
    public boolean isColored() {
        return this.color != null;
    }

    /**
     * @return whether the material is animated
     */
    public boolean isAnimated() {
        return this.frames > 1;
    }

    /**
     * @return the materials' color
     */
    public float[] getColor() {
        return this.color;
    }

    /**
     * @return the material's texture
     */
    public Texture getTexture() {
        return this.texture;
    }

    /**
     * Cleans up the material
     */
    public void cleanup() {
        if (this.texture != null) this.texture.cleanup();
    }

    /**
     * Represents how a material should blend its texture and its color when it has both
     * NONE - uses texture sampling completely
     * MULTIPLICATIVE - multiplies the color by the texture sample
     * AVERAGED - finds the average of the sample and the color
     */
    public enum BLEND_MODE {NONE, MULTIPLICATIVE, AVERAGED}
}
