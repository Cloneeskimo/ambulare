package graphics;

import utils.Node;
import utils.Utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a font by having a font sheet texture containing all the characters of the font in ASCII order. Correctly
 * formatted font sheets can start at any character, but they may NOT skip characters, and the sheet must be a perfect
 * grid where each cell is the same size. Since some characters are skinnier than others, amounts (in pixels) to cut
 * off of each letter (horizontally and symmetrically) should be defined in the separate required info file
 */
public class Font {

    /**
     * Data
     */
    private Map<Character, Integer> charCutoffs; // map from ASCII characters to the amount of horizontal cutoff
    private Texture sheet; // font sheet
    private int charsPerRow, charsPerCol; // amount of characters per row and column of the font sheet
    private int stdCharCutoff; // standard cutoff for a character if none specified
    private char startingChar; // the first character of the font sheet (top left)

    /**
     * Constructs this Font
     * @param sheetResPath the resource-relative path to the font sheet
     * @param infoResPath the resource-relative path to the font info file. See font_info.txt for an example
     */
    public Font(String sheetResPath, String infoResPath) {
        this.sheet = new Texture(sheetResPath); // load sheet
        Node info = Node.resToNode(infoResPath); // load info
        try { // try to parse data
            this.charsPerRow = Integer.parseInt(info.getChild("chars_per_row").getValue()); // parse characters per row
            this.charsPerCol = Integer.parseInt(info.getChild("chars_per_col").getValue()); // parse characters per column
            this.startingChar = (char)Integer.parseInt(info.getChild("starting_char").getValue()); // parse starting character
            this.processCharCutoffs(info.getChild("char_cutoffs")); // parse character cutoffs
        } catch (Exception e) { // if exception
            Utils.handleException(e, "Font", "Font(String, String)", true); // handle exception
        }
    }

    /**
     * Processes the character cutoff map based on the given cutoff info
     * @param cutOffInfo the Node containing the cutoff info from this Font's info file
     */
    private void processCharCutoffs(Node cutOffInfo) {
        this.charCutoffs = new HashMap<>(); // initialize map
        this.stdCharCutoff = 0; // unless a standard cutoff is found, the default with be 0
        for (Node c : cutOffInfo.getChildren()) { // for each child (each corresponding to a cutoff)
            if (c.getName().toUpperCase().equals("STANDARD")) this.stdCharCutoff = Integer.parseInt(c.getValue()); // standard cutoff
            else if (c.getName().toUpperCase().equals("COLON")) this.charCutoffs.put(':', Integer.parseInt(c.getValue())); // colon gets special treatment due to importance in Node structure
            else this.charCutoffs.put(c.getName().charAt(0), Integer.parseInt(c.getValue())); // add to map regularly for every other character
        }
    }

    /**
     * Calculates the texture coordinates for this Font of the given character
     * @param c the character whose texture coordinates to calculate
     * @param cutoff whether or not to use the given character's horizontal cutoff value to make the character contain less empty space
     * @return the 2-dimensional texture coordinates (length 8 array)
     */
    public float[] getCharTexCoords(char c, boolean cutoff) {
        if (c < this.startingChar) // if character is before the starting character
            Utils.handleException(new Exception("Invalid character '" + c + "' when starting character is '" + this.startingChar + "'"),
                    "Font", "getCharTexCoords(char, boolean)", true); // throw an exception

        // make necessary row/column calculations
        int loc = (c - this.startingChar); // the location of the character in the font sheet (where loc = 0 is top left)
        int row = loc / this.charsPerRow; // the row of the character in the font sheet
        int col = loc - (row * this.charsPerRow); // the column of the character in the font sheet
        float fracRow = 1 / (float)charsPerRow; // fraction of a row for a single character
        float fracCol = 1 / (float)charsPerCol; // fraction of a column for a single character

        // calculate texture coordinates
        float texCoords[] = new float[] {
                (float)col / charsPerRow, (float)row / charsPerCol + fracCol, // top left
                (float)col / charsPerRow, (float)row / charsPerCol, // bottom left
                (float)col / charsPerRow + fracRow, (float)row / charsPerCol, // bottom right
                (float)col / charsPerRow + fracRow, (float)row / charsPerCol + fracCol // top right
        };

        // account for cutoff then return
        if (cutoff) {
            float cutoffFactor = (float)getCharCutoff(c) / (float)this.sheet.getWidth(); // calculate how much to cutoff
            texCoords[0] += cutoffFactor; // cut off top left
            texCoords[2] += cutoffFactor; // cut off bottom left
            texCoords[4] -= cutoffFactor; // cut off bottom right
            texCoords[6] -= cutoffFactor; // cut off top right
        }
        return texCoords; // return
    }

    /**
     * Finds the appropriate horizontal cutoff for the given character
     * @param c the character whose cutoff to find
     * @return the given character's unique cutoff or this Font's standard cutoff if the given char has no unique one
     */
    public int getCharCutoff(char c) {
        Integer cutoff = this.charCutoffs.get(c); // attempt to get
        if (cutoff == null) cutoff = this.stdCharCutoff; // if non-existent, use standard cutoff
        return cutoff; // return
    }

    /**
     * @return the width of a single character in this Font's font sheet
     */
    public float getCharWidth() { return (float)this.sheet.getWidth() / (float)this.charsPerRow; }

    /**
     * @return the height of a single character in this Font's font sheet
     */
    public float getCharHeight() { return (float)this.sheet.getHeight() / (float)this.charsPerCol; }

    /**
     * @return this Font's font sheet
     */
    public Texture getSheet() { return this.sheet; }
}
