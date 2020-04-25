package graphics;

import utils.Node;
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
     * Members
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
     * Updates the material's color
     * @param color the new color
     */
    public void setColor(float[] color) { this.color = color; }

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

    /**
     * Creates a material from a node. The optional children of the node are listed below:
     * - texture_path: the path to the texture for the material. If no texture path is given, no texture will be used
     * - resource_relative: whether the given texture path is resource relative. If this flag is not set, the path will
     *      be assumed to be resource-relative
     * - color: the color to use for the material. If this is not set and there is a texture, no color will be used. If
     *      this is not set and there is no texture, white will be used.
     *  - blend_mode: the blend mode to use when there is both a color and a texture ("none", "multiplicative", or
     *      "averaged"). If this is not set, "none" will be used
     * If materials have errors while parsing, no crashes will occur
     * @param node the node to create the material from
     */
    public Material(Node node) {
        String texPath = null; // texture path starts as null
        boolean resPath = true; // resource-relative starts as true
        this.blendMode = BLEND_MODE.NONE; // blend mode starts at no blending
        try { // try to parse material
            for (Node c : node.getChildren()) { // go through each child and parse the values
                String n = c.getName(); // get name
                // parse the data
                if (n.equals("texture_path")) texPath = c.getValue();
                else if (n.equals("resource_relative")) resPath = Boolean.parseBoolean(c.getValue());
                else if (n.equals("color")) this.color = Utils.strToColor(c.getValue());
                else if (n.equals("blend_mode")) this.blendMode = BLEND_MODE.valueOf(c.getValue().toUpperCase());
                else // if unrecognized child, log but don't crash
                    Utils.log("Unrecognized child given for material info: " + c + ". Ignoring.",
                            "graphics.Material", "Material(Node)", false);
            }
        } catch (Exception e) { // log any errors but don't crash
            Utils.log("Unable to parse the following material info: " + node.toString() + "\n for reason:" +
                    e.getMessage(), "graphics.Material", "Material(Node)", false);
        }
        if (texPath != null) this.texture = new Texture(texPath, resPath); // if a texture was given, use the texture
        // if no texture or color was specified, default to white
        if (!this.isColored() && !this.isTextured()) this.color = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
    }
}
