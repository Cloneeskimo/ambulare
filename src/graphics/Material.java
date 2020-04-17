package graphics;

import utils.Utils;

/**
 * Represents a Material that defines how a Model will render.
 */
public class Material {

    /**
     * Represents how a Material should blend its Texture and its color when it has both
     * NONE - uses texture sampling completely
     * MULTIPLICATIVE - multiplies the color by the texture sample
     * AVERAGED - finds average of the sample and the color
     */
    public enum BLEND_MODE { NONE, MULTIPLICATIVE, AVERAGED }

    /**
     * Data
     */
    private float[] color; // the color of this material if it has one
    private Texture texture; // the texture of this material if it has one
    private BLEND_MODE blendMode; // how this Material blends its texture and color when it has both

    /**
     * Constructs this Material based on the given texture, color, and blend flag
     * @param texture the texture to use for this Material
     * @param color the 4-dimensional color to use for this Material (must be a length-4 float array)
     * @param blendMode what blending mode to use when both a Texture and a color are present (described above)
     */
    public Material(Texture texture, float[] color, BLEND_MODE blendMode) {
        this.texture = texture; // set texture
        this.blendMode = blendMode; // set blend mode
        if (color != null) { // if a color is provided
            if (color.length == 4) this.color = color; // use this color if it is properly formatted
            else { // if an invalid color array is given, just log it
                if (this.texture == null) // if there is no texture either, throw an exception
                    Utils.handleException(new Exception("Material with no texture given an invalid color: " + color), "Material", "Material(Texture, float[], boolean)", true);
                Utils.log("Invalid color array given: " + color + ", assuming colorless", "Material", "Material(Texture, float[], boolean)", false); // if there is a texture though, just ignore color
            }
        }
    }

    /**
     * Constructs this Material with only a Texture
     * @param texture the Texture to use for this Material
     */
    public Material(Texture texture) { this(texture, null, BLEND_MODE.NONE); } // call other constructor

    /**
     * Constructs this Material with only a Color
     * @param color the 4-dimensional color to use for this Material (must be a length-4 float array)
     */
    public Material(float[] color) { this(null, color, BLEND_MODE.NONE); } // call other constructor

    /**
     * @return whether this Material is textured
     */
    public boolean isTextured() { return this.texture != null; }

    /**
     * @return whether this Material is colored
     */
    public boolean isColored() { return this.color != null; }

    /**
     * @return this Material's blend mode
     */
    public BLEND_MODE getBlendMode() { return this.blendMode; }

    /**
     * @return this Material's Texture
     */
    public Texture getTexture() { return this.texture; }

    /**
     * @return this Material's 4-dimensional color
     */
    public float[] getColor() { return this.color; }

    /**
     * Cleans up this Material
     */
    public void cleanup() { if (this.texture != null) this.texture.cleanup(); }
}
