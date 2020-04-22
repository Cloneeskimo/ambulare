package gameobject;

import graphics.Material;
import graphics.MultiTexCoordModel;
import graphics.Texture;

/**
 * This extends GameObject to implement mouse interactivity and have three different texture animations for each
 * state of mouse interactivity (no interaction, hover, and press) - practically simulating a button
 */
public class TexturedButton extends GameObject implements MIHSB.MouseInteractable {

    /**
     * Data
     */
    private final int[] frameCounts; // the amount of frames in the texture for each state of mouse interactivity
    private final int MIID;          // the mouse interactivity id given in the constructor
    private int framesToUse = 1;     // which set of frames to use (which state of mouse interactivity the button is in)

    /**
     * Constructs the textured button with animation within any single mouse interactivity state
     * @param w the width of the button in grid cells
     * @param h the height of the button in grid cells
     * @param texResPath the resource-relative path to the texture. The texture should have its frames in the
     *                   following order: no interaction frames, then hover frames, then pressed frames
     * @param defaultFrames how many frames in the texture correspond to the no interaction state
     * @param hoverFrames how many frames in the texture correspond to the hover state
     * @param pressedFrames how many frames in the texture correspond to the pressed state
     * @param frameTime how much time (in seconds) to show a frame
     * @param MIID the mouse interactivity ID of the button
     */
    public TexturedButton(int w, int h, String texResPath, int defaultFrames, int hoverFrames, int pressedFrames,
                          float frameTime, int MIID) {
        super(MultiTexCoordModel.getStdMultiTexGridRect(w, h, defaultFrames + hoverFrames + pressedFrames),
                new Material(new Texture(texResPath))); // call super, creating necessary components
        /* here we check if there is more than one frame for any mouse interactivity state. If there isn't, there is no
           need to keep time for texture animation */
        if (defaultFrames + hoverFrames + pressedFrames > 3) { // if there is actually animation
            this.giveTexAnim(defaultFrames + hoverFrames + pressedFrames, frameTime); // start the animation
        }
        this.frameCounts = new int[] {defaultFrames, hoverFrames, pressedFrames}; // keep count of frames for each state
        this.framesToUse = 0; // start by using the no interactivity frames
        this.MIID = MIID; // save the mouse interactivity ID as a member
    }

    /**
     * Constructs the textured button without any animation within mouse interactivity states (the texture will still
     * change when the state changes however). This constructor basically assumes that the texture at the given path
     * has one frame for each mouse interactivity state
     * @param w the width of the button in grid cells
     * @param h the height of the button in grid cells
     * @param texResPath the resource-relative path to the texture. The texture should have its frames in the
     *                   following order: no interaction frame, then the hover frame, then the pressed frame
     * @param MIID the mouse interactivity ID of the button
     */
    public TexturedButton(int w, int h, String texResPath, int MIID) {
        this(w, h, texResPath, 1, 1, 1, 0, MIID); // call other constructor
    }

    /**
     * Updates the texture animation if there is one
     * @param interval the amount of time, in seconds, to account for
     */
    @Override
    protected void updateTexAnim(float interval) {
        this.frameTimeLeft -= interval; // update frame time left
        if (this.frameTimeLeft <= 0f) { // if frame is over
            this.frameTimeLeft += this.frameTime; // reset time
            this.frame++; // move to next frame
            if (this.frame >= this.frameCounts[framesToUse]) this.frame = 0; // return to first frame if reached end
            this.setAppropriateFrame(); // calculate the appropriate frame to tell the model to use
        }
    }

    /**
     * Calculates the appropriate frame to tell the multi-tex coordinate model to shift to, given the mouse
     * interactivity state of the button and how far along the animation it is (if there is an animation)
     */
    private void setAppropriateFrame() {
        this.frame = frame % this.frameCounts[framesToUse]; // make sure frame is within bounds of current MI state
        int mFrame = this.frame; // start with current frame
        if (this.framesToUse > 0) { // if we passed first MI state
            mFrame += this.frameCounts[0]; // we can skip its frames in the texture
            if (this.framesToUse > 1) mFrame += this.frameCounts[1]; // and if we passed second MI state, skip those too
        }
        ((MultiTexCoordModel)this.model).setFrame(mFrame); // tell the model the calculated frame
    }

    /**
     * Responds to mouse hovering by switching to the hover frame(s)
     * @param x the x position of the mouse in either world or camera-view space, depending on whether the
     *          implementing object reacts to a camera
     * @param y the y position of the mouse in either world or camera-view space, depending on whether the
     *          implementing object reacts to a camera
     */
    @Override
    public void onHover(float x, float y) {
        this.framesToUse = 1; // switch to hover frames
        this.setAppropriateFrame(); // calculate new appropriate frame for model
    }

    /**
     * Responds to hovering ending by switching to the no-interaction frame(s)
     */
    @Override
    public void onDoneHovering() {
        this.framesToUse = 0; // switch to no interaction frames
        this.setAppropriateFrame(); // calculate new appropriate frame for model
    }

    /**
     * Responds to a press by switching to the press frame(s)
     */
    @Override
    public void onPress() {
        this.framesToUse = 2; // switch to press frames
        this.setAppropriateFrame(); // calculate new appropriate frame for model
    }

    /**
     * Responds to a mouse release by switching to the hover frame(s)
     */
    @Override
    public void onRelease() {
        this.framesToUse = 1; // switch to hover frames
        this.setAppropriateFrame(); // calculate new appropriate frame for model
    }

    /**
     * The mouse interaction ID of a text button is the ID given to it when constructing
     * @return the ID given when constructing
     */
    @Override
    public int getID() {
        return this.MIID;
    }
}
