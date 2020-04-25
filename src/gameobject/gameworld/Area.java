package gameobject.gameworld;

import graphics.Material;
import org.lwjgl.system.CallbackI;
import utils.Node;
import utils.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Area {

    private String name;
    private Block[][] blockMap;

    /**
     * Creates an area from the given node. The required children of the node are listed below:
     * - key: contains children that map letters in the 'map' child to materials to use for the block placed there.
     *      - each child's name should be a single character. If more characters are provided, only the first character
     *        is used. If multiple materials are mapped to a single character, only the last one read will be used
     *      - each child must either (A) have a child of its own that is a parse-able material node, (B) have a value
     *        that specifies where to look for a parse-able material node, or (C) have the value 'empty' which denotes
     *        that no block should be placed there (space also represents a lack of block but this may not work properly
     *        for leading spaces). Case (B) is useful for materials that will be used many places throughout many
     *        different areas. For case (B), the value should be formatted as follows: "[resfrom:from] [path]" where
     *        [path] is the path to look for the material and [resfrom/from] denotes that the path is resource-relative
     *        or not resource-relative, respectively. If not resource-relative, the path should be relative to the
     *        Ambulare data folder
     *      - if a child is improperly formatted, it will be ignored but the occurrence may be logged
     * - layout: contains children that are sequences of characters that will be converted into blocks using the key
     *        child. the value of the children is the character sequence and the name means nothing. they are read in
     *        the order that they are listed (higher children are considered to be in a higher row than lower children,
     *        and the same principle applies horizontally within each child). If a character appears that is not in the
     *        key, it will be ignored and interpreted as empty space
     * The optional children of the node are listed below:
     * - name: the name of the area. If not provided, the area will be named "Unnamed"
     * If areas have errors while parsing, depending on the error, crashes may occur
     * @param node the node to create the area from
     */
    public Area(Node node) {
        try { // try to parse node

            // parse key
            Map<Character, Material> key = new HashMap<>(); // use hashmap
            key.put(' ', null); // spaces mean empty
            Node keyData = node.getChild("key"); // get key child
            if (keyData == null) Utils.handleException(new Exception("Area node-file did not produce a key. A key is " +
                    "required"), "gameobject.gameworld.Area", "Area(Node)", true); // if no key child, crash
            for (Node c : keyData.getChildren()) { // go through children
                String v = c.getValue(); // get value of the child
                if (v.length() >= 4) { // if the mapping has a value
                    String[] tokens = v.split(" "); // split up the value
                    // if the value is "empty", put null into the map to represent empty
                    if (tokens[0].toUpperCase().equals("EMPTY")) key.put(c.getName().charAt(0), null);
                    else if (tokens[0].toUpperCase().equals("FROM")) // if value specifies to load material from path
                        // load material from path and put into map
                        key.put(c.getName().charAt(0), new Material(Node.fileToNode(tokens[1], true)));
                    else if (tokens[0].toUpperCase().equals("RESFROM")) // if specifies to load material from res-path
                        // load material from res-path and put into map
                        key.put(c.getName().charAt(0), new Material(Node.resToNode(tokens[1])));
                    else // if unusual value, log but don't crash
                        Utils.log("Unrecognized value for a key child: " + tokens[0] + ". Ignoring.",
                                "gameobject.gameworld.Area", "Area(Node)", false);
                } else {
                    // if no value, must be an explicitly stated material - load directly
                    if (c.getChildren().size() < 1)
                        Utils.log("key contains character '" + c.getName().charAt(0) + "' but provides no" +
                        "material. Ignoring.", "gameobject.gameworld.Area", "Area(Node)", false);
                    else key.put(c.getName().charAt(0), new Material(c.getChild(0))); // and put into map
                }
            }

            // parse layout
            Node layoutData = node.getChild("layout"); // get layout child
            if (layoutData == null) Utils.handleException(new Exception("Area node-file did not produce a layout. A " +
                    "layout is required"), "gameobject.gameworld.Area", "Area(Node)", true); // no layout child -> crash
            List<Node> rows = layoutData.getChildren(); // get rows as a separate list
            int w = 0;
            for (int y = 0; y < rows.size(); y++) { // loop through each row
                // figure out the widest row so that block map can be created with appropriate width
                if (rows.get(y).getValue().length() > w) w = rows.get(y).getValue().length();
            }
            this.blockMap = new Block[w][rows.size()]; // create block map
            for (int y = 0; y < rows.size(); y++) { // loop through each row
                for (int x = 0; x < rows.get(y).getValue().length(); x++) { // loop through each character in the rpw
                    Material m = key.get(rows.get(y).getValue().charAt(x)); // get the material at that spot
                    if (m != null) blockMap[x][y] = new Block(m, x, y); // put into block map if not empty
                }
            }

            // parse other
            this.name = "Unnamed"; // name starts as unnamed
            for (Node c : node.getChildren()) { // go through each child and parse the values
                String n = c.getName(); // get name of child
                if (n.equals("name")) { // if thee child is for area name
                    this.name = c.getValue(); // save name
                } else { // if none of the above
                    if (!n.equals("layout") && !(n.equals("key"))) // if it's not layout or key
                        Utils.log("Unrecognized child given for area info: " + c + ". Ignoring.",
                                "gameobject.gameworld.Area", "Area(Node)", false); // log unused child
                }
            }
        } catch (Exception e) { // if error while parsing
            Utils.handleException(new Exception("Unable to parse the following area info: " + node.toString() + "\n " +
                    "for sreason:" + e.getMessage()), "gameobject.gameworld.Area", "Area(Node)", true); // crash
        }
    }
}
