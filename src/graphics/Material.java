package graphics;

import utils.Utils;

/**
 * Represents a Material that defines how a Model will render.
 */
public class Material {

    /**
     * Data
     */
    private float[] color; // the color of this material if it has one
    private Texture texture; // the texture of this material if it has one
    private boolean blend; // whether to blend color and texture when rendering

    /**
     * Constructs this Material based on the given texture, color, and blend flag
     * @param texture the texture to use for this Material
     * @param color the 4-dimensional color to use for this Material (must be a length-4 float array)
     * @param blend whether to blend the given texture and color when rendering (will only use texture otherwise, or color if texture is null)
     */
    public Material(Texture texture, float[] color, boolean blend) {
        this.texture = texture; // set texture
        this.blend = blend; // set blend flag
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
    public Material(Texture texture) { this(texture, null, false); } // call other constructor

    /**
     * Constructs this Material with only a Color
     * @param color the 4-dimensional color to use for this Material (must be a length-4 float array)
     */
    public Material(float[] color) { this(null, color, false); } // call other constructor

    /**
     * @return whether this Material is textured
     */
    public boolean isTextured() { return this.texture != null; }

    /**
     * @return whether this Material is colored
     */
    public boolean isColored() { return this.color != null; }

    /**
     * @return whether this Material should blend its texture and color
     */
    public boolean blend() { return this.blend; }

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
