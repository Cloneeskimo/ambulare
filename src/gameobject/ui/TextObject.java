package gameobject.ui;

import gameobject.GameObject;
import graphics.*;
import utils.Global;

/*
 * TextObject.java
 * Ambulare
 * Jacob Oaks
 * 4/19/20
 */

/**
 * A game object designed to easily and efficiently display text
 */
public class TextObject extends GameObject {

    /**
     * Static Data
     */
    public static final float DEFAULT_SIZE = 0.1f; /* defines the default text size in normalized coordinates. That
        is, text will, by default, be 0.1f tall and however wide necessary to accommodate all characters */
    private static final float[] DEFAULT_COLOR = Global.getThemeColor(Global.ThemeColor.WHITE); // default is white

    /**
     * Members
     */
    private String text;       // the current text
    protected final Font font; // the font used to display the text

    /**
     * Constructs the text object with the given color
     *
     * @param font  the font to use when rendering the text
     * @param text  the starting text to display
     * @param color the color the text should be
     */
    public TextObject(Font font, String text, float[] color) {
        // call super using a material that has the font sheet as a texture, and blend with the given color
        super(0f, 0f, new Model(new float[]{}, new float[]{}, new int[]{}),
                new Material(font.getSheet(), color, Material.BlendMode.MULTIPLICATIVE));
        this.font = font; // save font as member
        this.setText(text); // set the text to the given starting text
    }

    /**
     * Constructs the text object with the default color
     *
     * @param font the font to use when rendering the text
     * @param text the text to display
     */
    public TextObject(Font font, String text) {
        // call other constructor with the default text color
        this(font, text, new float[]{DEFAULT_COLOR[0], DEFAULT_COLOR[1], DEFAULT_COLOR[2], DEFAULT_COLOR[3]});
    }

    /**
     * Will convert a text object into a game object with a normal quad model and a regular texture. For text objects
     * whose text will not be changing often, this is a good idea to call. Note that the resulting game object will
     * not be able to change its text, but it will be much more efficiently rendered
     *
     * @return a game object representing the text object without changeable text
     */
    public GameObject solidify() {
        // create the texture with the text pre-rendered
        Texture t = Texture.makeSheet(this.material, this.model, 1, 1, this.getPixelWidth(),
                (int)font.getCharHeight(), 0, false); // make a texture with the text
        GameObject r = new GameObject(new Model(t.getModelCoords(font.getCharHeight() / DEFAULT_SIZE),
                Model.getStdRectTexCoords(),  Model.getStdRectIdx()), new Material(t)); // create a new game object with the texture
        r.setScale(this.model.getXScale(), this.model.getYScale()); // scale the game object to text object's scale
        return r; // return final game object
    }

    /**
     * @return the wwidth of the text object in pixels, as if it were a solidifed texture
     */
    protected int getPixelWidth() {
        int w = 0; // int to store total width of texture
        for (int i = 0; i < this.text.length(); i++) // for each character in the text object
            w += (font.getCharWidth() - font.getCharCutoff(text.charAt(i)) * 2); // add its width to total width
        return w; // return final pixel width
    }

    /**
     * Refreshes this TextObject by recalculating its model coordinates and texture coordinates
     * This is somewhat of a heavy-ish operation, so it should be avoided unless the text has actually changed
     *
     * @param text the new text
     */
    private void refreshModel(String text) {

        // create relevant variables
        float[] modelCoords = new float[text.length() * 8]; // model coordinate array
        float[] texCoords = new float[text.length() * 8]; // texture coordinate array
        int[] idx = new int[text.length() * 6]; // indices array
        float charWidth = font.getCharWidth(); // get character width from the font
        float charHeight = font.getCharHeight(); // get character height from the font
        float charAr = charWidth / charHeight; // calculate aspect ratio of a character from the font

        // preprocess to find widths and total width
        float[] widths = new float[text.length()]; // width for each character
        float width = 0; // total width
        for (int i = 0; i < text.length(); i++) { // for each character
            float cw = charWidth - (float) (font.getCharCutoff(text.charAt(i)) * 2); // width of particular character
            float modelcw = (cw / charWidth * DEFAULT_SIZE) * charAr; // width in terms of standard square model size
            widths[i] = modelcw; // add character width
            width += modelcw; // add to cumulative width
        }

        // go through each character
        float x = -width / 2; // start on the left of the item and work our way to the right
        for (int i = 0; i < text.length(); i++) { // for each character in the text
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
            modelCoords[s + 1] = modelCoords[s + 7] = (DEFAULT_SIZE / 2); // top left and top right y
            modelCoords[s + 3] = modelCoords[s + 5] = (-DEFAULT_SIZE / 2); // bottom left and bottom right y

            // indices
            s = i * 6;
            idx[s] = i * 4;
            idx[s + 1] = idx[s + 4] = i * 4 + 1;
            idx[s + 2] = idx[s + 3] = i * 4 + 3;
            idx[s + 5] = i * 4 + 2;
        }
        float sx = this.model.getXScale(), sy = this.model.getYScale(); // get scaling factor of previous model
        this.model = new Model(modelCoords, texCoords, idx); // create and set new model
        this.setScale(sx, sy); // re-apply scale
    }

    /**
     * Changes the text of this text object and refreshes the model if the text is actually different
     *
     * @param text the new text to set
     * @return whether the text was actually changed or not
     */
    public boolean setText(String text) {
        if (!text.equals(this.text)) { // check if text actually changed since model refreshing is relatively heavy
            this.text = text; // set text
            this.refreshModel(text); // refresh model
            return true; // return that text was changed
        }
        return false; // if not, return not actually changed
    }

    /**
     * Appends text to the text object
     *
     * @param text the text to append
     */
    public void appendText(String text) {
        this.setText(this.text + text);
    }

    /**
     * Removes a given amount of the last characters
     *
     * @param n the amount of last characters to remove. If n is greater than this text's length, the text will become
     *          an empty string. If n is less than 1, nothing will happen
     */
    public void removeLastChars(int n) {
        if (n < 1) return; // ignore negative or zero values
        if (n >= this.text.length()) this.setText(""); // remove all text if n >= text length
        else this.setText(this.text.substring(0, this.text.length() - n)); // remove n characters
    }

    /**
     * Renders the text object if there is text to display
     *
     * @param sp the shader program to use to render
     */
    @Override
    public void render(ShaderProgram sp) {
        if (!this.text.equals("")) super.render(sp); // only render if there is actually text
    }

    /**
     * Updates the opacity of the text object
     *
     * @param opacity the new opacity from 0f to 1f
     */
    public void setOpacity(float opacity) {
        this.material.getColor()[3] = opacity;
    }

    /**
     * @return the text object's text
     */
    public String getText() {
        return this.text;
    }
}
