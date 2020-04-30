package graphics;

import utils.Node;
import utils.Utils;

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
     *
     * @param glow      the glow of the light source
     * @param reach     the reach of the light source
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
        this(new float[]{1.3f, 1.3f, 1.0f, 1.0f}, 5f, 1.3f);
    }

    /**
     * Constructs the light source by compiling the information from a given node. If the value of the root node starts
     * with the statements 'from' or 'resfrom', the next statement will be assumed to be a different path at which to
     * find the light source info. This is useful for reusing the same light source in multiple settings. 'from' assumes
     * the following path is relative to the Ambulare data folder (in the user's home folder) while 'resfrom' assumes
     * the following path is relative to the Ambulares's resource path. Note that these kinds of statements cannot be
     * chained together. A light source node can have the following children:
     * <p>
     * - glow [optional][default: 1.3f 1.3f 1.0f 1.0f]: defines the glow of the light. See members for a better
     * explanation. Glow should be a formatted as four properly formatted float values (one for each color component)
     * separated by spaces
     * <p>
     * - reach [optional][default: 5f]: defines how far the light's reach is in blocks. See members for a better
     * explanation
     * <p>
     * - intensity [optional][default: 1.3f]: defines how intense the light should be. See members for a better
     * explanation
     * <p>
     * Note that, if any of the info above is improperly formatted, a message saying as much will be logged. As such,
     * when designing light sources to be loaded into the game, the logs should be checked often to make sure the
     * loading process is unfolding correctly
     */
    public LightSource(Node info) {
        this(); // start at defaults

        // load from elsewhere if from or resfrom statement used
        String value = info.getValue(); // get value
        if (value != null) { // if there is a value
            // check for a from statement
            if (value.length() >= 4 && value.substring(0, 4).toUpperCase().equals("FROM"))
                // update info with node at the given path in the from statement
                info = Node.fileToNode(info.getValue().substring(5), true);
                // check for a resfrom statement
            else if (value.length() >= 7 && value.substring(0, 7).toUpperCase().equals("RESFROM"))
                // update info with node at the given path in the from statement
                info = Node.resToNode(info.getValue().substring(8));
            if (info == null) // if the new info is null, then throw an exception stating the path is invalid
                Utils.handleException(new Exception(Utils.getImproperFormatErrorLine("(res)from statement",
                        "LightSource", "invalid path in (res)from statement: " + value,
                        false)), "graphics.LightSource", "LightSource(Node)", true);
        }

        // parse node
        if (!info.hasChildren()) return; // if no children, just return with default properties
        for (Node c : info.getChildren()) { // loop through children
            String n = c.getName(); // get name of child
            if (n.equals("glow")) { // glow
                float[] glow = Utils.strToColor(c.getValue()); // try to convert to a float array
                if (glow == null) // log if unsuccessful
                    Utils.log(Utils.getImproperFormatErrorLine("glow", "LightSource",
                            "must be four valid floating point numbers separated by spaces",
                            true), "graphics.LightSource", "LightSource(Node)", false);
                else this.glow = glow; // otherwise use the glow
            } else if (n.equals("reach")) {
                try {
                    this.reach = Float.parseFloat(c.getValue()); // try to convert to a float
                } catch (Exception e) { // log if unsuccessful
                    Utils.log(Utils.getImproperFormatErrorLine("reach", "LightSource",
                            "must be a proper floating pointer number greater than 0", true),
                            "graphics.LightSource", "LightSource(Node)", false);
                }
                if (this.reach <= 0f) { // if zero or negative reach, log
                    Utils.log(Utils.getImproperFormatErrorLine("reach", "LightSource",
                            "must be a proper floating pointer number greater than 0", true),
                            "graphics.LightSource", "LightSource(Node)", false);
                    this.reach = 5f; // reset to default
                }
            } else if (n.equals("intensity")) {
                try {
                    this.intensity = Float.parseFloat(c.getValue()); // try to convert to a float
                } catch (Exception e) { // log if unsuccessful
                    Utils.log(Utils.getImproperFormatErrorLine("intensity", "LightSource",
                            "must be a proper floating pointer number greater than 0", true),
                            "graphics.LightSource", "LightSource(Node)", false);
                }
                if (this.intensity <= 0f) { // if zero or negative reach, log
                    Utils.log(Utils.getImproperFormatErrorLine("intensity", "LightSource",
                            "must be a proper floating pointer number greater than 0", true),
                            "graphics.LightSource", "LightSource(Node)", false);
                    this.intensity = 1.3f; // reset to default
                }
            } else
                Utils.log("Unrecognized child given for light source:\n" + c + "Ignoring.",
                        "graphics.LightSource", "LightSource(Node)", false); // log unused child
        }
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
