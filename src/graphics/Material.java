package graphics;

import utils.Global;
import utils.Node;
import utils.Utils;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.glActiveTexture;

/**
 * Defines how an object should render by allowing various combinations of texture and color
 */
public class Material {

    /**
     * Members
     */
    private float[] color;        // the color of this material if it has one
    private BlendMode blendMode; // how this Material blends its texture and color when it has both
    protected Texture texture;    // the texture of this material if it has one

    /**
     * Constructs the material based on the given texture, color, and blend flag
     *
     * @param texture   the texture to use
     * @param color     the 4-dimensional color to use (must be a length-4 float array)
     * @param blendMode what blending mode to use when both a Texture and a color are present (described above)
     */
    public Material(Texture texture, float[] color, BlendMode blendMode) {
        this.texture = texture; // set texture
        this.blendMode = blendMode; // set blend mode
        if (color != null) { // if a color is provided
            if (color.length == 4) this.color = color; // use this color if it is properly formatted
            else { // if an invalid color array is given, just log it
                if (this.texture == null) // if there is no texture either, throw an exception
                    Utils.handleException(new Exception("Material with no texture given an invalid color: " + color),
                            "graphics.Material", "Material(Texture, float[], boolean)", true);
                Utils.log("Invalid color array given: " + color + ", assuming colorless", "graphics.Material",
                        "Material(Texture, float[], BlendMode)", false); // if texture, ignore color
            }
        }
    }

    /**
     * Constructs the material with only a texture
     *
     * @param texture the texture to use
     */
    public Material(Texture texture) {
        this(texture, null, BlendMode.NONE);
    }

    /**
     * Constructs the material with only a color
     *
     * @param color the 4-dimensional color to use (must be a length-4 float array)
     */
    public Material(float[] color) {
        this(null, color, BlendMode.NONE);
    }

    /**
     * Constructs the material by exactly duplicating the given other material
     *
     * @param other the other material to duplicate
     */
    public Material(Material other) {
        this(other.texture, other.color, other.blendMode);
    }

    /**
     * Sets the necessary uniforms in the given shader program to render using the material
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
        sp.setUniform("blend", this.blendMode == Material.BlendMode.NONE ? 0 :
                (this.blendMode == Material.BlendMode.MULTIPLICATIVE ? 1 : 2)); // set blend uniform
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
        if (this.isTextured() && this.texture != Global.FONT.getSheet()) this.texture.cleanup();
    }

    /**
     * Represents how a material should blend its texture and its color when it has both
     * NONE - uses texture sampling completely
     * MULTIPLICATIVE - multiplies the color by the texture sample
     * AVERAGED - finds the average of the sample and the color
     */
    public enum BlendMode {NONE, MULTIPLICATIVE, AVERAGED}
}
