package graphics;

/**
 * Defines properties of a light source independent of position
 */
public class LightSource {

    /**
     * Members
     */
    private float[] glow; /* the glow of the light. This is multiplied after the intensity of the light has been used
        to brighten the object. As such, values lass than 1f should be avoided for glow as they will remove color which
        is counterintuitive to lighting in general. Instead, accentuate certain color components by having the glow
        be above 1f for them */
    private float reach; // how far the light source can reach from its center (in blocks)
    private float intensity; /* how intense/bright the light source is. This should generally be between 0.2f (very dim)
        and 1.8f (extremely bright) but values outside of that range are accepted */

    /**
     * Constructs the light source with a custom glow, reach, and intensity (see members)
     * @param glow the glow of the light source
     * @param reach the reach of the light source
     * @param intensity the intensity of the light source
     */
    public LightSource(float[] glow, float reach, float intensity) {
        this.glow = glow;
        this.reach = reach;
        this.intensity = intensity;
    }

    /**
     * Constructs the light source with the default glow, reach, and intensity (see members)
     */
    public LightSource() {
        this(new float[] { 1.3f, 1.3f, 1.0f, 1.0f }, 5f, 1.3f);
    }

    /**
     * @return the light source's glow
     */
    public float[] getGlow() {
        return this.glow;
    }

    /**
     * @return the light source's reach
     */
    public float getReach() {
        return this.reach;
    }

    /**
     * @return the light source's intensity
     */
    public float getIntensity() {
        return this.intensity;
    }
}
