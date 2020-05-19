package graphics;

import utils.Node;
import utils.NodeLoader;
import utils.Utils;

import java.util.Map;

/*
 * LightSource.java
 * Ambulare
 * Jacob Oaks
 * 4/29/20
 */

/**
 * Defines properties of a light source independent of position. Light sources can be loaded from node-files
 */
public class LightSource {

    /**
     * Members
     */
    private float[] glow;       /* the glow of the light. This is multiplied after the intensity of the light has been
                                   used to brighten the object. As such, values lass than 1f should be avoided for glow
                                   as they will remove color which is counterintuitive to lighting in general. Instead,
                                   accentuate certain color components by having the glow be above 1f for them */
    private float reach;        // how far the light source can reach from its center (in blocks)
    private float intensity;    /* how intense/bright the light source is. This should generally be between 0.2f
                                   (very dim) and 1.8f (extremely bright) but values outside of that range are
                                    accepted */
    private float flickerSpeed; /* the light's flicker speed is defined as a range of values that can be
                                   added/subtracted to a multiplier used on the light's reach every render to create a
                                   flicker effect */

    /**
     * Constructs the light source with a custom glow, reach, and intensity (see members)
     *
     * @param glow      the glow of the light source
     * @param reach     the reach of the light source
     * @param intensity the intensity of the light source
     * @param flickerSpeed the speed of the flicker of the light source
     */
    public LightSource(float[] glow, float reach, float intensity, float flickerSpeed) {
        this.glow = glow;
        this.reach = reach;
        this.intensity = intensity;
        this.flickerSpeed = flickerSpeed;
    }

    /**
     * Constructs the light source with the default glow, reach, and intensity (see members)
     */
    public LightSource() {
        this(new float[]{1.3f, 1.3f, 1.0f, 1.0f}, 5f, 1.3f, 0.04f);
    }

    /**
     * Constructs the light source by compiling the information from a given node. Light source nodes can use (res)from
     * statements. For information on (res)from statements, see utils.NodeLoader. A light source node can have the
     * following children:
     * <p>
     * - glow [optional][default: 1.3f 1.3f 1.0f 1.0f]: defines the glow of the light. See members for a better
     * explanation. Glow should be a formatted as four properly formatted float values (one for each color component)
     * separated by spaces
     * <p>
     * - reach [optional][default: 5f][0, 50f]: defines how far the light's reach is in blocks. See members for more
     * info on reach
     * <p>
     * - intensity [optional][default: 1.3f][0f, 10f]: defines how intense the light should be. See members for more
     * info on intensity
     * <p>
     * - flicker_speed [optional][default: 0.03][0f, positive infinity]: defines the speed of the light's flicker. See
     * members for more info on flicker and flicker speed
     *
     * @param data the node to use to create the light source
     */
    public LightSource(Node data) {
        this(); // start at defaults

        /*
         * Load light source information using node loader
         */
        data = NodeLoader.checkForFromStatement("LightSource", data);
        Map<String, Object> lightSource = NodeLoader.loadFromNode("LightSource", data,
                new NodeLoader.LoadItem[]{
                        new NodeLoader.LoadItem<>("glow", "1f 1f 1f 1f", String.class)
                                .useTest((v, sb) -> {
                            float[] c = Utils.strToColor(v);
                            if (c == null) {
                                sb.append("Must be four valid rgba float values separated by a space");
                                sb.append("\nFor example: '1f 0f 1f 0.5' for a half-transparent purple");
                                return false;
                            }
                            this.glow = c;
                            return true;
                        }),
                        new NodeLoader.LoadItem<>("reach", 5f, Float.class)
                                .setLowerBound(0f).setUpperBound(50f),
                        new NodeLoader.LoadItem<>("intensity", 1.3f, Float.class)
                                .setLowerBound(0f).setUpperBound(10f),
                        new NodeLoader.LoadItem<>("flicker_speed", 0.03f, Float.class)
                                .setLowerBound(0f)
                });

        /*
         * Apply loaded information
         */
        this.glow = Utils.strToColor((String) lightSource.get("glow")); // save glow as member
        this.reach = (Float) lightSource.get("reach"); // save reach as member
        this.intensity = (Float) lightSource.get("intensity"); // save intensity as member
        this.flickerSpeed = (Float) lightSource.get("flicker_speed"); // save flicker as member
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

    /**
     * @return the light source's flicker speed
     */
    public float getFlickerSpeed() { return this.flickerSpeed; }
}
