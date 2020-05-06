package graphics;

/*
 * LightSourceMaterial.java
 * Ambulare
 * Jacob Oaks
 * 4/29/20
 */

/**
 * Extends material by allowing the material to be a source of light. This requires that the material is update with
 * a position before uniforms are set for the lights to be placed at the correct positions
 */
public class LightSourceMaterial extends Material {

    /**
     * Members
     */
    LightSource light;                // the light source
    float lightXOffset, lightYOffset; // the offset to apply to the positions received in setPos()
    float lightX, lightY;             // the light's position

    /**
     * Constructs the light source material with a texture, color, and blend mode
     *
     * @param texture   the texture to use
     * @param color     the color to use
     * @param blendMode the blend mode representing how to blend color and texture (see Material for more details on
     *                  textures/color/blending)
     * @param ls        the light source to use for the material
     */
    public LightSourceMaterial(Texture texture, float[] color, BlendMode blendMode, LightSource ls) {
        super(texture, color, blendMode);
        this.light = ls;
    }

    /**
     * Constructs the light source material with only a texture
     *
     * @param texture the texture to use
     * @param ls      the light source to use for the material
     */
    public LightSourceMaterial(Texture texture, LightSource ls) {
        super(texture);
        this.light = ls;
    }

    /**
     * Constructs the light source material with only a color
     *
     * @param color the color to use
     * @param ls    the light source to use for the material
     */
    public LightSourceMaterial(float[] color, LightSource ls) {
        super(color);
        this.light = ls;
    }

    /**
     * Sets the necessary uniforms in the given shader program to render using the light source material. Note that
     * setPos() should be called on the light source material constantly to update the positioning of the light
     *
     * @param sp the shader program whose uniforms are to be sed
     */
    @Override
    public void setUniforms(ShaderProgram sp) {
        super.setUniforms(sp); // set normal material uniforms
        sp.putInLightArrayUniform(this.light, this.lightX, this.lightY); // insert material's light into light array
    }

    /**
     * Sets the position of the light in the material. A defined offset will be added to the given position
     *
     * @param x the x position
     * @param y the y position
     */
    public void setPos(float x, float y) {
        this.lightX = x + this.lightXOffset;
        this.lightY = y + this.lightYOffset;
    }

    /**
     * Sets the offset to apply to given positions when setPos() is called
     *
     * @param ox the x offset
     * @param oy the y offset
     */
    public void setOffset(float ox, float oy) {
        this.lightXOffset = ox;
        this.lightYOffset = oy;
    }
}
