package graphics;

import utils.Utils;

/**
 * Defines how a game object will render.
 */
public class Material {

    /**
     * Represents how a material should blend its texture and its color when it has both
     * NONE - uses texture sampling completely
     * MULTIPLICATIVE - multiplies the color by the texture sample
     * AVERAGED - finds the average of the sample and the color
     */
    public enum BLEND_MODE { NONE, MULTIPLICATIVE, AVERAGED }

    /**
     * Converts a given string to a BLEND_MODE (defined above)
     * @param s the string to convert - not case-sensitive
     * @return the corresponding BLEND_MODE or null if not recognized
     */
    public static BLEND_MODE strToBM(String s) {
        s = s.toUpperCase(); // convert to uppercase
        if (s.equals("MULTIPLICATIVE")) return BLEND_MODE.MULTIPLICATIVE; // multiplicative
        if (s.equals("AVERAGED")) return BLEND_MODE.AVERAGED; // averaged
        if (s.equals("NONE")) return BLEND_MODE.NONE; // none
        return null; // null
    }

    /**
     * Data
     */
    private float[] color;        // the color of this material if it has one
    private Texture texture;      // the texture of this material if it has one
    private BLEND_MODE blendMode; // how this Material blends its texture and color when it has both

    /**
     * Constructs the material based on the given texture, color, and blend flag
     * @param texture the texture to use
     * @param color the 4-dimensional color to use (must be a length-4 float array)
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
     * Constructs the material with only a texture
     * @param texture the texture to use
     */
    public Material(Texture texture) { this(texture, null, BLEND_MODE.NONE); } // call other constructor

    /**
     * Constructs the material with only a color
     * @param color the 4-dimensional color to use (must be a length-4 float array)
     */
    public Material(float[] color) { this(null, color, BLEND_MODE.NONE); } // call other constructor

    /**
     * @return whether the material is textured
     */
    public boolean isTextured() { return this.texture != null; }

    /**
     * @return whether the material is colored
     */
    public boolean isColored() { return this.color != null; }

    /**
     * @return the material's blend mode
     */
    public BLEND_MODE getBlendMode() { return this.blendMode; }

    /**
     * @return the material's texture
     */
    public Texture getTexture() { return this.texture; }

    /**
     * @return the material's 4-dimensional color
     */
    public float[] getColor() { return this.color; }

    /**
     * Cleans up the material
     */
    public void cleanup() { if (this.texture != null) this.texture.cleanup(); }
}
