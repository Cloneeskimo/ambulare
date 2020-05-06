package graphics;

import utils.Node;
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
 * kind of information. See the constructor for information oh how to format font info node-files
 */
public class Font {

    /**
     * Members
     */
    private Map<Character, Integer> charCutoffs; // map from ASCII characters to the amount of horizontal cutoff
    private Texture sheet;                       // font sheet
    private int charsPerRow, charsPerCol;        // amount of characters per row and column of the font sheet
    private int stdCharCutoff;                   // standard cutoff for a character if none specified
    private char startingChar;                   // the first character of the font sheet (top left)

    /**
     * Constructs the area by compiling information from a given node about the font sheet texture at the given
     * resource-relative path. An font node can have the following children:
     * <p>
     * - chars_per_row [required]: how many characters there are per row in the font sheet
     * <p>
     * - chars_per_col [required]: how many characters there are per column in the font sheet
     * <p>
     * - starting_char [required]: the first character in the font sheet (at the top-left)
     * <p>
     * - char_cutoffs: how much horizontal cutoff to apply to each character to avoid empty space. This child should
     * then contain children for (1) each character with a unique cutoff where the name is the character (except for
     * colon (':') where the name should be 'colon' to avoid being confused with a node-file separator, and the number
     * symbol ('#') where the name should be 'number' to avoid being confused with a node-file comment), and the value
     * is the amount of pixels to cut off, and (2) a child named 'standard' to define a standard cutoff for characters
     * not otherwise listed
     * <p>
     * Note that, if any of the info above is improperly formatted, a message saying as much will be logged. As
     * such, when designing fonts to be loaded into the game, the logs should be checked often to make sure the
     * loading process is unfolding correctly
     *
     * @param sheetResPath the resource-relative path to the font sheet. Correctly formatted font sheets can start at
     *                     any character, but they may NOT skip characters, and the sheet must be a perfect grid where
     *                     each cell is the same size
     * @param info         the node containing the information about the font
     */
    public Font(String sheetResPath, Node info) {
        this.sheet = new Texture(sheetResPath, true); // load sheet
        try { // try to parse node-file infos
            this.charsPerRow = Integer.parseInt(info.getChild("chars_per_row").getValue()); // parse characters per row
            this.charsPerCol = Integer.parseInt(info.getChild("chars_per_col").getValue()); // parse characters per col
            this.startingChar = (char) Integer.parseInt(info.getChild("starting_char").getValue()); // parse start char
            this.processCharCutoffs(info.getChild("char_cutoffs")); // parse character cutoffs
        } catch (Exception e) { // if exception
            Utils.handleException(new Exception(Utils.getImproperFormatErrorLine("Font", "Font",
                    e.getMessage(), false)), "graphics.Font", "Font(String, String)", true); // crash
        }
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
                    this.startingChar + "'"), "Font", "getCharTexCoords(char, boolean)", true); // throw error

        // make necessary row/column calculations
        int loc = (c - this.startingChar); // the location of the character in the font sheet (where loc 0 is top left)
        int row = loc / this.charsPerRow; // the row of the character in the font sheet
        int col = loc - (row * this.charsPerRow); // the column of the character in the font sheet
        float fracRow = 1 / (float) charsPerRow; // fraction of a row for a single character
        float fracCol = 1 / (float) charsPerCol; // fraction of a column for a single character

        // calculate texture coordinates
        float texCoords[] = new float[]{
                (float) col / charsPerRow, (float) row / charsPerCol + fracCol, // top left
                (float) col / charsPerRow, (float) row / charsPerCol, // bottom left
                (float) col / charsPerRow + fracRow, (float) row / charsPerCol, // bottom right
                (float) col / charsPerRow + fracRow, (float) row / charsPerCol + fracCol // top right
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
