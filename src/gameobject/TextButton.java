package gameobject;

import graphics.Font;

/**
 * This extends TextObjects by implementing mouse interactivity and having three separate colors to denote different
 * levels of said interactivity - practically simulating a button
 */
public class TextButton extends TextObject implements MIHSB.MouseInteractable {

    /**
     * Static Data
     */
    private static final float[] DEFAULT_HOVER_COLOR = new float[]{0.5f, 0.5f, 0.5f, 1.0f}; // default hover color
    private static final float[] DEFAULT_PRESS_COLOR = new float[]{1.0f, 1.0f, 0.0f, 1.0f}; // default press colorS

    /**
     * Members
     */
    private final float[] defaultC, hoverC, pressC; // the colors for no interaction, hovering, and pressing
    private final int MIID;                         // the unique mouse interactable ID of the button

    /**
     * Constructs the text button with the given custom colors
     *
     * @param font     the font to use for the text
     * @param text     the text to display
     * @param defaultC the color to use when no interaction is occurring
     * @param hoverC   the color to use when the button is being hovered
     * @param pressC   the color to use when the mouse is pressed down over the button
     * @param MIID     the mouse interactable ID to assign the text button
     */
    public TextButton(Font font, String text, float[] defaultC, float[] hoverC, float[] pressC, int MIID) {
        super(font, text, defaultC); // call super
        this.defaultC = defaultC; // save default color as member
        this.hoverC = hoverC; // save hover color as member
        this.pressC = pressC; // save press color as member
        this.MIID = MIID; // save mouse interaction ID as member
    }

    /**
     * Constructs the text button with the default colors. The defaults for hover and press are defined above, while
     * the default for no interaction is the same default as a normal text object
     *
     * @param font the font to use for the text
     * @param text the text to display
     * @param MIID the mouse interactable ID to assign the text button
     */
    public TextButton(Font font, String text, int MIID) {
        super(font, text);
        this.defaultC = this.material.getColor(); // get default text color from the material (super will have set it)
        this.hoverC = DEFAULT_HOVER_COLOR; // use default hover color
        this.pressC = DEFAULT_PRESS_COLOR; // use default press color
        this.MIID = MIID;
    }

    /**
     * When a text button is hovered, it will change in color to the hover color
     *
     * @param x the x position of the mouse in either world or camera-view space, depending on whether the
     *          implementing object reacts to a camera
     * @param y the y position of the mouse in either world or camera-view space, depending on whether the
     *          implementing object reacts to a camera
     */
    @Override
    public void onHover(float x, float y) {
        this.material.setColor(hoverC); // change to hover color if button isn't also pressed
    }

    /**
     * When a text button is no longer hovered, it will change in color to the default color
     */
    @Override
    public void onDoneHovering() {
        this.material.setColor(this.defaultC); // change to default color
    }

    /**
     * When a text button is pressed, it will change in color to the pressed color
     */
    @Override
    public void onPress() {
        this.material.setColor(this.pressC); // change to pressed color
    }

    /**
     * When a text button is released, it will change in color back to the default color
     */
    @Override
    public void onRelease() {
        this.material.setColor(this.hoverC); // change to hover color (mouse hasn't left yet)
    }

    /**
     * The mouse interaction ID of a text button is the ID given to it when constructing
     *
     * @return the ID given when constructing
     */
    @Override
    public int getID() {
        return this.MIID; // return the ID
    }
}
