package graphics;

import utils.Node;
import utils.NodeLoader;
import utils.Utils;

import java.util.HashMap;
import java.util.Map;

/*
 * Font.java
 * Ambulare
 * Jacob Oaks
 * 4/18/20
 */

/**
 * Represents a font by containing a font sheet texture with all characters in the font and info about the font such
 * as cutoffs, characters per row, and characters per column. Fonts must be loaded using a node-file to lay out this
 * kind of information. See the constructor for information on how to format font info node-files. There should be some
 * space between characters both horizontally and vertically in the font sheet not accounted for by cutoffs. Otherwise,
 * characters will be directly next to each other. For example, if one grid cell of the font sheet is 32 pixels by 32
 * pixels, and the character in a grid is 25 by 25 pixels wide, that leaves 7 pixels of space around the edges. The
 * cutoff for this character should be 3 or 4 to leave some space between characters after cutoff (as opposed to cutting
 * off all 7 pixels of space). All center points of characters must be equidistant from each other within an axis but
 * not across both. In other words, all grid cells must be the same width and same height but width doesn't need to
 * equal height
 */
public class Font {

    /**
     * Members
     */
    private Map<Character, Integer> charCutoffs; // map from ASCII characters to the amount of horizontal cutoff
    private Texture sheet;                       // the font sheet texture
    private int charsPerRow, charsPerCol;        // amount of characters per row and column of the font sheet
    private int stdCharCutoff;                   // standard cutoff for a character if none specified
    private char startingChar;                   // the first character of the font sheet (top left)

    /**
     * Constructs the font by creating a font sheet texture at the given path and then constructing the font information
     * from the given node. A font info node can have the following children:
     * <p>
     * - chars_per_row [required]: how many characters there are per row in the font sheet
     * <p>
     * - chars_per_col [required]: how many characters there are per column in the font sheet
     * <p>
     * - starting_char [required][0, 255]: the first character in the font sheet (at the top-left)
     * <p>
     * - char_cutoffs [optional][default: 0][0, INFINITY]: how much horizontal cutoff to apply to each character to
     * avoid empty space (on pixels). This child should then contain children for (1) each character with a unique
     * cutoff where the name is the character (except for colon (':') where the name should be 'colon' to avoid being
     * confused with a node-file separator, and the number symbol ('#') where the name should be 'number' to avoid being
     * confused with a node-file comment), and the value is the amount of pixels to cut off, and (2) a child named
     * 'standard' to define a standard cutoff for characters not otherwise listed
     *
     * @param sheetPath the path to the font sheet. Correctly formatted font sheets can start at any character, but they
     *                  may NOT skip characters, and the sheet must be a perfect grid where each cell is the same size
     * @param data      the node containing the information about the font
     */
    public Font(Utils.Path sheetPath, Node data) {

        /*
         * Load font information using node loader
         */
        this.sheet = new Texture(sheetPath); // load sheet
        Map<String, Object> font = NodeLoader.loadFromNode("Font", data, new NodeLoader.LoadItem[]{
                new NodeLoader.LoadItem<>("chars_per_row", null, Integer.class).makeRequired(),
                new NodeLoader.LoadItem<>("chars_per_col", null, Integer.class).makeRequired(),
                new NodeLoader.LoadItem<>("starting_char", null, Integer.class).makeRequired()
                        .setUpperBound(255),
                new NodeLoader.LoadItem<>("char_cutoffs", new Node("char_cutoffs"), Node.class)
        });

        /*
         * Apply loaded information
         */
        this.charsPerRow = (Integer) font.get("chars_per_row"); // save characters per row as member
        this.charsPerCol = (Integer) font.get("chars_per_col"); // save characters per column as member
        this.startingChar = (char) (int) font.get("starting_char"); // save starting character as member
        this.processCharCutoffs((Node) font.get("char_cutoffs")); // process cutoffs
    }

    /**
     * Processes the character cutoff map based on the given cutoff info
     *
     * @param cutOffInfo the node containing the cutoff info from the font info's node-file
     */
    private void processCharCutoffs(Node cutOffInfo) {
        this.charCutoffs = new HashMap<>(); // initialize map
        this.stdCharCutoff = 0; // unless a standard cutoff is found, the default with be 0
        for (Node c : cutOffInfo.getChildren()) { // for each child (each corresponding to a cutoff)
            String n = c.getName().toLowerCase(); // get the name
            if (n.equals("standard")) this.stdCharCutoff = Integer.parseInt(c.getValue()); // standard cutoff
                // colon gets special treatment because it's the node-file separator character
            else if (n.equals("colon")) this.charCutoffs.put(':', Integer.parseInt(c.getValue()));
                // number get special treatment because it's the node-file comment character
            else if (n.equals("number")) this.charCutoffs.put('#', Integer.parseInt(c.getValue()));
            else this.charCutoffs.put(c.getName().charAt(0),
                        Integer.parseInt(c.getValue())); // add to map normally for every other character
        }
    }

    /**
     * Calculates the texture coordinates of the given character using this font
     *
     * @param c      the character whose texture coordinates to calculate
     * @param cutoff whether or not to use the given character's horizontal cutoff value to make the character contain
     *               less empty space
     * @return the 2-dimensional texture coordinates (a length 8 array)
     */
    public float[] getCharTexCoords(char c, boolean cutoff) {
        if (c < this.startingChar) // if character is before the starting character
            Utils.handleException(new Exception("Invalid character '" + c + "' when starting character is '" +
                    this.startingChar + "'"), this.getClass(), "getCharTexCoords", true); // crash

        // make necessary row/column calculations
        int loc = (c - this.startingChar); // the location of the character in the font sheet (where loc 0 is top left)
        int row = loc / this.charsPerRow; // the row of the character in the font sheet
        int col = loc - (row * this.charsPerRow); // the column of the character in the font sheet
        float fracRow = 1 / (float) charsPerRow; // fraction of a row for a single character
        float fracCol = 1 / (float) charsPerCol; // fraction of a column for a single character

        // calculate texture coordinates
        float texCoords[] = new float[]{
                (float) col / charsPerRow, (float) row / charsPerCol, // top left
                (float) col / charsPerRow, (float) row / charsPerCol + fracCol, // bottom left
                (float) col / charsPerRow + fracRow, (float) row / charsPerCol + fracCol, // bottom right
                (float) col / charsPerRow + fracRow, (float) row / charsPerCol // top right
        };

        // account for cutoff then return
        if (cutoff) {
            float cutoffFactor = (float) getCharCutoff(c) / (float) this.sheet.getWidth(); // calculate how much to
            // cutoff
            texCoords[0] += cutoffFactor; // cut off top left
            texCoords[2] += cutoffFactor; // cut off bottom left
            texCoords[4] -= cutoffFactor; // cut off bottom right
            texCoords[6] -= cutoffFactor; // cut off top right
        }
        return texCoords; // return
    }

    /**
     * Finds the appropriate horizontal cutoff for the given character
     *
     * @param c the character whose cutoff to find
     * @return the given character's unique cutoff or the standard cutoff if the given char has no unique one
     */
    public int getCharCutoff(char c) {
        Integer cutoff = this.charCutoffs.get(c); // attempt to get
        if (cutoff == null) cutoff = this.stdCharCutoff; // if non-existent, use standard cutoff
        return cutoff; // return
    }

    /**
     * @return the width of a single character in the font sheet
     */
    public float getCharWidth() {
        return (float) this.sheet.getWidth() / (float) this.charsPerRow;
    }

    /**
     * @return the height of a single character in the font sheet
     */
    public float getCharHeight() {
        return (float) this.sheet.getHeight() / (float) this.charsPerCol;
    }

    /**
     * @return this font sheet
     */
    public Texture getSheet() {
        return this.sheet;
    }
}
