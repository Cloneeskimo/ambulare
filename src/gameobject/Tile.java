package gameobject;

import graphics.Material;
import graphics.Model;
import graphics.Texture;
import utils.Node;
import utils.Utils;

/**
 * A general square-modeled class with simple constructors as well as a name property
 * Tiles can be constructed using a node-file or with all default properties
 */
public class Tile extends GameObject {

    /**
     * Data
     */
    private static final float[] DEFAULT_COLOR = new float[] {0.5f, 0.5f, 0.5f, 0.5f}; // default color
    private static final float DEFAULT_X = 0, DEFAULT_Y = 0; // default position
    private static final Model STD_SQUARE_MODEL = Model.getStdSquare(); // re-use standard square model for all tiles
    private String name = "unnamed"; // name of the Tile

    /**
     * Constructs this tile using the default properties listed above. No texture will be given
     */
    public Tile() {
        super(DEFAULT_X, DEFAULT_Y, Tile.STD_SQUARE_MODEL, new Material(DEFAULT_COLOR)); // call super with defaults
    }

    /**
     * Constructs the tile using data from a node-file
     * The node-file can contain the following children but does not need to contain any:
     *  - name: the name of the Tile - defaults to "unnamed"
     *  - tex_res_path: the resource-relative path of the Texture to use - defaults to null
     *  - color: the color of the tile. color should be formatted as four float values separated by a space, each
     *    representing a color component (r, g, b, and a) - defaults to Tile.DEFAULT_COLOR
     *  - blend_mode: how to blend the color and texture for this tile. See Material.BLEND_MODE for more details.
     *    defaults to NONE
     *  - x: the x position - defaults to 0f
     *  - y: the y position - defaults to 0f
     * @param resPath the resource-relative path to the node-file
     */
    public Tile(String resPath) {
        this(); // call default constructor
        Node n = Node.resToNode(resPath); // load Node

        // create defaults for the material
        float[] color = DEFAULT_COLOR; // default color defined above
        String trs = null; // default texture is null (no texture)
        Material.BLEND_MODE bm = Material.BLEND_MODE.NONE; // default blend mode is NONE

        // go through data
        try {
            for (Node c : n.getChildren()) { // go through each child
                if (c.getName().equals("name")) this.name = c.getValue(); // name
                else if (c.getName().equals("tex_res_path")) trs = c.getValue(); // texture resource-relative path
                else if (c.getName().equals("color")) { // color
                    color = Utils.strToColor(c.getValue()); // convert to float array
                    if (c == null) // if incorrectly formatted color
                        Utils.handleException(new Exception(Node.getNodeParseErrorMsg("Tile",
                                "incorrectly formatted color", resPath)), "gameobject.Tile", "Tile(String)",
                                true); // throw exception
                } else if (c.getName().equals("blend_mode")) { // blend move
                    bm = Material.strToBM(c.getValue()); // convert to BLEND_MODE
                    if (bm == null) { // if incorrectly formatted
                        Utils.handleException(new Exception(Node.getNodeParseErrorMsg("Tile",
                                "incorrectly formatted blend_mode", resPath)), "gameobject.Tile", "Tile(String)",
                                true); // throw exception
                    }
                }
                else if (c.getName().equals("x")) this.x = Float.parseFloat(c.getValue()); // x
                else if (c.getName().equals("y")) this.y = Float.parseFloat(c.getValue()); // y
            }
        } catch (Exception e) { // catch general exceptions
            Utils.handleException(new Exception(Node.getNodeParseErrorMsg("Tile", "incorrectly formatted: " + e.getMessage(),
                    resPath)), "Tile", "Tile(String)", true); // handle them
        }
        this.material = new Material(trs == null ? null : new Texture(trs), color, bm); // create Material
    }

    /**
     * @return the name of the tile
     */
    public String getName() { return this.name; }
}
