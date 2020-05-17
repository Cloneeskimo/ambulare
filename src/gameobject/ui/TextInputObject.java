package gameobject.ui;

import graphics.Font;
import utils.Global;

import static org.lwjgl.glfw.GLFW.*;

/*
 * TextInputObject.java
 * Ambulare
 * Jacob Oaks
 * 5/5/2020
 */

/**
 * Extends text objects by allowing for configurable text input. An enter callback can be provided to be called whenever
 * the user presses enter (signifying they have finished a task), assuming that keyboard input is properly forwarded to
 * the text input object. A minimum length can be provided to specify the minimum length for said callback to be invoked
 * and a maximum length can be provided past which the text cannot lengthen. Default text and a default text color can
 * be provided for when the input field is empty. Most input (except for the backspace and enter keys) are automatically
 * gathered through setting a character callback with the window. Other settings specifying the allowed input can be
 * changed, as detailed in the members
 */
public class TextInputObject extends TextObject {

    /**
     * Members
     */
    private float[] defaultColor;             // the color to make the text when the input is empty
    private float[] inputColor;               // the color to make the text when the input is not empty
    private String defaultText;               // the text to display when the input is empty
    private EnterCallback ec;                 // a callback to invoke when enter is pressed and the input is long enough
    private int maxLength = -1;               // the maximum length the input can grow the be. If -1, no max is enforced
    private int minLength = -1;               // the minimum length for an enter callback to be called
    private boolean acceptInput = true;       // whether input is currently being accepted
    private boolean acceptNonLetters = false; // whether non-letter characters should be accepted
    private boolean onlyFirstCapital = true;  // whether capitalization is strictly enforced to only the first character
    private boolean empty = true;

    /**
     * Constructor
     *
     * @param font         the font to use for the text
     * @param defaultText  the text to display when the input is empty
     * @param defaultColor the color to use when the input is empty
     * @param inputColor   the color to use when the input is not empty
     */
    public TextInputObject(Font font, String defaultText, float[] defaultColor, float[] inputColor) {
        super(font, defaultText, defaultColor); // call text object's constructor
        // register a character inoput callback with the game window
        glfwSetCharCallback(Global.gameWindow.getHandle(), (w, c) -> {
            this.parseChar((char) c); // parse character input when it is received
        });
        this.defaultColor = defaultColor; // save the default color as member
        this.defaultText = defaultText; // save the default text as member
        this.inputColor = inputColor; // save the input color as member
    }

    /**
     * Responds to keyboard input events. This should be called by outside scopes whenever there is a keyboard event
     * otherwise backspace will not work and enter callbacks will not be invoked
     *
     * @param key    the key in question
     * @param action the action of the key
     */
    public void keyboardInput(int key, int action) {
        if (this.acceptInput) { // if the text input object is currently accepting input
            // if backspace was released and the text input object is not empty
            if (key == GLFW_KEY_BACKSPACE && action == GLFW_RELEASE && !this.empty) {
                if (this.getText().length() < 2) { // if there is only one character in the input
                    this.empty = true; // flag that the text input object is now empty
                    this.setText(this.defaultText); // display the default text
                    this.material.setColor(this.defaultColor); // and use the default color
                } else this.removeLastChars(1); // otherwise just remove the last character
            } else if (key == GLFW_KEY_ENTER && action == GLFW_RELEASE) { // if enter was released
                if (this.ec != null) { // if the text input object has an enter callback
                    // if the input is not long enough, return
                    if (this.minLength >= 0 && (this.getText().length() < this.minLength || this.empty)) return;
                    this.ec.callback(); // otherwise invoke the enter callback
                }
            }
        }
    }

    /**
     * Parses a character input and determines whether or not to append it to the input based on the text input object's
     * settings and length
     *
     * @param c the character that was pressed
     */
    private void parseChar(Character c) {
        if (this.acceptInput) { // only append if the text input object is currently accepting input
            // if the input is already at the maximum length, refuse the input
            if (this.maxLength >= 0 && !this.empty && this.getText().length() >= this.maxLength) return;
            boolean letter = ((c > 96 && c < 123) || (c > 64 && c < 91)); // determine whether the input was a letter
            // if only letters are accepted and the input was not a letter, refuse the input
            if (!this.acceptNonLetters && !letter) return;
            if (this.onlyFirstCapital && letter) { // if strict capitalization is being enforced
                if (this.empty) c = c.toString().toUpperCase().charAt(0); // capitalize if input is the first character
                else c = c.toString().toLowerCase().charAt(0); // or make lowercase otherewise
            }
            this.appendChar(c); // append the character
        }
    }

    /**
     * Appends the given character to the input text object
     *
     * @param c the object to append
     */
    private void appendChar(Character c) {
        if (this.empty) { // if this is empty
            this.setText(c.toString()); // remove the default text and use the single character
            this.material.setColor(this.inputColor); // set to color to the input color
            this.empty = false; // flag that the input is no longer empty
        } else this.appendText(c.toString()); // otherwise just append like normal
    }

    /**
     * Sets the given enter callback to be invoked when enter is pressed by the user and the input is long enough
     *
     * @param ec the enter callback to call when input is valid
     */
    public void setEnterCallback(EnterCallback ec) {
        this.ec = ec; // save enter callback as member
    }

    /**
     * Sets the maximum length of the input
     *
     * @param maxLength the maximum length
     */
    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength; // save new maximum length as member
    }

    /**
     * Sets the minimum length of the input
     *
     * @param minLength the minimum length
     */
    public void setMinLength(int minLength) {
        this.minLength = minLength; // save new minimum length as member
    }

    /**
     * Tells the text input object whether or not it should currently be accepting input
     *
     * @param acceptInput whether to accept input
     */
    public void setAcceptInput(boolean acceptInput) {
        this.acceptInput = acceptInput; // save flag as member
    }

    /**
     * Tells the text input object whether or not it should accept non-letter characters as input
     *
     * @param acceptNonLetters whether to accept non-letters
     */
    public void setAcceptNonLetters(boolean acceptNonLetters) {
        this.acceptNonLetters = acceptNonLetters; // save flag as member
    }

    /**
     * Tells the text input object whether or not to enforce strict capitalization where the first character is
     * capitalized and the rest are lowercase
     *
     * @param onlyFirstCapital whether to enforce strict capitalization
     */
    public void setOnlyFirstCapital(boolean onlyFirstCapital) {
        this.onlyFirstCapital = onlyFirstCapital; // save flag as member
    }

    /**
     * Defines a callback to be invoked when enter is pressed and the input of the text input object is long enough
     */
    @FunctionalInterface
    public interface EnterCallback {
        void callback(); // will be called when the described circumstances occur
    }
}