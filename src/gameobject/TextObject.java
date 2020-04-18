package gameobject;

import graphics.Font;
import graphics.Material;
import graphics.Model;
import graphics.ShaderProgram;

/**
 * a GameObject designed to easily and accessibly display text
 */
public class TextObject extends GameObject {

    /**
     * Data
     */
    public static final float[] DEFAULT_COLOR = new float[] { 1.0f, 1.0f, 1.0f, 1.0f }; // the default color of a TextObject
    private String text; // the current text of this TextObject
    private Font font; // the Font used by this TextObject

    /**
     * Constructs this TextObject
     * @param font the Font to use when rendering text for this TextObject
     * @param text the text to display
     * @param color the color the text should be
     * @param x the x position of this TextObject
     * @param y the y position of this TextObject
     */
    public TextObject(Font font, String text, float[] color, float x, float y) {
        super(x, y, new Model(new float[]{}, new float[]{}, new int[]{}), // call super
                new Material(font.getSheet(), color, Material.BLEND_MODE.MULTIPLICATIVE));
        this.font = font; // save font
        this.setText(text); // set text
    }

    /**
     * Constructs this TextObject by playing it at x = y = 0f and assigning it the default color
     * @param font the Font to use when rendering text for this TextObject
     * @param text the text to display
     */
    public TextObject(Font font, String text) { this(font, text, TextObject.DEFAULT_COLOR, 0f, 0f); } // call other constructor

    /**
     * Refreshes this TextObject by recalculating its Model's model coordinates and texture coordinates
     * This is somewhat of a heavy-ish operation, so it should be avoided unless the text has actually changed
     * @param text the new text
     */
    private void refreshModel(String text) {

        // create relevant variables
        float[] modelCoords = new float[text.length() * 8]; // model coordinate array
        float[] texCoords = new float[text.length() * 8]; // texture coordinate array
        int[] idx = new int[text.length() * 6]; // indices array
        float charWidth = font.getCharWidth(); // get character width

        // preprocess to find widths and total width
        float[] widths = new float[text.length()]; // width for each character
        float width = 0; // total width
        for (int i = 0; i < text.length(); i++) { // for each character
            float cw =  charWidth - (float)(font.getCharCutoff(text.charAt(i)) * 2); // width of particular character
            float modelcw = (cw / charWidth * Model.STD_SQUARE_SIZE); // width in terms of standard square model size
            widths[i] = modelcw; // add character width
            width += modelcw; // add to cumulative width
        }

        // go through each character
        float x = -width / 2; // start on the left of the item and work our way to the right
        for (int i = 0; i < text.length(); i++) {
            int s = i * 8; // starting index for all assignments

            // get character and texture coordinates
            char c = text.charAt(i); // get character
            float[] tc = font.getCharTexCoords(c, true); // get texture coordinates for character
            for (int j = 0; j < tc.length; j++) texCoords[s + j] = tc[j]; // copy over texture coordinates

            // model coordinates x
            modelCoords[s] = modelCoords[s + 2] = x; // top left and bottom left x
            x += widths[i]; // increment x by character width
            modelCoords[s + 4] = modelCoords[s + 6] = x; // bottom right and top right x

            // model coordinates y
            modelCoords[s + 1] = modelCoords[s + 7] = (-Model.STD_SQUARE_SIZE / 2); // top left and top right y
            modelCoords[s + 3] = modelCoords[s + 5] = (Model.STD_SQUARE_SIZE / 2); // bottom left and bottom right y

            // indices
            s = i * 6;
            idx[s] = i * 4;
            idx[s + 1] = idx[s + 4] = i * 4 + 1;
            idx[s + 2] = idx[s + 3] = i * 4 + 3;
            idx[s + 5] = i * 4 + 2;
        }
        this.model = new Model(modelCoords, texCoords, idx); // create and set model
    }

    /**
     * Changes the text of this TextObject and refreshes the Model if the text is actually different
     * @param text the new text to set
     * @return whether the text was actually changed or not
     */
    public boolean setText(String text) {
        if (!text.equals(this.text)) { // check if text actually changed first since model refreshing is relatively heavy
            this.text = text; // set text
            this.refreshModel(text); // refresh model
            return true; // return that text was changed
        }
        return false; // if not, return not actually changed
    }

    /**
     * Appends text to the TextObject
     * @param text the text to append
     */
    public void appendText(String text) { this.setText(this.text + text); }

    /**
     * Removes a given amount of the last characters of this TextObject
     * @param n the amount of last characters to remove. If n is greater than this text's length, the text will become
     *          an empty String. If n is less than 1, nothing will happen
     */
    public void removeLastChars(int n) {
        if (n < 1) return; // ignore negative or zero values
        if (n >= this.text.length()) this.setText(""); // remove all text if n >= text length
        this.setText(this.text.substring(0, this.text.length() - n)); // remove n characters
    }

    /**
     * Renders this TextObject if there is text to display
     * @param sp the ShaderProgram to use to render this TextObject
     */
    @Override
    public void render(ShaderProgram sp) {
        if (!this.text.equals("")) super.render(sp); // only render if there is text to render
    }
}
