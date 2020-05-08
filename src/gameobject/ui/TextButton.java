package gameobject.ui;

import graphics.Font;
import utils.Global;
import utils.MouseInputEngine;

/*
 * TextButton.java
 * Ambulare
 * Jacob Oaks
 * 4/21/2020
 */

/**
 * This extends TextObjects by implementing mouse interaction and having three separate colors to denote different
 * levels of mouse interaction - practically simulating a button
 */
public class TextButton extends TextObject implements MouseInputEngine.MouseInteractive {

    /**
     * Static Data
     */
    private static final float[] DEFAULT_NORMAL_COLOR = Global.getThemeColor(Global.ThemeColor.GRAY); // default normal
    private static final float[] DEFAULT_HOVER_COLOR = Global.getThemeColor(Global.ThemeColor.WHITE); // default hover
    private static final float[] DEFAULT_PRESS_COLOR = Global.getThemeColor(Global.ThemeColor.GREEN); // default press

    /**
     * Members
     */
    private final float[] defaultC, hoverC, pressC;     // the colors for no interaction, hovering, and pressing
    private final MouseInputEngine.MouseCallback[] mcs; // array for mouse callbacks

    /**
     * Constructs the text button with the given custom colors
     *
     * @param font     the font to use for the text
     * @param text     the text to display
     * @param defaultC the color to use when no interaction is occurring
     * @param hoverC   the color to use when the button is being hovered
     * @param pressC   the color to use when the mouse is pressed down over the button
     */
    public TextButton(Font font, String text, float[] defaultC, float[] hoverC, float[] pressC) {
        super(font, text, defaultC); // call super
        this.defaultC = defaultC; // save default color as member
        this.hoverC = hoverC; // save hover color as member
        this.pressC = pressC; // save press color as members
        this.mcs = new MouseInputEngine.MouseCallback[4]; // create array for callbacks
    }

    /**
     * Constructs the text button with the default colors. The defaults for hover and press are defined above, while
     * the default for no interaction is the same default as a normal text object
     *
     * @param font the font to use for the text
     * @param text the text to display
     */
    public TextButton(Font font, String text) {
        this(font, text, // call other constructor using the given font and text
                new float[]{DEFAULT_NORMAL_COLOR[0], DEFAULT_NORMAL_COLOR[1], DEFAULT_NORMAL_COLOR[2],
                        DEFAULT_NORMAL_COLOR[3]}, // with the default normal color
                new float[]{DEFAULT_HOVER_COLOR[0], DEFAULT_HOVER_COLOR[1], DEFAULT_HOVER_COLOR[2],
                    DEFAULT_HOVER_COLOR[3]}, // the default hover color
                new float[]{DEFAULT_PRESS_COLOR[0], DEFAULT_PRESS_COLOR[1], DEFAULT_PRESS_COLOR[2],
                    DEFAULT_PRESS_COLOR[3]} // and the default press color
        );
    }

    /**
     * Saves the given mouse callback to be called when the given kind of input occurs
     *
     * @param type the mouse input type to give a callback for
     * @param mc   the callback
     */
    @Override
    public void giveCallback(MouseInputEngine.MouseInputType type, MouseInputEngine.MouseCallback mc) {
        MouseInputEngine.MouseInteractive.saveCallback(type, mc, this.mcs); // save callback
    }

    /**
     * Responds to mouse interaction by invoking any corresponding callbacks and updating the text colors
     *
     * @param type the type of mouse input that occurred
     * @param x    the x position of the mouse in world coordinate or camera-view coordinates, depending on the mouse
     *             input engine's camera usage flag for this particular implementing object
     * @param y    the y position of the mouse in world coordinate or camera-view coordinates, depending on the mouse
     */
    @Override
    public void mouseInteraction(MouseInputEngine.MouseInputType type, float x, float y) {
        switch (type) { // switch on type of input
            case HOVER: // on hover
            case RELEASE: // or release
                this.material.setColor(this.hoverC); // change to hover color (mouse hasn't left yet)
                break;
            case DONE_HOVERING: // on done hovering
                this.material.setColor(this.defaultC); // change to default color
                break;
            case PRESS: // on press
                this.material.setColor(this.pressC); // change to pressed color
                break;
        }
        MouseInputEngine.MouseInteractive.invokeCallback(type, this.mcs, x, y); // invoke callback
    }

    /**
     * Updates the opacity of the text button
     * @param opacity the new opacity from 0f to 1f
     */
    @Override
    public void setOpacity(float opacity) {
        super.setOpacity(opacity); // update current color
        this.defaultC[3] = opacity; // update default color
        this.hoverC[3] = opacity; // update hover color
        this.pressC[3] = opacity; // update press colors
    }
}
